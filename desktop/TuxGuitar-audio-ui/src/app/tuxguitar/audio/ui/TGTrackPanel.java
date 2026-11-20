package app.tuxguitar.audio.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.keybindings.KeyBindingActionManager;
import app.tuxguitar.app.system.language.TGLanguageEvent;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.main.TGRightDock;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.audio.TGSoundChainFollow;
import app.tuxguitar.audio.TGSoundChainFollow.Key;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.TGSoundChainRows.Control;
import app.tuxguitar.audio.TGSoundChainStrip;
import app.tuxguitar.audio.TGTrackPanelWidth;
import app.tuxguitar.audio.TGTrackSoundChoices;
import app.tuxguitar.editor.TGEditorManager;
import app.tuxguitar.event.TGEvent;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.appearance.UIAppearance;
import app.tuxguitar.ui.layout.UIScrollBarPanelLayout;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UIScrollBarPanel;
import app.tuxguitar.ui.widget.UIToggleButton;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The Track panel, docked at the right of the main window (TGRightDock) as Guitar Pro's Track inspector, for the
 * caret's track, top to bottom: its colour and name (TGTrackHeader); SOUNDS, the [MIDI | VST3] switch and the
 * instrument (TGTrackSoundSection); the sound chain as a list in the order the sound flows (the source, then each
 * effect, an arrow down into each after the source, a level meter under each, Track out and Master at the foot), one
 * picked; the actions on the picked one (Edit / Hide editor, Disable / Enable, Up, Remove); Add effect; "Sound
 * Chain..." opening the dialog (Scan again and the scan report are there) and, while the MIDI Port is not the TuxGuitar
 * Synthesizer, a quiet line saying the chains are not heard. Its content is fitted to its width, never wider, so none of
 * it goes past its right edge: a chip's name too long for it ends in "…" (its whole in the tooltip), a line wraps, a
 * drop-down is as wide as its row leaves it (TGSoundChainButtons.narrow). Built in sections so more can follow (notation, tuning, interpretation); under them the picked plugin's
 * own editor, or TuxGuitar's parameter rows for one without (TGSoundChainEditorArea: an editor wider than the panel
 * goes to a window of its own); it scrolls when taller than the window. Nothing of it is at the foot of the window.
 * Shown and hidden by View > Sound Chain (F8), the choice kept in its own config at once, as is the panel's width
 * (TGTrackPanelWidth) once its divider is dragged. It reads the track again only when what it shows changed
 * (TGSoundChainFollow), not on each move of the caret; what is enabled is decided by the rows (TGSoundChainButtons),
 * the changes run through TGSoundChainCommands. Glue only; on the UI thread.
 */
final class TGTrackPanel implements TGEventListener {

	static final String VISIBLE = "visible";
	static final String WIDTH = "width";
	// how wide Track out's and Master's meters are, and the held peak beside their name ("-60.0 dB")
	private static final float OUTPUT_WIDTH = 120f;
	private static final float NUMBER_WIDTH = 56f;
	// the most the margins take from the panel's width around a control of a section, both sides (a chip in its group:
	// 9 a side), so a control given the panel's width less this fits in it
	private static final float INSET = 20f;
	// the panel's rows: the header, the sound's three, the chain, the actions on the picked one, Add effect, the dialog,
	// the editor area
	private static final int HEADER = 1;
	private static final int SOUND = 2;
	private static final int CHAIN = 5;
	private static final int PICKED = 6;
	private static final int ADD = 7;
	private static final int DIALOG = 8;
	private static final int EDITOR = 9;

