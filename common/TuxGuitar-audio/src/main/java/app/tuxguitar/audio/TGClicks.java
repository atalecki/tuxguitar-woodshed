package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Clicks of the drums (the Count In's, the Metronome's, see TGDrumTap): the sample of a timeline on which each click's
 * note reaches the drums, in order, and whether it is accented (beat 1, see MidiClick.isAccented). A click is the
 * built-in click, which sounds out whatever follows its note: it is not held. Immutable.
 */
public final class TGClicks {

	public static final TGClicks NONE = new TGClicks(new ArrayList<Long>(), new ArrayList<Boolean>());

	private final long[] positions;
	private final boolean[] accents;

	// as many accents as positions
	TGClicks(List<Long> positions, List<Boolean> accents) {
		this.positions = positions.stream().mapToLong(Long::longValue).toArray();
		this.accents = new boolean[accents.size()];
		for (int click = 0; click < this.accents.length; click++) {
			this.accents[click] = accents.get(click);
		}
	}

	// the clicks of each, one list after the other
	static TGClicks join(List<TGClicks> parts) {
		List<Long> positions = new ArrayList<Long>();
		List<Boolean> accents = new ArrayList<Boolean>();
		for (TGClicks part : parts) {
			for (int click = 0; click < part.positions.length; click++) {
				positions.add(part.positions[click]);
				accents.add(part.accents[click]);
			}
		}
		return new TGClicks(positions, accents);
	}

	// where each click's note reaches the drums
	public long[] positions() {
		return this.positions.clone();
	}

	// whether each is accented
	public boolean[] accents() {
		return this.accents.clone();
	}
}
