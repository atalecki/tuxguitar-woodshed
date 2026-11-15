package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.controller.TGViewContext;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.editor.action.transport.TGTransportPractiseAction;
import app.tuxguitar.practice.TGPracticeGrouping;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnitKind;
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
 * Transport > Interleaved Practice: what a turn plays (measures, lines or pages), the pool (from, to), the number of
 * turns, the fixed groups (measures per line, lines per page) and the two rules; Start runs TGTransportPractiseAction
 * with them (a refusal shows as the app's error message). It opens with the settings of the last start. The pool's
 * fields cannot go past the song's last unit of the kind chosen. With the interval clock on, the session is timed (a
 * turn's minutes, a break's seconds, the session's minutes, at least a turn's, and a Count In before every pass or not)
 * instead of counting its turns: the fields of the one not in use are greyed out.
 */
public class TGPracticeDialog {

	private static final int MOST = 9999;
	private static final int MOST_TURN_MINUTES = 60;
	private static final int MOST_BREAK_SECONDS = 600;
	private static final int MOST_SESSION_MINUTES = 600;

	private final TGViewContext context;
	private UIFactory factory;
	private UITableLayout layout;
	private UIWindow dialog;
	private UIDropDownSelect<TGPracticeUnitKind> unit;
	private UISpinner from;
	private UISpinner to;
	private UISpinner turns;
	private UISpinner measuresPerLine;
	private UISpinner linesPerPage;
	private UICheckBox noConsecutiveRepeats;
	private UICheckBox completeCycle;
	private UICheckBox intervalClock;
	private UISpinner turnMinutes;
	private UISpinner breakSeconds;
	private UISpinner sessionMinutes;
	private UICheckBox countInEveryPass;

	public TGPracticeDialog(TGViewContext context) {
		this.context = context;
	}

