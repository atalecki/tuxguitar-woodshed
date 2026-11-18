package app.tuxguitar.audio;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import app.tuxguitar.vst3.TGVst3EditorOpened;
import app.tuxguitar.vst3.TGVst3EditorOpened.Kind;

/**
 * The Sound Chain panel's editor area (the room under the chain where the picked plugin's editor shows) as a state
 * machine, the only holder of its state: for each event (a chip picked, what the host did, Hide editor, the panel shown
 * or hidden, Open in window, Put back, a stop...) the step the area takes (the editor it closes, the one it opens and
 * where: inside the area, or in a window of its own) and what it shows then, with the line it says instead of an
 * editor. The panel executes the steps and holds no state of its own.
 *
 * Its rules: the picked plugin's editor shows inside the area unless the user hid it (Hide editor, remembered), the panel
 * is hidden (F8), or the plugin is remembered in a window of its own (Open in window, per plugin, until Put back in the
 * panel or that window's close box); an editor too large for the panel opens in a window of its own; a plugin has one
 * view at a time, so an editor opened elsewhere (the Sound Chain dialog's Edit) leaves the area saying so. A plugin
 * without an editor of its own shows TuxGuitar's parameters there. Not thread-safe: the UI thread only.
 */
public final class TGEditorDock {

	// what the area shows: nothing (no plugin picked), the editor opening, the plugin's own editor, TuxGuitar's
	// parameters, a line saying the editor is in a window of its own (as chosen or opened elsewhere), or too large for
	// the panel, that the plugin stopped, that the editor could not be opened; or nothing because it is hidden (Hide
	// editor, or the panel hidden)
	public enum Shows {
		NOTHING, OPENING, OWN_EDITOR, PARAMETERS, OWN_WINDOW, TOO_LARGE, STOPPED, REFUSED, HIDDEN
	}

	// where a step opens an editor
	public enum How {
		INSIDE(" inside"), IN_A_WINDOW(" in a window");

		private final String said;

		How(String said) {
			this.said = said;
		}
	}

	// what the area shows for what the host did: its own editor inside, TuxGuitar's parameters (none of its own), a
	// window of its own (OWN_WINDOW, OWNED), too large
	private static final Map<Kind, Shows> OPENED = new EnumMap<Kind, Shows>(Kind.class);
	static {
		OPENED.put(Kind.EMBEDDED, Shows.OWN_EDITOR);
		OPENED.put(Kind.NONE, Shows.PARAMETERS);
		OPENED.put(Kind.OWN_WINDOW, Shows.OWN_WINDOW);
		OPENED.put(Kind.OWNED, Shows.OWN_WINDOW);
		OPENED.put(Kind.TOO_LARGE, Shows.TOO_LARGE);
	}

	// what the button beside the line does: Open in window, Put back in the panel, Show (a window too large), Show editor
	// (after a stop)
	private static final Map<Shows, Function<TGEditorDock, Step>> PRESSED = new EnumMap<Shows, Function<TGEditorDock, Step>>(Shows.class);
	static {
		PRESSED.put(Shows.OWN_EDITOR, TGEditorDock::popOut);
		PRESSED.put(Shows.OWN_WINDOW, TGEditorDock::putBack);
		PRESSED.put(Shows.TOO_LARGE, TGEditorDock::showWindow);
		PRESSED.put(Shows.STOPPED, TGEditorDock::again);
	}

	// what becomes of an editor the area opened: still open (its plugin may have asked another size), moved elsewhere,
	// closed
	private interface Follow {
		Step follow(TGEditorDock dock, Element element, TGVst3EditorOpened opened);
	}

	private static final Map<TGPluginEditor.State, Follow> FOLLOWED = new EnumMap<TGPluginEditor.State, Follow>(TGPluginEditor.State.class);
	static {
		FOLLOWED.put(TGPluginEditor.State.OPEN, TGEditorDock::fitted);
		FOLLOWED.put(TGPluginEditor.State.MOVED, (dock, element, opened) -> dock.moved(element));
		FOLLOWED.put(TGPluginEditor.State.CLOSED, (dock, element, opened) -> dock.closed(element));
	}

	// the plugins (by class id) remembered in a window of their own
	private final Set<String> pinned;
	// Hide editor chosen (remembered by the panel)
	private boolean hidden;
	private boolean panelShown = true;
	private Element element;
	private Shows shows = Shows.NOTHING;
	private Line line;
	// the size of the editor inside, as the host said it last, and the area's width in units when last asked
	private TGVst3EditorOpened size = TGVst3EditorOpened.NONE;
	private int unitWidth;

