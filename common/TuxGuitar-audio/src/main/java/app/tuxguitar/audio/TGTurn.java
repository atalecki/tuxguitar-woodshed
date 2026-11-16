package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

import app.tuxguitar.practice.TGPracticePlan;
import app.tuxguitar.practice.TGPracticeSpan;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * A turn of a timeline: a break (silence, none unless set), a Count In (TGCountIn.NONE with Count In off), then
 * measures as they play, one after the other. Playing the song is one turn; interleaved practice plays one turn per
 * unit, or, with the interval clock, one per pass of each turn's unit: the first opens the practice turn, after its
 * break; the others continue it, without one. A unit's turn knows the span it plays (TGPracticeSpan), the song's none.
 * Immutable.
 */
public final class TGTurn {

	private final long rest;
	private final boolean opening;
	private final TGCountIn countIn;
	private final List<TGPlayedMeasure> measures;
	private final Optional<TGPracticeSpan> span;

	public TGTurn(TGCountIn countIn, List<TGPlayedMeasure> measures) {
		this(0, true, countIn, measures, Optional.empty());
	}

	private TGTurn(long rest, boolean opening, TGCountIn countIn, List<TGPlayedMeasure> measures, Optional<TGPracticeSpan> span) {
		this.rest = rest;
		this.opening = opening;
		this.countIn = countIn;
		this.measures = Collections.unmodifiableList(new ArrayList<TGPlayedMeasure>(measures));
		this.span = span;
	}

	// the same turn after a break of so many samples: it opens a practice turn
	public TGTurn afterBreak(long rest) {
		return new TGTurn(rest, true, this.countIn, this.measures, this.span);
	}

	// the same turn as a pass after the first of a practice turn, with the Count In given: it continues the turn before
	// it, with no break
	public TGTurn continuing(TGCountIn countIn) {
		return new TGTurn(0, false, countIn, this.measures, this.span);
	}

	// a unit of interleaved practice, the span it plays, in its turn (from 0), at the tempo scale: its Count In of as many
	// clicks as the function of its first measure's header says (0: none), then what the span plays of its measures (see
	// TGSongTime.unit)
	public static TGTurn unit(TGSong song, TGPracticeSpan span, int turn, TGTempoScale scale, ToIntFunction<TGMeasureHeader> clicks, int sampleRate) {
		List<TGPlayedMeasure> measures = TGSongTime.unit(song, span, turn, scale);
		return new TGTurn(0, true, TGCountIn.of(clicks, measures, sampleRate), measures, Optional.of(span));
	}

	// a practice turn as its session's schedule plans it, its first pass the timeline's index-th turn, as unit plays the
	// plan's span at its tempo scale: a turn per pass, each that span; the first opens it after the break before it
	// (samples) with its Count In; the others continue it, with theirs when a Count In comes before every pass
	public static List<TGTurn> planned(TGSong song, TGPracticePlan plan, int index, long rest, ToIntFunction<TGMeasureHeader> clicks, int sampleRate) {
		TGPracticeSpan span = plan.span(song);
		List<TGTurn> passes = new ArrayList<TGTurn>();
		for (int pass = 0; pass < plan.getPasses(); pass++) {
			TGTurn played = unit(song, span, index + pass, plan.getScale(), clicks, sampleRate);
			passes.add(pass == 0 ? played.afterBreak(rest) : played.continuing(plan.isCountInEveryPass() ? played.countIn : TGCountIn.NONE));
		}
		return passes;
	}

	// the same turn, its Count In and measures replaced: its break, whether it opens a practice turn, and its span are kept
	public TGTurn with(TGCountIn countIn, List<TGPlayedMeasure> measures) {
		return new TGTurn(this.rest, this.opening, countIn, measures, this.span);
	}

	// the span it plays, a unit's (none: it plays the song)
	public Optional<TGPracticeSpan> getSpan() {
		return this.span;
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
