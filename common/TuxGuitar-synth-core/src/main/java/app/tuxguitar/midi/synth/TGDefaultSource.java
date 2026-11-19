package app.tuxguitar.midi.synth;

import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;

/**
 * The default instrument a channel plays when it has none of its own (no sound chain, or one whose source is General
 * MIDI): an app installs one on the synthesizer's manager (TGSynthManager.addExtension), so every synthesizer of the
 * context, live or rendering offline, asks the same one. Without one, such a channel plays General MIDI, as it does
 * when the source given does not open. A General MIDI source marked plain (a track's sound set to MIDI) plays its
 * program: no default takes its place.
 */
@FunctionalInterface
public interface TGDefaultSource {

	// the parameter of a General MIDI source that, saying "true", marks it plain General MIDI: its program plays
	String PLAIN_GENERAL_MIDI = "general-midi.plain";

	// whether a chain's source gives its place to the default: a General MIDI source not marked plain
	static boolean givesWay(TGProgramElement source) {
		return GervillProcessorFactory.TYPE.equals(source.getType()) && !Boolean.parseBoolean(source.getParameters().get(PLAIN_GENERAL_MIDI));
	}

	// the source a channel of the bank and program plays, an element of an id of its own; null: its General MIDI program
	TGProgramElement sourceFor(int bank, int program);

	// the source it gave a channel did not open (the processor that could not open it; null when no factory makes its
	// type): the channel plays General MIDI instead
	default void notOpened(TGProgramElement source, TGMidiProcessor processor) {
	}
}
