package app.tuxguitar.util.log;

import java.util.logging.LogRecord;

/**
 * A log record that keeps the name of the thread that made it (java.util.logging keeps only its number).
 */
public class TGLogRecord extends LogRecord {

	private static final long serialVersionUID = 1L;

	private final String threadName;

	public TGLogRecord(TGLogLevel level, String message, Throwable thrown, String loggerName) {
		super(level.toJul(), message);
		this.setThrown(thrown);
		this.setLoggerName(loggerName);
		this.threadName = Thread.currentThread().getName();
	}

	public String getThreadName() {
		return this.threadName;
	}
}
