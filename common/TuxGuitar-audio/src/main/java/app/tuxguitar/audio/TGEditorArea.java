package app.tuxguitar.audio;

/**
 * The Sound Chain panel's editor area as numbers: how high an editor inside it may be (a share of TuxGuitar's window,
 * the rest is the score's), and how high the area is for an editor the host reports in device pixels. The toolkit lays
 * the area out in units of its own (scaled pixels), so the height is the view's pixels times the area's units over its
 * pixels across: no toolkit's zoom is asked, whatever its scaling does.
 */
public final class TGEditorArea {

	// an editor higher than this share of the inside of TuxGuitar's window is too large for the panel: it opens in a
	// window of its own (the score is never squeezed)
	public static final double MAX_SHARE = 0.5;

	private TGEditorArea() {
	}

	// in units, for a view pixelHeight pixels high, the area being pixelWidth pixels and unitWidth units wide (a part of
	// a unit counts whole); 0 while the area has no width in pixels
	public static int height(int pixelHeight, int pixelWidth, int unitWidth) {
		return (pixelWidth > 0 ? (int) Math.ceil((double) pixelHeight * unitWidth / pixelWidth) : 0);
	}
}