	public TGEditorDock(Collection<String> pinned, boolean hidden) {
		this.pinned = new TreeSet<String>(pinned);
		this.hidden = hidden;
	}

	public Shows shows() {
		return this.shows;
	}

	// the plugin whose editor the area is about, or null
	public Element element() {
		return this.element;
	}

	// what the area says instead of an editor, or null when it says nothing
	public Line line() {
		return this.line;
	}

	// the plugins remembered in a window of their own, by class id, in order (for the panel to keep)
	public Set<String> getPinned() {
		return Collections.unmodifiableSet(this.pinned);
	}

	public boolean isHidden() {
		return this.hidden;
	}

	// the key of the text of the panel's Edit button: Hide editor, or Show editor once hidden
	public String editKey() {
		return (this.hidden ? "audio.sound-chain.show-editor" : "audio.sound-chain.hide-editor");
	}

	// the area's height in the toolkit's units, the area being that wide: the editor inside's, else none
	public int height(int unitWidth) {
		return (this.shows == Shows.OWN_EDITOR ? TGEditorArea.height(this.size.getHeight(), this.size.getAreaWidth(), unitWidth) : 0);
	}

	// whether the editor inside is to be fitted again: the area's width changed (a change of its height alone does not)
	public boolean refits(int unitWidth) {
		boolean changed = (unitWidth != this.unitWidth);
		this.unitWidth = unitWidth;
		return (changed && this.shows == Shows.OWN_EDITOR);
	}

	// the plugins remembered in a window of their own, as text (class ids, a comma between) and back
	public static Set<String> pinned(String text) {
		return Arrays.stream(text.split(",")).filter(id -> !id.isEmpty()).collect(Collectors.toCollection(TreeSet::new));
	}

	public static String pinned(Set<String> pinned) {
		return String.join(",", pinned);
	}

	// a chip picked, or the track switched (its source picked); null: no VST3 plugin there. The same plugin again changes
	// nothing
	public Step picked(Element picked) {
		return (Objects.equals(picked, this.element) ? Step.NONE : this.show(picked));
	}

	// Hide editor (the room given back) or Show editor, and the panel hidden (F8) or shown: the view closes, or opens
	// again
	public Step visibility(boolean editorHidden, boolean panel) {
		this.hidden = editorHidden;
		this.panelShown = panel;
		return this.show(this.element);
	}

	// Show editor after a stop: the editor opens again where it goes
	public Step again() {
		return this.show(this.element);
	}

	// what the area shows for the element, the view it shows now closed
	private Step show(Element shown) {
		Element closed = this.inArea();
		this.element = shown;
		this.into(this.wanted());
		return new Step(closed, (this.opens() ? shown : null), (this.shows == Shows.OWN_WINDOW ? How.IN_A_WINDOW : How.INSIDE));
	}

	// the element whose view (its own editor, TuxGuitar's parameters, or one opening) is in the area, or null
	private Element inArea() {
		return (this.shows == Shows.OPENING || this.shows == Shows.OWN_EDITOR || this.shows == Shows.PARAMETERS ? this.element : null);
	}

	private Shows wanted() {
		if (this.element == null) {
			return Shows.NOTHING;
		}
		return (this.hidden || !this.panelShown ? Shows.HIDDEN : this.placed());
	}

	// a window of its own when the plugin is remembered there, else inside the area
	private Shows placed() {
		return (this.pinned.contains(this.element.classId) ? Shows.OWN_WINDOW : Shows.OPENING);
	}

	private boolean opens() {
		return (this.shows == Shows.OPENING || this.shows == Shows.OWN_WINDOW);
	}

	// the area shows that now, with its line
	private void into(Shows now) {
		this.shows = now;
		this.line = Line.of(now, this.element);
	}

	// what the host did for the element: shown where it was wanted, or else, when no longer wanted there, a view inside
	// the area (or TuxGuitar's parameters) closes; too large, it opens in a window of its own; a window opening for one
	// too large keeps the line
	public Step opened(Element element, TGVst3EditorOpened opened) {
		return (this.awaits(element) ? this.shown(element, opened) : unwanted(element, opened.getKind()));
	}

	// the editor inside the area took another size (fitted to its width, or as its plugin asked)
	public Step fitted(Element element, TGVst3EditorOpened fitted) {
		if (this.shows == Shows.OWN_EDITOR && element.equals(this.element)) {
			this.size = fitted;
		}
		return Step.NONE;
	}

