package app.tuxguitar.vst3;

import java.io.File;

/**
 * A module in the VST3 folders that a scan left out, and why: the host's refusal (not a VST3 module, say), or the stop
 * of the host process scanning it.
 */
public final class TGVst3SkippedModule {

	private final File module;
	private final String reason;

	public TGVst3SkippedModule(File module, String reason) {
		this.module = module;
		this.reason = reason;
	}

	public File getModule() {
		return this.module;
	}

	public String getReason() {
		return this.reason;
	}
}
