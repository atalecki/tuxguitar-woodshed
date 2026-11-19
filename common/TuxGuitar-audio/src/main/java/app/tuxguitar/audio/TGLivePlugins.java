package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.audio.action.TGKeepDefaultInstrumentAction;
import app.tuxguitar.audio.action.TGSetPluginStateAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.midi.synth.TGAudioProcessor;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGSynthChannel;
import app.tuxguitar.midi.synth.TGSynthChannelListener;
import app.tuxguitar.midi.synth.TGSynthChannelProcessor;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.util.singleton.TGSingletonUtil;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Gestures;
import app.tuxguitar.vst3.TGVst3Instance;
import app.tuxguitar.vst3.TGVst3Parameter;
import app.tuxguitar.vst3.TGVst3PluginProcessor;
import app.tuxguitar.vst3.TGVst3State;

/**
 * The VST3 plugins of the song's sound chains as they run, each known by its channel and the id of its element of the
 * channel's chain: those of a render that runs (it binds them; the synthesizer playing live tells this listener each
 * time a channel's chain changes); otherwise one kept open for the element, as the TuxGuitar Synthesizer keeps its
 * chains open, opened from the song's element and opened again when the element changes (by undo, say). A plugin's
 * editor is its own, or, without one, TuxGuitar's parameter editor; it closes once its plugin no longer runs (the chain
 * changed, the channel closed).
 *
 * A gesture in a plugin's own editor, or a preset chosen there, is heard with its host's replies, on the thread that
 * called the plugin (live, the audio thread), which only notes it. Draining, on the thread that edits the song (an
 * app's UI thread), changes the song: each element whose plugin ended gestures keeps that plugin's state, by one
 * TGSetPluginStateAction run on an action context of its own (one undo step per element).
 *
 * The default instrument a channel plays as its source (TGDefaultInstrumentSource.edited) is edited too, though it is
 * no element of the song: the first change made in its editor keeps it in the song with that change (Keep in song,
 * TGKeepDefaultInstrumentAction, one undo step), its plugin and editor going on as the source's; opening and closing its
 * editor changes nothing, nor does what the plugin does once the default is another one (the settings changed). The
 * synthesizer runs a plugin it hands over between the default and the chain's source as the other's (synth-core's
 * TGSynthChannel): its editor goes on as that element's, so undoing the keep leaves the default's editor open, the plugin
 * agreeing with the default as it was when the editor opened (its next change keeps it again), and Redo leaves it open
 * as the source's; an editor that never edited the default closes instead.
 */
public final class TGLivePlugins implements TGSynthChannelListener {

	// what a render that is not live (an export) binds its plugins to: nothing
	public static final TGLivePlugins NONE = new TGLivePlugins(null, false);
	// how long an editor waits at most for the live synthesizer to run a plugin just added (it loads in a few hundred
	// milliseconds; the app opens editors on its UI thread, which waits meanwhile)
	static final long LOAD_WAIT = 5000;

	private static final TGLog LOG = TGLog.get(TGLivePlugins.class);
	// the refusal to open a plugin's editor, in TuxGuitar's language: {0} the plugin, {1} the host's reason (the
	// plugin's own: TGPluginStates.opened)
	private static final String EDITOR_NOT_OPENED = "audio.plugin.refusal.editor-not-opened";

	private final TGContext context;
	private final boolean live;
	// by key (see key): the instances of the renders that run
	private final Map<String, TGVst3Instance> running = new HashMap<String, TGVst3Instance>();
	// by key: the processors kept open while no render runs the element, and the element's parameters they were opened with
	private final Map<String, Kept> kept = new HashMap<String, Kept>();
	private final Map<String, Editor> editors = new HashMap<String, Editor>();
	// the channels whose chains the live synthesizer runs
	private final Set<Integer> played = new HashSet<Integer>();
	// the gestures ended, in order, until drained (the audio thread adds, the app's UI thread drains)
	private final ConcurrentLinkedQueue<Gesture> ended = new ConcurrentLinkedQueue<Gesture>();

