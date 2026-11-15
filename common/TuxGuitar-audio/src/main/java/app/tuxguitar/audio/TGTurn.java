package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * A turn of a timeline: a break (silence, none unless set), a Count In (TGCountIn.NONE with Count In off), then
 * measures as they play, one after the other. Playing the song is one turn; interleaved practice plays one turn per
 * unit, or, with the interval clock, one per pass of each turn's unit: the first opens the practice turn, after its
 * break; the others continue it, without one. Immutable.
 */
public final class TGTurn {

	private final long rest;
	private final boolean opening;
	private final TGCountIn countIn;
	private final List<TGPlayedMeasure> measures;

	public TGTurn(TGCountIn countIn, List<TGPlayedMeasure> measures) {
		this(0, true, countIn, measures);
	}

	private TGTurn(long rest, boolean opening, TGCountIn countIn, List<TGPlayedMeasure> measures) {
		this.rest = rest;
		this.opening = opening;
		this.countIn = countIn;
		this.measures = Collections.unmodifiableList(new ArrayList<TGPlayedMeasure>(measures));
	}

	// a turn after a break of so many samples
	public static TGTurn afterBreak(long rest, TGCountIn countIn, List<TGPlayedMeasure> measures) {
		return new TGTurn(rest, true, countIn, measures);
	}

	// a pass after the first of a practice turn: it continues the turn before it, with no break
	public static TGTurn pass(TGCountIn countIn, List<TGPlayedMeasure> measures) {
		return new TGTurn(0, false, countIn, measures);
	}

	// a unit of interleaved practice, measures numbered first to last, in its turn (from 0): its Count In of as many clicks
	// as the function of its first measure's header says (0: none) at the Percentage of Tempo, then its measures (see
	// TGSongTime.unit)
	public static TGTurn unit(TGSong song, int first, int last, int turn, int percent, ToIntFunction<TGMeasureHeader> clicks, int sampleRate) {
		List<TGPlayedMeasure> measures = TGSongTime.unit(song, first, last, turn, percent);
		return new TGTurn(TGCountIn.of(clicks, measures, sampleRate), measures);
	}

	// the same turn, its Count In and measures replaced: its break, and whether it opens a practice turn, are kept
	public TGTurn with(TGCountIn countIn, List<TGPlayedMeasure> measures) {
		return new TGTurn(this.rest, this.opening, countIn, measures);
	}

	// the samples of the break before it
	public long getRest() {
		return this.rest;
	}

	// whether it opens a practice turn (a pass after the first does not)
	public boolean isOpening() {
		return this.opening;
	}

	public TGCountIn getCountIn() {
		return this.countIn;
	}

	public List<TGPlayedMeasure> getMeasures() {
		return this.measures;
	}
}
