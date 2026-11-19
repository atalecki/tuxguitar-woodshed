package app.tuxguitar.vst3;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import app.tuxguitar.util.log.TGLog;

/**
 * A plugin open in the VST3 host process: it processes blocks of up to the maximum block agreed when it opened, keeps
 * the latency and tail it last reported, and whether it restores its state in the background (and what it said that
 * restoration does), and hands what it tells
 * its host (edits from its own editor, restarts) to its listener. Its parameters are set and read by plain value.
 */
public final class TGVst3Instance implements AutoCloseable {

	// a tail that never ends (a drone, say)
	public static final int INFINITE_TAIL = -1;
	// how long a state restoration in the background is waited for at most, in milliseconds from when it was first
	// heard: a plugin that never says it has finished is not waited for forever
	public static final long RESTORING_WAIT = 5000;
	// the flag of a parameter that chooses the plugin's presets (Vst::ParameterInfo::kIsProgramChange)
	private static final int PROGRAM_CHANGE = 1 << 15;

	// what the reply to an open editor request says (see TGVst3Protocol), as the log says it
	private static final String[] EDITOR_OPENED = {": no editor of its own", ": editor opened, unowned", ": editor opened, owned by TuxGuitar's window"};

	private static final TGLog LOG = TGLog.get(TGVst3Instance.class);

	private final TGVst3Host host;
	private final TGVst3SharedBlocks blocks;
	private final String name;
	private final int id;
	private final int inputs;
	private final int outputs;
	private final boolean editor;
	private final List<TGVst3Parameter> parameters;
	// set by the reader thread of the host when a late reply comes
	private volatile int latency;
	private volatile int tail;
	// its state restoration in the background as last heard, null while it is not restoring
	private volatile Restoring restoring;
	private TGVst3Listener listener;
	// the state and the parameters set since that the song and the plugin last agreed on (the song gave them, or took the
	// state), null before any
	private volatile Map.Entry<TGVst3State, Map<Integer, Double>> agreed;

	private TGVst3Instance(TGVst3Host host, TGVst3SharedBlocks blocks, String name, int id, int inputs, int outputs, boolean editor, List<TGVst3Parameter> parameters) {
		this.host = host;
		this.blocks = blocks;
		this.name = name;
		this.id = id;
		this.inputs = inputs;
		this.outputs = outputs;
		this.editor = editor;
		this.parameters = parameters;
	}

	// reads the reply to an open request of the plugin of that name
	static TGVst3Instance opened(TGVst3Host host, TGVst3SharedBlocks blocks, String name, DataInputStream in) throws IOException {
		int id = in.readInt();
		int inputs = in.readInt();
		int outputs = in.readInt();
		boolean editor = (in.readInt() == 1);
		List<TGVst3Parameter> parameters = new ArrayList<TGVst3Parameter>();
		for (int count = in.readInt(); count > 0; count--) {
			parameters.add(new TGVst3Parameter(in.readInt(), TGVst3Request.readString(in), in.readDouble(), in.readDouble(), in.readDouble(), in.readInt(), (in.readInt() & PROGRAM_CHANGE) != 0));
		}
		TGVst3Instance instance = new TGVst3Instance(host, blocks, name, id, inputs, outputs, editor, Collections.unmodifiableList(parameters));
		instance.readTiming(in);
		host.opened(id, instance);
		return instance;
	}

	private Void readTiming(DataInputStream in) throws IOException {
		this.latency = in.readInt();
		this.tail = in.readInt();
		this.heardRestoring(in.readInt() == 1, TGVst3Request.readString(in));
		return null;
	}

	// a restoration heard again keeps when it was first heard
	private void heardRestoring(boolean restoring, String what) {
		Restoring heard = this.restoring;
		this.restoring = (!restoring ? null : new Restoring(heard != null ? heard.since : System.nanoTime(), what));
	}

	private TGVst3Request request(int operation) {
		return new TGVst3Request(operation).writeInt(this.id);
	}

	// "VST3 plugin name", as the log names it
	private String label() {
		return "VST3 plugin " + this.name;
	}

	// the plugin's name, as its module says
	public String getName() {
		return this.name;
	}

