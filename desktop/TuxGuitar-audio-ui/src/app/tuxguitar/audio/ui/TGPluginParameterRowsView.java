package app.tuxguitar.audio.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.tuxguitar.app.util.TGContinousControlSelectionListener;
import app.tuxguitar.app.util.TGContinuousControl;
import app.tuxguitar.audio.TGParameterColumns;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGPluginParameterRow;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.resource.UISize;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UIScale;
import app.tuxguitar.ui.widget.UITextField;
import app.tuxguitar.util.TGContext;

/**
 * TuxGuitar's parameter rows of a plugin without an editor of its own (TGPluginEditor's rows), in any container (the
 * Parameters window, the Sound Chain panel's editor area): a row per parameter, its title, a slider over its range, its
 * plain value and Set, laid out in columns (TGParameterColumns: down each column first). A slider moved, once it rests,
 * or a value typed and set, is handed to the setter: one undoable change of the song (TGSoundChainCommands.set). Glue
 * only: the rows decide positions and values, the editor when it follows its plugin.
 */
final class TGPluginParameterRowsView {

	// how long a slider rests before its value is set (one undo step per rest, not per move)
	private static final int REST_MILLISECONDS = 250;
	// the cells of a row: title, slider, value, Set
	private static final int CELLS = 4;

	// sets the parameter (by its id) to the plain value
	interface Setter {
		void set(int parameter, double plain);
	}

	private final TGContext context;
	private final TGPluginEditor editor;
	private final Setter setter;
	private final UIPanel panel;
	private final List<Row> rows = new ArrayList<Row>();

	TGPluginParameterRowsView(TGContext context, UIFactory factory, UILayoutContainer parent, TGPluginEditor editor, Setter setter) {
		this.context = context;
		this.editor = editor;
		this.setter = setter;
		this.panel = factory.createPanel(parent, false);
		this.panel.setLayout(new UITableLayout());
		editor.getRows().forEach(row -> this.rows.add(new Row(factory, row)));
		this.columns(1);
	}

	UIPanel getControl() {
		return this.panel;
	}

	boolean isOf(TGPluginEditor other) {
		return (this.editor == other);
	}

	int count() {
		return this.rows.size();
	}

	// the size of the rows in one column, in the toolkit's units
	UISize measure() {
		this.panel.computePackedSize(null, null);
		return this.panel.getPackedSize();
	}

	// the rows laid out in that many columns, down each column first
	void columns(int columns) {
		int rows = TGParameterColumns.rows(this.rows.size(), columns);
		for (int i = 0; i < this.rows.size(); i++) {
			this.rows.get(i).place(TGParameterColumns.row(i, rows) + 1, TGParameterColumns.column(i, rows) * CELLS);
		}
	}

	// the rows show the values the plugin holds now (an undo, a redo); false once it no longer runs
	boolean follow() {
		Optional.of(this.editor).filter(TGPluginEditor::isOpen).ifPresent(this::show);
		return this.editor.isOpen();
	}

	private void show(TGPluginEditor open) {
		open.refresh();
		List<TGPluginParameterRow> rows = open.getRows();
		for (int i = 0; i < this.rows.size(); i++) {
			this.rows.get(i).show(rows.get(i));
		}
	}

	void dispose() {
		this.panel.dispose();
	}

	private UITableLayout layout() {
		return (UITableLayout) this.panel.getLayout();
	}

	private final class Row implements TGContinuousControl {

		private TGPluginParameterRow row;
		private final UILabel title;
		private final UIScale slider;
		private final UITextField value;
		private final UIButton set;

		Row(UIFactory factory, TGPluginParameterRow row) {
			this.title = factory.createLabel(panel);
			this.title.setText(row.getTitle() + ":");
			this.slider = factory.createHorizontalScale(panel);
			this.slider.setMinimum(0);
			this.slider.setMaximum(row.positions());
			this.slider.setIncrement(1);
			this.slider.addSelectionListener(new TGContinousControlSelectionListener(this, REST_MILLISECONDS));
			this.value = factory.createTextField(panel);
			this.set = TGDialogParts.button(factory, panel, 1, CELLS, "audio.sound-chain.set", this::typed);
			this.show(row);
		}

		// at that row of the table, its cells from the column after first
		void place(int number, int first) {
			layout().set(this.title, number, first + 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false);
			layout().set(this.slider, number, first + 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 1, 240f, null, null);
			layout().set(this.value, number, first + 3, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 80f, null, null);
			layout().set(this.set, number, first + 4, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, false, false, 1, 1, 80f, 25f, null);
		}

		// the row's value, on the slider and typed
		void show(TGPluginParameterRow row) {
			this.row = row;
			this.slider.setValue(row.position(row.getPlain()));
			this.value.setText(String.valueOf(row.getPlain()));
		}

		// the slider rests: its value is set
		public void doActionWhenStable() {
			double plain = this.row.plain(this.slider.getValue());
			this.value.setText(String.valueOf(plain));
			setter.set(this.row.getId(), plain);
		}

		public TGContext getContext() {
			return context;
		}

		// the value typed is set when it is inside the range; otherwise the field shows the slider's value again
		private void typed() {
			Optional<Double> plain = Optional.ofNullable(this.row.parse(this.value.getText()));
			plain.ifPresent(typed -> this.slider.setValue(this.row.position(typed)));
			plain.ifPresent(typed -> setter.set(this.row.getId(), typed));
			this.value.setText(String.valueOf(plain.orElse(this.row.plain(this.slider.getValue()))));
		}
	}
}
