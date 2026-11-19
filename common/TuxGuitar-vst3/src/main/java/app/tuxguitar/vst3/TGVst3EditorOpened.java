package app.tuxguitar.vst3;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * What the host did when asked to open a plugin's own editor: none (the plugin has no editor of its own for the
 * platform), a window of its own, a window TuxGuitar's window owns, inside a window of TuxGuitar (embedded), or nothing
 * because the editor is too large for that window (TOO_LARGE: higher than the limit, or wider than the window and unable
 * to shrink to it). Inside a window, or too large, with the size its view takes, or would take (device pixels), and
 * whether it can be resized, and the width of the inside of that window; a window of its own says no size.
 */
public final class TGVst3EditorOpened {

	// in the order the host numbers them, from 0
	public enum Kind {
		NONE, OWN_WINDOW, OWNED, EMBEDDED, TOO_LARGE
	}

	// no editor of its own is open (as a request to fit one that is not open answers)
	public static final TGVst3EditorOpened NONE = new TGVst3EditorOpened(Kind.NONE, 0, 0, false, 0);

	private final Kind kind;
	private final int width;
	private final int height;
	private final boolean resizable;
	private final int areaWidth;

	TGVst3EditorOpened(Kind kind, int width, int height, boolean resizable, int areaWidth) {
		this.kind = kind;
		this.width = width;
		this.height = height;
		this.resizable = resizable;
		this.areaWidth = areaWidth;
	}

	// what the host did, as it says it
	public static TGVst3EditorOpened of(Kind kind, int width, int height, boolean resizable, int areaWidth) {
		return new TGVst3EditorOpened(kind, width, height, resizable, areaWidth);
	}

	// the reply to an embed or fit request: kind, width, height, resizable (1 or 0), the width of the window's inside
	static TGVst3EditorOpened read(DataInputStream in) throws IOException {
		return new TGVst3EditorOpened(Kind.values()[in.readInt()], in.readInt(), in.readInt(), in.readInt() == 1, in.readInt());
	}

	public Kind getKind() {
		return this.kind;
	}

	// whether an editor of the plugin's own is open now (in a window of its own or inside one of TuxGuitar)
	public boolean isOpen() {
		return (this.kind == Kind.OWN_WINDOW || this.kind == Kind.OWNED || this.kind == Kind.EMBEDDED);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public boolean isResizable() {
		return this.resizable;
	}

	// the width of the inside of the window it is in, or would be in (device pixels; 0 in a window of its own)
	public int getAreaWidth() {
		return this.areaWidth;
	}

	// the same editor, its view now that size (it asked for it)
	public TGVst3EditorOpened resized(int width, int height) {
		return new TGVst3EditorOpened(this.kind, width, height, this.resizable, this.areaWidth);
	}

	public String toString() {
		return this.kind + " " + this.width + "x" + this.height + " " + (this.resizable ? "resizable" : "fixed");
	}
}
