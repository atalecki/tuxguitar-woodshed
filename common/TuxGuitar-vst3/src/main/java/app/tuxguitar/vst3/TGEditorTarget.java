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

	private TGEditorTarget(long parent, long owner, double maxShare) {
		this.parent = parent;
		this.owner = owner;
		this.maxShare = maxShare;
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