	private final TGContext context;
	private final UIFactory factory;
	private final TGConfigManager config;
	private final TGSoundChainButtons buttons = new TGSoundChainButtons();
	private final TGSoundChainCommands commands;
	// each text (re)set in the language chosen
	private final List<Runnable> texts = new ArrayList<Runnable>();
	private final List<UIToggleButton> chips = new ArrayList<UIToggleButton>();
	// each chip's whole name, which it shows cut to the panel's width
	private final Map<UIToggleButton, String> wholes = new HashMap<UIToggleButton, String>();
	// the chain list's groups: each element's chip, after its arrow, its meter under it
	private final List<UIPanel> links = new ArrayList<UIPanel>();
	private TGSoundChainMeters meters;
	private List<TGMeterCanvas> canvases = new ArrayList<TGMeterCanvas>();
	private List<String> names = new ArrayList<String>();
	private UIScrollBarPanel scroll;
	private UIPanel panel;
	private TGTrackHeader header;
	private TGTrackSoundSection sound;
	private UIPanel chain;
	private UILabel note;
	private UITableLayout noteLayout;
	private UIDropDownSelect<TGVst3Plugin> effects;
	private UIButton edit;
	private TGSoundChainEditorArea area;
	// the plugins offered: the last scan's
	private List<TGVst3Plugin> offered;
	private Key shown = Key.NONE;
	private TGSoundChainRows rows = TGSoundChainRows.NONE;
	private int picked = -1;
	// the width the content is fitted to: the panel's, less the scrollbar's
	private float fitted;

	TGTrackPanel(TGContext context, TGConfigManager config) {
		this.context = context;
		this.factory = TGApplication.getInstance(context).getFactory();
		this.config = config;
		this.commands = new TGSoundChainCommands(context, () -> this.shown.getTrack(), () -> this.later(this::follow));
	}

	// in the right dock's slot, its editor area under the chain and its actions; shown as last chosen (the first time:
	// shown) unless the audio it edits is off
	void create(boolean audio) {
		this.fitted = this.width();
		this.createPanel();
		this.header = new TGTrackHeader(this.context, this.factory, this.panel, HEADER, this.commands);
		this.texts.add(this.header::retell);
		this.sound = new TGTrackSoundSection(this.factory, this.panel, SOUND, this.commands, this.buttons);
		this.texts.add(this.sound::retell);
		this.chain = this.section(CHAIN);
		this.picked(this.section(PICKED));
		this.add(this.section(ADD));
		this.dialog(this.section(DIALOG));
		boolean visible = (audio && this.config.getBooleanValue(VISIBLE, true));
		this.meters = new TGSoundChainMeters(this.context, this.panel);
		this.area = new TGSoundChainEditorArea(this.context, this.factory, this.section(EDITOR), 1, this.commands, this.config, this::layout);
		this.texts.forEach(Runnable::run);
		this.show(visible);
		TGRightDock.getInstance(this.context).setDragged(this::dragged);
		TGEditorManager.getInstance(this.context).addUpdateListener(this);
		TuxGuitar.getInstance().getLanguageManager().addLoader(this);
		TGLivePlugins.getInstance(this.context).setRequests((channel, element) -> this.later(() -> this.requested(channel, element)));
		this.onUpdate();
	}

	// a column that scrolls when taller than the window, as wide as the slot whatever its content (which is fitted to it)
	private void createPanel() {
		UIPanel slot = TGRightDock.getInstance(this.context).getSlot();
		this.scroll = this.factory.createScrollBarPanel(slot, true, false, false);
		this.scroll.setLayout(new UIScrollBarPanelLayout(false, true, true, false, false, true));
		this.scroll.getVScroll().addSelectionListener(event -> this.scroll.layout());
		((UITableLayout) slot.getLayout()).set(this.scroll, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, null, null, 0f);
		((UITableLayout) slot.getLayout()).set(this.scroll, UITableLayout.PACKED_WIDTH, 0f);
		this.panel = this.factory.createPanel(this.scroll, false);
		this.panel.setLayout(new UITableLayout());
		this.panel.getLayout().set(UITableLayout.IGNORE_INVISIBLE, true);
		this.panel.addResizeListener(event -> this.resized());
	}

	// the content's width changed (the divider dragged, the scrollbar come or gone): fitted to it
	private void resized() {
		float width = this.panel.getBounds().getWidth();
		if (width != this.fitted) {
			this.later(() -> this.fit(width));
		}
	}

