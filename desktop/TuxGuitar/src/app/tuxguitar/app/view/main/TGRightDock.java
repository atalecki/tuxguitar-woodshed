package app.tuxguitar.app.view.main;

import java.util.function.BinaryOperator;

import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.resource.UICursor;
import app.tuxguitar.ui.widget.UIDivider;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * A place at the right of the main window, beside the score (right of its tab folder), where a plugin docks a panel of
 * its own (the Track panel) while it is connected: a divider, then the slot. The plugin makes its panel in the slot and
 * shows it at a width, or hides it; a drag of the divider asks the plugin the width it leaves, given the width shown
 * and how far it went (the plugin keeps it within its range, and remembers it). Neither the divider nor the slot takes
 * room while hidden. Glue only; on the UI thread.
 */
public final class TGRightDock {

	// how wide the divider is
	private static final float DIVIDER = 4f;

	private final TGContext context;
	private UITableLayout layout;
	private UIDivider divider;
	private UIPanel slot;
	// the width a drag leaves, given the width shown and how far it went (to the right: positive)
	private BinaryOperator<Float> dragged = (shown, by) -> shown - by;

	private TGRightDock(TGContext context) {
		this.context = context;
	}

	public static TGRightDock getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGRightDock.class.getName(), TGRightDock::new);
	}

	// the divider and the slot at the column of the container's single row, and the next (its table layout ignores what
	// is hidden); hidden until a plugin shows its panel
	void create(UIFactory factory, UIPanel container, int column) {
		this.layout = (UITableLayout) container.getLayout();
		this.divider = factory.createVerticalDivider(container);
		this.divider.setCursor(UICursor.SIZEWE);
		this.divider.addMouseDragListener(event -> this.drag(event.getPosition().getX()));
		this.layout.set(this.divider, 1, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, false, true, 1, 1, null, null, 0f);
		this.layout.set(this.divider, UITableLayout.PACKED_WIDTH, DIVIDER);
		this.slot = factory.createPanel(container, false);
		this.slot.setLayout(new UITableLayout(0f));
		this.slot.getLayout().set(UITableLayout.IGNORE_INVISIBLE, true);
		this.layout.set(this.slot, 1, column + 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, false, true, 1, 1, null, null, 0f);
		this.divider.setVisible(false);
		this.slot.setVisible(false);
	}

	public UIPanel getSlot() {
		return this.slot;
	}

	// the slot shown at the width, its divider with it, or both hidden; the window laid out again
	public void show(boolean shown, float width) {
		this.width(width);
		this.divider.setVisible(shown);
		this.slot.setVisible(shown);
		this.layout();
	}

	// the width a drag of the divider leaves, given the width shown and how far it went
	public void setDragged(BinaryOperator<Float> dragged) {
		this.dragged = dragged;
	}

	private void drag(float by) {
		this.width(this.dragged.apply(this.slot.getBounds().getWidth(), by));
		this.layout();
	}

	private void width(float width) {
		this.layout.set(this.slot, UITableLayout.PACKED_WIDTH, width);
		this.layout.set(this.slot, UITableLayout.MAXIMUM_PACKED_WIDTH, width);
	}

	public void layout() {
		TGWindow.getInstance(this.context).getWindow().layout();
	}
}
