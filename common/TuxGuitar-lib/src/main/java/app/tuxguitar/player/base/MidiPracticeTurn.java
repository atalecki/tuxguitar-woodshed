package app.tuxguitar.player.base;

import app.tuxguitar.practice.TGPracticeSpan;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * A pass of a turn of interleaved practice in the sequence the player plays (without the interval clock a turn is one
 * pass): from its tick, the break before it (silence; only before a turn's first pass, and none before the session's
 * first turn), its Count In (so many clicks, a beat of its first measure's time signature apart; none with Count In off
 * or, but before every pass, in the turn's passes after the first), then its span (TGPracticeSpan: its unit's measures,
 * first to last, each once in score order, one after the other, or its beats and their joint), all at its tempo scale.
 * Its break and Count In are at its first measure's tempo. It knows the turn of the session it plays (from 1).
 * Immutable.
 */
public final class MidiPracticeTurn {

	private static final long SECOND = 1000000L;

	private final TGPracticeSpan span;
	private final long start;
	private final long rest;
	private final int clicks;
	private final int turn;
	private final TGTempoScale scale;

	// a turn of its own of whole measures: the session's first, with no break, at the written tempo
	public MidiPracticeTurn(TGMeasureHeader first, TGMeasureHeader last, long start, int clicks) {
		this(TGPracticeSpan.measures(first, last), start, clicks);
	}

	// a turn of its own: the session's first, with no break, at the written tempo
	public MidiPracticeTurn(TGPracticeSpan span, long start, int clicks) {
		this(span, start, 0, clicks, 1, TGTempoScale.percent(100));
	}

	// a pass of whole measures of the session's turn, after a break of so many ticks, at the tempo scale
	public MidiPracticeTurn(TGMeasureHeader first, TGMeasureHeader last, long start, long rest, int clicks, int turn, TGTempoScale scale) {
		this(TGPracticeSpan.measures(first, last), start, rest, clicks, turn, scale);
	}

	// a pass of the session's turn, after a break of so many ticks, at the tempo scale
	public MidiPracticeTurn(TGPracticeSpan span, long start, long rest, int clicks, int turn, TGTempoScale scale) {
		this.span = span;
		this.start = start;
		this.rest = rest;
		this.clicks = clicks;
		this.turn = turn;
		this.scale = scale;
	}

	// the first pass of the session's turn, at the tempo scale, after a break of so many seconds at it (in ticks at its
	// first measure's tempo, to the nearest)
	static MidiPracticeTurn afterBreak(TGPracticeSpan span, long start, int seconds, TGTempoScale scale, int clicks, int turn) {
		long rest = Math.round(seconds * (double) SECOND * TGDuration.QUARTER_TIME / scale.usq(span.getFirst().getTempo()));
		return new MidiPracticeTurn(span, start, rest, clicks, turn, scale);
	}

	// what it plays of the song
	public TGPracticeSpan getSpan() {
		return this.span;
	}

	public TGMeasureHeader getFirst() {
		return this.span.getFirst();
	}

	public TGMeasureHeader getLast() {
		return this.span.getLast();
	}

	// where its break starts (its Count In, without one; its first measure, without either)
	public long getStart() {
		return this.start;
	}

	// the ticks of its break
	public long getRest() {
		return this.rest;
	}

	// where its Count In starts (its first measure, without one)
	public long getCountIn() {
		return this.start + this.rest;
	}

	public int getClicks() {
		return this.clicks;
	}

	// the session's turn it is a pass of, from 1
	public int getTurn() {
		return this.turn;
	}

	// how fast it plays the song
	public TGTempoScale getScale() {
		return this.scale;
	}

	// ticks between two clicks: the unit of its first measure's time signature
	public long getBeat() {
		return this.getFirst().getTimeSignature().getDenominator().getTime();
	}

	// how far its span is moved from where the song has it
	public long getMove() {
		return this.getCountIn() + this.clicks * this.getBeat() - this.span.getStart();
	}

	// where the pass after it starts
	public long getEnd() {
		return this.span.getEnd() + this.getMove();
	}

	// the same pass from another tick
	public MidiPracticeTurn from(long tick) {
		return new MidiPracticeTurn(this.span, tick, this.rest, this.clicks, this.turn, this.scale);
	}

	// the same pass played again from the tick after a pause: no break, a Count In of so many clicks
	MidiPracticeTurn again(long tick, int clicks) {
		return new MidiPracticeTurn(this.span, tick, 0, clicks, this.turn, this.scale);
	}

	// whether the tick is in its break
	boolean rests(long tick) {
		return (tick >= this.start && tick < this.getCountIn());
	}

	// the song's tick its tick plays: its span's start all through its break and Count In
	public long songTick(long tick) {
		return Math.max(this.span.getStart(), tick - this.getMove());
	}

	// the microseconds from the tick to the one given, at its tempo scale, of what it plays between them: its break and
	// Count In at its first measure's tempo, what it plays of each measure at the measure's own
	long micros(long from, long to) {
		long micros = overlap(this.start, this.span.getStart() + this.getMove(), from, to) * this.scale.usq(this.getFirst().getTempo());
		for (TGMeasureHeader header : this.span.headers()) {
			long at = this.span.startIn(header) + this.getMove();
			micros += overlap(at, at + this.span.ticksIn(header), from, to) * this.scale.usq(header.getTempo());
		}
		return micros / TGDuration.QUARTER_TIME;
	}

	// the ticks two ranges share (from their first tick to before their second)
	private static long overlap(long start, long end, long from, long to) {
		return Math.max(0, Math.min(end, to) - Math.max(start, from));
	}
}
