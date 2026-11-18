package app.tuxguitar.midi.synth;

/**
 * The default instrument a channel plays when it has none of its own (no sound chain, or one whose source is General
 * MIDI): an app installs one on the synthesizer's manager (TGSynthManager.addExtension), so every synthesizer of the
 * context, live or rendering offline, asks the same one. Without one, such a channel plays General MIDI.
 */
@FunctionalInterface
public interface TGDefaultSource {

	// the source a channel of the bank and program plays, an element of an id of its own; null: its General MIDI program
	TGProgramElement sourceFor(int bank, int program);
}