	// the host process it runs in
	public TGVst3Host getHost() {
		return this.host;
	}

	// the channels of its main input (none for an instrument) and main output
	public int getInputs() {
		return this.inputs;
	}

	public int getOutputs() {
		return this.outputs;
	}

	public boolean hasEditor() {
		return this.editor;
	}

	// as they were when it opened
	public List<TGVst3Parameter> getParameters() {
		return this.parameters;
	}

	// the id of the parameter with the title, or -1
	public int findParameter(String title) {
		for (TGVst3Parameter parameter : this.parameters) {
			if (parameter.getTitle().equals(title)) {
				return parameter.getId();
			}
		}
		return -1;
	}

	// in samples, as last reported
	public int getLatency() {
		return this.latency;
	}

	// in samples, or INFINITE_TAIL, as last reported
	public int getTail() {
		return this.tail;
	}

	// whether it restores its state in the background (a capture loading on a thread of its own: it does not play as its
	// state says yet), as it told its host (IProgress), for RESTORING_WAIT at most. Its host is asked only while it was
	// last heard restoring; a host that stopped has nothing to wait for. Any thread may ask
	public boolean isRestoring() {
		return this.isRestoring(RESTORING_WAIT);
	}

	// for the milliseconds at most
	boolean isRestoring(long wait) {
		Restoring heard = this.restoring;
		return (heard != null && System.nanoTime() - heard.since < TimeUnit.MILLISECONDS.toNanos(wait) && this.getRestoring() != null);
	}

	// what it restores in the background, as it said when it began (its description: the file it loads, say; empty when
	// it said nothing), however long it has; null when it does not. Its host is asked only while it was last heard
	// restoring; a host that stopped has nothing to restore. Any thread may ask
	public String getRestoring() {
		if (this.restoring == null) {
			return null;
		}
		try {
			this.host.call(this.request(TGVst3Protocol.TIMING), this::readTiming);
		} catch (TGVst3Exception e) {
			LOG.debug(this.label() + ": not asked whether it still restores its state, its host is gone: " + e.getMessage());
			return null;
		}
		Restoring heard = this.restoring;
		return (heard != null ? heard.what : null);
	}

	// live: the first frames of the block's two sides go through the plugin, in place, with the events on their samples;
	// false, and the frames silent, when the plugin's reply is late (see TGVst3Host)
	public boolean process(float[][] block, int frames, TGVst3Transport transport, List<TGVst3NoteEvent> events) throws TGVst3Exception {
		return this.process(block, frames, transport, events, false);
	}

	// offline (an export) the block waits for its reply, however late it is, unless the host is taken for stopped
	public boolean process(float[][] block, int frames, TGVst3Transport transport, List<TGVst3NoteEvent> events, boolean offline) throws TGVst3Exception {
		boolean answered = (!this.host.isLate() && this.processInTime(block, frames, transport, events, offline));
		if (!answered) {
			for (float[] side : block) {
				java.util.Arrays.fill(side, 0, frames, 0f);
			}
		}
		return answered;
	}

	private boolean processInTime(float[][] block, int frames, TGVst3Transport transport, List<TGVst3NoteEvent> events, boolean offline) throws TGVst3Exception {
		this.blocks.write(block, frames);
		TGVst3Request request = this.request(TGVst3Protocol.PROCESS).writeInt(frames);
		request.writeInt(transport.isPlaying() ? 1 : 0).writeDouble(transport.getTempo()).writeInt(transport.getNumerator())
			.writeInt(transport.getDenominator()).writeDouble(transport.getQuarters()).writeLong(transport.getSamples()).writeInt(offline ? 1 : 0);
		request.writeInt(events.size());
		for (TGVst3NoteEvent event : events) {
			request.writeInt(event.getType()).writeInt(event.getOffset()).writeInt(event.getPitch()).writeInt(event.getNoteId()).writeFloat(event.getValue());
		}
		boolean answered = this.host.callInTime(request, this::readTiming, offline);
		if (answered) {
			this.blocks.read(block, frames);
		}
		return answered;
	}

