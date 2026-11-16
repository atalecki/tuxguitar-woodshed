package app.tuxguitar.app.view.dialog.transport;

import java.util.function.Supplier;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.controller.TGViewContext;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.practice.TGPracticeWording;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UICheckBox;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UISpinner;
import app.tuxguitar.ui.widget.UIWindow;

/**
 * The form of a practice dialog (Transport > Interleaved Practice, Interleaved Click-Up): a modal window of numbered
 * rows, each a field after its label, a checkbox or a line of text, and last Start and Cancel. Start closes the window,
 * then runs the start action with the settings the form gives (a refusal shows as the app's error message).
 */
final class TGPracticeForm {

	private final TGViewContext context;
	private final UIFactory factory;
	private final UITableLayout layout;
	private final UIWindow window;
	private UIDropDownSelect<TGPracticeUnitKind> units;

	// the window, titled by the key's message, over the view's parent
	TGPracticeForm(TGViewContext context, String title) {
		this.context = context;
		this.factory = TGApplication.getInstance(context.getContext()).getFactory();
		this.layout = new UITableLayout();
		this.window = this.factory.createWindow(context.getAttribute(TGViewContext.ATTRIBUTE_PARENT), true, false);
		this.window.setLayout(this.layout);
		this.window.setText(TuxGuitar.getProperty(title));
	}

	// the unit kinds (a measure, a line, a page, a beat), the one given chosen
	UIDropDownSelect<TGPracticeUnitKind> units(int row, String key, TGPracticeUnitKind chosen) {
		this.units = this.dropDown(row, key);
		for (TGPracticeUnitKind kind : TGPracticeUnitKind.values()) {
			this.units.addItem(new UISelectItem<TGPracticeUnitKind>(TuxGuitar.getProperty("transport.practice.unit." + kind.getName()), kind));
		}
		this.units.setSelectedValue(chosen);
		return this.units;
	}

	// an empty drop-down list
	<T> UIDropDownSelect<T> dropDown(int row, String key) {
		UIDropDownSelect<T> dropDown = this.factory.createDropDownSelect(this.window);
		this.label(row, key, dropDown);
		return dropDown;
	}

	// a value from the least to the most: one outside comes to the nearer end
	UISpinner spinner(int row, String key, int value, int least, int most) {
		UISpinner spinner = this.createSpinner(value, least, most);
		this.label(row, key, spinner);
		return spinner;
	}

	// as spinner, for an end of a range of the units chosen (after units): its label names the kind the range is given
	// in when that is another ("Pool from (measures)" for beats), anew as another unit is chosen
	UISpinner rangeSpinner(int row, String key, int value, int least, int most) {
		UISpinner spinner = this.createSpinner(value, least, most);
		UILabel label = this.label(row, key, spinner);
		this.nameRange(label, key);
		this.units.addSelectionListener(event -> this.nameRange(label, key));
		return spinner;
	}

	private UISpinner createSpinner(int value, int least, int most) {
		UISpinner spinner = this.factory.createSpinner(this.window);
		limit(spinner, least, most);
		spinner.setValue(Math.max(least, Math.min(value, most)));
		return spinner;
	}

	// a label of another length lays the window out again
	private void nameRange(UILabel label, String key) {
		label.setText(TGPracticeWording.rangeField(key, this.units.getSelectedValue(), TuxGuitar::getProperty) + ":");
		this.window.layout();
	}

	// the spinner's values from the least to the most (the least not past the most), its value brought to the nearer end
	// when outside; set so that neither bound is ever past the other on the way
	static void limit(UISpinner spinner, int least, int most) {
		int value = spinner.getValue();
		spinner.setMinimum(0);
		spinner.setMaximum(most);
		spinner.setMinimum(least);
		spinner.setValue(Math.max(least, Math.min(value, most)));
	}

	// the control on the row, after its label
	private UILabel label(int row, String key, UIControl control) {
		UILabel label = this.factory.createLabel(this.window);
		label.setText(TuxGuitar.getProperty(key) + ":");
		this.layout.set(label, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
		this.layout.set(control, row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false);
		return label;
	}

	UICheckBox checkBox(int row, String key, boolean selected) {
		UICheckBox checkBox = this.factory.createCheckBox(this.window);
		checkBox.setText(TuxGuitar.getProperty(key));
		checkBox.setSelected(selected);
		this.layout.set(checkBox, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 2);
		return checkBox;
	}

	// a line of text across the row, empty at first
	UILabel text(int row) {
		UILabel text = this.factory.createLabel(this.window);
		this.layout.set(text, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 2);
		return text;
	}

	// Start (the default button: it runs the action, with the settings as the attribute, once the window has closed) and
	// Cancel
	void buttons(int row, String action, String attribute, Supplier<Object> settings) {
		UITableLayout buttonsLayout = new UITableLayout(0f);
		UIPanel buttons = this.factory.createPanel(this.window, false);
		buttons.setLayout(buttonsLayout);
		this.layout.set(buttons, row, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, true, 1, 2);

		UIButton start = this.factory.createButton(buttons);
		start.setText(TuxGuitar.getProperty("transport.practice.start"));
		start.setDefaultButton();
		start.addSelectionListener(event -> this.start(action, attribute, settings.get()));
		buttonsLayout.set(start, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 80f, 25f, null);

		UIButton cancel = this.factory.createButton(buttons);
		cancel.setText(TuxGuitar.getProperty("cancel"));
		cancel.addSelectionListener(event -> this.window.dispose());
		buttonsLayout.set(cancel, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 80f, 25f, null);
		buttonsLayout.set(cancel, UITableLayout.MARGIN_RIGHT, 0f);
	}

	// the practice as set starts (or is refused, saying why); the window closes
	private void start(String action, String attribute, Object settings) {
		TGActionProcessor processor = new TGActionProcessor(this.context.getContext(), action);
		processor.setAttribute(attribute, settings);
		this.window.dispose();
		processor.process();
	}

	// the window laid out again, as a text of another length needs
	void layout() {
		this.window.layout();
	}

	void open() {
		TGDialogUtil.openDialog(this.window, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}
}
