package app.tuxguitar.vst3;

/**
 * Told, on the hosting's restart thread, that a plugin whose host process stopped by itself (it crashed, or was ended
 * for not answering: hung) was opened again from its element (started: the plugin as it runs now), or left silent
 * (started: null; it was started again once already since it was last given its element), with why it stopped.
 */
public interface TGVst3RestartListener {

	void restarted(TGVst3Instance stopped, TGVst3Instance started, boolean hung, TGVst3Exception why);
}
