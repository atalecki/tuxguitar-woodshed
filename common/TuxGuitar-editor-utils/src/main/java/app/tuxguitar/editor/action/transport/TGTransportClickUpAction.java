package app.tuxguitar.editor.action.transport;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.practice.TGClickUpLadder;
import app.tuxguitar.practice.TGClickUpSettings;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * Starts interleaved click-up of the song, as set, live (MidiPlayer.practise of the ladder's course): its steps in
 * order, each at its tempo after a Count In, Repeat step and Previous step going back over them (TGTransportStepAction).
 * A click-up that cannot be played as set, or a sequencer that cannot play one, is refused with the reason (a
 * TGActionException). The settings of the last start, refused or not, are kept for the app's run, for the dialog to
 * show again; at first measures 1 to 4, from 60% to 100% by 5%, 1 pass a step.
 */
public class TGTransportClickUpAction extends TGTransportStartAction<TGClickUpSettings> {

	public static final String NAME = "action.transport.click-up";

	public static final String ATTRIBUTE_SETTINGS = "clickUpSettings";

	public static final int FIRST_PASSAGE_TO = 4;

	public TGTransportClickUpAction(TGContext context) {
		super(context, NAME, ATTRIBUTE_SETTINGS, TGTransportClickUpAction::first);
	}

	// the settings the last click-up started with, or was refused with
	public static TGClickUpSettings getLastSettings(TGContext context) {
		return getLastSettings(context, NAME, TGTransportClickUpAction::first);
	}

	private static TGClickUpSettings first() {
		TGClickUpSettings settings = new TGClickUpSettings();
		settings.setPassageTo(FIRST_PASSAGE_TO);
		return settings;
	}

	protected void start(TGClickUpSettings settings, TGSong song, MidiPlayer player) throws TGPracticeRefusal, MidiPlayerException {
		player.practise(TGClickUpLadder.of(settings, song).course());
	}
}
