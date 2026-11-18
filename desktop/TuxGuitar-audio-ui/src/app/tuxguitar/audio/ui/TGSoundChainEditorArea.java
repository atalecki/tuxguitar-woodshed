package app.tuxguitar.audio.ui;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.view.main.TGBottomDock;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.audio.TGEditorDock;
import app.tuxguitar.audio.TGEditorDock.Element;
import app.tuxguitar.audio.TGEditorDock.How;
import app.tuxguitar.audio.TGEditorDock.Line;
import app.tuxguitar.audio.TGEditorDock.Step;
import app.tuxguitar.audio.TGEditorPlacement;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.vst3.TGVst3Exception;

/**
 * The Sound Chain panel's editor area, under its chain and action lines: the picked plugin's own editor inside it
 * (a window of the plugin's host process, a child of this area's native window), as wide as the area when it can be,
 * as high as it needs (Q3: no border to drag), with a line above naming it and offering Open in window; or a line saying
 * where the editor is instead (in a window of its own, too large for the panel, stopped). TGEditorDock decides all of
 * it; this executes its steps (TGLivePlugins closes a view, TGSoundChainCommands opens an editor where the step says)
 * and shows what it shows, on the UI thread. What Hide editor and Open in window remember is kept in the panel's config
 * at once. Glue only.
 */
final class TGSoundChainEditorArea {

	// the config keys: Hide editor chosen; the plugins (class ids) remembered in a window of their own
	static final String HIDDEN = "editor-hidden";
	static final String POPPED_OUT = "popped-out";

	private static final TGLog LOG = TGLog.get(TGSoundChainEditorArea.class);

	private final TGContext context;
	private final TGConfigManager config;
	private final TGSoundChainCommands commands;
	private final TGEditorDock dock;
	private final Map<How, TGEditorPlacement> placements = new EnumMap<How, TGEditorPlacement>(How.class);
	// the editor opened for each element the area knows of (its own, or TuxGuitar's parameter editor)
	private final Map<Element, TGPluginEditor> editors = new HashMap<Element, TGPluginEditor>();
	private final UIPanel header;
	private final UILabel text;
	private final UIButton button;
	private final UIPanel area;
	private final UIPanel parent;

	TGSoundChainEditorArea(TGContext context, UIFactory factory, UIPanel parent, int row, TGSoundChainCommands commands, TGConfigManager config) {
		this.context = context;
		this.config = config;
		this.commands = commands;
		this.parent = parent;
		this.dock = new TGEditorDock(TGEditorDock.pinned(config.getStringValue(POPPED_OUT, "")), config.getBooleanValue(HIDDEN, false));
		this.header = factory.createPanel(parent, false);
		this.header.setLayout(new UITableLayout(0f));
		this.text = factory.createLabel(this.header);
		this.button = factory.createButton(this.header);
		this.button.addSelectionListener(event -> this.run(this.dock.press()));
		UITableLayout headerLayout = (UITableLayout) this.header.getLayout();
		headerLayout.set(this.text, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false);
		headerLayout.set(this.button, 1, 2, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, false, false);
		this.area = factory.createPanel(parent, false);
		this.area.addResizeListener(event -> this.resized());
		((UITableLayout) parent.getLayout()).set(this.header, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 1, null, null, 2f);
		((UITableLayout) parent.getLayout()).set(this.area, row + 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
		this.placements.put(How.INSIDE, TGEditorPlacement.embedded(() -> TGNativeWindow.handle(this.area), this::owner));
		this.placements.put(How.IN_A_WINDOW, TGNativeWindow.floating(context));
		this.show();
	}

	private long owner() {
		return TGNativeWindow.handle(TGWindow.getInstance(this.context).getWindow());
	}

	// a chip picked, or the track switched (its source picked); null: no VST3 plugin there
	void picked(Element element) {
		this.run(this.dock.picked(element));
	}

	// the panel shown or hidden (F8)
	void panel(boolean shown) {
		this.run(this.dock.visibility(this.dock.isHidden(), shown));
	}

	// Hide editor / Show editor, remembered
	void toggle(boolean panelShown) {
		this.run(this.dock.visibility(!this.dock.isHidden(), panelShown));
	}

	// the key of the panel's Edit button's text: Hide editor or Show editor
	String editKey() {
		return this.dock.editKey();
	}

	// the step done (the view closed, the editor opened where it says), what the dock remembers kept, what it shows shown
	private void run(Step step) {
		Optional.ofNullable(step.getClose()).map(this.editors::remove).ifPresent(TGLivePlugins.getInstance(this.context)::close);
		Optional.ofNullable(step.getOpen()).ifPresent(element -> this.open(element, step.getHow()));
		this.config.setValue(HIDDEN, this.dock.isHidden());
		this.config.setValue(POPPED_OUT, TGEditorDock.pinned(this.dock.getPinned()));
		this.config.save();
		this.show();
	}

	private void open(Element element, How how) {
		this.commands.edit(element.getRow(), this.placements.get(how), editor -> this.later(() -> this.opened(element, editor)));
	}

	// the editor the host opened, followed from now on: what becomes of it goes to the dock
	private void opened(Element element, TGPluginEditor editor) {
		this.editors.put(element, editor);
		editor.watch(() -> this.later(() -> this.followed(element, editor)));
		this.run(this.dock.opened(element, editor.getOpened()));
	}

	private void followed(Element element, TGPluginEditor editor) {
		this.run(this.dock.followed(element, editor.getState(), editor.getOpened(), this.editors.get(element) == editor));
	}

	// the line above the area, and the area as high as the editor inside it
	private void show() {
		Line line = this.dock.line();
		this.header.setVisible(line != null);
		Optional.ofNullable(line).ifPresent(this::say);
		float height = this.dock.height(Math.round(this.parent.getBounds().getWidth()));
		((UITableLayout) this.parent.getLayout()).set(this.area, UITableLayout.PACKED_HEIGHT, height);
		TGBottomDock.getInstance(this.context).layout();
	}

	private void say(Line line) {
		this.text.setText(line.text(TuxGuitar::getProperty));
		this.button.setVisible(line.getButton() != null);
		Optional.ofNullable(line.getButton()).map(TuxGuitar::getProperty).ifPresent(this.button::setText);
	}

	// the area's width changed (the window resized): the editor inside takes it, when it can
	private void resized() {
		if (this.dock.refits(Math.round(this.parent.getBounds().getWidth()))) {
			this.fit();
		}
	}

	private void fit() {
		Element shown = this.dock.element();
		try {
			this.run(this.dock.fitted(shown, TGLivePlugins.getInstance(this.context).fit(this.editors.get(shown))));
		} catch (TGVst3Exception e) {
			LOG.debug("the editor of " + shown + " was not fitted to the panel: " + e.getMessage());
		}
	}

	// the texts in the language chosen
	void retell() {
		this.show();
	}

	// before the panel goes: the view inside closes first (its host must not wait on a window that is gone)
	void dispose() {
		this.run(this.dock.visibility(this.dock.isHidden(), false));
	}

	private void later(Runnable done) {
		TGSoundChainButtons.later(this.context, this.area, done);
	}
}