	// the plain value the plugin took
	public double setParameter(int parameter, double plain) throws TGVst3Exception {
		return this.host.call(this.request(TGVst3Protocol.SET_PARAMETER).writeInt(parameter).writeDouble(plain), in -> {
			double taken = in.readDouble();
			this.readTiming(in);
			return taken;
		});
	}

	public double getParameter(int parameter) throws TGVst3Exception {
		return this.host.call(this.request(TGVst3Protocol.GET_PARAMETER).writeInt(parameter), in -> in.readDouble());
	}

	public TGVst3State getState() throws TGVst3Exception {
		return this.host.call(this.request(TGVst3Protocol.GET_STATE), in -> new TGVst3State(TGVst3Request.readBytes(in), TGVst3Request.readBytes(in), readResources(in)));
	}

	private static Map<String, byte[]> readResources(DataInputStream in) throws IOException {
		Map<String, byte[]> resources = new HashMap<String, byte[]>();
		for (int count = in.readInt(); count > 0; count--) {
			resources.put(TGVst3Request.readString(in), TGVst3Request.readBytes(in));
		}
		return resources;
	}

	public void setState(TGVst3State state) throws TGVst3Exception {
		TGVst3Request request = this.request(TGVst3Protocol.SET_STATE).writeBytes(state.getComponent()).writeBytes(state.getController());
		request.writeInt(state.getResources().size());
		state.getResources().forEach((name, bytes) -> request.writeString(name).writeBytes(bytes));
		this.host.call(request, this::readTiming);
	}

	// the song holds the state now, without parameters set since (it took the state from the plugin)
	public void agree(TGVst3State state) {
		this.agreed = agreement(state);
	}

	// whether the song already holds the state, without parameters set since: taking it again would change nothing
	public boolean isAgreed(TGVst3State state) {
		return agreement(state).equals(this.agreed);
	}

	private static Map.Entry<TGVst3State, Map<Integer, Double>> agreement(TGVst3State state) {
		return Map.entry(state, Collections.<Integer, Double>emptyMap());
	}

	// the song gives the plugin the state, then the parameters set since (by id, plain values): set, unless they are what
	// the two last agreed on, so what the plugin's own editor did since that the song has not taken (a capture loaded)
	// stays
	public void restore(TGVst3State state, Map<Integer, Double> parameters) throws TGVst3Exception {
		Map.Entry<TGVst3State, Map<Integer, Double>> wanted = Map.entry(state, Collections.unmodifiableMap(new HashMap<Integer, Double>(parameters)));
		if (!wanted.equals(this.agreed)) {
			this.setState(state);
			for (Map.Entry<Integer, Double> parameter : parameters.entrySet()) {
				this.setParameter(parameter.getKey(), parameter.getValue());
			}
			this.agreed = wanted;
		}
	}

	// what still sounds (tails) is dropped
	public void reset() throws TGVst3Exception {
		this.host.call(this.request(TGVst3Protocol.RESET), in -> null);
	}

	// the plugin's own editor, shown in front in a window of the host process that the owner window owns (a native window
	// handle; 0, or a handle that is no window: none), so it stays in front of that window and minimises with it; one
	// already open is brought to the front. The window's caption is the title. False when it has none (or the platform
	// is not supported)
	public boolean openEditor(long owner, String title) throws TGVst3Exception {
		return this.openEditor(TGEditorTarget.floating(owner), title).isOpen();
	}

	// the plugin's own editor where the target says: floating (see above), or inside a window of TuxGuitar, handing it
	// the keys the target passes, as the host's reply says (EMBED_EDITOR). A plugin has one view at a time: an editor open
	// elsewhere closes first
	public TGVst3EditorOpened openEditor(TGEditorTarget target, String title) throws TGVst3Exception {
		return (target.isEmbedded() ? this.embed(target, title) : this.floating(target, title));
	}

	private TGVst3EditorOpened embed(TGEditorTarget target, String title) throws TGVst3Exception {
		TGVst3Request request = this.request(TGVst3Protocol.EMBED_EDITOR).writeLong(target.getParent()).writeDouble(target.getMaxShare());
		request.writeString(title).writeInt(target.getKeys().length);
		for (int key : target.getKeys()) {
			request.writeInt(key);
		}
		TGVst3EditorOpened opened = this.host.call(request, TGVst3EditorOpened::read);
		LOG.debug(this.label() + ": editor " + opened);
		return opened;
	}

