package app.tuxguitar.editor.action.transport;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * Starts practice of the song live, as set (the settings, given as the attribute the action names): interleaved
 * practice, interleaved click-up. Practice that cannot be played as set, or a sequencer that cannot play it, is refused
 * with the reason (a TGActionException). The settings of the last start, refused or not, are kept for the app's run,
 * one per action, for its dialog to show again; at first, as the action gives them.
 */
public abstract class TGTransportStartAction<S> extends TGActionBase {

	private final String attribute;
	private final Supplier<S> first;

	protected TGTransportStartAction(TGContext context, String name, String attribute, Supplier<S> first) {
		super(context, name);
		this.attribute = attribute;
		this.first = first;
	}

	// the settings the action named last started with, or was refused with; at first as first gives them
	protected static <S> S getLastSettings(TGContext context, String name, Supplier<S> first) {
		return last(context, name, first).get();
	}

	private static <S> AtomicReference<S> last(TGContext context, String name, Supplier<S> first) {
		return TGSingletonUtil.getInstance(context, TGTransportStartAction.class.getName() + " " + name, unused -> new AtomicReference<S>(first.get()));
	}

	protected void processAction(TGActionContext context) {
		S settings = context.getAttribute(this.attribute);
		last(getContext(), getName(), this.first).set(settings);
		try {
			this.start(settings, TGDocumentManager.getInstance(getContext()).getSong(), MidiPlayer.getInstance(getContext()));
			context.setAttribute(ATTRIBUTE_SUCCESS, Boolean.TRUE);
		} catch (TGPracticeRefusal refusal) {
			throw new TGActionException(refusal.getMessage());
		} catch (MidiPlayerException e) {
			throw new TGActionException(e);
		}
	}

	// the player plays the song's practice, as set
	protected abstract void start(S settings, TGSong song, MidiPlayer player) throws TGPracticeRefusal, MidiPlayerException;
}
