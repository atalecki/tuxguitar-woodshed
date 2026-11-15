package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.editor.TGEditorManager;
import app.tuxguitar.editor.util.TGSyncProcessLocked;
import app.tuxguitar.event.TGEvent;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGPracticeTurn;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.util.TGContext;

/**
 * Under the Transport window's position: the turn of the interleaved practice session held, playing or paused ("Turn 3
 * of 12: Line 5" in English, see MidiPlayer.getPracticeTurn), worded by the app's messages; empty without one. With the
 * interval clock, the time left in the turn ("Turn 3: Line 5, 1:42 left") or, in a break, in the break and the next
 * turn's unit ("Break, 0:12 left. Next: Line 7"). Shown again on each redraw and update, until the label goes.
 */
public class TGPracticeTurnLabel implements TGEventListener {

	private final TGContext context;
	private final UILabel label;
	private final TGSyncProcessLocked show;

	private TGPracticeTurnLabel(TGContext context, UILabel label) {
		this.context = context;
		this.label = label;
		this.show = new TGSyncProcessLocked(context, this::show);
	}

	// the label shows the turn from now on
	public static void attach(TGContext context, UILabel label) {
		TGPracticeTurnLabel turn = new TGPracticeTurnLabel(context, label);
		TGEditorManager editor = TGEditorManager.getInstance(context);
		editor.addRedrawListener(turn);
		editor.addUpdateListener(turn);
		label.addDisposeListener(event -> {
			editor.removeRedrawListener(turn);
			editor.removeUpdateListener(turn);
		});
	}

	private void show() {
		if (!this.label.isDisposed()) {
			this.show(MidiPlayer.getInstance(this.context).getPracticeTurn().map(TGPracticeTurnLabel::text).orElse(""));
		}
	}

	private static String text(TGPracticeTurn turn) {
		return (turn.isTimed() ? timed(turn) : counted(turn));
	}

	// "Turn 3 of 12: Line 5": transport.practice.turn, the unit's kind by transport.practice.turn.<kind>
	private static String counted(TGPracticeTurn turn) {
		return TuxGuitar.getProperty("transport.practice.turn", new String[] {String.valueOf(turn.getTurn()), String.valueOf(turn.getTurns()), kind(turn),
			String.valueOf(turn.getUnit().getNumber())});
	}

	// "Turn 3: Line 5, 1:42 left" (transport.practice.timed-turn) or "Break, 0:12 left. Next: Line 7"
	// (transport.practice.break)
	private static String timed(TGPracticeTurn turn) {
		String key = (turn.isBreak() ? "transport.practice.break" : "transport.practice.timed-turn");
		return TuxGuitar.getProperty(key, new String[] {String.valueOf(turn.getTurn()), kind(turn), String.valueOf(turn.getUnit().getNumber()),
			String.format("%d:%02d", turn.getSecondsLeft() / 60, turn.getSecondsLeft() % 60)});
	}

	private static String kind(TGPracticeTurn turn) {
		return TuxGuitar.getProperty("transport.practice.turn." + turn.getUnit().getKind().getName());
	}

	// a text of another length lays the window out again
	private void show(String turn) {
		if (!turn.equals(this.label.getText())) {
			this.label.setText(turn);
			((UILayoutContainer) this.label.getParent()).layout();
		}
	}

	public void processEvent(TGEvent event) {
		this.show.process();
	}
}
