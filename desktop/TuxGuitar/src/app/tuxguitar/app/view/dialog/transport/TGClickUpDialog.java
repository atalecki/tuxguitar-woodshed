package app.tuxguitar.app.view.dialog.transport;

import java.util.Locale;
import java.util.OptionalInt;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.view.controller.TGViewContext;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.transport.TGTransportClickUpAction;
import app.tuxguitar.practice.TGClickUpLadder;
import app.tuxguitar.practice.TGClickUpSettings;
import app.tuxguitar.practice.TGClickUpTempoMode;
import app.tuxguitar.practice.TGPracticeGrouping;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.ui.widget.UICheckBox;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UISpinner;

/**
 * Transport > Interleaved Click-Up: what a section is (a measure, a line, a page), the passage (from, to), the fixed
 * groups, how the tempo ladder is given (percent of the written tempo, or bpm), its start, goal and increment, the passes
 * of each step, whether each step has a Count In (the click-up's own, whether or not Transport > Count In is on) and,
 * if so, whether one comes before every pass (greyed out without one before each step); under them, how many steps that
 * makes. Start runs
 * TGTransportClickUpAction with them (a refusal shows as the app's error message). It opens with the settings of the
 * last start. The fields stop a value the click-up could not play: the passage lies in the song's units of the kind
 * chosen, from not past to; the ladder starts at 1 or more, its goal not below its start, its increment and the passes
 * 1 or more; the group sizes 1 or more, each greyed out unless the kind is made of it. Choosing another tempo unit gives
 * the ladder anew in it (TGClickUpSettings.setTempo).
 */
public class TGClickUpDialog {

	private static final int MOST = 9999;

	private final TGViewContext context;
	private TGPracticeForm form;
	private UIDropDownSelect<TGPracticeUnitKind> unit;
	private UISpinner from;
	private UISpinner to;
	private UISpinner measuresPerLine;
	private UISpinner linesPerPage;
	private UIDropDownSelect<TGClickUpTempoMode> tempoMode;
	private UISpinner start;
	private UISpinner goal;
	private UISpinner increment;
	private UISpinner passes;
	private UICheckBox countInEachStep;
	private UICheckBox countInEveryPass;
	private UILabel steps;

	public TGClickUpDialog(TGViewContext context) {
		this.context = context;
	}

	public void show() {
		TGClickUpSettings last = TGTransportClickUpAction.getLastSettings(this.context.getContext());
		this.form = new TGPracticeForm(this.context, "transport.click-up.title");
		this.unit = this.form.units(1, "transport.click-up.unit", last.getUnit());
		this.from = this.spinner(2, "transport.click-up.passage-from", last.getPassageFrom());
		this.to = this.spinner(3, "transport.click-up.passage-to", last.getPassageTo());
		this.measuresPerLine = this.spinner(4, "transport.practice.measures-per-line", last.getMeasuresPerLine());
		this.linesPerPage = this.spinner(5, "transport.practice.lines-per-page", last.getLinesPerPage());
		this.tempoMode = this.tempoModes(6, last.getTempoMode());
		this.start = this.spinner(7, "transport.click-up.start", last.getStart());
		this.goal = this.spinner(8, "transport.click-up.goal", last.getGoal());
		this.increment = this.spinner(9, "transport.click-up.increment", last.getIncrement());
		this.passes = this.spinner(10, "transport.click-up.passes", last.getPasses());
		this.countInEachStep = this.form.checkBox(11, "transport.click-up.count-in-each-step", last.isCountInEachStep());
		this.countInEveryPass = this.form.checkBox(12, "transport.practice.count-in-every-pass", last.isCountInEveryPass());
		this.steps = this.form.text(13);
		this.form.buttons(14, TGTransportClickUpAction.NAME, TGTransportClickUpAction.ATTRIBUTE_SETTINGS, this::settings);
		this.limit();
		this.unit.addSelectionListener(event -> this.limit());
		this.countInEachStep.addSelectionListener(event -> this.limit());
		this.tempoMode.addSelectionListener(event -> this.giveTempoAnew());
		for (UISpinner field : new UISpinner[] {this.from, this.to, this.measuresPerLine, this.linesPerPage, this.start, this.goal, this.increment, this.passes}) {
			field.addSelectionListener(event -> this.limit());
		}
		this.form.open();
	}

