package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Clicks of the drums (the Count In's, the Metronome's, see TGDrumTap): the sample of a timeline on which each click's
 * note reaches the drums, in order. A click is the built-in click, which sounds out whatever follows its note: it is
 * not held. Immutable.
 */
public final class TGClicks {

	public static final TGClicks NONE = new TGClicks(new ArrayList<Long>());

	private final long[] positions;

	TGClicks(List<Long> positions) {
		this.positions = positions.stream().mapToLong(Long::longValue).toArray();
	}

	// the clicks of each, one list after the other
	static TGClicks join(List<TGClicks> parts) {
		List<Long> positions = new ArrayList<Long>();
		for (TGClicks part : parts) {
			for (long position : part.positions) {
				positions.add(position);
			}
		}
		return new TGClicks(positions);
	}

	// where each click's note reaches the drums
	public long[] positions() {
		return this.positions.clone();
	}
}
