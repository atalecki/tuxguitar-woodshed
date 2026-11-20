package app.tuxguitar.audio.ui;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.keybindings.KeyBindingActionManager;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.audio.TGEditorDock;
import app.tuxguitar.audio.TGEditorDock.Element;
import app.tuxguitar.audio.TGEditorDock.How;
import app.tuxguitar.audio.TGEditorDock.Line;
import app.tuxguitar.audio.TGEditorDock.Step;
import app.tuxguitar.audio.TGEditorKeys;
import app.tuxguitar.audio.TGEditorPlacement;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UIWrapLabel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.vst3.TGEditorTarget;
import app.tuxguitar.vst3.TGVst3Exception;

/**
 * The Track panel's editor area, under its chain and actions: the picked plugin's own editor inside it (a window of the
 * plugin's host process, a child of this area's native window), as wide as the area when it can be, as high as it needs
 * (Q3: no border to drag), with a line above naming it and offering Open in window; or a line saying where the editor
 * is instead (in a window of its own, too large for the panel: wider than the panel when it opens or once the panel is
 * made narrower, or higher than the limit; stopped); for a plugin without an editor of its own, TuxGuitar's parameter
 * rows under that line (TGSoundChainParameterRoom). The line wraps at the panel's width, its button under it.
 * TGEditorDock decides all of it; this executes its steps (TGLivePlugins closes a view, TGSoundChainCommands opens an
 * editor where the step says) and shows what it shows, on the UI thread. What Hide editor and Open in window remember is kept in the panel's config at once. While the editor
 * inside has the focus, TuxGuitar's shortcuts with Ctrl, Alt or a function key typed there come to the area (the host
 * passes them), whose key listener runs them; plain keys stay with the plugin. Glue only.
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
	private final UIWrapLabel text;
	private final UIButton button;
	private final UIPanel area;
	private final UIPanel parent;
	// the panel laid out again once the area's height changed
	private final Runnable laidOut;
	// TuxGuitar's parameter rows of a plugin without an editor of its own
	private final TGSoundChainParameterRoom room;

	TGSoundChainEditorArea(TGContext context, UIFactory factory, UIPanel parent, int row, TGSoundChainCommands commands, TGConfigManager config, Runnable laidOut) {
		this.context = context;
		this.laidOut = laidOut;
		this.config = config;
		this.commands = commands;
		this.parent = parent;
		this.dock = new TGEditorDock(TGEditorDock.pinned(config.getStringValue(POPPED_OUT, "")), config.getBooleanValue(HIDDEN, false));
		this.header = factory.createPanel(parent, false);
		this.header.setLayout(new UITableLayout(0f));
		this.header.getLayout().set(UITableLayout.IGNORE_INVISIBLE, true);
		this.text = factory.createWrapLabel(this.header);
		this.button = factory.createButton(this.header);
		this.button.addSelectionListener(event -> this.run(this.dock.press()));
		UITableLayout headerLayout = (UITableLayout) this.header.getLayout();
		headerLayout.set(this.text, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false);
		headerLayout.set(this.button, 2, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
		this.area = factory.createPanel(parent, false);
		this.area.addResizeListener(event -> this.resized());
		KeyBindingActionManager.getInstance(context).appendListenersTo(this.area);
		((UITableLayout) parent.getLayout()).set(this.header, row, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 1, null, null, 2f);
		((UITableLayout) parent.getLayout()).set(this.area, row + 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
		this.placements.put(How.INSIDE, TGEditorPlacement.embedded(() -> TGNativeWindow.handle(this.area), this::owner, this::keys));
		this.placements.put(How.IN_A_WINDOW, TGNativeWindow.floating(context));
		this.room = new TGSoundChainParameterRoom(context, factory, parent, row + 2, laidOut);
		this.show();
	}

	// TuxGuitar's shortcuts with Ctrl, Alt or a function key, as bound now (TGEditorKeys): typed in the editor inside the
	// area, they come to the area, whose key listener runs them
	private int[] keys() {
		List<List<String>> shortcuts = KeyBindingActionManager.getInstance(this.context).getKeyBindingActions().stream().map(action -> action.getCombination().getKeyStrings()).collect(Collectors.toList());
		return TGEditorKeys.passed(shortcuts);
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

	// a plugin of the track shown asked to show its editor
	void requested(Element element) {
		this.run(this.dock.requested(element));
	}

	// a plugin of another track asked to show its editor: in a window of its own
	void elsewhere(int channel, String element) {
		try {
			TGLivePlugins.getInstance(this.context).openEditor(channel, element, TGEditorTarget.floating(this.owner()), "");
		} catch (TGVst3Exception e) {
			LOG.debug("the editor a VST3 plugin asked for was not shown: " + e.getMessage());
		}
	}

	// the key of the panel's Edit button's text: Hide editor or Show editor
	String editKey() {
		return this.dock.editKey();
	}

	// the step done (the view closed, the editor opened where it says), what the dock remembers kept, what it shows shown
	private void run(Step step) {
		Optional.ofNullable(step.getClose()).ifPresent(this::close);
		Optional.ofNullable(step.getOpen()).ifPresent(element -> this.open(element, step.getHow()));
		this.config.setValue(HIDDEN, this.dock.isHidden());
		this.config.setValue(POPPED_OUT, TGEditorDock.pinned(this.dock.getPinned()));
		this.config.save();
		this.show();
	}

	// the element's view in the area closes: its own editor, or TuxGuitar's parameter rows
	private void close(Element element) {
		Optional.ofNullable(this.editors.remove(element)).ifPresent(this::close);
	}

	private void close(TGPluginEditor editor) {
		TGLivePlugins.getInstance(this.context).close(editor);
		this.room.close(editor);
	}

	private void open(Element element, How how) {
		this.commands.edit(element.getRow(), this.placements.get(how), editor -> this.later(() -> this.opened(element, editor)));
	}

	// the editor the host opened, followed from now on: what becomes of it goes to the dock
	private void opened(Element element, TGPluginEditor editor) {
		this.editors.put(element, editor);
		editor.watch(() -> this.later(() -> this.followed(element, editor)));
		Step step = this.dock.opened(element, editor.getOpened());
		if (this.dock.parameters(element)) {
			this.room.show(editor, (parameter, plain) -> this.commands.set(element.getRow(), parameter, plain));
		}
		this.run(step);
	}

	private void followed(Element element, TGPluginEditor editor) {
		this.room.follow(editor);
		this.run(this.dock.followed(element, editor, this.editors.get(element) == editor));
	}

	// the line above the area, and the area as high as the editor inside it
	private void show() {
		Line line = this.dock.line();
		this.header.setVisible(line != null);
		Optional.ofNullable(line).ifPresent(this::say);
		float height = this.dock.height(Math.round(this.parent.getBounds().getWidth()));
		((UITableLayout) this.parent.getLayout()).set(this.area, UITableLayout.PACKED_HEIGHT, height);
		this.laidOut.run();
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

	// the line above the area wraps at the width given (the panel's, less its margins)
	void fit(float most) {
		((UITableLayout) this.header.getLayout()).set(this.text, UITableLayout.MAXIMUM_PACKED_WIDTH, most);
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
