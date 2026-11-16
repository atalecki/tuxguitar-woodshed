package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.app.view.controller.TGViewContext;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.transport.TGTransportPractiseAction;
import app.tuxguitar.practice.TGPracticeGrouping;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.ui.widget.UICheckBox;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UISpinner;

/**
 * Transport > Interleaved Practice: what a turn plays (measures, lines, pages or beats), the pool (from, to), the units
 * per block (a turn plays a block of so many units in a row: from 1 to as many as the pool holds, labelled by the unit,
 * "Measures per block"; only this dialog has it, not Interleaved Click-Up), the number of turns, the fixed groups
 * (measures per line, lines per page), the two rules and whether each turn has a Count In (interleaved practice's own,
 * whether or not Transport > Count In is on); Start runs TGTransportPractiseAction with them (a refusal shows as the
 * app's error message). It opens with the settings of the last start. The pool's fields cannot go past the song's last
 * unit of the kind chosen (for beats, its last measure: a pool of beats is given in measures, and the pool's labels say
 * so). With the interval clock on, the session is timed (a turn's minutes, a break's seconds, the session's minutes, at
 * least a turn's, and a Count In before every pass or not) instead of counting its turns: the fields of the one not in
 * use are greyed out, and "Count In before every pass" too without a Count In before each turn.
 */
public class TGPracticeDialog {

	private static final int MOST = 9999;
	private static final int MOST_TURN_MINUTES = 60;
	private static final int MOST_BREAK_SECONDS = 600;
	private static final int MOST_SESSION_MINUTES = 600;