	public void show() {
		TGPracticeSettings last = TGTransportPractiseAction.getLastSettings(this.context.getContext());
		UIWindow parent = this.context.getAttribute(TGViewContext.ATTRIBUTE_PARENT);
		this.factory = TGApplication.getInstance(this.context.getContext()).getFactory();
		this.layout = new UITableLayout();
		this.dialog = this.factory.createWindow(parent, true, false);
		this.dialog.setLayout(this.layout);
		this.dialog.setText(TuxGuitar.getProperty("transport.practice.title"));

		this.unit = this.factory.createDropDownSelect(this.dialog);
		this.label(1, "transport.practice.unit", this.unit);
		this.addUnits();
		this.unit.setSelectedValue(last.getUnit());
		this.from = this.spinner(2, "transport.practice.pool-from", last.getPoolFrom());
		this.to = this.spinner(3, "transport.practice.pool-to", last.getPoolTo());
		this.turns = this.spinner(4, "transport.practice.turns", last.getTurns());
		this.measuresPerLine = this.spinner(5, "transport.practice.measures-per-line", last.getMeasuresPerLine());
		this.linesPerPage = this.spinner(6, "transport.practice.lines-per-page", last.getLinesPerPage());
		this.noConsecutiveRepeats = this.checkBox(7, "transport.practice.no-consecutive-repeats", last.isNoConsecutiveRepeats());
		this.completeCycle = this.checkBox(8, "transport.practice.complete-cycle", last.isCompleteCycle());
		this.intervalClock = this.checkBox(9, "transport.practice.interval-clock", last.isIntervalClock());
		this.turnMinutes = this.spinner(10, "transport.practice.turn-minutes", last.getTurnMinutes(), 1, MOST_TURN_MINUTES);
		this.breakSeconds = this.spinner(11, "transport.practice.break-seconds", last.getBreakSeconds(), 0, MOST_BREAK_SECONDS);
		this.sessionMinutes = this.spinner(12, "transport.practice.session-minutes", last.getSessionMinutes(), 1, MOST_SESSION_MINUTES);
		this.countInEveryPass = this.checkBox(13, "transport.practice.count-in-every-pass", last.isCountInEveryPass());
		this.buttons(14);
		this.limitPool();
		this.unit.addSelectionListener(event -> this.limitPool());
		this.measuresPerLine.addSelectionListener(event -> this.limitPool());
		this.linesPerPage.addSelectionListener(event -> this.limitPool());
		this.limitSession();
		this.turnMinutes.addSelectionListener(event -> this.limitSession());
		this.timeTheSession();
		this.intervalClock.addSelectionListener(event -> this.timeTheSession());

		TGDialogUtil.openDialog(this.dialog, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}

	private void addUnits() {
		for (TGPracticeUnitKind kind : TGPracticeUnitKind.values()) {
			this.unit.addItem(new UISelectItem<TGPracticeUnitKind>(TuxGuitar.getProperty("transport.practice.unit." + kind.getName()), kind));
		}
	}

	// the control on the row, after its label
	private void label(int row, String key, UIControl control) {
		UILabel label = this.factory.createLabel(this.dialog);
		label.setText(TuxGuitar.getProperty(key) + ":");
		this.layout.set(label, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
		this.layout.set(control, row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false);
	}

	// from 0, so that the session's own refusal says what is wrong with a 0
	private UISpinner spinner(int row, String key, int value) {
		return this.spinner(row, key, value, 0, MOST);
	}

	// a value from the least to the most: one outside comes to the nearer end
	private UISpinner spinner(int row, String key, int value, int least, int most) {
		UISpinner spinner = this.factory.createSpinner(this.dialog);
		spinner.setMinimum(least);
		spinner.setMaximum(most);
		spinner.setValue(Math.max(least, Math.min(value, most)));
		this.label(row, key, spinner);
		return spinner;
	}

	// the pool cannot reach past the song's last unit of the kind chosen, as the group sizes set make it
	private void limitPool() {
		int songMeasures = TGDocumentManager.getInstance(this.context.getContext()).getSong().countMeasureHeaders();
		int most = TGPracticeGrouping.poolLimit(this.unit.getSelectedValue(), this.measuresPerLine.getValue(), this.linesPerPage.getValue(), songMeasures);
		limit(this.from, most);
		limit(this.to, most);
	}

	private static void limit(UISpinner spinner, int most) {
		spinner.setMaximum(most);
		spinner.setValue(Math.min(spinner.getValue(), most));
	}

	// a session lasts at least a turn's minutes
	private void limitSession() {
		this.sessionMinutes.setMinimum(this.turnMinutes.getValue());
		this.sessionMinutes.setValue(Math.max(this.sessionMinutes.getValue(), this.turnMinutes.getValue()));
	}

	// the interval clock's fields in use while it is on, the number of turns while it is off
	private void timeTheSession() {
		boolean timed = this.intervalClock.isSelected();
		this.turns.setEnabled(!timed);
		this.turnMinutes.setEnabled(timed);
		this.breakSeconds.setEnabled(timed);
		this.sessionMinutes.setEnabled(timed);
		this.countInEveryPass.setEnabled(timed);
	}

	private UICheckBox checkBox(int row, String key, boolean selected) {
		UICheckBox checkBox = this.factory.createCheckBox(this.dialog);
		checkBox.setText(TuxGuitar.getProperty(key));
		checkBox.setSelected(selected);
		this.layout.set(checkBox, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 2);
		return checkBox;
	}

	private void buttons(int row) {
		UITableLayout buttonsLayout = new UITableLayout(0f);
		UIPanel buttons = this.factory.createPanel(this.dialog, false);
		buttons.setLayout(buttonsLayout);
		this.layout.set(buttons, row, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, true, 1, 2);

		UIButton start = this.factory.createButton(buttons);
		start.setText(TuxGuitar.getProperty("transport.practice.start"));
		start.setDefaultButton();
		start.addSelectionListener(event -> this.start());
		buttonsLayout.set(start, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 80f, 25f, null);

		UIButton cancel = this.factory.createButton(buttons);
		cancel.setText(TuxGuitar.getProperty("cancel"));
		cancel.addSelectionListener(event -> this.dialog.dispose());
		buttonsLayout.set(cancel, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, 80f, 25f, null);
		buttonsLayout.set(cancel, UITableLayout.MARGIN_RIGHT, 0f);
	}

	// the session as set starts (or is refused, saying why); the dialog closes
	private void start() {
		TGActionProcessor processor = new TGActionProcessor(this.context.getContext(), TGTransportPractiseAction.NAME);
		processor.setAttribute(TGTransportPractiseAction.ATTRIBUTE_SETTINGS, this.settings());
		this.dialog.dispose();
		processor.process();
	}

	private TGPracticeSettings settings() {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setUnit(this.unit.getSelectedValue());
		settings.setPool(this.from.getValue(), this.to.getValue());
		settings.setTurns(this.turns.getValue());
		settings.setMeasuresPerLine(this.measuresPerLine.getValue());
		settings.setLinesPerPage(this.linesPerPage.getValue());
		settings.setNoConsecutiveRepeats(this.noConsecutiveRepeats.isSelected());
		settings.setCompleteCycle(this.completeCycle.isSelected());
		settings.setIntervalClock(this.intervalClock.isSelected());
		settings.setTurnMinutes(this.turnMinutes.getValue());
		settings.setBreakSeconds(this.breakSeconds.getValue());
		settings.setSessionMinutes(this.sessionMinutes.getValue());
		settings.setCountInEveryPass(this.countInEveryPass.isSelected());
		return settings;
	}
}