	// the button beside the line pressed
	public Step press() {
		return PRESSED.getOrDefault(this.shows, dock -> Step.NONE).apply(this);
	}

	// what became of the editor the area opened for the element (current: the one it follows now)
	public Step followed(Element element, TGPluginEditor.State state, TGVst3EditorOpened opened, boolean current) {
		return (current ? FOLLOWED.get(state).follow(this, element, opened) : Step.NONE);
	}

	// the editor the area showed closed without the area closing it (its plugin ran again: an undo, a change of the
	// chain): it opens again where it goes
	private Step closed(Element element) {
		boolean shown = (this.inArea() != null || this.shows == Shows.OWN_WINDOW);
		return (shown && element.equals(this.element) ? this.show(element) : Step.NONE);
	}

	private Step shown(Element element, TGVst3EditorOpened opened) {
		this.size = opened;
		this.into(this.shows == Shows.TOO_LARGE ? Shows.TOO_LARGE : OPENED.get(opened.getKind()));
		return (opened.getKind() == Kind.TOO_LARGE ? new Step(null, element, How.IN_A_WINDOW) : Step.NONE);
	}

	private static Step unwanted(Element opened, Kind kind) {
		return (kind == Kind.EMBEDDED || kind == Kind.NONE ? new Step(opened, null, How.INSIDE) : Step.NONE);
	}

	// whether the area waits for what the host does with the element's editor
	private boolean awaits(Element opened) {
		boolean waits = (this.shows == Shows.OPENING || this.shows == Shows.OWN_WINDOW || this.shows == Shows.TOO_LARGE);
		return (waits && opened.equals(this.element));
	}

	// Show (an editor too large for the panel): its window comes to the front
	public Step showWindow() {
		return new Step(null, this.element, How.IN_A_WINDOW);
	}

	// Open in window: the editor goes to a window of its own (the host closes the view inside), remembered for the plugin
	public Step popOut() {
		this.pinned.add(this.element.classId);
		this.into(Shows.OWN_WINDOW);
		return new Step(null, this.element, How.IN_A_WINDOW);
	}

	// Put back in the panel: forgotten, the editor goes inside the area again (the host closes its window)
	public Step putBack() {
		this.pinned.remove(this.element.classId);
		this.into(Shows.OPENING);
		return new Step(null, this.element, How.INSIDE);
	}

	// the close box of the window the element's editor is in: put back in the panel when it was in a window of its own
	public Step floatingClosed(Element closed) {
		return (this.shows == Shows.OWN_WINDOW && closed.equals(this.element) ? this.putBack() : Step.NONE);
	}

	// another editor of the element opened elsewhere (the Sound Chain dialog's Edit: a window of its own; a plugin has one
	// view at a time): the area says so
	public Step moved(Element elsewhere) {
		if (this.shows == Shows.OWN_EDITOR && elsewhere.equals(this.element)) {
			this.into(Shows.OWN_WINDOW);
		}
		return Step.NONE;
	}

	// the plugin stopped (it hung: ended by the watchdog, or its host crashed with the reason), started again or not
	public Step stopped(Element stopped, boolean hung, String reason, boolean restarted) {
		if (stopped.equals(this.element)) {
			this.shows = Shows.STOPPED;
			this.line = Line.stopped(stopped, hung, reason, restarted);
		}
		return Step.NONE;
	}

	// the element's editor could not be opened (the plugin did not load): the area says why (a text in TuxGuitar's
	// language)
	public Step refused(Element refused, String reason) {
		if (this.shows == Shows.OPENING && refused.equals(this.element)) {
			this.shows = Shows.REFUSED;
			this.line = new Line(null, new Object[] {reason}, null);
		}
		return Step.NONE;
	}

	// a plugin asking to show its editor (VST3 requestOpenEditor): the picked one, the area shown, opens again where it
	// goes unless it shows already; another, or the area hidden, opens in a window of its own
	public Step requested(Element asking) {
		boolean here = (asking.equals(this.element) && this.shows != Shows.HIDDEN);
		return (here ? this.again(asking) : new Step(null, asking, How.IN_A_WINDOW));
	}

	private Step again(Element asking) {
		return (this.inArea() != null ? Step.NONE : this.show(asking));
	}

	/**
	 * A plugin of a sound chain as the area knows it: its channel, its element of the chain, its plugin (class id), its
	 * name as the area says it, and its row. Equal by channel, element and plugin.
	 */
	public static final class Element {

		private final int channel;
		private final String id;
		private final String classId;
		private final String name;
		private final int row;