	// the content fitted to the width: the chips' names cut to it, the lines wrapping at it; laid out again
	private void fit(float width) {
		this.fitted = width;
		this.wholes.forEach(this::cut);
		this.noteLayout.set(this.note, UITableLayout.MAXIMUM_PACKED_WIDTH, width - INSET);
		this.area.fit(width - INSET);
		this.layout();
	}

	// the chip's whole name cut to the width fitted to, as the chip measures it
	private void cut(UIToggleButton chip, String whole) {
		chip.setText(TGTrackSoundChoices.shorten(whole, text -> this.fits(chip, text)));
	}

	private boolean fits(UIToggleButton chip, String text) {
		chip.setText(text);
		chip.computePackedSize(null, null);
		return chip.getPackedSize().getWidth() <= this.fitted - INSET;
	}

	// a section of the column, its controls in a table of its own
	private UIPanel section(int row) {
		UIPanel section = this.factory.createPanel(this.panel, false);
		section.setLayout(new UITableLayout(0f));
		section.getLayout().set(UITableLayout.IGNORE_INVISIBLE, true);
		((UITableLayout) this.panel.getLayout()).set(section, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false, 1, 1, null, null, 2f);
		return section;
	}

	// the actions on the picked one, two by two
	private void picked(UIPanel section) {
		this.edit = this.buttons.put(Control.EDIT, this.place(section, 1, 1, this.factory.createButton(section)));
		this.edit.addSelectionListener(event -> this.toggleEditor());
		this.texts.add(() -> this.edit.setText(TuxGuitar.getProperty(this.area.editKey())));
		this.buttons.put(Control.TOGGLE, this.button(section, 1, 2, "audio.sound-chain.disable", () -> this.commands.toggle(this.picked)));
		this.buttons.put(Control.MOVE_UP, this.button(section, 2, 1, "audio.sound-chain.up", () -> this.commands.moveUp(this.picked)));
		this.buttons.put(Control.REMOVE, this.button(section, 2, 2, "audio.sound-chain.remove", () -> this.commands.remove(this.picked)));
	}

	// a VST3 effect picked, and Add
	private void add(UIPanel section) {
		UILabel label = this.place(section, 1, 1, this.factory.createLabel(section));
		this.texts.add(() -> label.setText(TuxGuitar.getProperty("audio.sound-chain.effect") + ":"));
		this.effects = this.place(section, 2, 1, this.factory.<TGVst3Plugin>createDropDownSelect(section));
		TGSoundChainButtons.narrow((UITableLayout) section.getLayout(), this.effects);
		this.effects.addSelectionListener(event -> this.apply());
		this.buttons.put(Control.ADD_EFFECT, this.button(section, 2, 2, "audio.sound-chain.add-effect", () -> this.commands.addEffect(this.effects.getSelectedValue())));
	}

	// "Sound Chain...", and the quiet line under it
	private void dialog(UIPanel section) {
		this.buttons.put(Control.OPEN_DIALOG, this.button(section, 1, 1, "audio.sound-chain", () -> new TGSoundChainDialog(this.context, this.shown.getTrack()).open()));
		this.note = this.factory.createWrapLabel(section);
		this.noteLayout = (UITableLayout) section.getLayout();
		this.noteLayout.set(this.note, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false, 1, 2, null, null, 0f);
		this.note.setVisible(false);
	}

	// at the cell of the section, filling it
	private <T extends UIControl> T place(UIPanel section, int row, int column, T control) {
		((UITableLayout) section.getLayout()).set(control, row, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false);
		KeyBindingActionManager.getInstance(this.context).appendListenersTo(control);
		return control;
	}

	private UIButton button(UIPanel section, int row, int column, String key, Runnable action) {
		UIButton button = this.place(section, row, column, this.factory.createButton(section));
		this.texts.add(() -> button.setText(TuxGuitar.getProperty(key)));
		button.addSelectionListener(event -> action.run());
		return button;
	}

	boolean isVisible() {
		return TGRightDock.getInstance(this.context).getSlot().isVisible();
	}

