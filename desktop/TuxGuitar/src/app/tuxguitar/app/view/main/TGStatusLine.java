package app.tuxguitar.app.view.main;

import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The line at the foot of the main window, under the fretboard, that shows a quiet hint (a plugin still loading, say):
 * hidden, taking no room, while there is none. Glue only: what it shows is decided where the hint comes from. On the UI
 * thread.
 */
public class TGStatusLine {

	private final TGContext context;
	private UILabel label;

	private TGStatusLine(TGContext context) {
		this.context = context;
	}

	public static TGStatusLine getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGStatusLine.class.getName(), TGStatusLine::new);
	}

	// at the row of the panel at the foot of the window, its last (its table layout ignores what is hidden)
	void create(UIFactory factory, UIPanel bottom, int row) {
		this.label = factory.createLabel(bottom);
		this.label.setVisible(false);
		((UITableLayout) bottom.getLayout()).set(this.label, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false);
	}

	// the hint, or none (null)
	public void show(String hint) {
		if (this.isCreated()) {
			this.showIn(hint);
		}
	}

	private void showIn(String hint) {
		boolean visible = (hint != null);
		this.label.setText(visible ? hint : "");
		this.setVisible(visible);
	}

	private boolean isCreated() {
		return (this.label != null && !this.label.isDisposed());
	}

	// the window is laid out again only when the line appears or goes
	private void setVisible(boolean visible) {
		if (visible != this.label.isVisible()) {
			this.label.setVisible(visible);
			TGWindow.getInstance(this.context).getWindow().layout();
		}
	}
}
