package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Clicks of the General MIDI drums (the Count In's, the Metronome's, see TGDrumTap): the sample of a timeline on which
 * each click's note reaches the drums, in order, and how many samples it is held. Immutable.
 */
public final class TGClicks {

	public static final TGClicks NONE = new TGClicks(new ArrayList<Long>(), new ArrayList<Long>());

	private final long[] positions;
	private final long[] holds;

	// as many holds as positions
	TGClicks(List<Long> positions, List<Long> holds) {
		this.positions = array(positions);
		this.holds = array(holds);
	}

	private static long[] array(List<Long> values) {
		return values.stream().mapToLong(Long::longValue).toArray();
	}

	// where each click's note reaches the drums
	public long[] positions() {
		return this.positions.clone();
	}

	// how long the click is held
	public long hold(int click) {
		return this.holds[click];
	}
}
