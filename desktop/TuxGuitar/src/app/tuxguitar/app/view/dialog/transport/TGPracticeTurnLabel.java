package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.editor.TGEditorManager;
import app.tuxguitar.editor.action.transport.TGTransportStepAction;
import app.tuxguitar.editor.util.TGSyncProcessLocked;
import app.tuxguitar.event.TGEvent;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGClickUpStep;
import app.tuxguitar.practice.TGPracticeTurn;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.util.TGContext;

/**
 * Under the Transport window's position: the turn of the interleaved practice session held, playing or paused ("Turn 3
 * of 12: Line 5" in English, see MidiPlayer.getPracticeTurn), worded by the app's messages; empty without one. With the
 * interval clock, the time left in the turn ("Turn 3: Line 5, 1:42 left") or, in a break, in the break and the next
 * turn's unit ("Break, 0:12 left. Next: Line 7"). For interleaved click-up, the step, its sections and its tempo ("Step 7
 * of 23: lines 2-3 at 84 bpm", one section alone "line 3", in percent "at 70%"). The controls of Repeat step and
 * Previous step beside it are enabled while there are steps to go back over (TGTransportStepAction.isEnabled). Shown
 * again on each redraw and update, until the label goes.
 */
public class TGPracticeTurnLabel implements TGEventListener {

	private final TGContext context;
	private final UILabel label;
	private final UIControl[] steps;
	private final TGSyncProcessLocked show;

	private TGPracticeTurnLabel(TGContext context, UILabel label, UIControl[] steps) {
		this.context = context;
		this.label = label;
		this.steps = steps;
		this.show = new TGSyncProcessLocked(context, this::show);
	}

	// the label shows the turn from now on, the controls of the step commands enabled while there are steps
	public static void attach(TGContext context, UILabel label, UIControl... steps) {
		TGPracticeTurnLabel turn = new TGPracticeTurnLabel(context, label, steps);
		TGEditorManager editor = TGEditorManager.getInstance(context);
		editor.addRedrawListener(turn);
		editor.addUpdateListener(turn);
		label.addDisposeListener(event -> {
			editor.removeRedrawListener(turn);
			editor.removeUpdateListener(turn);
		});
		turn.show.process();
	}

	private void show() {
		if (!this.label.isDisposed()) {
			this.show(MidiPlayer.getInstance(this.context).getPracticeTurn().map(TGPracticeTurnLabel::text).orElse(""));
			this.enableSteps(TGTransportStepAction.isEnabled(this.context));
		}
	}

	private void enableSteps(boolean enabled) {
		for (UIControl step : this.steps) {
			step.setEnabled(enabled);
		}
	}

	private static String text(TGPracticeTurn turn) {
		return turn.getStep().map(step -> step(turn, step)).orElseGet(() -> (turn.isTimed() ? timed(turn) : counted(turn)));
	}

	// "Step 7 of 23: lines 2-3 at 84 bpm": transport.click-up.step, the tempo by transport.click-up.tempo.<mode>
	private static String step(TGPracticeTurn turn, TGClickUpStep step) {
		String tempo = TuxGuitar.getProperty(TGClickUpDialog.tempoKey("transport.click-up.tempo.", step.getTempoMode()), new String[] {String.valueOf(step.getRung())});
		return TuxGuitar.getProperty("transport.click-up.step", new String[] {String.valueOf(turn.getTurn()), String.valueOf(turn.getTurns()), sections(turn), tempo});
	}

	// "line 3" (transport.click-up.section.<kind>), "lines 2-3" (transport.click-up.sections.<kind>)
	private static String sections(TGPracticeTurn turn) {
		String from = String.valueOf(turn.getFrom().getNumber());
		String to = String.valueOf(turn.getTo().getNumber());
		String kind = turn.getFrom().getKind().getName();
		return (from.equals(to) ? TuxGuitar.getProperty("transport.click-up.section." + kind, new String[] {from})
			: TuxGuitar.getProperty("transport.click-up.sections." + kind, new String[] {from, to}));
	}

	// "Turn 3 of 12: Line 5": transport.practice.turn, the unit's kind by transport.practice.turn.<kind>
	private static String counted(TGPracticeTurn turn) {
		return TuxGuitar.getProperty("transport.practice.turn", new String[] {String.valueOf(turn.getTurn()), String.valueOf(turn.getTurns()), kind(turn),
			String.valueOf(turn.getFrom().getNumber())});
	}

	// "Turn 3: Line 5, 1:42 left" (transport.practice.timed-turn) or "Break, 0:12 left. Next: Line 7"
	// (transport.practice.break)
	private static String timed(TGPracticeTurn turn) {
		String key = (turn.isBreak() ? "transport.practice.break" : "transport.practice.timed-turn");
		return TuxGuitar.getProperty(key, new String[] {String.valueOf(turn.getTurn()), kind(turn), String.valueOf(turn.getFrom().getNumber()),
			String.format("%d:%02d", turn.getSecondsLeft() / 60, turn.getSecondsLeft() % 60)});
	}

	private static String kind(TGPracticeTurn turn) {
		return TuxGuitar.getProperty("transport.practice.turn." + turn.getFrom().getKind().getName());
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
