package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestLog {

	private Logger logger;
	private TGLogRecorder recorder;
	private TGLog log;

	@BeforeEach
	public void setUp() {
		this.logger = Logger.getLogger("test.log." + System.nanoTime());
		this.logger.setUseParentHandlers(false);
		this.recorder = new TGLogRecorder();
		this.logger.addHandler(this.recorder);
		this.log = new TGLog(this.logger);
	}

	@AfterEach
	public void tearDown() {
		this.logger.removeHandler(this.recorder);
	}

	private void logAtEachLevel() {
		this.log.error("an error", new IllegalStateException("broken"));
		this.log.warn("a warning");
		this.log.warn("a warning with its cause", new IllegalArgumentException("bad"));
		this.log.info("some news");
		this.log.debug("a detail");
		this.log.trace("a step");
	}

	@Test
	public void atWarnOnlyErrorsAndWarningsAreLogged() {
		this.logger.setLevel(TGLogLevel.WARN.toJul());
		this.logAtEachLevel();

		assertEquals(Arrays.asList("ERROR an error (java.lang.IllegalStateException: broken)", "WARN a warning",
				"WARN a warning with its cause (java.lang.IllegalArgumentException: bad)"), this.recorder.recorded());
	}

	@Test
	public void atTraceEverythingIsLogged() {
		this.logger.setLevel(TGLogLevel.TRACE.toJul());
		this.logAtEachLevel();

		assertEquals(Arrays.asList("ERROR an error (java.lang.IllegalStateException: broken)", "WARN a warning",
				"WARN a warning with its cause (java.lang.IllegalArgumentException: bad)", "INFO some news",
				"DEBUG a detail", "TRACE a step"), this.recorder.recorded());
	}

	@Test
	public void theLogSaysWhichLevelsItShows() {
		this.logger.setLevel(TGLogLevel.DEBUG.toJul());

		assertTrue(this.log.isEnabled(TGLogLevel.INFO));
		assertTrue(this.log.isDebugEnabled());
		assertFalse(this.log.isTraceEnabled());
	}

	@Test
	public void aRecordKeepsTheNameOfTheThreadThatLoggedIt() throws InterruptedException {
		this.logger.setLevel(TGLogLevel.INFO.toJul());
		Thread thread = new Thread(() -> this.log.info("from elsewhere"), "audio-callback");
		thread.start();
		thread.join();

		assertTrue(this.recorder.text().contains("INFO  [audio-callback] "), this.recorder.text());
	}

	@Test
	public void aClassOrNamedLogBelongsToTuxGuitarsLog() {
		Logger root = Logger.getLogger(TGLogSetup.ROOT_LOGGER);
		TGLogSetup.getInstance();
		java.util.logging.Level before = root.getLevel();
		TGLogRecorder recorder = TGLogRecorder.attach(TGLogLevel.INFO);
		try {
			TGLog.get(TestLog.class).info("from a class");
			TGLog.named("console.err").warn("from the console");
			assertEquals(java.util.logging.Level.INFO, root.getLevel());
		} finally {
			recorder.detach();
		}

		assertTrue(recorder.text().contains("] util.log.TestLog - from a class"), recorder.text());
		assertTrue(recorder.text().contains("] console.err - from the console"), recorder.text());
		assertEquals(before, root.getLevel());
	}

	@Test
	public void aLevelThatIsOffAllocatesNothing() {
		this.logger.setLevel(TGLogLevel.WARN.toJul());
		this.guardedLogging(1000);

		long allocated = this.allocatedBy(10000);

		assertTrue(allocated < 1024, allocated + " bytes allocated");
		assertTrue(this.recorder.recorded().isEmpty());
	}

	@Test
	public void theSameLoggingAtALevelThatIsOnAllocates() {
		this.logger.setLevel(TGLogLevel.TRACE.toJul());

		long allocated = this.allocatedBy(10000);

		assertTrue(allocated > 100 * 1024, allocated + " bytes allocated");
		assertEquals(20000, this.recorder.recorded().size());
	}

	private long allocatedBy(int count) {
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		long thread = Thread.currentThread().getId();
		long before = threads.getThreadAllocatedBytes(thread);
		this.guardedLogging(count);
		return threads.getThreadAllocatedBytes(thread) - before;
	}

	// the way the audio thread logs: a level check before any message is built
	private void guardedLogging(int count) {
		for (int i = 0; i < count; i++) {
			if (this.log.isTraceEnabled()) {
				this.log.trace("block " + i);
			}
			this.log.debug("a constant message");
		}
	}
}
