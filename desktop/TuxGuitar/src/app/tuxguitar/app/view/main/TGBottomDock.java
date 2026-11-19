package app.tuxguitar.app.view.main;

import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * A place at the foot of the main window, under the fretboard and above the quiet status line, where a plugin docks a
 * panel of its own (the Sound Chain panel) while it is connected: the plugin makes its panel in the slot, shows or
 * hides it, and has the window laid out again (layout). The slot takes no room while it holds nothing shown. Glue only;
 * on the UI thread.
 */
public final class TGBottomDock {

	private final TGContext context;
	private UIPanel slot;

	private TGBottomDock(TGContext context) {
		this.context = context;
	}

	public static TGBottomDock getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGBottomDock.class.getName(), TGBottomDock::new);
	}

	// the slot at the row of the panel at the foot of the window (its table layout ignores what is hidden)
	void create(UIFactory factory, UIPanel bottom, int row) {
		this.slot = factory.createPanel(bottom, false);
		this.slot.setLayout(new UITableLayout(0f));
		this.slot.getLayout().set(UITableLayout.IGNORE_INVISIBLE, true);
		((UITableLayout) bottom.getLayout()).set(this.slot, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
	}

	public UIPanel getSlot() {
		return this.slot;
	}

	public void layout() {
		TGWindow.getInstance(this.context).getWindow().layout();
	}
}
