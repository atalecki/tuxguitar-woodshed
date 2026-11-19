package app.tuxguitar.audio;

import app.tuxguitar.midi.synth.TGDefaultSource;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.song.models.TGChannel;

/**
 * The sound a track has, its channel's, as its sound chain's source says it: MIDI, its General MIDI program played as it
 * is, inside TuxGuitar (the source plain General MIDI, TGSoundChain.midi: no default instrument in its place); VST3, a
 * VST3 instrument of its own or else the default instrument of its kind (any other source); an audio track's takes,
 * which no switch changes. The effects after the source are the same whatever the sound.
 */
public enum TGSoundMode {
	MIDI, VST3, TAKES;

	public static TGSoundMode of(TGChannel channel) {
		return of(channel, TGSoundChain.get(channel));
	}

	public static TGSoundMode of(TGChannel channel, TGProgram chain) {
		if (channel.isAudioChannel()) {
			return TAKES;
		}
		return (isMidi(chain.getReceiver()) ? MIDI : VST3);
	}

	// plain General MIDI: a General MIDI source that does not give way to the default
	static boolean isMidi(TGProgramElement source) {
		return GervillProcessorFactory.TYPE.equals(source.getType()) && !TGDefaultSource.givesWay(source);
	}
}
