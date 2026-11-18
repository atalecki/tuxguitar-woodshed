package app.tuxguitar.audio.action;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.audio.TGDefaultPlay;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.vst3.TGVst3Exception;

/**
 * The Sound Chain dialog's "Keep in song": the default instrument the channel plays (TGDefaultInstrumentSource.play)
 * becomes its own instrument, its state in the song (Woodshed Guitar's holds the kind's profile), so the channel no
 * longer follows the settings and sounds the same wherever the song opens; the effects stay. Refused when the source is
 * not the default or no default instrument plays (General MIDI does); refused saying why when the instrument does not
 * open.
 */
public class TGKeepDefaultInstrumentAction extends TGSoundChainAction {

	public static final String NAME = "action.audio.sound-chain.keep-default-instrument";

	public TGKeepDefaultInstrumentAction(TGContext context) {
		super(context, NAME);
	}

	protected boolean change(TGActionContext context, TGChannel channel, TGProgram chain) {
		TGDefaultInstrumentSource defaults = TGDefaultInstrumentSource.install(getContext());
		TGDefaultPlay play = defaults.play(channel);
		if (!TGSoundChainRows.of(channel, play).canKeep()) {
			return false;
		}
		try {
			chain.setReceiver(defaults.kept(play));
		} catch (TGVst3Exception e) {
			throw new TGActionException(e.getMessage(), e);
		}
		return true;
	}
}
