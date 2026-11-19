package app.tuxguitar.audio.action;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.audio.TGSoundMode;
import app.tuxguitar.midi.synth.TGDefaultSource;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.util.TGContext;

/**
 * Sets a track's sound to MIDI: its General MIDI program plays as it is, in place of its VST3 instrument (the effects
 * stay). The instrument of its own it replaced, its state with it, is kept in the song while on MIDI, so the switch back
 * (TGSetVst3SoundAction) restores it; the default instrument of its kind is not kept (back, the default plays again).
 * Refused on MIDI already, and on an audio track, whose source is its takes.
 */
public class TGSetMidiSoundAction extends TGSoundChainAction {

	public static final String NAME = "action.audio.sound.set-midi";

	public TGSetMidiSoundAction(TGContext context) {
		super(context, NAME);
	}

	protected boolean change(TGActionContext context, TGChannel channel, TGProgram chain) {
		if (TGSoundMode.of(channel, chain) != TGSoundMode.VST3) {
			return false;
		}
		chain.setReceiver(TGSoundChain.midi());
		return true;
	}

	protected TGProgramElement kept(TGChannel channel, TGProgramElement replaced) {
		return (TGDefaultSource.givesWay(replaced) ? null : replaced);
	}
}
