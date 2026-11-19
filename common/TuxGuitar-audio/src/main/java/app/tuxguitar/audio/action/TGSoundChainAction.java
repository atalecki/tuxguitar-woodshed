package app.tuxguitar.audio.action;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.util.TGContext;

/**
 * Changes the sound chain of a channel (the context's channel). The chain is part of the song: the change is undone like
 * any channel change. A change the chain does not allow is refused and changes nothing. The instrument a track's sound
 * set to MIDI replaced is kept with the chain while on MIDI (TGSoundChain.kept).
 */
public abstract class TGSoundChainAction extends TGActionBase {

	protected TGSoundChainAction(TGContext context, String name) {
		super(context, name);
	}

	protected void processAction(TGActionContext context) {
		TGChannel channel = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_CHANNEL);
		TGProgram chain = TGSoundChain.get(channel);
		TGProgramElement source = chain.getReceiver();
		if (this.change(context, channel, chain)) {
			TGSoundChain.save(channel, chain, this.kept(channel, source), getSongManager(context).getFactory());
			context.setAttribute(ATTRIBUTE_SUCCESS, Boolean.TRUE);
		}
	}

	// changes the chain of the channel; false when the change is refused
	protected abstract boolean change(TGActionContext context, TGChannel channel, TGProgram chain);

	// the instrument the song keeps while the track is on MIDI, the source replaced given: the one kept already (the
	// chain saved keeps it only while its source is MIDI's)
	protected TGProgramElement kept(TGChannel channel, TGProgramElement replaced) {
		return TGSoundChain.kept(channel);
	}
}
