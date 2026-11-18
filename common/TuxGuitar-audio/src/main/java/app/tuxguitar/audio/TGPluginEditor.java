package app.tuxguitar.audio;

import java.util.List;
import java.util.stream.Collectors;

import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.vst3.TGVst3EditorOpened;
import app.tuxguitar.vst3.TGVst3Exception;

/**
 * An open editor of a VST3 plugin of a sound chain: the plugin's own editor, in a window of its host process or inside a
 * window of TuxGuitar (as the host opened it: getOpened; too large for that window, nothing is open), or, for a plugin
 * without one, TuxGuitar's parameter editor, which shows a row per parameter of the plugin (its preset list is no
 * parameter there), at the values the plugin held when they were read. It is open while its plugin runs; it has moved
 * once another editor of the plugin's own opened (a plugin has one view at a time). Its watcher (one at a time; an app's
 * window) is told each time the plugin may hold other values (its chain ran again: a change, an undo, a redo; the rows
 * are read again on refresh), once it moved and once the editor closes; on any thread.
 */
public final class TGPluginEditor {

	private static final TGLog LOG = TGLog.get(TGPluginEditor.class);

	// the rows as the plugin holds its values now
	interface Rows {
		List<TGPluginParameterRow> read() throws TGVst3Exception;
	}

	// what became of it: open, moved (another editor of the plugin's own opened), closed (its plugin no longer runs, or
	// it was closed)
	public enum State {
		OPEN, MOVED, CLOSED
	}

	private final TGVst3EditorOpened opened;
	private final Rows plugin;
	private volatile List<TGPluginParameterRow> rows;
	private State state = State.OPEN;
	private Runnable watcher = () -> { };

	TGPluginEditor(TGVst3EditorOpened opened, Rows plugin) throws TGVst3Exception {
		this.opened = opened;
		this.plugin = plugin;
		this.rows = plugin.read();
	}

	// whether the plugin has an editor of its own (open, or too large for the window it was to go in)
	public boolean isOwn() {
		return (this.opened.getKind() != TGVst3EditorOpened.Kind.NONE);
	}

	// what the host did with the plugin's own editor
	public TGVst3EditorOpened getOpened() {
		return this.opened;
	}

	public List<TGPluginParameterRow> getRows() {
		return this.rows;
	}

	// whether a row shows the parameter (by its id) at the plain value
	boolean shows(int parameter, double plain) {
		return this.rows.stream().anyMatch(row -> row.getId() == parameter && row.getPlain() == plain);
	}

	// the titles of the rows
	public List<String> getParameters() {
		return this.rows.stream().map(TGPluginParameterRow::getTitle).collect(Collectors.toList());
	}

	// the rows read again from the plugin (a plugin gone keeps the rows it had)
	public void refresh() {
		try {
			this.rows = this.plugin.read();
		} catch (TGVst3Exception e) {
			// its host stopped: nothing more to read
			LOG.debug("a VST3 plugin's parameters were not read again: " + e.getMessage());
		}
	}

	public synchronized State getState() {
		return this.state;
	}

	public synchronized boolean isOpen() {
		return (this.state != State.CLOSED);
	}

	// the watcher from now on; told at once when the editor is closed already
	public synchronized void watch(Runnable watcher) {
		this.watcher = watcher;
		if (this.state == State.CLOSED) {
			watcher.run();
		}
	}

	// its plugin may hold other values
	synchronized void changed() {
		this.watcher.run();
	}

	// another editor of the plugin's own opened: this one is not open there any more
	void moved() {
		this.become(State.MOVED);
	}

	// its plugin no longer runs, or it was closed
	void close() {
		this.become(State.CLOSED);
	}

	private synchronized void become(State state) {
		this.state = state;
		this.watcher.run();
	}
}
