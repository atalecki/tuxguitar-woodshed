package app.tuxguitar.editor.action.transport;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * Starts an interleaved practice session of the song, as set, live (MidiPlayer.practise), its turns chosen from a new
 * random seed each time. A session that cannot be played as set, or a sequencer that cannot play one, is refused with
 * the reason (a TGActionException). The settings of the last start, refused or not, are kept for the app's run, for
 * the dialog to show again; at first measures 1 to 4, 12 turns.
 */
public class TGTransportPractiseAction extends TGTransportStartAction<TGPracticeSettings> {

	public static final String NAME = "action.transport.practise";

	public static final String ATTRIBUTE_SETTINGS = "practiceSettings";

	public static final int FIRST_POOL_TO = 4;
	public static final int FIRST_TURNS = 12;

	public TGTransportPractiseAction(TGContext context) {
		super(context, NAME, ATTRIBUTE_SETTINGS, TGTransportPractiseAction::first);
	}

	// the settings the last session started with, or was refused with
	public static TGPracticeSettings getLastSettings(TGContext context) {
		return getLastSettings(context, NAME, TGTransportPractiseAction::first);
	}

	private static TGPracticeSettings first() {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, FIRST_POOL_TO);
		settings.setTurns(FIRST_TURNS);
		return settings;
	}

	protected void start(TGPracticeSettings settings, TGSong song, MidiPlayer player) throws TGPracticeRefusal, MidiPlayerException {
		player.practise(TGPracticeSession.start(settings, song), System.nanoTime());
	}
}