	private TGVst3EditorOpened floating(TGEditorTarget target, String title) throws TGVst3Exception {
		TGVst3Request request = this.request(TGVst3Protocol.OPEN_EDITOR).writeLong(target.getOwner()).writeString(title);
		for (int side : target.getFrame()) {
			request.writeInt(side);
		}
		int[] opened = this.host.call(request, in -> new int[] {in.readInt(), in.readInt()});
		LOG.debug(this.label() + EDITOR_OPENED[opened[0]] + (opened[0] == 2 ? " (" + TGVst3EditorWindow.awareness(opened[1]) + ")" : ""));
		return new TGVst3EditorOpened(TGVst3EditorOpened.Kind.values()[opened[0]], 0, 0, false, 0);
	}

	// its editor inside a window of TuxGuitar takes the width of that window's inside now, when its view can be
	// resized: the size it takes (kind NONE, 0 by 0, when no editor is inside a window)
	public TGVst3EditorOpened fitEditor() throws TGVst3Exception {
		return this.host.call(this.request(TGVst3Protocol.FIT_EDITOR), TGVst3EditorOpened::read, TGVst3Host.EDITOR_MILLIS);
	}

	// its own editor's window as the system shows it (for checks)
	public TGVst3EditorWindow getEditorWindow() throws TGVst3Exception {
		return this.host.call(this.request(TGVst3Protocol.EDITOR_WINDOW), TGVst3EditorWindow::read);
	}

	// TuxGuitar's window waits EDITOR_MILLIS at most (a host whose editor hangs is then taken for stopped)
	public void closeEditor() throws TGVst3Exception {
		this.host.call(this.request(TGVst3Protocol.CLOSE_EDITOR), in -> null, TGVst3Host.EDITOR_MILLIS);
		LOG.debug(this.label() + ": editor closed");
	}

	// a message to the plugin's controller; its answer (the binary attribute "report"), empty when it gave none
	public byte[] message(String messageId, Map<String, Long> integers, Map<String, byte[]> binaries) throws TGVst3Exception {
		TGVst3Request request = this.request(TGVst3Protocol.MESSAGE).writeString(messageId).writeInt(integers.size());
		integers.forEach((key, value) -> request.writeString(key).writeLong(value));
		request.writeInt(binaries.size());
		binaries.forEach((key, value) -> request.writeString(key).writeBytes(value));
		return this.host.call(request, in -> {
			byte[] report = TGVst3Request.readBytes(in);
			this.readTiming(in);
			return report;
		});
	}

	public void setListener(TGVst3Listener listener) {
		this.listener = listener;
	}

	void notice(TGVst3Notice notice) {
		if (this.listener != null) {
			this.listener.notice(notice);
		}
	}

	// closed in the host; a host that stopped, or that still owes this plugin's late reply, has nothing it can close
	public void close() {
		try {
			if (!this.host.isLate()) {
				this.host.call(this.request(TGVst3Protocol.CLOSE), in -> null);
			}
		} catch (TGVst3Exception e) {
			// the host is gone, and the plugin with it
			LOG.debug(this.label() + ": not closed in its host, which is gone: " + e.getMessage());
		}
		this.host.closed(this.id);
		this.blocks.close();
		LOG.info(this.label() + " closed");
	}

	// values as a plugin reads them from a binary attribute: doubles in the machine's byte order
	public static byte[] doubles(double... values) {
		ByteBuffer bytes = ByteBuffer.allocate(values.length * Double.BYTES).order(ByteOrder.nativeOrder());
		for (double value : values) {
			bytes.putDouble(value);
		}
		return bytes.array();
	}

	// a state restoration in the background: when it was first heard (System.nanoTime) and what it said it does
	private static final class Restoring {

		private final long since;
		private final String what;

		private Restoring(long since, String what) {
			this.since = since;
			this.what = what;
		}
	}
}
