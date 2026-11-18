package app.tuxguitar.vst3;

/**
 * The requests TuxGuitar sends the VST3 host process (native/host/main.cpp is the other side). A request is one of these
 * operations and its fields; its reply is a status (0 done, 1 refused, then the reason), the operation's fields, then
 * what the plugins told the host since the last reply. All of it big-endian, strings and bytes as a length and the
 * bytes (UTF-8 for strings). A plugin's timing is its latency, its tail, whether it is restoring its state in the
 * background (1, else 0: a state restoration it told of through IProgress is under way, so it does not yet play as its
 * state says) and what that restoration said it does (its description: the file it loads, say; empty for nothing).
 */
final class TGVst3Protocol {

	static final int VERSION = 9;

	// -> version, process id
	static final int HELLO = 1;
	// module path -> count, then for each: class id, name, instrument (1) or effect (0)
	static final int SCAN = 2;
	// module path, class id, sample rate (double), maximum block, shared file path -> instance, input and output
	// channels, editor (1 or 0), parameters (count, then id, title, plain value, plain values at either end of the
	// range, steps, flags), timing
	static final int OPEN = 3;
	// instance
	static final int CLOSE = 4;
	// instance, frames, transport (playing, tempo, numerator, denominator, quarters, samples, offline: 1 for a render
	// that is not live, the plugin may take its time), events (count, then type, offset, pitch, note id, value) ->
	// timing
	static final int PROCESS = 5;
	// instance, parameter, plain value -> plain value, timing
	static final int SET_PARAMETER = 6;
	// instance, parameter -> plain value
	static final int GET_PARAMETER = 7;
	// instance -> component state, controller state, the parts the plugin kept apart (count, then name and bytes; the
	// host tells it, through the state's stream, that it keeps parts apart)
	static final int GET_STATE = 8;
	// instance, component state, controller state, the parts kept apart (count, then name and bytes) -> timing
	static final int SET_STATE = 9;
	// instance
	static final int RESET = 10;
	// process id -> network sockets it listens on (-1: unknown)
	static final int LISTENERS = 11;
	// instance, owner window (a native window handle; 0: none), title of the window, the frame it opens in when it is
	// made (left, top, right, bottom on the screen; all 0: centred on its owner; moved onto a visible screen; its size
	// kept only when the view can be resized) -> the editor: none of its own (0),
	// opened in a window of its own (1), or in a window the owner window owns (2); shown in front either way, an editor
	// already open too, and titled; then the DPI awareness of the window owning it (see EDITOR_WINDOW)
	static final int OPEN_EDITOR = 12;
	// instance
	static final int CLOSE_EDITOR = 13;
	// instance, message id, integers (count, then key and long), binaries (count, then key and bytes) -> report, timing
	static final int MESSAGE = 14;
	// (for checks) instance -> its editor's window and that window's owner (native handles; 0: none), shown (1 or 0), in
	// front of the host's other shown windows (1 or 0), its frame on the screen (left, top, right, bottom), the size
	// inside the frame (width, height), its title, the DPI of its screen, whether it has a border the user can drag (1 or
	// 0), the window it is in (0: none), whether it is inside a window of TuxGuitar (1 or 0), the DPI awareness of its
	// window and of the window owning it or that it is in (each 0 unaware, 1 system aware, 2 per monitor, 3 per monitor
	// version 2; -1 for no window)
	static final int EDITOR_WINDOW = 15;
	// instance -> timing
	static final int TIMING = 16;
	// instance, parent window (a native handle), maximum share (double: of the height of the inside of the parent's
	// top-level window), title -> kind (0 none of its own, 3 inside the parent window, 4 too large: higher than that
	// share, or wider than the parent's inside and unable to shrink to it; nothing is kept open), width, height (the size
	// its view takes, or would), resizable (1 or 0), the width of the parent's inside (device pixels). Inside, the editor is a child of the parent window once the reply is sent, of the parent's DPI awareness, as
	// wide as the parent's inside when it can be resized; an editor open elsewhere closes first (a new view)
	static final int EMBED_EDITOR = 17;
	// instance -> kind (3: inside a window, 0: no editor inside one), width, height, resizable, the width of the window's
	// inside: an editor inside a window that can be resized takes the width of that window's inside now
	static final int FIT_EDITOR = 18;
	// -> nothing: whether the host still answers (the thread that runs the plugins' editors answers it)
	static final int PING = 19;

	private TGVst3Protocol() {
	}

	// the host's process id, once its greeting says it speaks this version
	static long greeted(int version, long pid) throws TGVst3Exception {
		if (version != VERSION) {
			throw new TGVst3Exception("the VST3 host speaks version " + version + ", not " + VERSION);
		}
		return pid;
	}
}