	// View > Sound Chain, F8: shown or hidden, the choice kept at once (a crash keeps it); the editor area follows
	void toggle() {
		boolean visible = !this.isVisible();
		this.config.setValue(VISIBLE, visible);
		this.config.save();
		this.show(visible);
		this.onUpdate();
	}

	// the panel at its width, or hidden, the editor area's view with it; the meters measure only while shown
	private void show(boolean visible) {
		this.area.panel(visible);
		this.meters.shown(visible);
		TGRightDock.getInstance(this.context).show(visible, this.width());
	}

	// the width the panel is shown at: as last left, else the default
	private float width() {
		return TGTrackPanelWidth.shown(this.config.getFloatValue(WIDTH, 0f));
	}

	// a drag of the panel's divider: the width it leaves, kept at once
	private Float dragged(Float shown, Float by) {
		float width = TGTrackPanelWidth.dragged(shown, by);
		this.config.setValue(WIDTH, width);
		this.config.save();
		return width;
	}

	// Hide editor / Show editor
	private void toggleEditor() {
		this.area.toggle(this.isVisible());
		this.edit.setText(TuxGuitar.getProperty(this.area.editKey()));
	}

	void dispose() {
		this.meters.shown(false);
		this.area.dispose();
		TGEditorManager.getInstance(this.context).removeUpdateListener(this);
		TuxGuitar.getInstance().getLanguageManager().removeLoader(this);
		this.scroll.dispose();
		TGRightDock.getInstance(this.context).show(false, 0f);
	}

	// an update of the app (a caret move, a change of the song, of the MIDI Port...) or another language
	public void processEvent(TGEvent event) {
		boolean language = TGLanguageEvent.EVENT_TYPE.equals(event.getEventType());
		this.later(language ? this::retell : this::onUpdate);
	}

	// the texts in the language chosen, the rows too (the default source says what plays in it)
	private void retell() {
		this.texts.forEach(Runnable::run);
		this.area.retell();
		this.show(TGSoundChainFollow.key(this.context, TGAudioToolPlugin.caretTrack()));
	}

	// nothing is read while the panel is hidden
	private void onUpdate() {
		if (this.isVisible()) {
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
			this.layout();
		}
	}

	// the track's rows read (this touches the disk), the row picked as TGSoundChainFollow says
	private void show(Key now) {
		this.rows = now.rows(this.context);
		List<String> read = this.rows.getElements(TuxGuitar.getProperty("audio.sound-chain.source"));
		this.picked = TGSoundChainFollow.picked(this.shown, now, this.picked, read.size());
		this.shown = now;
		this.header.show(now.getTrack(), now.title(TuxGuitar.getProperty("audio.sound-chain.no-track")));
		this.sound.show(this.rows, TGSoundChainRows.instruments(TGVst3Hosting.getInstance(this.context).getPlugins()));
		this.links.forEach(UIControl::dispose);
		this.links.clear();
		this.chips.clear();
		this.wholes.clear();
		this.canvases = new ArrayList<TGMeterCanvas>();
		this.names = new ArrayList<String>();
		TGSoundChainStrip.links(read).forEach(this::link);
		this.outputs(!read.isEmpty());
		this.meters.follow(this.rows.getMeters(), this.canvases, this.names);
		this.offer(TGVst3Hosting.getInstance(this.context).getPlugins());
		this.pick(this.picked);
		this.layout();
	}

	// an element of the chain list: the arrow down into it, its chip, its level meter under the chip
	private void link(TGSoundChainStrip.Link link) {
		UIPanel group = this.group();
		this.arrow(group, link.isArrowed());
		this.chips.add(this.place(group, 2, 1, this.chip(group, link.getRow(), link.getText())));
		this.canvases.add(TGMeterCanvas.mono(this.factory, this.appearance(), group, 3, 1, this.clearing(link.getRow())));
		this.names.add(TuxGuitar.getProperty("audio.sound-chain.meter.after", new String[] {link.getText()}));
	}

