package app.tuxguitar.midi.synth;

/**
 * The default instrument a channel plays when it has none of its own (no sound chain, or one whose source is General
 * MIDI): an app installs one on the synthesizer's manager (TGSynthManager.addExtension), so every synthesizer of the
 * context, live or rendering offline, asks the same one. Without one, such a channel plays General MIDI, as it does
 * when the source given does not open.
 */
@FunctionalInterface
public interface TGDefaultSource {

	// the source a channel of the bank and program plays, an element of an id of its own; null: its General MIDI program
	TGProgramElement sourceFor(int bank, int program);

	// the source it gave a channel did not open (the processor that could not open it; null when no factory makes its
	// type): the channel plays General MIDI instead
	default void notOpened(TGProgramElement source, TGMidiProcessor processor) {
	}
}
