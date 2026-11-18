package app.tuxguitar.vst3;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import app.tuxguitar.midi.synth.TGAudioBuffer;
import app.tuxguitar.midi.synth.TGAudioProcessor;
import app.tuxguitar.player.base.MidiTransport;

/**
 * A VST3 plugin as a processor of the TuxGuitar Synthesizer: its element's parameters open it in the host process (the
 * plugin they name, its state, the parameters set since), and each block goes through it in 32-bit float. When the host
 * process stops, the plugin is gone: it sounds nothing, and the hosting reports the stop.
 */
abstract class TGVst3Processor implements TGAudioProcessor, TGVst3PluginProcessor {

	static final int FRAMES = TGAudioBuffer.BUFFER_SIZE / 2;
	// the message Woodshed Guitar loads a profile by ("path": the file, UTF-8)
	private static final String LOAD = "Woodshed.load";

	private final TGVst3Hosting hosting;
	private final boolean instrument;
	private final float[][] block = new float[TGAudioBuffer.CHANNELS][FRAMES];
	// the part of a block told one span of the transport
	private final float[][] part = new float[TGAudioBuffer.CHANNELS][FRAMES];
	// what the song does over each block, as the clock's owner writes it; stopped until one is given
	private MidiTransport transport = new MidiTransport();
	private float sampleRate = TGAudioBuffer.SAMPLE_RATE;
	private TGVst3Plugin plugin;
	// the plugin as it runs: written by the processor's own changes and by a restart, read by the audio thread
	private volatile TGVst3Instance instance;
	// guards the swaps of instance (never held over a host's request)
	private final Object swap = new Object();
	// the element's parameters it was last given, and whether it was started again since (once: a plugin that stops
	// again is left silent until it is given its element again)
	private Map<String, String> parameters;
	private boolean restarted;
	// why no plugin is open (null while one is, and before the first)
	private TGVst3Exception problem;
	// the state the plugin opened with
	private TGVst3State opened;
	// the profile it was asked to load since it opened or last took an element's state (see load), or null
	private String profile;

	TGVst3Processor(TGVst3Hosting hosting, boolean instrument) {
		this.hosting = hosting;
		this.instrument = instrument;
	}

	public void setSampleRate(float sampleRate) {
		this.sampleRate = sampleRate;
	}

	// the plugin the parameters name (opened again when it is another one), then its state and the parameters set since
	public synchronized void restoreParameters(Map<String, String> parameters) {
		TGVst3Plugin wanted = TGVst3Element.plugin(parameters, this.instrument);
		this.parameters = new java.util.HashMap<String, String>(parameters);
		this.restarted = false;
		try {
			if (!this.plays(wanted)) {
				this.open(wanted);
			}
			this.restore(parameters);
		} catch (TGVst3Exception e) {
			this.lost(e, this.instance);
		}
	}

	private boolean plays(TGVst3Plugin wanted) {
		return (this.instance != null && wanted != null && wanted.getModule().equals(this.plugin.getModule()) && wanted.getClassId().equals(this.plugin.getClassId()));
	}

	// the plugin, keeping the state it opened with (what an element without a state of its own is)
	private void open(TGVst3Plugin wanted) throws TGVst3Exception {
		this.close();
		this.plugin = wanted;
		if (wanted == null) {
			throw new TGVst3Exception("the element names no VST3 plugin");
		}
		TGVst3Instance opened = this.hosting.open(wanted, this.sampleRate, FRAMES);
		synchronized (this.swap) {
			this.instance = opened;
		}
		opened.getHost().whenStopped(() -> this.stopped(opened));
		this.opened = opened.getState();
		this.problem = null;
		this.profile = null;
	}

	// its host stopped by itself: restarting (TuxGuitar plays live), the plugin is opened again from its element, off the
	// thread that found it, once until it is given its element again
	private void stopped(TGVst3Instance gone) {
		if (this.hosting.isRestarting()) {
			this.hosting.restartLater(() -> this.restart(gone));
		}
	}

	private void restart(TGVst3Instance gone) {
		TGVst3Host host = gone.getHost();
		TGVst3Instance started = (this.restarts(gone) ? this.reopen() : null);
		this.hosting.restarted(gone, started, host.wasSilent(), host.getStop());
	}

	// whether the plugin that stopped starts again: still what it runs (or dropped), its element known, not started
	// again since it was given it
	private synchronized boolean restarts(TGVst3Instance gone) {
		boolean restarts = (!this.restarted && this.parameters != null && (this.instance == gone || this.instance == null));
		this.restarted = (this.restarted || restarts);
		return restarts;
	}

	// opened again from the element it was last given; null when it does not open
	private synchronized TGVst3Instance reopen() {
		try {
			this.open(this.plugin);
			this.restore(this.parameters);
		} catch (TGVst3Exception e) {
			this.lost(e, this.instance);
		}
		return this.instance;
	}

	// the element's state, else the one the plugin opened with (a plugin running an element the song changed goes back to
	// it; one running the same element again keeps what its editor did since); an element without a state that names a
	// profile (a default instrument's) has it loaded, from the state it opened with
	private void restore(Map<String, String> parameters) throws TGVst3Exception {
		if (this.instance == null) {
			return;
		}
		TGVst3State state = TGVst3Element.state(parameters);
		String profile = parameters.get(TGVst3Element.PROFILE);
		if (state == null && profile != null) {
			this.load(profile);
			return;
		}
		this.instance.restore(state != null ? state : this.opened, TGVst3Element.parameters(parameters));
		this.profile = null;
	}