	// a group of the chain list, one row under the other
	private UIPanel group() {
		UIPanel group = this.factory.createPanel(this.chain, false);
		group.setLayout(new UITableLayout(0f));
		((UITableLayout) this.chain.getLayout()).set(group, this.links.size() + 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_TOP, true, false, 1, 1, null, null, 1f);
		this.links.add(group);
		return group;
	}

	private void arrow(UIPanel group, boolean arrowed) {
		if (arrowed) {
			UILabel arrow = this.factory.createLabel(group);
			arrow.setText(TuxGuitar.getProperty("audio.track.flows-down"));
			((UITableLayout) group.getLayout()).set(arrow, 1, 1, UITableLayout.ALIGN_CENTER, UITableLayout.ALIGN_CENTER, true, false);
		}
	}

	// Track out, when a track is shown, then Master
	private void outputs(boolean track) {
		if (track) {
			this.output("audio.sound-chain.meter.out", true);
		}
		this.output("audio.sound-chain.meter.master", false);
	}

	// Track out (after the track's Volume and Balance) and Master: their name, the held peak beside it, two bars under
	private void output(String key, boolean arrowed) {
		UIPanel group = this.group();
		this.arrow(group, arrowed);
		UILabel name = this.factory.createLabel(group);
		name.setText(TuxGuitar.getProperty(key));
		((UITableLayout) group.getLayout()).set(name, 2, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
		UILabel number = this.factory.createLabel(group);
		((UITableLayout) group.getLayout()).set(number, 2, 2, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, true, false, 1, 1, NUMBER_WIDTH, null, 0f);
		this.canvases.add(TGMeterCanvas.stereo(this.factory, this.appearance(), group, 3, 1, OUTPUT_WIDTH, number, this.clearing(this.canvases.size())));
		this.names.add(TuxGuitar.getProperty(key));
	}

	// a click on the meter of the row clears its lamp
	private Runnable clearing(int row) {
		return () -> this.meters.clear(row);
	}

	private UIAppearance appearance() {
		return TGApplication.getInstance(this.context).getApplication().getAppearance();
	}

	// an element's chip: its name cut to the panel's width, the whole in the tooltip
	private UIToggleButton chip(UIPanel group, int row, String text) {
		UIToggleButton chip = this.factory.createToggleButton(group);
		this.wholes.put(chip, text);
		this.cut(chip, text);
		chip.setToolTipText(text);
		chip.addSelectionListener(event -> this.pick(row));
		return chip;
	}

	// the drop-down offers the effects the last scan found, offered again only after another scan
	private void offer(List<TGVst3Plugin> plugins) {
		if (plugins != this.offered) {
			TGSoundChainButtons.offer(this.effects, TGSoundChainRows.effects(plugins));
			this.offered = plugins;
		}
	}

	// a plugin asked to show its editor (VST3 requestOpenEditor): on the track shown its chip is picked and the area
	// shows it where it goes; on another track it opens in a window of its own
	private void requested(int channel, String element) {
		int row = this.commands.row(channel, element);
		Optional.of(row).filter(asked -> asked >= 0).ifPresentOrElse(this::pickAsked, () -> this.area.elsewhere(channel, element));
	}

	private void pickAsked(int row) {
		this.pick(row);
		this.area.requested(this.commands.element(row));
	}

	// the row picked; its plugin's editor shows in the editor area
	private void pick(int row) {
		this.picked = row;
		IntStream.range(0, this.chips.size()).forEach(chip -> this.chips.get(chip).setSelected(chip == row));
		this.apply();
		this.area.picked(this.commands.element(row));
	}

	private void apply() {
		this.buttons.apply(this.rows, this.picked, this.effects.getSelectedValue(), null);
		this.sound.apply(this.rows);
	}

	private void layout() {
		this.scroll.layout();
		TGRightDock.getInstance(this.context).layout();
	}

	private void later(Runnable done) {
		TGSoundChainButtons.later(this.context, this.scroll, done);
	}
}