		public Element(int channel, String id, String classId, String name, int row) {
			this.channel = channel;
			this.id = id;
			this.classId = classId;
			this.name = name;
			this.row = row;
		}

		// its row in the sound chain editors (0 the source, then each effect)
		public int getRow() {
			return this.row;
		}

		public int getChannel() {
			return this.channel;
		}

		public String getId() {
			return this.id;
		}

		public String getClassId() {
			return this.classId;
		}

		public String getName() {
			return this.name;
		}

		public boolean equals(Object other) {
			return (other instanceof Element && this.key().equals(((Element) other).key()));
		}

		public int hashCode() {
			return this.key().hashCode();
		}

		private String key() {
			return this.channel + "/" + this.id + "/" + this.classId;
		}

		public String toString() {
			return this.name;
		}
	}

	/**
	 * What the area does: the view it closes (the element's editor inside it, or TuxGuitar's parameters there), then the
	 * editor it opens (null: none) and where.
	 */
	public static final class Step {

		static final Step NONE = new Step(null, null, How.INSIDE);

		private final Element close;
		private final Element open;
		private final How how;

		Step(Element close, Element open, How how) {
			this.close = close;
			this.open = open;
			this.how = how;
		}

		public Element getClose() {
			return this.close;
		}

		public Element getOpen() {
			return this.open;
		}

		public How getHow() {
			return this.how;
		}

		public String toString() {
			String closing = (this.close != null ? "close " + this.close : "");
			String opening = (this.open != null ? "open " + this.open + this.how.said : "");
			String both = Arrays.asList(closing, opening).stream().filter(part -> !part.isEmpty()).collect(Collectors.joining(", "));
			return (both.isEmpty() ? "nothing" : both);
		}
	}

	/**
	 * What the area says instead of an editor: a message key and its arguments (or, with no key, a text in TuxGuitar's
	 * language already), and the key of the button beside it (null: none).
	 */
	public static final class Line {

		// the line of each state that says one, and its button
		private static final Map<Shows, String[]> SAID = new EnumMap<Shows, String[]>(Shows.class);
		static {
			SAID.put(Shows.OWN_EDITOR, new String[] {"audio.sound-chain.editor.inside", "audio.sound-chain.editor.open-in-window"});
			SAID.put(Shows.PARAMETERS, new String[] {"audio.sound-chain.editor.parameters", null});
			SAID.put(Shows.OWN_WINDOW, new String[] {"audio.sound-chain.editor.in-window", "audio.sound-chain.editor.put-back"});
			SAID.put(Shows.TOO_LARGE, new String[] {"audio.sound-chain.editor.too-large", "audio.sound-chain.editor.show"});
		}

		private final String key;
		private final Object[] arguments;
		private final String button;

		Line(String key, Object[] arguments, String button) {
			this.key = key;
			this.arguments = arguments;
			this.button = button;
		}

		// the line of the state, naming the plugin; null for a state that says none
		static Line of(Shows shows, Element element) {
			String[] said = SAID.get(shows);
			return (said != null ? new Line(said[0], new Object[] {element.getName()}, said[1]) : null);
		}

		// a stop: it hung or stopped (the reason given), started again (with Show editor) or left silent
		static Line stopped(Element element, boolean hung, String reason, boolean restarted) {
			Object[] arguments = (hung ? new Object[] {element.getName()} : new Object[] {element.getName(), reason});
			return (restarted ? new Line(key(hung, "-restarted"), arguments, "audio.sound-chain.editor.show-editor") : new Line(key(hung, "-silent"), arguments, null));
		}

		private static String key(boolean hung, String end) {
			return "audio.sound-chain.editor." + (hung ? "hung" : "stopped") + end;
		}

		// the text: the key said in TuxGuitar's language with the arguments (translated: key, arguments -> text), or the
		// text it holds
		public String text(BiFunction<String, String[], String> translated) {
			String[] arguments = Arrays.stream(this.arguments).map(String::valueOf).toArray(String[]::new);
			return (this.key != null ? translated.apply(this.key, arguments) : arguments[0]);
		}

		// null: the first argument is the text
		public String getKey() {
			return this.key;
		}

		public Object[] getArguments() {
			return this.arguments;
		}

		public String getButton() {
			return this.button;
		}

		public String toString() {
			String arguments = Arrays.asList(this.arguments).stream().map(String::valueOf).collect(Collectors.joining(", "));
			return (this.key != null ? this.key + "(" + arguments + ")" : arguments) + (this.button != null ? " [" + this.button + "]" : "");
		}
	}
}
