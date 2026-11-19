package app.tuxguitar.audio.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.TGSoundChainRows.Control;
import app.tuxguitar.audio.TGSoundMode;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UIToggleButton;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The Track panel's SOUNDS heading, its [MIDI | VST3] switch (the sound the track has pressed; the other switches to
 * it, one undoable change) and the instrument: on MIDI the General MIDI program (in TuxGuitar's language; a percussion
 * channel's numbered), on VST3 the default instrument of the track's kind ("Default instrument (what it plays)") or a
 * VST3 instrument the scan found, and Keep in song. A pick other than the one shown changes the track. An audio track
 * (its takes) and no track show neither switch nor instrument. What shows and what is picked is the rows' to say
 * (TGSoundChainRows); glue only, on the UI thread.
 */
final class TGTrackSoundSection {

	private final TGSoundChainCommands commands;
	private final UILabel heading;
	private final UIPanel switches;
	private final UIToggleButton midi;
	private final UIToggleButton vst3;
	private final UIPanel instrument;
	private final UILabel instrumentLabel;
	private final UIDropDownSelect<Short> programs;
	private final UIDropDownSelect<Integer> instruments;
	private final UIButton keep;
	// the VST3 instruments offered (null: the default), the one picked, and the program the channel has
	private List<TGVst3Plugin> offered = new ArrayList<TGVst3Plugin>();
	private int picked = -1;
	private int program = -1;

	TGTrackSoundSection(UIFactory factory, UIPanel parent, int row, TGSoundChainCommands commands, TGSoundChainButtons buttons) {
		this.commands = commands;
		UITableLayout layout = (UITableLayout) parent.getLayout();
		this.heading = factory.createLabel(parent);
		layout.set(this.heading, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 1, null, null, 2f);
		this.switches = this.panel(factory, parent, row + 1);
		this.midi = this.toggle(factory, 1, this.commands::toMidi);
		this.vst3 = this.toggle(factory, 2, this.commands::toVst3);
		this.instrument = this.panel(factory, parent, row + 2);
		this.instrumentLabel = factory.createLabel(this.instrument);
		this.place(this.instrumentLabel, 1, 1, false);
		this.programs = factory.<Short>createDropDownSelect(this.instrument);
		this.place(this.programs, 1, 2, true);
		this.programs.addSelectionListener(event -> this.pickProgram());
		this.instruments = factory.<Integer>createDropDownSelect(this.instrument);
		this.place(this.instruments, 1, 2, true);
		this.instruments.addSelectionListener(event -> this.pickInstrument());
		this.keep = buttons.put(Control.KEEP, factory.createButton(this.instrument));
		this.keep.addSelectionListener(event -> this.commands.keep());
		((UITableLayout) this.instrument.getLayout()).set(this.keep, 2, 2, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, false, false);
	}

	private UIPanel panel(UIFactory factory, UIPanel parent, int row) {
		UIPanel panel = factory.createPanel(parent, false);
		panel.setLayout(new UITableLayout(0f));
		panel.getLayout().set(UITableLayout.IGNORE_INVISIBLE, true);
		((UITableLayout) parent.getLayout()).set(panel, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false);
		return panel;
	}

	private UIToggleButton toggle(UIFactory factory, int column, Runnable switched) {
		UIToggleButton toggle = factory.createToggleButton(this.switches);
		toggle.addSelectionListener(event -> switched.run());
		((UITableLayout) this.switches.getLayout()).set(toggle, 1, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false);
		return toggle;
	}

	private void place(UIControl control, int row, int column, boolean grows) {
		((UITableLayout) this.instrument.getLayout()).set(control, row, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, grows, false);
	}

	// the texts in the language chosen
	void retell() {
		this.heading.setText(TuxGuitar.getProperty("audio.track.sounds"));
		Map.of(this.midi, "audio.track.midi", this.vst3, "audio.track.vst3").forEach(TGTrackSoundSection::say);
		this.keep.setText(TuxGuitar.getProperty("audio.sound-chain.keep"));
	}

	// a switch's text, and its tooltip saying what it plays (the key's ".tip")
	private static void say(UIToggleButton toggle, String key) {
		toggle.setText(TuxGuitar.getProperty(key));
		toggle.setToolTipText(TuxGuitar.getProperty(key + ".tip"));
	}

	// the track's sound as the rows read it, the VST3 instruments the scan found offered
	void show(TGSoundChainRows rows, List<TGVst3Plugin> scanned) {
		TGSoundMode mode = rows.getMode();
		this.switches.setVisible(rows.isSwitched());
		this.instrument.setVisible(rows.isSwitched());
		this.midi.setSelected(mode == TGSoundMode.MIDI);
		this.vst3.setSelected(mode == TGSoundMode.VST3);
		this.programs.setVisible(mode == TGSoundMode.MIDI);
		this.instruments.setVisible(mode == TGSoundMode.VST3);
		this.keep.setVisible(mode == TGSoundMode.VST3);
		this.instrumentLabel.setText(TuxGuitar.getProperty(mode == TGSoundMode.MIDI ? "audio.track.program" : "audio.track.instrument") + ":");
		this.offerPrograms(rows);
		this.offerInstruments(rows, scanned);
	}

	// each switch enabled only to the other sound (the rows say)
	void apply(TGSoundChainRows rows) {
		this.midi.setEnabled(rows.isEnabled(Control.TO_MIDI, -1, null, null));
		this.vst3.setEnabled(rows.isEnabled(Control.TO_VST3, -1, null, null));
	}

	private void offerPrograms(TGSoundChainRows rows) {
		List<String> names = rows.programChoices(TuxGuitar.getProperty("instrument.program"));
		this.program = rows.getProgram();
		this.programs.setIgnoreEvents(true);
		this.programs.removeItems();
		IntStream.range(0, names.size()).forEach(program -> this.programs.addItem(new UISelectItem<Short>(names.get(program), (short) program)));
		this.programs.setSelectedValue((short) this.program);
		this.programs.setIgnoreEvents(false);
	}

	private void offerInstruments(TGSoundChainRows rows, List<TGVst3Plugin> scanned) {
		String plays = rows.defaultName(TuxGuitar.getProperty("audio.track.general-midi"));
		String byDefault = TuxGuitar.getProperty("audio.track.default-instrument", new String[] {plays});
		this.offered = rows.instrumentChoices(scanned);
		this.picked = rows.pickedInstrument(this.offered);
		this.instruments.setIgnoreEvents(true);
		this.instruments.removeItems();
		IntStream.range(0, this.offered.size()).forEach(choice -> this.instruments.addItem(new UISelectItem<Integer>(this.name(choice, byDefault), choice)));
		this.instruments.setSelectedValue(this.picked);
		this.instruments.setIgnoreEvents(false);
	}

	private String name(int choice, String byDefault) {
		return Optional.ofNullable(this.offered.get(choice)).map(TGVst3Plugin::getName).orElse(byDefault);
	}

	// another program picked: the channel's
	private void pickProgram() {
		Short picked = this.programs.getSelectedValue();
		if (picked != this.program) {
			this.commands.program(picked);
		}
	}

	// another instrument picked: the source (the default, or the VST3 instrument)
	private void pickInstrument() {
		int picked = this.instruments.getSelectedValue();
		if (picked != this.picked) {
			Optional.ofNullable(this.offered.get(picked)).ifPresentOrElse(this.commands::useAsSource, this.commands::useDefault);
		}
	}
}
