package app.tuxguitar.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
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

	@Test
	public void aRecordNotMadeByTuxGuitarShowsItsThreadNumberAndLevelName() {
		LogRecord record = new LogRecord(Level.CONFIG, "configured");
		record.setInstant(TIME);
		record.setLoggerName("app.tuxguitar.Other");
		record.setThreadID(7);

		assertEquals("2026-11-14T10:05:03.042+01:00 DEBUG [thread 7] Other - configured" + NL, this.formatter.format(record));
	}
}
