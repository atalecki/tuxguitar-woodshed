package app.tuxguitar.util.log;

/**
 * The log level in force and where it came from. The command line (-Dtuxguitar.log.level) wins; else the debug helper
 * plugin, when enabled, raises the settings' level to at least DEBUG; else the settings' level; else WARN.
 */
public final class TGLogChoice {

	// the system property that sets the level from the command line
	public static final String LEVEL_PROPERTY = "tuxguitar.log.level";

	public enum Source {
		COMMAND_LINE("-D" + LEVEL_PROPERTY),
		DEBUG_HELPER("debug helper plugin"),
		SETTINGS("settings"),
		DEFAULT("default");

		private final String label;

		private Source(String label) {
			this.label = label;
		}

		public String toString() {
			return this.label;
		}
	}

	private final TGLogLevel level;
	private final Source source;

	private TGLogChoice(TGLogLevel level, Source source) {
		this.level = level;
		this.source = source;
	}

	public static TGLogChoice resolve(TGLogLevel commandLine, TGLogLevel settings, boolean debugHelper) {
		if (commandLine != null) {
			return new TGLogChoice(commandLine, Source.COMMAND_LINE);
		}
		TGLogChoice chosen = (settings != null ? new TGLogChoice(settings, Source.SETTINGS) : new TGLogChoice(TGLogLevel.WARN, Source.DEFAULT));
		if (debugHelper && !chosen.level.shows(TGLogLevel.DEBUG)) {
			return new TGLogChoice(TGLogLevel.DEBUG, Source.DEBUG_HELPER);
		}
		return chosen;
	}

	public TGLogLevel getLevel() {
		return this.level;
	}

	public Source getSource() {
		return this.source;
	}

	public String toString() {
		return this.level + " (" + this.source + ")";
	}
}
