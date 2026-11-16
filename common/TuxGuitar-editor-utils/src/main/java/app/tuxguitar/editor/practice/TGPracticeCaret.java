package app.tuxguitar.editor.practice;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.util.TGContext;

/**
 * The caret during an interleaved practice session (TGPracticeUnderWay): it moves freely, playing or paused (a move of
 * the caret is let through, see TGPracticeEdits.disables), and moves only itself, never the player, so the session goes
 * on (moving the player ends it). Outside a session, while the song plays, moving the caret moves the player there too,
 * as it always has.
 */
public final class TGPracticeCaret {

	// the app's caret actions are named so
	public static final String CARET_ACTIONS = "action.caret.";

	private TGPracticeCaret() {
	}

	// whether moving the caret moves the player too: while the song plays, outside a session
	public static boolean movesPlayer(TGContext context) {
		return (MidiPlayer.getInstance(context).isRunning() && TGPracticeUnderWay.getInstance(context).getCourse() == null);
	}

	// whether the action moves the caret
	public static boolean isMove(String id) {
		return id.startsWith(CARET_ACTIONS);
	}
}
