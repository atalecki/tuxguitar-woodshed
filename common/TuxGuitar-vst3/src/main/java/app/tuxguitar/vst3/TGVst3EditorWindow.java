package app.tuxguitar.vst3;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The window of a plugin's own editor as the system shows it, read from the host process (for checks): its native
 * handle and its owner's (0: none), whether it is shown and in front of the host's other windows, its frame on the
 * screen, the size of the area inside the frame, where the plugin's view is, and its caption.
 */
public final class TGVst3EditorWindow {

	private final long window;
	private final long owner;
	private final boolean shown;
	private final boolean front;
	private final String frame;
	private final String inside;
	private final String title;

	private TGVst3EditorWindow(long window, long owner, boolean shown, boolean front, String frame, String inside, String title) {
		this.window = window;
		this.owner = owner;
		this.shown = shown;
		this.front = front;
		this.frame = frame;
		this.inside = inside;
		this.title = title;
	}

	// the reply to an editor window request (see TGVst3Protocol)
	static TGVst3EditorWindow read(DataInputStream in) throws IOException {
		return new TGVst3EditorWindow(in.readLong(), in.readLong(), in.readInt() == 1, in.readInt() == 1,
			in.readInt() + "," + in.readInt() + " " + in.readInt() + "," + in.readInt(), in.readInt() + "x" + in.readInt(), TGVst3Request.readString(in));
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
