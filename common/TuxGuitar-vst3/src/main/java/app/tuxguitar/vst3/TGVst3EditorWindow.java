package app.tuxguitar.vst3;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The window of a plugin's own editor as the system shows it, read from the host process (for checks): its native
 * handle and its owner's (0: none), whether it is shown and in front of the host's other windows, its frame on the
 * screen, the size of the area inside the frame, where the plugin's view is, its caption, and the DPI it is drawn at.
 */
public final class TGVst3EditorWindow {

	private final long window;
	private final long owner;
	private final boolean shown;
	private final boolean front;
	private final String frame;
	private final String inside;
	private final String title;
	private final int dpi;
	private final boolean perMonitorDpiAware;

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
		this.perMonitorDpiAware = (in.readInt() == 1);
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
		return this.perMonitorDpiAware;
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
