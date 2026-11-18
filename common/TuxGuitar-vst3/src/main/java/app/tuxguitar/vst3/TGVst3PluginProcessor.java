package app.tuxguitar.vst3;

/**
 * A processor of the TuxGuitar Synthesizer that runs a VST3 plugin: the plugin as it is open in its host process.
 */
public interface TGVst3PluginProcessor {

	// null while no plugin is open
	TGVst3Instance getInstance();

	// why no plugin is open: the host's refusal, or its stop; null while one is open
	String getProblem();

	// the same, as the log keeps it: in English
	String getLoggedProblem();
}
