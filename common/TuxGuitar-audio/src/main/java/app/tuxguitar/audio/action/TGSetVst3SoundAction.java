package app.tuxguitar.audio.action;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.audio.TGSoundMode;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.util.TGContext;

/**
 * Sets a track's sound back to VST3: the instrument the song kept while on MIDI (TGSetMidiSoundAction) is the source
 * again, as it was; with none kept, the default instrument of the track's kind. The effects stay. Refused unless the
 * track is on MIDI.
 */
public class TGSetVst3SoundAction extends TGSoundChainAction {

	public static final String NAME = "action.audio.sound.set-vst3";

	public TGSetVst3SoundAction(TGContext context) {
		super(context, NAME);
	}

	protected boolean change(TGActionContext context, TGChannel channel, TGProgram chain) {
		if (TGSoundMode.of(channel, chain) != TGSoundMode.MIDI) {
			return false;
		}
		TGProgramElement kept = TGSoundChain.kept(channel);
		chain.setReceiver(kept != null ? kept : TGSoundChain.source(GervillProcessorFactory.TYPE));
		return true;
	}
}
