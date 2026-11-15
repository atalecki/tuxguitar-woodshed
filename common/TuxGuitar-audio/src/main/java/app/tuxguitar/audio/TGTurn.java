package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.tuxguitar.song.models.TGSong;

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

	// a unit of interleaved practice, measures numbered first to last, in its turn (from 0): its Count In of so many clicks
	// (0: none) at the Percentage of Tempo, then its measures (see TGSongTime.unit)
	public static TGTurn unit(TGSong song, int first, int last, int turn, int percent, int clicks, int sampleRate) {
		List<TGPlayedMeasure> measures = TGSongTime.unit(song, first, last, turn, percent);
		return new TGTurn(TGCountIn.of(clicks, measures, sampleRate), measures);
	}

	public TGCountIn getCountIn() {
		return this.countIn;
	}

	public List<TGPlayedMeasure> getMeasures() {
		return this.measures;
	}
}
