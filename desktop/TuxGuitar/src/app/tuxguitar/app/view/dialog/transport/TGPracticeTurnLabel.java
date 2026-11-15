package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.editor.TGEditorManager;
import app.tuxguitar.editor.util.TGSyncProcessLocked;
import app.tuxguitar.event.TGEvent;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.util.TGContext;

/**
 * Under the Transport window's position: the turn of the interleaved practice session held, playing or paused ("Turn 3
 * of 12: Line 5", see MidiPlayer.getPracticeTurn); empty without one. Shown again on each redraw and update, until the
 * label goes.
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
			this.show(MidiPlayer.getInstance(this.context).getPracticeTurn());
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