	// from 1, which every field of it needs
	private UISpinner spinner(int row, String key, int value) {
		return this.form.spinner(row, key, value, 1, MOST);
	}

	private UIDropDownSelect<TGClickUpTempoMode> tempoModes(int row, TGClickUpTempoMode chosen) {
		UIDropDownSelect<TGClickUpTempoMode> modes = this.form.dropDown(row, "transport.click-up.tempo-mode");
		for (TGClickUpTempoMode mode : TGClickUpTempoMode.values()) {
			modes.addItem(new UISelectItem<TGClickUpTempoMode>(TuxGuitar.getProperty(tempoKey("transport.click-up.tempo-mode.", mode)), mode));
		}
		modes.setSelectedValue(chosen);
		return modes;
	}

	// the key's prefix, then the tempo mode's name: "percent", "bpm"
	static String tempoKey(String prefix, TGClickUpTempoMode mode) {
		return prefix + mode.name().toLowerCase(Locale.ROOT);
	}

	private TGSong song() {
		return TGDocumentManager.getInstance(this.context.getContext()).getSong();
	}

	// each field's bounds as the others set them, the group sizes in use, and the steps the settings make
	private void limit() {
		TGPracticeUnitKind kind = this.unit.getSelectedValue();
		int units = TGPracticeGrouping.poolLimit(kind, this.measuresPerLine.getValue(), this.linesPerPage.getValue(), song().countMeasureHeaders());
		TGPracticeForm.limit(this.from, 1, units);
		TGPracticeForm.limit(this.to, this.from.getValue(), units);
		TGPracticeForm.limit(this.goal, this.start.getValue(), MOST);
		this.measuresPerLine.setEnabled(kind != TGPracticeUnitKind.MEASURE);
		this.linesPerPage.setEnabled(kind == TGPracticeUnitKind.PAGE);
		this.countInEveryPass.setEnabled(this.countInEachStep.isSelected());
		this.steps.setText(steps(this.settings(), song()));
		this.form.layout();
	}

	// "23 steps" ("1 step"), or why the settings cannot be played
	private static String steps(TGClickUpSettings settings, TGSong song) {
		try {
			return steps(TGClickUpLadder.of(settings, song).getSteps().size());
		} catch (TGPracticeRefusal refusal) {
			return refusal.getMessage();
		}
	}

	private static String steps(int count) {
		return TuxGuitar.getProperty((count == 1 ? "transport.click-up.one-step" : "transport.click-up.steps"), new String[] {String.valueOf(count)});
	}

	// the ladder given anew in the unit chosen, against the written tempo of the passage's first measure (settings that
	// cannot be played keep it as it is: the read-out says why)
	private void giveTempoAnew() {
		TGClickUpSettings settings = this.settings();
		writtenBpm(settings, song()).ifPresent(written -> this.giveTempo(settings, written));
		this.limit();
	}

	private static OptionalInt writtenBpm(TGClickUpSettings settings, TGSong song) {
		try {
			return OptionalInt.of(TGClickUpLadder.of(settings, song).getWrittenBpm());
		} catch (TGPracticeRefusal refusal) {
			return OptionalInt.empty();
		}
	}

	// the goal's least lowered first, so that it can take a goal below the start before
	private void giveTempo(TGClickUpSettings settings, int writtenBpm) {
		settings.setTempo(settings.getTempoMode(), writtenBpm);
		this.start.setValue(settings.getStart());
		TGPracticeForm.limit(this.goal, settings.getStart(), MOST);
		this.goal.setValue(settings.getGoal());
		this.increment.setValue(settings.getIncrement());
	}

	private TGClickUpSettings settings() {
		TGClickUpSettings settings = new TGClickUpSettings();
		settings.setUnit(this.unit.getSelectedValue());
		settings.setPassageFrom(this.from.getValue());
		settings.setPassageTo(this.to.getValue());
		settings.setMeasuresPerLine(this.measuresPerLine.getValue());
		settings.setLinesPerPage(this.linesPerPage.getValue());
		settings.setTempoMode(this.tempoMode.getSelectedValue());
		settings.setStart(this.start.getValue());
		settings.setGoal(this.goal.getValue());
		settings.setIncrement(this.increment.getValue());
		settings.setPasses(this.passes.getValue());
		settings.setCountInEachStep(this.countInEachStep.isSelected());
		settings.setCountInEveryPass(this.countInEveryPass.isSelected());
		return settings;
	}
}
