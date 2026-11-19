package app.tuxguitar.audio;

/**
 * How wide the Track panel docked at the right of the main window is, in the toolkit's points (which follow the
 * screen's scale): 300 the first time, then as last left, kept within 220 to 600; a drag of the divider at its left edge
 * to the right narrows it by as much, to the left widens it, within the same range.
 */
public final class TGTrackPanelWidth {

	static final float DEFAULT = 300f;
	static final float MIN = 220f;
	static final float MAX = 600f;

	private TGTrackPanelWidth() {
	}

	// the width shown: the one remembered (0 or less: none yet), else the default
	public static float shown(float remembered) {
		return (remembered > 0f ? within(remembered) : DEFAULT);
	}

	// the width a drag of the divider by (right: positive) leaves the width shown
	public static float dragged(float shown, float by) {
		return within(shown - by);
	}

	private static float within(float width) {
		return Math.max(MIN, Math.min(MAX, width));
	}
}
