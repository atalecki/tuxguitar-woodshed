package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

public class TestLogFormatter {

	private static final String NL = System.lineSeparator();
	private static final Instant TIME = Instant.parse("2026-11-14T09:05:03.042Z");

	private final TGLogFormatter formatter = new TGLogFormatter(ZoneOffset.ofHours(1));

	private TGLogRecord record(TGLogLevel level, String logger, Throwable thrown) {
		TGLogRecord record = new TGLogRecord(level, "the host stopped", thrown, logger);
		record.setInstant(TIME);
		return record;
	}

	@Test
	public void aLineHasTheTimeWithMillisTheLevelTheThreadTheShortLoggerNameAndTheMessage() {
		String line = this.formatter.format(record(TGLogLevel.WARN, "app.tuxguitar.vst3.TGVst3Host", null));

		assertEquals("2026-11-14T10:05:03.042+01:00 WARN  [" + Thread.currentThread().getName() + "] vst3.TGVst3Host - the host stopped" + NL, line);
	}

	@Test
	public void aLoggerOutsideTuxGuitarKeepsItsFullName() {
		String line = this.formatter.format(record(TGLogLevel.DEBUG, "org.example.Thing", null));

		assertTrue(line.contains(" DEBUG [") && line.contains("] org.example.Thing - "), line);
	}

	@Test
	public void anErrorIsFollowedByItsStackTrace() {
		String text = this.formatter.format(record(TGLogLevel.ERROR, "app.tuxguitar.Main", new IllegalStateException("boom", new RuntimeException("cause"))));

		assertTrue(text.startsWith("2026-11-14T10:05:03.042+01:00 ERROR ["), text);
		assertTrue(text.contains(" - the host stopped" + NL + "java.lang.IllegalStateException: boom" + NL + "\tat app.tuxguitar.util.log.TestLogFormatter."), text);
		assertTrue(text.contains("Caused by: java.lang.RuntimeException: cause"), text);
	}

	// the message as a formatter for that system writes it, without the line's start
	private static String written(String os, String home, String message, Throwable thrown) {
		Properties system = new Properties();
		system.setProperty("os.name", os);
		system.setProperty("user.home", home);
		TGLogRecord record = new TGLogRecord(TGLogLevel.WARN, message, thrown, "app.tuxguitar.Main");
		String text = new TGLogFormatter(ZoneOffset.UTC, TGLogHome.of(system)).format(record);
		return text.substring(text.indexOf(" - ") + 3);
	}

	private static final String WINDOWS = "Windows 11";
	private static final String LINUX = "Linux";
	private static final String SOMEONE = "C:\\Users\\someone";
	private static final String BOB = "/home/bob";

	// each row: what it shows; the system's os.name and user.home; a message; the message as the log writes it
	private static final String[][] HOME_FOLDERS = {
		{"anywhere in a line, as often as it appears", WINDOWS, SOMEONE,
			"cannot load \"C:\\Users\\someone\\AppData\\Local\\Programs\\Common\\VST3\\Amp.vst3\": denied (user folder C:\\Users\\someone)",
			"cannot load \"~\\AppData\\Local\\Programs\\Common\\VST3\\Amp.vst3\": denied (user folder ~)"},
		{"on Windows, with either slash", WINDOWS, SOMEONE, "file:/C:/Users/someone/AppData/x.log", "file:/~/AppData/x.log"},
		{"on Windows, in any case and with mixed slashes", WINDOWS, SOMEONE, "c:\\USERS/SomeOne\\Music/a.gp5", "~\\Music/a.gp5"},
		{"elsewhere, in its own case only", LINUX, BOB, "/home/bob/x and /home/Bob/x", "~/x and /home/Bob/x"},
		{"not a folder that only starts like it", WINDOWS, SOMEONE, "C:\\Users\\someoneelse\\x C:\\Users\\someone.old\\x",
			"C:\\Users\\someoneelse\\x C:\\Users\\someone.old\\x"},
		{"not a folder that only ends like it", LINUX, BOB, "/mnt/home/bob/x /home/bob-2/x", "/mnt/home/bob/x /home/bob-2/x"},
		{"a home folder given with a slash at its end", "Windows 10", SOMEONE + "\\", "C:\\Users\\someone\\x", "~\\x"},
		{"a home folder without a name shortens nothing", LINUX, "/", "/ x / y", "/ x / y"},
		{"nor does one that is no folder", LINUX, "?", "a ? b", "a ? b"},
	};

	@Test
	public void theHomeFolderIsWrittenAsATilde() {
		List<String> expected = new ArrayList<String>();
		List<String> written = new ArrayList<String>();
		for (String[] row : HOME_FOLDERS) {
			expected.add(row[0] + ": " + row[4] + NL);
			written.add(row[0] + ": " + written(row[1], row[2], row[3], null));
		}
		assertEquals(expected, written);
	}

	@Test
	public void theStackTraceIsShortenedToo() {
		String text = written(WINDOWS, SOMEONE, "failed", new IOException("C:\\Users\\someone\\x.gp5 (denied)", new IOException("C:/Users/someone/y")));

		assertTrue(text.startsWith("failed" + NL + "java.io.IOException: ~\\x.gp5 (denied)" + NL + "\tat "), text);
		assertTrue(text.contains("Caused by: java.io.IOException: ~/y" + NL), text);
	}

	@Test
	public void byDefaultTheHomeFolderIsThatOfThisJava() {
		String home = System.getProperty("user.home");
		String line = new TGLogFormatter(ZoneOffset.UTC).format(new TGLogRecord(TGLogLevel.WARN, "in " + home + File.separator + "x", null, "app.tuxguitar.Main"));

		assertTrue(line.endsWith(" - in ~" + File.separator + "x" + NL), line);
	}

	@Test
	public void aRecordNotMadeByTuxGuitarShowsItsThreadNumberAndLevelName() {
		LogRecord record = new LogRecord(Level.CONFIG, "configured");
		record.setInstant(TIME);
		record.setLoggerName("app.tuxguitar.Other");
		record.setThreadID(7);

		assertEquals("2026-11-14T10:05:03.042+01:00 DEBUG [thread 7] Other - configured" + NL, this.formatter.format(record));
	}
}
