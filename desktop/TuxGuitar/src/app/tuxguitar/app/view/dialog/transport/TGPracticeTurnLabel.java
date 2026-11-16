package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.editor.TGEditorManager;
import app.tuxguitar.editor.action.transport.TGTransportStepAction;
import app.tuxguitar.editor.util.TGSyncProcessLocked;
import app.tuxguitar.event.TGEvent;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGPracticeWording;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.util.TGContext;

/**
 * Under the Transport window's position: the turn of the interleaved practice session held, playing or paused (see
 * MidiPlayer.getPracticeTurn), in the app's messages as TGPracticeWording words it ("Turn 3 of 12: Line 5", "Step 7 of
 * 23: lines 2-3 at 84 bpm", a beat "measure 2 beat 3"); empty without one. The controls of Repeat step and
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
			this.show(MidiPlayer.getInstance(this.context).getPracticeTurn().map(turn -> TGPracticeWording.turn(turn, TuxGuitar::getProperty)).orElse(""));
			this.enableSteps(TGTransportStepAction.isEnabled(this.context));
		}
	}

	private void enableSteps(boolean enabled) {
		for (UIControl step : this.steps) {
			step.setEnabled(enabled);
		}
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
