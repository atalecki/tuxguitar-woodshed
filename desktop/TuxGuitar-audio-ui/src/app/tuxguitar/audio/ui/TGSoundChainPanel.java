package app.tuxguitar.audio.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.keybindings.KeyBindingActionManager;
import app.tuxguitar.app.system.language.TGLanguageEvent;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.main.TGBottomDock;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.audio.TGSoundChainFollow;
import app.tuxguitar.audio.TGSoundChainFollow.Key;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.TGSoundChainRows.Control;
import app.tuxguitar.editor.TGEditorManager;
import app.tuxguitar.event.TGEvent;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIContainer;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UIToggleButton;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The Sound Chain panel, docked at the foot of the main window (TGBottomDock): the caret's track's sound chain as a
 * line of buttons in the order the sound flows (the track's name, the source, then each effect), one picked, and under
 * it the actions on the picked one and on the chain, as the Sound Chain dialog has them (Edit, Disable/Enable, Move
 * left, Remove, Add effect, Use as source, Default instrument, Keep in song), then "Sound Chain..." opening the dialog
 * (Scan again and the scan report are there) and, while the MIDI Port is not the TuxGuitar Synthesizer, a quiet line
 * saying the chains are not heard. Each line wraps when the window is narrow (TGSoundChainFlowLayout). It reads the
 * chain again only when what it shows changed (TGSoundChainFollow), not on each move of the caret; what is enabled is
 * decided by the rows (TGSoundChainButtons), the changes run through TGSoundChainCommands. Shown or hidden by View >
 * Sound Chain (F8), the choice kept in its own config at once. Glue only; on the UI thread.
 */
final class TGSoundChainPanel implements TGEventListener {

	static final String VISIBLE = "visible";

	private final TGContext context;
	private final UIFactory factory;
	private final TGConfigManager config;
	private final TGSoundChainButtons buttons = new TGSoundChainButtons();
	private final TGSoundChainCommands commands;
	// each text (re)set in the language chosen
	private final List<Runnable> texts = new ArrayList<Runnable>();
	private final List<UIToggleButton> chips = new ArrayList<UIToggleButton>();
	private UIPanel panel;
	private UIPanel chain;
	private UILabel title;
	private UILabel note;
	private UIDropDownSelect<TGVst3Plugin> effects;
	private UIDropDownSelect<TGVst3Plugin> instruments;
	// the plugins offered: the last scan's
	private List<TGVst3Plugin> offered;
	private Key shown = Key.NONE;
	private TGSoundChainRows rows = TGSoundChainRows.NONE;
	private int picked = -1;

	TGSoundChainPanel(TGContext context, TGConfigManager config) {
		this.context = context;
		this.factory = TGApplication.getInstance(context).getFactory();
		this.config = config;
		this.commands = new TGSoundChainCommands(context, () -> this.shown.getTrack(), () -> this.later(this::follow));
	}

	// in the dock's slot; shown as last chosen (the first time: shown) unless the audio it edits is off
	void create(boolean audio) {
		UIPanel slot = TGBottomDock.getInstance(this.context).getSlot();
		this.panel = this.factory.createPanel(slot, false);
		this.panel.setLayout(new UITableLayout(0f));
		((UITableLayout) slot.getLayout()).set(this.panel, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
		this.chain = this.line(1);
		this.title = this.factory.createLabel(this.chain);
		this.actions(this.line(2));
		this.texts.forEach(Runnable::run);
		this.panel.setVisible(audio && this.config.getBooleanValue(VISIBLE, true));
		TGEditorManager.getInstance(this.context).addUpdateListener(this);
		TuxGuitar.getInstance().getLanguageManager().addLoader(this);
		this.onUpdate();
	}

	private UIPanel line(int row) {
		UIPanel line = this.factory.createPanel(this.panel, false);
		line.setLayout(new TGSoundChainFlowLayout(() -> this.later(TGBottomDock.getInstance(this.context)::layout)));
		((UITableLayout) this.panel.getLayout()).set(line, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 2f);
		return line;
	}

	// the groups of the action line: on the picked one, the effects, the instruments, the dialog (and the quiet line)
	private void actions(UIPanel line) {
		UIPanel picked = this.group(line);
		this.buttons.put(Control.EDIT, this.button(picked, "audio.sound-chain.edit", () -> TGSoundChainButtons.edit(this.context, this.commands, this.shown.getTrack(), this.picked, this::later, TGWindow.getInstance(this.context).getWindow())));
		this.buttons.put(Control.TOGGLE, this.button(picked, "audio.sound-chain.disable", () -> this.commands.toggle(this.picked)));
		this.buttons.put(Control.MOVE_UP, this.button(picked, "audio.sound-chain.move-left", () -> this.commands.moveUp(this.picked)));
		this.buttons.put(Control.REMOVE, this.button(picked, "audio.sound-chain.remove", () -> this.commands.remove(this.picked)));
		UIPanel effect = this.group(line);
		this.label(effect, "audio.sound-chain.effect");
		this.effects = this.select(effect);
		this.buttons.put(Control.ADD_EFFECT, this.button(effect, "audio.sound-chain.add-effect", () -> this.commands.addEffect(this.effects.getSelectedValue())));
		UIPanel instrument = this.group(line);
		this.label(instrument, "audio.sound-chain.instrument");
		this.instruments = this.select(instrument);
		this.buttons.put(Control.USE_AS_SOURCE, this.button(instrument, "audio.sound-chain.choose-instrument", () -> this.commands.useAsSource(this.instruments.getSelectedValue())));
		this.buttons.put(Control.DEFAULT_INSTRUMENT, this.button(instrument, "audio.sound-chain.default-instrument", this.commands::useDefault));
		this.buttons.put(Control.KEEP, this.button(instrument, "audio.sound-chain.keep", this.commands::keep));
		UIPanel dialog = this.group(line);
		this.buttons.put(Control.OPEN_DIALOG, this.button(dialog, "audio.sound-chain", () -> new TGSoundChainDialog(this.context, this.shown.getTrack()).open()));
		this.note = this.factory.createLabel(dialog);
		this.place(dialog, this.note);
		this.note.setVisible(false);
	}

	// a group of controls side by side, kept together on the action line
	private UIPanel group(UIPanel line) {
		UIPanel group = this.factory.createPanel(line, false);
		group.setLayout(new UITableLayout(0f));
		return group;
	}

	// the next cell of the group's single row
	private <T extends UIControl> T place(UIContainer group, T control) {
		((UITableLayout) ((UIPanel) group).getLayout()).set(control, 1, group.getChildren().size(), UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
		KeyBindingActionManager.getInstance(this.context).appendListenersTo(control);
		return control;
	}

	private UIButton button(UIPanel group, String key, Runnable action) {
		UIButton button = this.place(group, this.factory.createButton(group));
		this.texts.add(() -> button.setText(TuxGuitar.getProperty(key)));
		button.addSelectionListener(event -> action.run());
		return button;
	}

	private void label(UIPanel group, String key) {
		UILabel label = this.place(group, this.factory.createLabel(group));
		this.texts.add(() -> label.setText(TuxGuitar.getProperty(key) + ":"));
	}

	private UIDropDownSelect<TGVst3Plugin> select(UIPanel group) {
		UIDropDownSelect<TGVst3Plugin> select = this.place(group, this.factory.<TGVst3Plugin>createDropDownSelect(group));
		select.addSelectionListener(event -> this.apply());
		return select;
	}

	boolean isVisible() {
		return this.panel.isVisible();
	}

	// View > Sound Chain, F8: shown or hidden, the choice kept at once (a crash keeps it)
	void toggle() {
		boolean visible = !this.panel.isVisible();
		this.panel.setVisible(visible);
		this.config.setValue(VISIBLE, visible);
		this.config.save();
		this.onUpdate();
		TGBottomDock.getInstance(this.context).layout();
	}

	void dispose() {
		TGEditorManager.getInstance(this.context).removeUpdateListener(this);
		TuxGuitar.getInstance().getLanguageManager().removeLoader(this);
		this.panel.dispose();
		TGBottomDock.getInstance(this.context).layout();
	}

	// an update of the app (a caret move, a change of the song, of the MIDI Port...) or another language
	public void processEvent(TGEvent event) {
		boolean language = TGLanguageEvent.EVENT_TYPE.equals(event.getEventType());
		this.later(language ? this::retell : this::onUpdate);
	}

	// the texts in the language chosen, the rows too (the default source says what plays in it)
	private void retell() {
		this.texts.forEach(Runnable::run);
		this.show(TGSoundChainFollow.key(this.context, TGAudioToolPlugin.caretTrack()));
	}

	// nothing is read while the panel is hidden
	private void onUpdate() {
		if (this.panel.isVisible()) {
			this.follow();
		}
	}

	private void follow() {
		Key now = TGSoundChainFollow.key(this.context, TGAudioToolPlugin.caretTrack());
		this.note(TGSoundChainFollow.note(this.context));
		if (TGSoundChainFollow.changed(this.shown, now)) {
			this.show(now);
		}
	}

	private void note(String text) {
		this.note.setText(text);
		if (this.note.isVisible() == text.isEmpty()) {
			this.note.setVisible(!text.isEmpty());
			TGBottomDock.getInstance(this.context).layout();
		}
	}

	// the track's rows read (this touches the disk), the row picked as TGSoundChainFollow says
	private void show(Key now) {
		this.rows = now.rows(this.context);
		List<String> read = this.rows.getRows(TuxGuitar.getProperty("audio.sound-chain.source"));
		this.picked = TGSoundChainFollow.picked(this.shown, now, this.picked, read.size());
		this.shown = now;
		this.title.setText(now.title(TuxGuitar.getProperty("audio.sound-chain.no-track")));
		this.chips.forEach(UIControl::dispose);
		this.chips.clear();
		IntStream.range(0, read.size()).forEach(row -> this.chips.add(this.chip(row, read.get(row))));
		this.offer(TGVst3Hosting.getInstance(this.context).getPlugins());
		this.pick(this.picked);
		TGBottomDock.getInstance(this.context).layout();
	}

	private UIToggleButton chip(int row, String text) {
		UIToggleButton chip = this.factory.createToggleButton(this.chain);
		chip.setText(text);
		chip.addSelectionListener(event -> this.pick(row));
		KeyBindingActionManager.getInstance(this.context).appendListenersTo(chip);
		return chip;
	}

	// the drop-downs offer what the last scan found, offered again only after another scan
	private void offer(List<TGVst3Plugin> plugins) {
		if (plugins != this.offered) {
			TGSoundChainButtons.offer(this.effects, TGSoundChainRows.effects(plugins));
			TGSoundChainButtons.offer(this.instruments, TGSoundChainRows.instruments(plugins));
			this.offered = plugins;
		}
	}

	private void pick(int row) {
		this.picked = row;
		IntStream.range(0, this.chips.size()).forEach(chip -> this.chips.get(chip).setSelected(chip == row));
		this.apply();
	}

	private void apply() {
		this.buttons.apply(this.rows, this.picked, this.effects.getSelectedValue(), this.instruments.getSelectedValue());
	}

	private void later(Runnable done) {
		TGSoundChainButtons.later(this.context, this.panel, done);
	}
}
