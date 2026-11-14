package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.tuxguitar.util.TGVersion;

public class TestLogSetup {

	@TempDir
	Path folder;

	private final Logger logger = Logger.getLogger("test.setup." + System.nanoTime());
	private TGLogSetup setup;

	@AfterEach
	public void tearDown() {
		if (this.setup != null) {
			this.setup.closeFile();
		}
	}

	private TGLogSetup setup(String commandLine) {
		this.setup = new TGLogSetup(this.logger, commandLine);
		return this.setup;
	}

	private String file(String name) throws IOException {
		return new String(Files.readAllBytes(this.folder.resolve(name)), StandardCharsets.UTF_8);
	}

	@Test
	public void theLoggersLevelFollowsTheChoice() {
		TGLogSetup setup = setup(null);
		assertEquals(Level.WARNING, this.logger.getLevel());

		setup.setSettingsLevel(TGLogLevel.INFO);
		assertEquals(Level.INFO, this.logger.getLevel());

		setup.setDebugHelper(true);
		assertEquals(Level.FINE, this.logger.getLevel());
		assertEquals("DEBUG (debug helper plugin)", setup.getChoice().toString());

		setup.setDebugHelper(false);
		assertEquals(Level.INFO, this.logger.getLevel());
	}

	@Test
	public void theCommandLineLevelHoldsWhateverTheSettingsSay() {
		TGLogSetup setup = setup("trace");
		setup.setSettingsLevel(TGLogLevel.ERROR);

		assertEquals(Level.FINEST, this.logger.getLevel());
	}

	@Test
	public void theFileStartsWithTheVersionsTheLevelAndItsPathThenTheMessagesTheLevelShows() throws IOException {
		TGLogSetup setup = setup(null);
		File current = setup.openFile(this.folder.toFile());
		TGLog log = new TGLog(this.logger);
		log.warn("a warning");
		log.info("some news");

		String text = file("tuxguitar-0.log");
		assertEquals(this.folder.resolve("tuxguitar-0.log").toFile().getAbsolutePath(), current.getAbsolutePath());
		assertTrue(text.contains(" INFO  [" + Thread.currentThread().getName() + "] " + this.logger.getName() + " - TuxGuitar "
				+ TGVersion.CURRENT.getVersion() + " started; Java " + System.getProperty("java.version") + " ("), text);
		assertTrue(text.contains("; " + System.getProperty("os.name") + " " + System.getProperty("os.version")), text);
		assertTrue(text.contains(" - log level WARN (default)" + System.lineSeparator()), text);
		assertTrue(text.contains(" - log file " + current.getAbsolutePath() + " (5 files of 5120 KB)"), text);
		assertTrue(text.contains(" WARN  [") && text.contains(" - a warning"), text);
		assertFalse(text.contains("some news"), text);
	}

	@Test
	public void aLevelChangeIsWrittenEvenWhenTheNewLevelHidesNews() throws IOException {
		TGLogSetup setup = setup(null);
		setup.openFile(this.folder.toFile());
		setup.setSettingsLevel(TGLogLevel.ERROR);
		setup.setSettingsLevel(TGLogLevel.ERROR);

		String text = file("tuxguitar-0.log");
		assertTrue(text.contains(" - log level ERROR (settings)"), text);
		assertEquals(text.indexOf("log level ERROR"), text.lastIndexOf("log level ERROR"), text);
	}

	@Test
	public void anUnknownCommandLineLevelIsWrittenAndIgnored() throws IOException {
		TGLogSetup setup = setup("loud");
		setup.openFile(this.folder.toFile());

		String text = file("tuxguitar-0.log");
		assertEquals(Level.WARNING, this.logger.getLevel());
		assertTrue(text.contains(" - unknown log level \"loud\" in -Dtuxguitar.log.level, ignored"), text);
	}

	@Test
	public void aNewSessionWritesAfterThePreviousOne() throws IOException {
		TGLogSetup setup = setup(null);
		TGLog log = new TGLog(this.logger);
		setup.openFile(this.folder.toFile());
		log.warn("first session");
		setup.closeFile();
		log.warn("between sessions");
		setup.openFile(this.folder.toFile());
		log.warn("second session");

		String text = file("tuxguitar-0.log");
		assertTrue(text.indexOf("first session") < text.indexOf("second session"), text);
		assertFalse(text.contains("between sessions"), text);
	}

	@Test
	public void aFullFileMovesUpAndTheOldestIsDropped() throws IOException {
		TGLogSetup setup = setup(null);
		TGLog log = new TGLog(this.logger);
		setup.openFile(this.folder.toFile(), 2000, 3);
		for (int i = 0; i < 200; i++) {
			log.warn("message " + i + " ......................................................................");
		}

		assertTrue(file("tuxguitar-0.log").contains("message 199 "));
		assertTrue(this.folder.resolve("tuxguitar-1.log").toFile().length() > 0);
		assertTrue(this.folder.resolve("tuxguitar-2.log").toFile().length() > 0);
		assertFalse(this.folder.resolve("tuxguitar-3.log").toFile().exists());
		assertFalse(file("tuxguitar-2.log").contains("message 0 "));
		assertTrue(this.folder.resolve("tuxguitar-1.log").toFile().length() < 2000 + 400);
	}
}