	// Woodshed Guitar loads the profile, as its editor's Load profile... does, in the background (it restores its state
	// meanwhile, so Play waits for it, see isBusy), unless it was asked to since it opened or last took an element's state
	// (a default kept in the song, then the keep undone: the default is as it was opened afresh, its editor's changes gone).
	// Asked by the host, the load is no change the user made: the plugin does not tell it (nothing is kept in the song)
	private void load(String profile) throws TGVst3Exception {
		if (!profile.equals(this.profile)) {
			this.instance.restore(this.opened, Collections.<Integer, Double>emptyMap());
			this.instance.message(LOAD, Collections.singletonMap("host", 1L), Collections.singletonMap("path", profile.getBytes(StandardCharsets.UTF_8)));
			this.profile = profile;
		}
	}

	public synchronized void storeParameters(Map<String, String> parameters) {
		TGVst3Instance stored = this.instance;
		try {
			if (stored != null) {
				TGVst3State state = stored.getState();
				TGVst3Element.store(parameters, this.plugin, state);
				stored.agree(state);
			}
		} catch (TGVst3Exception e) {
			this.lost(e, stored);
		}
	}

	public void fillBuffer(TGAudioBuffer buffer) {
		buffer.read(this.block);
		this.process(this.block);
		buffer.write(this.block);
	}

	public void setTransport(MidiTransport transport) {
		this.transport = transport;
	}

	// the block in parts, one per span of the transport, each told its span, with the notes on its frames
	public void process(float[][] samples) {
		TGVst3Instance playing = this.instance;
		try {
			if (playing != null) {
				List<TGVst3NoteEvent> events = this.takeEvents();
				MidiTransport told = this.transport;
				for (int span = 0; span < told.count(); span++) {
					int from = told.get(span).getFrame();
					int to = (span + 1 < told.count() ? told.get(span + 1).getFrame() : FRAMES);
					this.process(playing, samples, from, to, transport(told.get(span)), TGVst3NoteEvent.between(events, from, to), told.isOffline());
				}
				return;
			}
		} catch (TGVst3Exception e) {
			this.lost(e, playing);
		}
		for (float[] side : samples) {
			java.util.Arrays.fill(side, 0f);
		}
	}

	private void process(TGVst3Instance playing, float[][] samples, int from, int to, TGVst3Transport transport, List<TGVst3NoteEvent> events, boolean offline) throws TGVst3Exception {
		for (int side = 0; side < samples.length; side++) {
			System.arraycopy(samples[side], from, this.part[side], 0, to - from);
		}
		playing.process(this.part, to - from, transport, events, offline);
		for (int side = 0; side < samples.length; side++) {
			System.arraycopy(this.part[side], 0, samples[side], from, to - from);
		}
	}

	private static TGVst3Transport transport(MidiTransport.Span state) {
		return new TGVst3Transport(state.isPlaying(), state.getTempo(), state.getNumerator(), state.getDenominator(), state.getQuarters(), state.getSamples());
	}

	// the note events of the block about to be processed
	List<TGVst3NoteEvent> takeEvents() {
		return Collections.emptyList();
	}

	// the host process stopped and the plugin with it (or refused it): closed, it is silent until opened again, and the
	// reason kept; only when the plugin that failed is still the one it runs (a restart may have opened another)
	private void lost(TGVst3Exception reason, TGVst3Instance failed) {
		if (this.dropped(failed)) {
			this.problem = reason;
			java.util.Optional.ofNullable(failed).ifPresent(TGVst3Instance::close);
		}
	}

	// whether the plugin that failed (null: none opened) was the one it runs: it runs none from now on
	private boolean dropped(TGVst3Instance failed) {
		synchronized (this.swap) {
			boolean dropped = (this.instance == failed);
			this.instance = (dropped ? null : this.instance);
			return dropped;
		}
	}

	// as the user is told it, when it is read (TGVst3Exception.getLocalizedMessage)
	public String getProblem() {
		return (this.problem != null ? this.problem.getLocalizedMessage() : null);
	}

	public String getLoggedProblem() {
		return (this.problem != null ? this.problem.getMessage() : null);
	}

	public int getLatency() {
		return (this.instance != null ? this.instance.getLatency() : 0);
	}

	// a plugin that never stops sounding has an endless tail
	public int getTail() {
		if (this.instance == null) {
			return 0;
		}
		int tail = this.instance.getTail();
		return (tail == TGVst3Instance.INFINITE_TAIL ? TGAudioProcessor.INFINITE_TAIL : tail);
	}

	// the plugin as it is open in the host process, or null
	public TGVst3Instance getInstance() {
		return this.instance;
	}

	public boolean isOpen() {
		return (this.instance != null);
	}

	// while its plugin restores its state in the background, for a while at most (see TGVst3Instance.isRestoring): Play
	// waits for it, so the first note sounds as the plugin's state says
	public boolean isBusy() {
		TGVst3Instance instance = this.instance;
		return (instance != null && instance.isRestoring());
	}

	public void close() {
		TGVst3Instance closing;
		synchronized (this.swap) {
			closing = this.instance;
			this.instance = null;
		}
		if (closing != null) {
			closing.close();
		}
	}

	public void finalize() {
	}
}