	private final TGViewContext context;
	private TGPracticeForm form;
	private UIDropDownSelect<TGPracticeUnitKind> unit;
	private UISpinner from;
	private UISpinner to;
	private UISpinner unitsPerBlock;
	private UISpinner turns;
	private UISpinner measuresPerLine;
	private UISpinner linesPerPage;
	private UICheckBox noConsecutiveRepeats;
	private UICheckBox completeCycle;
	private UICheckBox countInEachTurn;
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
		this.form = new TGPracticeForm(this.context, "transport.practice.title");
		this.unit = this.form.units(1, "transport.practice.unit", last.getUnit());
		this.from = this.form.rangeSpinner(2, "transport.practice.pool-from", last.getPoolFrom(), 0, MOST);
		this.to = this.form.rangeSpinner(3, "transport.practice.pool-to", last.getPoolTo(), 0, MOST);
		this.unitsPerBlock = this.form.blockSpinner(4, last.getUnitsPerBlock(), 1, MOST);
		this.turns = this.spinner(5, "transport.practice.turns", last.getTurns());
		this.measuresPerLine = this.spinner(6, "transport.practice.measures-per-line", last.getMeasuresPerLine());
		this.linesPerPage = this.spinner(7, "transport.practice.lines-per-page", last.getLinesPerPage());
		this.noConsecutiveRepeats = this.form.checkBox(8, "transport.practice.no-consecutive-repeats", last.isNoConsecutiveRepeats());
		this.completeCycle = this.form.checkBox(9, "transport.practice.complete-cycle", last.isCompleteCycle());
		this.countInEachTurn = this.form.checkBox(10, "transport.practice.count-in-each-turn", last.isCountInEachTurn());
		this.intervalClock = this.form.checkBox(11, "transport.practice.interval-clock", last.isIntervalClock());
		this.turnMinutes = this.form.spinner(12, "transport.practice.turn-minutes", last.getTurnMinutes(), 1, MOST_TURN_MINUTES);
		this.breakSeconds = this.form.spinner(13, "transport.practice.break-seconds", last.getBreakSeconds(), 0, MOST_BREAK_SECONDS);
		this.sessionMinutes = this.form.spinner(14, "transport.practice.session-minutes", last.getSessionMinutes(), 1, MOST_SESSION_MINUTES);
		this.countInEveryPass = this.form.checkBox(15, "transport.practice.count-in-every-pass", last.isCountInEveryPass());
		this.form.buttons(16, TGTransportPractiseAction.NAME, TGTransportPractiseAction.ATTRIBUTE_SETTINGS, this::settings);
		this.limitPool();
		this.unit.addSelectionListener(event -> this.limitPool());
		this.measuresPerLine.addSelectionListener(event -> this.limitPool());
		this.linesPerPage.addSelectionListener(event -> this.limitPool());
		this.from.addSelectionListener(event -> this.limitBlock());
		this.to.addSelectionListener(event -> this.limitBlock());
		this.limitSession();
		this.turnMinutes.addSelectionListener(event -> this.limitSession());
		this.timeTheSession();
		this.intervalClock.addSelectionListener(event -> this.timeTheSession());
		this.countInEachTurn.addSelectionListener(event -> this.timeTheSession());
		this.form.open();
	}

	// from 0, so that the session's own refusal says what is wrong with a 0
	private UISpinner spinner(int row, String key, int value) {
		return this.form.spinner(row, key, value, 0, MOST);
	}

	// the pool cannot reach past the song's last unit of the kind chosen, as the group sizes set make it; nor a block
	// hold more than the pool
	private void limitPool() {
		int most = TGPracticeGrouping.poolLimit(this.unit.getSelectedValue(), this.measuresPerLine.getValue(), this.linesPerPage.getValue(), this.song().countMeasureHeaders());
		TGPracticeForm.limit(this.from, 0, most);
		TGPracticeForm.limit(this.to, 0, most);
		this.limitBlock();
	}

	// a block holds from 1 unit to as many as the pool, as the fields set so far make it
	private void limitBlock() {
		int most = TGPracticeGrouping.blockLimit(this.unit.getSelectedValue(), this.measuresPerLine.getValue(), this.linesPerPage.getValue(), this.from.getValue(),
			this.to.getValue(), this.song());
		TGPracticeForm.limit(this.unitsPerBlock, 1, most);
	}

	private TGSong song() {
		return TGDocumentManager.getInstance(this.context.getContext()).getSong();
	}

	// a session lasts at least a turn's minutes
	private void limitSession() {
		TGPracticeForm.limit(this.sessionMinutes, this.turnMinutes.getValue(), MOST_SESSION_MINUTES);
	}

	// the interval clock's fields in use while it is on, the number of turns while it is off; a Count In before every
	// pass only with one before each turn
	private void timeTheSession() {
		boolean timed = this.intervalClock.isSelected();
		this.turns.setEnabled(!timed);
		this.turnMinutes.setEnabled(timed);
		this.breakSeconds.setEnabled(timed);
		this.sessionMinutes.setEnabled(timed);
		this.countInEveryPass.setEnabled(timed && this.countInEachTurn.isSelected());
	}

	private TGPracticeSettings settings() {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setUnit(this.unit.getSelectedValue());
		settings.setUnitsPerBlock(this.unitsPerBlock.getValue());
		settings.setPool(this.from.getValue(), this.to.getValue());
		settings.setTurns(this.turns.getValue());
		settings.setMeasuresPerLine(this.measuresPerLine.getValue());
		settings.setLinesPerPage(this.linesPerPage.getValue());
		settings.setNoConsecutiveRepeats(this.noConsecutiveRepeats.isSelected());
		settings.setCompleteCycle(this.completeCycle.isSelected());
		settings.setCountInEachTurn(this.countInEachTurn.isSelected());
		settings.setIntervalClock(this.intervalClock.isSelected());
		settings.setTurnMinutes(this.turnMinutes.getValue());
		settings.setBreakSeconds(this.breakSeconds.getValue());
		settings.setSessionMinutes(this.sessionMinutes.getValue());
		settings.setCountInEveryPass(this.countInEveryPass.isSelected());
		return settings;
	}
}
