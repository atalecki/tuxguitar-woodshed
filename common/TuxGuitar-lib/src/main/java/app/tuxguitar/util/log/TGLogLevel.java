package app.tuxguitar.util.log;

import java.util.Locale;
import java.util.logging.Level;

/**
 * How much TuxGuitar writes to its log, from the least (errors only) to the most (trace). Each level is a
 * java.util.logging level underneath.
 */
public enum TGLogLevel {

	ERROR(Level.SEVERE),
	WARN(Level.WARNING),
	INFO(Level.INFO),
	DEBUG(Level.FINE),
	TRACE(Level.FINEST);

	private final Level jul;

	private TGLogLevel(Level jul) {
		this.jul = jul;
	}

	public Level toJul() {
		return this.jul;
	}

	// whether a log at this level shows what is logged at the other
	public boolean shows(TGLogLevel other) {
		return other.ordinal() <= this.ordinal();
	}

	// the level of that name, in any case, blanks around it ignored; null for any other text
	public static TGLogLevel parse(String text) {
		String name = (text != null ? text.trim().toUpperCase(Locale.ROOT) : "");
		for (TGLogLevel level : values()) {
			if (level.name().equals(name)) {
				return level;
			}
		}
		return null;
	}

	// the most severe level at or below a java.util.logging level: the name a record of that level is shown with
	static TGLogLevel named(Level level) {
		for (TGLogLevel candidate : values()) {
			if (candidate.jul.intValue() <= level.intValue()) {
				return candidate;
			}
		}
		return TRACE;
	}
}
