package app.tuxguitar.vst3;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The window of a plugin's own editor as the system shows it, read from the host process (for checks): its native
 * handle, its owner's and that of the window it is in (0: none), whether it is shown and in front of the host's other
 * windows, its frame on the screen, the size of the area inside the frame, where the plugin's view is, its caption, the
 * DPI it is drawn at, and its DPI awareness and that of the window owning it or that it is in.
 */
public final class TGVst3EditorWindow {

	// the DPI awareness of a window, as Windows has it: none (no window), unaware (scaled up by Windows), aware of the
	// main screen's DPI (scaled by Windows on the others), per monitor, per monitor version 2
	public static final int NO_WINDOW = -1;
	public static final int UNAWARE = 0;
	public static final int AWARE_SYSTEM = 1;
	public static final int AWARE_PER_MONITOR = 2;
	public static final int AWARE_PER_MONITOR_V2 = 3;
	private static final String[] AWARENESS = {"no window", "DPI unaware", "system DPI aware", "per-monitor DPI aware", "per-monitor DPI aware, version 2"};

	private final long window;
	private final long owner;
	private final boolean shown;
	private final boolean front;
	private final String frame;
	private final String inside;
	private final String title;
	private final int dpi;
	private final boolean resizable;
	private final long parent;
	private final boolean embedded;
	private final int awareness;
	private final int ownerAwareness;

	// the fields in the order of the reply
	private TGVst3EditorWindow(DataInputStream in) throws IOException {
		this.window = in.readLong();
		this.owner = in.readLong();
		this.shown = (in.readInt() == 1);
		this.front = (in.readInt() == 1);
		this.frame = in.readInt() + "," + in.readInt() + " " + in.readInt() + "," + in.readInt();
		this.inside = in.readInt() + "x" + in.readInt();
		this.title = TGVst3Request.readString(in);
		this.dpi = in.readInt();
		this.resizable = (in.readInt() == 1);
		this.parent = in.readLong();
		this.embedded = (in.readInt() == 1);
		this.awareness = in.readInt();
		this.ownerAwareness = in.readInt();
	}

	// whether it has a border the user can drag (its view can be resized)
	public boolean isResizable() {
		return this.resizable;
	}

	// the reply to an editor window request (see TGVst3Protocol)
	static TGVst3EditorWindow read(DataInputStream in) throws IOException {
		return new TGVst3EditorWindow(in);
	}

	// of the screen it is on (96: 100 %)
	public int getDpi() {
		return this.dpi;
	}

	// whether its process tells it each screen's own DPI (per monitor, version 2), rather than scaling its pixels
	public boolean isPerMonitorDpiAware() {
		return (this.awareness == AWARE_PER_MONITOR_V2);
	}

	// its DPI awareness: UNAWARE, AWARE_SYSTEM, AWARE_PER_MONITOR or AWARE_PER_MONITOR_V2; NO_WINDOW without one
	public int getAwareness() {
		return this.awareness;
	}

	// the DPI awareness of the window owning it, or that it is in (NO_WINDOW: none)
	public int getOwnerAwareness() {
		return this.ownerAwareness;
	}

	// the window it is in, inside a window of TuxGuitar (0: none, it floats)
	public long getParent() {
		return this.parent;
	}

	// whether it is inside a window of TuxGuitar
	public boolean isEmbedded() {
		return this.embedded;
	}

	// a DPI awareness as the log says it
	static String awareness(int awareness) {
		return AWARENESS[awareness + 1];
	}

	// its caption
	public String getTitle() {
		return this.title;
	}

	// 0 when no editor is open
	public long getWindow() {
		return this.window;
	}

	// 0 when it has none
	public long getOwner() {
		return this.owner;
	}

	public boolean isShown() {
		return this.shown;
	}

	// in front of the other shown windows of its host process
	public boolean isFront() {
		return this.front;
	}

	// "left,top right,bottom" on the screen
	public String getFrame() {
		return this.frame;
	}

	// "widthxheight" inside the frame
	public String getInside() {
		return this.inside;
	}
}
