package app.tuxguitar.editor.action.transport;

import java.util.concurrent.atomic.AtomicReference;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * Starts an interleaved practice session of the song, as set, live (MidiPlayer.practise), its turns chosen from a new
 * random seed each time. A session that cannot be played as set, or a sequencer that cannot play one, is refused with
 * the reason (a TGActionException). The settings of the last start, refused or not, are kept for the app's run, for the
 * dialog to show again; at first measures 1 to 4, 12 turns.
 */
public class TGTransportPractiseAction extends TGActionBase {

	public static final String NAME = "action.transport.practise";

	public static final String ATTRIBUTE_SETTINGS = "practiceSettings";

	public static final int FIRST_POOL_TO = 4;
	public static final int FIRST_TURNS = 12;

	public TGTransportPractiseAction(TGContext context) {
		super(context, NAME);
	}

	// the settings the last session started with, or was refused with
	public static TGPracticeSettings getLastSettings(TGContext context) {
		return last(context).get();
	}

	private static AtomicReference<TGPracticeSettings> last(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGTransportPractiseAction.class.getName(), unused -> new AtomicReference<TGPracticeSettings>(first()));
	}

	private static TGPracticeSettings first() {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, FIRST_POOL_TO);
		settings.setTurns(FIRST_TURNS);
		return settings;
	}

	protected void processAction(TGActionContext context) {
		TGPracticeSettings settings = context.getAttribute(ATTRIBUTE_SETTINGS);
		last(getContext()).set(settings);
		try {
			TGPracticeSession session = TGPracticeSession.start(settings, TGDocumentManager.getInstance(getContext()).getSong());
			MidiPlayer.getInstance(getContext()).practise(session, System.nanoTime());
			context.setAttribute(ATTRIBUTE_SUCCESS, Boolean.TRUE);
		} catch (TGPracticeRefusal refusal) {
			throw new TGActionException(refusal.getMessage());
		} catch (MidiPlayerException e) {
			throw new TGActionException(e);
		}
	}
}
