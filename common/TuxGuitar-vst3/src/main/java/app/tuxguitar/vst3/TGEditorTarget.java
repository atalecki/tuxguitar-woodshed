package app.tuxguitar.vst3;

/**
 * Where a plugin's own editor goes: floating, in a window of its own that the owner window owns (a native handle,
 * TuxGuitar's main window; 0: none), or embedded, inside the parent window (a native handle: the Sound Chain panel's
 * editor area), at most a share of the height of the inside of its top-level window (TuxGuitar's) high, the owner kept
 * for a window of its own later (a pop-out). A parent of 0 (a toolkit whose windows have no native handle) has nothing
 * to put an editor in: the editor floats.
 */
public final class TGEditorTarget {

	private final long parent;
	private final long owner;
	private final double maxShare;
	// floating: the frame its window opens in (left, top, right, bottom on the screen), or none
	private final int[] frame;

	private TGEditorTarget(long parent, long owner, double maxShare, int[] frame) {
		this.parent = parent;
		this.owner = owner;
		this.maxShare = maxShare;
		this.frame = frame;
	}

	private TGEditorTarget(long parent, long owner, double maxShare) {
		this(parent, owner, maxShare, new int[4]);
	}

	// the same, its window (when it floats and is made now) opening in that frame (left, top, right, bottom; null: where
	// it opens by itself, centred on its owner)
	public TGEditorTarget at(int[] frame) {
		return new TGEditorTarget(this.parent, this.owner, this.maxShare, (frame != null ? frame.clone() : new int[4]));
	}

	public int[] getFrame() {
		return this.frame.clone();
	}

	public static TGEditorTarget floating(long owner) {
		return new TGEditorTarget(0, owner, 0);
	}

	public static TGEditorTarget embedded(long parent, long owner, double maxShare) {
		return (parent != 0 ? new TGEditorTarget(parent, owner, maxShare) : floating(owner));
	}

	public boolean isEmbedded() {
		return (this.parent != 0);
	}

	// the window the editor is in (0: none, it floats)
	public long getParent() {
		return this.parent;
	}

	public long getOwner() {
		return this.owner;
	}

	// of the height of the inside of the top-level window it is in (0 when it floats)
	public double getMaxShare() {
		return this.maxShare;
	}

	public String toString() {
		return (this.isEmbedded() ? "inside " + this.parent + ", at most " + this.maxShare + " of its window high, " : "floating, ") + "owned by " + this.owner;
	}
}
