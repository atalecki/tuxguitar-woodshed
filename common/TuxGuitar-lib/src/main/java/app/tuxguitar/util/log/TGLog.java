package app.tuxguitar.util.log;

import java.util.logging.Logger;

/**
 * TuxGuitar's log, one per class: {@code private static final TGLog LOG = TGLog.get(MyClass.class);}
 *
 * A message at a level that is off costs a level check and nothing else. Where building the message costs something,
 * or on the audio thread, check the level first: {@code if (LOG.isTraceEnabled()) LOG.trace("block " + n);}
 */
public final class TGLog {

	private final Logger logger;

	TGLog(Logger logger) {
		this.logger = logger;
	}

	public static TGLog get(Class<?> type) {
		TGLogSetup.getInstance();
		return new TGLog(Logger.getLogger(type.getName()));
	}

	// a log that belongs to no class, such as "console.err", under TuxGuitar's
	public static TGLog named(String name) {
		TGLogSetup.getInstance();
		return new TGLog(Logger.getLogger(TGLogSetup.ROOT_LOGGER + "." + name));
	}

	public boolean isEnabled(TGLogLevel level) {
		return this.logger.isLoggable(level.toJul());
	}

	public boolean isDebugEnabled() {
		return this.isEnabled(TGLogLevel.DEBUG);
	}

	public boolean isTraceEnabled() {
		return this.isEnabled(TGLogLevel.TRACE);
	}

	public void error(String message, Throwable thrown) {
		this.log(TGLogLevel.ERROR, message, thrown);
	}

	public void warn(String message) {
		this.log(TGLogLevel.WARN, message, null);
	}

	public void warn(String message, Throwable thrown) {
		this.log(TGLogLevel.WARN, message, thrown);
	}

	public void info(String message) {
		this.log(TGLogLevel.INFO, message, null);
	}

	public void debug(String message) {
		this.log(TGLogLevel.DEBUG, message, null);
	}

	public void trace(String message) {
		this.log(TGLogLevel.TRACE, message, null);
	}

	public void log(TGLogLevel level, String message, Throwable thrown) {
		if (this.isEnabled(level)) {
			this.logger.log(new TGLogRecord(level, message, thrown, this.logger.getName()));
		}
	}
}
