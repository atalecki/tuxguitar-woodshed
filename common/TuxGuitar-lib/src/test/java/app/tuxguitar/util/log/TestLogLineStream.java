package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestLogLineStream {

	private final ByteArrayOutputStream console = new ByteArrayOutputStream();
	private Logger logger;
	private TGLogRecorder recorder;
	private TGLogLineStream stream;

	@BeforeEach
	public void setUp() {
		this.logger = Logger.getLogger("test.lines." + System.nanoTime());
		this.logger.setUseParentHandlers(false);
		this.logger.setLevel(TGLogLevel.TRACE.toJul());
		this.recorder = new TGLogRecorder();
		this.logger.addHandler(this.recorder);
		this.stream = new TGLogLineStream(this.console, new TGLog(this.logger), TGLogLevel.WARN, StandardCharsets.UTF_8);
	}

	@AfterEach
	public void tearDown() {
		this.logger.removeHandler(this.recorder);
	}

	@Test
	public void eachLineGoesToTheConsoleAsItIsAndToTheLogAtTheStreamsLevel() throws IOException {
		PrintStream printed = new PrintStream(this.stream, true, "UTF-8");
		printed.print("hello\r\nwörld\n");
		printed.write('!');
		printed.println();

		assertEquals("hello\r\nwörld\n!" + System.lineSeparator(), new String(this.console.toByteArray(), StandardCharsets.UTF_8));
		assertEquals(Arrays.asList("WARN hello", "WARN wörld", "WARN !"), this.recorder.recorded());
	}

	@Test
	public void aLineIsLoggedOnceItEndsOrTheStreamCloses() throws IOException {
		this.stream.write("abc".getBytes(StandardCharsets.UTF_8));
		assertEquals(Collections.emptyList(), this.recorder.recorded());

		this.stream.write("def\nghi".getBytes(StandardCharsets.UTF_8));
		this.stream.flush();
		assertEquals(Arrays.asList("WARN abcdef"), this.recorder.recorded());

		this.stream.close();
		assertEquals(Arrays.asList("WARN abcdef", "WARN ghi"), this.recorder.recorded());
	}

	@Test
	public void whatTheLogItselfPrintsGoesOnlyToTheConsole() throws IOException {
		final TGLogLineStream stream = this.stream;
		this.logger.addHandler(new Handler() {
			public void publish(LogRecord record) {
				try {
					stream.write(("echo of " + record.getMessage() + "\n").getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}

			public void flush() {
			}

			public void close() {
			}
		});
		stream.write("first\n".getBytes(StandardCharsets.UTF_8));

		assertEquals(Arrays.asList("WARN first"), this.recorder.recorded());
		assertEquals("first\necho of first\n", new String(this.console.toByteArray(), StandardCharsets.UTF_8));
	}
}
