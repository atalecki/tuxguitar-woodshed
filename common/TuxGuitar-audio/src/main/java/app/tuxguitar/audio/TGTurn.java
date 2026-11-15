package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A turn of a timeline: a Count In (TGCountIn.NONE with Count In off), then measures as they play, one after the other.
 * Playing the song is one turn; interleaved practice plays one turn per unit. Immutable.
 */
public final class TGTurn {

	private final TGCountIn countIn;
	private final List<TGPlayedMeasure> measures;

	public TGTurn(TGCountIn countIn, List<TGPlayedMeasure> measures) {
		this.countIn = countIn;
		this.measures = Collections.unmodifiableList(new ArrayList<TGPlayedMeasure>(measures));
	}

	public TGCountIn getCountIn() {
		return this.countIn;
	}

	public List<TGPlayedMeasure> getMeasures() {
		return this.measures;
	}
}
