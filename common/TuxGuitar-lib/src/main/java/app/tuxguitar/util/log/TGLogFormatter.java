package app.tuxguitar.util.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * One line per record: ISO time with milliseconds and offset, level, [thread], logger (without "app.tuxguitar."),
 * message; then the stack trace of the record's error, if it has one. The home folder is written as "~" in all of it
 * ({@link TGLogHome}).
 */
public class TGLogFormatter extends Formatter {

	private static final String TUXGUITAR = TGLogSetup.ROOT_LOGGER + ".";
	private static final String NL = System.lineSeparator();

	private final DateTimeFormatter time;
	private final TGLogHome home;

	public TGLogFormatter() {
		this(ZoneId.systemDefault());
	}

	public TGLogFormatter(ZoneId zone) {
		this(zone, TGLogHome.of(System.getProperties()));
	}

	TGLogFormatter(ZoneId zone, TGLogHome home) {
		this.time = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(zone);
		this.home = home;
	}

	public String format(LogRecord record) {
		StringBuilder line = new StringBuilder();
		line.append(this.time.format(record.getInstant())).append(' ');
		line.append(String.format("%-5s", TGLogLevel.named(record.getLevel()))).append(" [");
		line.append(threadOf(record)).append("] ");
		line.append(shortName(record.getLoggerName())).append(" - ");
		line.append(record.getMessage()).append(NL);
		line.append(stackTrace(record.getThrown()));
		return this.home.shorten(line.toString());
	}

	private static String threadOf(LogRecord record) {
		if (record instanceof TGLogRecord) {
			return ((TGLogRecord) record).getThreadName();
		}
		return "thread " + record.getThreadID();
	}

	private static String shortName(String logger) {
		return (logger != null && logger.startsWith(TUXGUITAR) ? logger.substring(TUXGUITAR.length()) : logger);
	}

	private static String stackTrace(Throwable thrown) {
		if (thrown == null) {
			return "";
		}
		StringWriter trace = new StringWriter();
		thrown.printStackTrace(new PrintWriter(trace));
		return trace.toString();
	}
}
