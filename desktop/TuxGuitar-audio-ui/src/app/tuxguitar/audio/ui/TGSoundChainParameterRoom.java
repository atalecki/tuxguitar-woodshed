package app.tuxguitar.audio.ui;

import java.util.Optional;

import app.tuxguitar.app.view.main.TGBottomDock;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.audio.TGParameterColumns;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UIScrollBarPanelLayout;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.resource.UISize;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UIScrollBarPanel;
import app.tuxguitar.util.TGContext;

/**
 * The room of the Sound Chain panel's editor area for TuxGuitar's parameter rows (TGPluginParameterRowsView) of the
 * plugin shown when it has no editor of its own, and of every plugin where the host embeds none (Linux, macOS): the rows
 * in as many columns as the panel's width holds, the room as high as the longest column but at most half of TuxGuitar's
 * window, the rows scrolling inside it beyond that (TGParameterColumns). Empty, it takes no room. Glue only.
 */
final class TGSoundChainParameterRoom {

	private static final int SCROLL_INCREMENT = 10;

	private final TGContext context;
	private final UIFactory factory;
	private final UIPanel parent;
	private final UIScrollBarPanel room;
	private Optional<TGPluginParameterRowsView> rows = Optional.empty();
	// one column of the rows shown: its width, and the height of a row
	private int cellWidth;
	private int rowHeight;
	private int columns;

	TGSoundChainParameterRoom(TGContext context, UIFactory factory, UIPanel parent, int row) {
		this.context = context;
		this.factory = factory;
		this.parent = parent;
		this.room = factory.createScrollBarPanel(parent, true, false, false);
		this.room.setLayout(new UIScrollBarPanelLayout(false, true, true, true, false, true));
		this.room.getVScroll().setIncrement(SCROLL_INCREMENT);
		this.room.getVScroll().addSelectionListener(event -> this.room.layout());
		this.room.addResizeListener(event -> this.resized());
		((UITableLayout) parent.getLayout()).set(this.room, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
		this.fit();
	}

	// the rows of TuxGuitar's parameter editor, each value given to the setter, instead of any shown
	void show(TGPluginEditor editor, TGPluginParameterRowsView.Setter setter) {
		this.clear();
		TGPluginParameterRowsView shown = new TGPluginParameterRowsView(this.context, this.factory, this.room, editor, setter);
		UISize column = shown.measure();
		this.cellWidth = Math.round(column.getWidth());
		this.rowHeight = Math.round(column.getHeight() / Math.max(1, shown.count()));
		this.rows = Optional.of(shown);
		this.fit();
	}

	// the editor followed may hold other values: its rows show them; gone once it no longer runs
	void follow(TGPluginEditor editor) {
		this.rows.filter(shown -> shown.isOf(editor)).filter(shown -> !shown.follow()).ifPresent(shown -> this.clear());
	}

	// the editor's rows gone (the area closed its view), the room empty; another's stay
	void close(TGPluginEditor editor) {
		this.rows.filter(shown -> shown.isOf(editor)).ifPresent(shown -> this.clear());
	}

	// the rows gone, the room empty
	private void clear() {
		this.rows.ifPresent(TGPluginParameterRowsView::dispose);
		this.rows = Optional.empty();
		this.fit();
	}

	// the panel's width changed: the rows take as many columns as it holds now
	private void resized() {
		if (this.columns() != this.columns) {
			this.fit();
		}
	}

	private int columns() {
		return TGParameterColumns.columns(Math.round(this.parent.getBounds().getWidth()), this.cellWidth, this.count());
	}

	private int count() {
		return this.rows.map(TGPluginParameterRowsView::count).orElse(0);
	}

	// the rows in columns, the room as high as they need (at most its share of TuxGuitar's window)
	private void fit() {
		this.columns = this.columns();
		this.rows.ifPresent(shown -> shown.columns(this.columns));
		int window = Math.round(TGWindow.getInstance(this.context).getWindow().getBounds().getHeight());
		int height = TGParameterColumns.height(TGParameterColumns.rows(this.count(), this.columns), this.rowHeight, window);
		this.room.setVisible(height > 0);
		((UITableLayout) this.parent.getLayout()).set(this.room, UITableLayout.PACKED_HEIGHT, (float) height);
		this.room.layout();
		TGBottomDock.getInstance(this.context).layout();
	}
}