	private TGLivePlugins(TGContext context, boolean live) {
		this.context = context;
		this.live = live;
	}

	public static TGLivePlugins getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGLivePlugins.class.getName(), key -> new TGLivePlugins(key, true));
	}

	private static String key(int channel, String element) {
		return channel + "/" + element;
	}

	// a live render runs the processor for the element of the channel's chain (one that runs no VST3 plugin is left out)
	public synchronized void bind(int channel, String element, Object processor) {
		TGVst3Instance instance = (processor instanceof TGVst3PluginProcessor ? ((TGVst3PluginProcessor) processor).getInstance() : null);
		if (instance != null && this.live) {
			this.running.put(key(channel, element), this.listened(channel, element, instance));
		}
	}

	// the processors of the synthesizer channel's chain (its source, when it has one, and its effects)
	public synchronized void bind(int channel, TGSynthChannel synthesizer) {
		TGProgram chain = synthesizer.getProgram();
		TGSynthChannelProcessor processors = synthesizer.getProcessor();
		if (chain.getReceiver() != null) {
			this.bind(channel, chain.getReceiver().getId(), processors.getProcessor());
		}
		for (int i = 0; i < chain.countOutputs(); i++) {
			this.bind(channel, chain.getOutput(i).getId(), processors.getAudioProcessor(chain.getOutput(i)));
		}
	}

	// the render that ran the channel's plugins ended
	public synchronized void unbind(int channel) {
		this.forget(channel);
		this.closeEditors();
	}

	private void forget(int channel) {
		this.running.keySet().removeIf(key -> key.startsWith(channel + "/"));
		this.played.remove(channel);
	}

	// the live synthesizer's channel runs the processors of its chain now, or none (closed): the editors of plugins that
	// still run there are told their plugins may hold other values (the chain ran again from the song's elements), and
	// an editor waiting for its plugin to run looks again
	public synchronized void chainChanged(TGSynthChannel synthesizer) {
		this.forget(synthesizer.getId());
		if (synthesizer.getProcessor() != null) {
			this.played.add(synthesizer.getId());
			this.bind(synthesizer.getId(), synthesizer);
		}
		this.closeEditors();
		this.editors.entrySet().stream().filter(editor -> editor.getKey().startsWith(synthesizer.getId() + "/")).forEach(editor -> editor.getValue().editor.changed());
		this.notifyAll();
	}

	private TGVst3Instance listened(int channel, String element, TGVst3Instance instance) {
		instance.setListener(new TGVst3Gestures(() -> this.ended.add(new Gesture(channel, element, instance))));
		return instance;
	}

	// the plugin of the element of the channel's chain as it runs
	public synchronized TGVst3Instance get(int channel, String element) throws TGVst3Exception {
		TGVst3Instance instance = this.running.get(key(channel, element));
		return (instance != null ? instance : this.kept(channel, element));
	}

	// the plugins of the renders that run, by channel then element
	public synchronized List<TGVst3Instance> getRunning() {
		return this.running.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());
	}

	// the plugin of the element as it runs, or the one kept open for it while it follows the song's element (it was
	// opened from what the element holds, or the element took its state since); null when neither is (none is opened)
	public synchronized TGVst3Instance find(int channel, String element) {
		Kept kept = this.kept.get(key(channel, element));
		boolean follows = (kept != null && kept.follows(TGSoundChain.element(this.chain(channel), element)));
		return this.running.getOrDefault(key(channel, element), (follows ? kept.instance : null));
	}

	private TGVst3Instance current(String key) {
		Kept kept = this.kept.get(key);
		return this.running.getOrDefault(key, (kept != null ? kept.instance : null));
	}

	// the plugin kept open for the element, opened again when the song's element is not what it was opened with
	private TGVst3Instance kept(int channel, String element) throws TGVst3Exception {
		TGProgram chain = this.chainOf(channel, element);
		TGProgramElement song = TGSoundChain.element(chain, element);
		Kept kept = this.kept.get(key(channel, element));
		if (kept == null || !kept.follows(song)) {
			this.close(kept);
			kept = this.open(channel, chain, song);
			this.kept.put(key(channel, element), kept);
		}
		return kept.instance;
	}

	// as the TuxGuitar Synthesizer opens it, at the audio device's rate; refused naming the plugin and the host's reason
	private Kept open(int channel, TGProgram chain, TGProgramElement element) throws TGVst3Exception {
		TGAudioProcessor processor = TGPluginStates.opened(this.context, element, chain);
		TGVst3Instance instance = ((TGVst3PluginProcessor) processor).getInstance();
		return new Kept(processor, this.listened(channel, element.getId(), instance), element.getParameters());
	}

	private void close(Kept kept) {
		if (kept != null) {
			kept.processor.close();
		}
	}

	// the sound chain of the channel of the document's song
	private TGProgram chain(int channel) {
		return TGSoundChain.get(this.channel(channel));
	}

	private TGChannel channel(int channel) {
		TGDocumentManager document = TGDocumentManager.getInstance(this.context);
		return document.getSongManager().getChannel(document.getSong(), channel);
	}

	// the chain the element runs in: the song's, else, for the default instrument the channel plays as its source, a
	// chain of its own
	private TGProgram chainOf(int channel, String element) {
		TGProgram chain = this.chain(channel);
		TGProgramElement played = (TGSoundChain.element(chain, element) == null ? this.byDefault(channel, element) : null);
		return (played != null ? TGPluginStates.alone(played) : chain);
	}

	// the default instrument the channel plays as its source, when it is the element; else null
	private TGProgramElement byDefault(int channel, String element) {
		TGProgramElement played = TGDefaultInstrumentSource.install(this.context).edited(this.channel(channel));
		return (played != null && played.getId().equals(element) ? played : null);
	}

	// each plugin kept open whose element no render runs (nothing else calls it) is asked for its first parameter, so
	// what its editor did since comes with the reply; one without parameters is not asked. Any thread may poll
	public synchronized void poll() {
		this.kept.forEach((key, kept) -> {
			if (!this.running.containsKey(key)) {
				ask(kept.instance);
			}
		});
	}

	private static void ask(TGVst3Instance instance) {
		instance.getParameters().stream().findFirst().ifPresent(first -> read(instance, first.getId()));
	}

	private static void read(TGVst3Instance instance, int parameter) {
		try {
			instance.getParameter(parameter);
		} catch (TGVst3Exception e) {
			// its host stopped: there is nothing more to hear from it
			LOG.debug("a VST3 plugin kept open was not asked: " + e.getMessage());
		}
	}

	// whether a gesture ended since the last drain (any thread may ask)
	public boolean hasEnded() {
		return !this.ended.isEmpty();
	}

	// the gestures ended since the last drain change the song: one change per element, with its plugin's state now, in
	// the order of each element's last gesture. Run where the app edits the song (its UI thread)
	public void drain() {
		Map<String, Gesture> last = new LinkedHashMap<String, Gesture>();
		for (Gesture gesture = this.ended.poll(); gesture != null; gesture = this.ended.poll()) {
			last.remove(key(gesture.channel, gesture.element));
			last.put(key(gesture.channel, gesture.element), gesture);
		}
		last.values().forEach(gesture -> this.store(gesture.channel, gesture.element, gesture.instance));
	}

	// the song's element keeps the plugin's state, one change of the song; none when the song holds that state already
	// (an editor closed having changed nothing). A plugin that is no element of the song's chain is the default
	// instrument the channel plays (TGDefaultInstrumentSource): kept in the song when its editor made the change, else
	// not (it loaded its profile: a setting of the computer, not of the song)
	private void store(int channel, String element, TGVst3Instance instance) {
		boolean song = (TGSoundChain.element(this.chain(channel), element) != null);
		try {
			TGVst3State state = instance.getState();
			if (!instance.isAgreed(state) && (song || this.edits(channel, element))) {
				this.take(channel, element, instance, state, song);
			}
		} catch (TGVst3Exception | TGActionException e) {
			// the plugin is gone, or the song refused the change: the song stays as it was
			LOG.warn("the state of the VST3 plugin of " + element + " on channel " + channel + " was not kept: " + e.getMessage());
		}
	}

	// whether the element is the default instrument the channel plays as its source, edited in the editor open for it
	private synchronized boolean edits(int channel, String element) {
		Editor editor = this.editors.get(key(channel, element));
		return (editor != null && editor.edits(this.byDefault(channel, element)));
	}

	// on an action context of its own: the song's element takes the state; or the default instrument is kept in the song
	// as the channel's source with it, its plugin (and editor) the source's from then on
	private void take(int channel, String element, TGVst3Instance instance, TGVst3State state, boolean song) {
		TGDocumentManager document = TGDocumentManager.getInstance(this.context);
		TGActionContext change = new TGActionContext() {
		};
		change.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG_MANAGER, document.getSongManager());
		change.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, document.getSong());
		change.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_CHANNEL, this.channel(channel));
		change.setAttribute(TGSetPluginStateAction.ATTRIBUTE_ELEMENT, element);
		change.setAttribute(TGSetPluginStateAction.ATTRIBUTE_STATE, state);
		change.setAttribute(TGKeepDefaultInstrumentAction.ATTRIBUTE_STATE, state);
		instance.agree(state);
		String kept = (song ? element : this.handOver(channel, element, TGSoundChain.source(TGVst3Element.INSTRUMENT).getId(), instance));
		TGActionManager.getInstance(this.context).execute(song ? TGSetPluginStateAction.NAME : TGKeepDefaultInstrumentAction.NAME, change);
		this.took(channel, kept, instance);
	}

	// the plugin of the element is the other element's from now on (the default instrument kept as the song's source):
	// what it runs as, is kept open as, and its editor; what its editor does is the other element's
	private synchronized String handOver(int channel, String from, String to, TGVst3Instance instance) {
		this.listened(channel, to, instance);
		move(this.running, key(channel, from), key(channel, to));
		move(this.kept, key(channel, from), key(channel, to));
		move(this.editors, key(channel, from), key(channel, to));
		return to;
	}

	private static <T> void move(Map<String, T> map, String from, String to) {
		Optional.ofNullable(map.remove(from)).ifPresent(value -> map.put(to, value));
	}

	// the plugin kept for the element did the change the song's element now holds, or gave the state it holds: it is not
	// opened again for it
	synchronized void took(int channel, String element, TGVst3Instance instance) {
		Kept kept = this.kept.get(key(channel, element));
		if (kept != null && kept.instance == instance) {
			this.kept.put(key(channel, element), new Kept(kept.processor, instance, TGSoundChain.element(this.chain(channel), element).getParameters()));
		}
	}

	// the plugin's own editor, shown in front in a window the owner window owns (a native window handle, TuxGuitar's
	// window; 0: none) with the title as its caption, or TuxGuitar's parameter editor for a plugin without one
	public synchronized TGPluginEditor openEditor(int channel, String element, long owner, String title) throws TGVst3Exception {
		return this.openEditor(channel, element, owner, title, LOAD_WAIT);
	}

	// waiting at most the milliseconds while the live synthesizer runs the channel but not the element yet (one just
	// added: the app sends the synthesizer the chain a moment later), so the editor is the running plugin's
	synchronized TGPluginEditor openEditor(int channel, String element, long owner, String title, long wait) throws TGVst3Exception {
		long end = System.currentTimeMillis() + wait;
		while (this.played.contains(channel) && !this.running.containsKey(key(channel, element)) && System.currentTimeMillis() < end) {
			this.await(end - System.currentTimeMillis());
		}
		TGVst3Instance instance = this.get(channel, element);
		TGPluginEditor editor = new TGPluginEditor(this.ownEditor(instance, channel, element, owner, title), () -> rows(instance));
		this.editors.put(key(channel, element), new Editor(editor, instance, this.edited(channel, element, instance)));
		return editor;
	}

	// the default instrument the channel plays as its source, when the element is, with its plugin's state once restored,
	// taken as what the song has (a change is what differs from it); else null
	private Default edited(int channel, String element, TGVst3Instance instance) throws TGVst3Exception {
		TGProgramElement played = (TGSoundChain.element(this.chain(channel), element) == null ? this.byDefault(channel, element) : null);
		if (played == null) {
			return null;
		}
		TGPluginStates.restored(instance);
		Default edited = new Default(played, instance.getState());
		instance.agree(edited.state);
		return edited;
	}

	// whether the plugin opened an editor of its own; refused naming the plugin and the reason
	private boolean ownEditor(TGVst3Instance instance, int channel, String element, long owner, String title) throws TGVst3Exception {
		try {
			return instance.openEditor(owner, title);
		} catch (TGVst3Exception e) {
			String plugin = TGVst3Element.getName(TGSoundChain.element(this.chain(channel), element));
			throw new TGVst3Exception(TGMessagesManager.getProperty(EDITOR_NOT_OPENED, new Object[] {plugin, e.getLocalizedMessage()}), e);
		}
	}

	// until the chain changes or the milliseconds pass (at least one: no wait without end); interrupted, the editor waits
	// no longer
	private void await(long millis) throws TGVst3Exception {
		try {
			this.wait(Math.max(1, millis));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new TGVst3Exception("interrupted while waiting for the plugin to run");
		}
	}

	// the rows TuxGuitar's parameter editor shows, at the values the plugin holds: not the parameter choosing presets
	private static List<TGPluginParameterRow> rows(TGVst3Instance instance) throws TGVst3Exception {
		List<TGPluginParameterRow> rows = new ArrayList<TGPluginParameterRow>();
		for (TGVst3Parameter parameter : instance.getParameters()) {
			if (!parameter.choosesPresets()) {
				rows.add(new TGPluginParameterRow(parameter.getId(), parameter.getTitle(), instance.getParameter(parameter.getId()), parameter.getMinimum(), parameter.getMaximum(), parameter.getSteps()));
			}
		}
		return rows;
	}

	// the editor open for the element, or null
	public synchronized TGPluginEditor getEditor(int channel, String element) {
		Editor editor = this.editors.get(key(channel, element));
		return (editor != null ? editor.editor : null);
	}

	// TuxGuitar's parameter editor open for the channel's effect (by its number; none: the VST3 instrument that is the
	// source, or the default instrument played there), or null when that plugin's editor is its own or none is open
	public synchronized TGPluginEditor parameterEditor(TGChannel channel, Integer effect) {
		TGProgramElement plugin = TGDefaultInstrumentSource.install(this.context).edited(channel, effect);
		TGPluginEditor editor = (plugin != null ? this.getEditor(channel.getChannelId(), plugin.getId()) : null);
		return (editor != null && !editor.isOwn() ? editor : null);
	}

	// the default instrument the channel plays as its source as the song keeps it, its state as when its editor opened,
	// when that editor (TuxGuitar's parameter editor: the plugin has none of its own) is open for it and shows another
	// value of the parameter (as the user sees it): the first value set there keeps it, as the first change in a
	// plugin's own editor does, its plugin and editor the source's from then on (see take); else null
	public synchronized TGProgramElement keepDefault(TGChannel channel, int parameter, double plain) {
		int id = channel.getChannelId();
		TGProgramElement played = TGDefaultInstrumentSource.install(this.context).edited(channel);
		Editor editor = (played != null ? this.editors.get(key(id, played.getId())) : null);
		if (editor == null || !editor.edits(played) || editor.editor.shows(parameter, plain)) {
			return null;
		}
		this.handOver(id, played.getId(), TGSoundChain.source(TGVst3Element.INSTRUMENT).getId(), editor.instance);
		return TGPluginStates.kept(played, editor.byDefault.state);
	}

	// the editors whose plugin is no longer the element's: one whose plugin runs as another element of the channel now
	// (handed over) goes on as that element's editor when it may (see goesOn); the others close
	private void closeEditors() {
		Map<String, Editor> moved = new HashMap<String, Editor>();
		this.editors.entrySet().removeIf(editor -> editor.getValue().instance != this.current(editor.getKey()) && this.leaves(editor.getValue(), moved));
		this.editors.putAll(moved);
	}

	// the editor leaves its element: for the one its plugin runs as now, when it goes on there, else closed
	private boolean leaves(Editor editor, Map<String, Editor> moved) {
		Optional<String> now = this.running.entrySet().stream().filter(running -> running.getValue() == editor.instance).map(Map.Entry::getKey).findFirst();
		now.filter(key -> this.goesOn(editor, key)).ifPresentOrElse(key -> moved.put(key, editor), editor::close);
		return true;
	}

	// an editor goes on as the editor of an element of the song's chain; as the default instrument's only when it was
	// opened for that default as the channel plays it now (its plugin agreeing with that default as it was then)
	private boolean goesOn(Editor editor, String key) {
		int slash = key.indexOf('/');
		int channel = Integer.parseInt(key.substring(0, slash));
		String element = key.substring(slash + 1);
		return (TGSoundChain.element(this.chain(channel), element) != null || editor.agrees(this.byDefault(channel, element)));
	}

	// the plugins kept open close (their editors with them)
	public synchronized void close() {
		new ArrayList<Kept>(this.kept.values()).forEach(this::close);
		this.kept.clear();
		this.running.clear();
		this.played.clear();
		this.editors.values().forEach(editor -> editor.editor.close());
		this.editors.clear();
	}

	private static final class Kept {
		private final TGAudioProcessor processor;
		private final TGVst3Instance instance;
		private final Map<String, String> parameters;

		Kept(TGAudioProcessor processor, TGVst3Instance instance, Map<String, String> parameters) {
			this.processor = processor;
			this.instance = instance;
			this.parameters = new HashMap<String, String>(parameters);
		}

		// whether it holds what the song's element holds: opened from it, or the element took its state since
		boolean follows(TGProgramElement element) {
			return this.parameters.equals(element.getParameters());
		}
	}

	// the default instrument an editor was opened for, as it was then: its element's parameters, its plugin's state
	private static final class Default {
		private final Map<String, String> parameters;
		private final TGVst3State state;

		Default(TGProgramElement played, TGVst3State state) {
			this.parameters = new HashMap<String, String>(played.getParameters());
			this.state = state;
		}
	}

	private static final class Editor {
		private final TGPluginEditor editor;
		private final TGVst3Instance instance;
		// the default instrument it was opened for (null: an element of the song)
		private final Default byDefault;

		Editor(TGPluginEditor editor, TGVst3Instance instance, Default byDefault) {
			this.editor = editor;
			this.instance = instance;
			this.byDefault = byDefault;
		}

		// whether it edits the default instrument played now (it was opened for it, and the default is still that one)
		boolean edits(TGProgramElement played) {
			return (played != null && this.byDefault != null && played.getParameters().equals(this.byDefault.parameters));
		}

		// whether it edits the default played now, its plugin agreeing with that default as it was when it opened (what
		// differs from it is a change)
		boolean agrees(TGProgramElement played) {
			boolean edits = this.edits(played);
			if (edits) {
				this.instance.agree(this.byDefault.state);
			}
			return edits;
		}

		// the plugin's own editor closes (a plugin that is gone has none left to close), TuxGuitar's with it
		boolean close() {
			try {
				this.instance.closeEditor();
			} catch (TGVst3Exception e) {
				// its host stopped, the editor with it
			}
			this.editor.close();
			return true;
		}
	}

	private static final class Gesture {
		private final int channel;
		private final String element;
		private final TGVst3Instance instance;

		Gesture(int channel, String element, TGVst3Instance instance) {
			this.channel = channel;
			this.element = element;
			this.instance = instance;
		}
	}
}
