package app.tuxguitar.player.base;

import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * A pass of a turn of interleaved practice in the sequence the player plays (without the interval clock a turn is one
 * pass): from its tick, the break before it (silence; only before a turn's first pass, and none before the session's
 * first turn), its Count In (so many clicks, a beat of its first measure's time signature apart; none with Count In off
 * or, but before every pass, in the turn's passes after the first), then its unit's measures, first to last, each once
 * in score order, one after the other. Its break and Count In are at its first measure's tempo. It knows the turn of the
 * session it plays (from 1). Immutable.
 */
public final class MidiPracticeTurn {

	private static final long SECOND = 1000000L;

	private final TGMeasureHeader first;
	private final TGMeasureHeader last;
	private final long start;
	private final long rest;
	private final int clicks;
	private final int turn;

	// a turn of its own: the session's first, with no break
	public MidiPracticeTurn(TGMeasureHeader first, TGMeasureHeader last, long start, int clicks) {
		this(first, last, start, 0, clicks, 1);
	}

	MidiPracticeTurn(TGMeasureHeader first, TGMeasureHeader last, long start, long rest, int clicks, int turn) {
		this.first = first;
		this.last = last;
		this.start = start;
		this.rest = rest;
		this.clicks = clicks;
		this.turn = turn;
	}

	// the first pass of the session's turn, after a break of so many seconds at the Percentage of Tempo (in ticks at its
	// first measure's tempo, to the nearest)
	static MidiPracticeTurn afterBreak(TGMeasureHeader first, TGMeasureHeader last, long start, int seconds, int percent, int clicks, int turn) {
		long rest = Math.round(seconds * (double) SECOND * TGDuration.QUARTER_TIME / MidiSequenceParser.usq(first.getTempo(), percent));
		return new MidiPracticeTurn(first, last, start, rest, clicks, turn);
	}

	public TGMeasureHeader getFirst() {
		return this.first;
	}

	public TGMeasureHeader getLast() {
		return this.last;
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

	// ticks between two clicks: the unit of its first measure's time signature
	public long getBeat() {
		return this.first.getTimeSignature().getDenominator().getTime();
	}

	// how far its measures are moved from where the song starts them
	public long getMove() {
		return this.getCountIn() + this.clicks * this.getBeat() - this.first.getStart();
	}

	// where the pass after it starts
	public long getEnd() {
		return this.last.getStart() + this.last.getLength() + this.getMove();
	}

	// the same pass from another tick
	public MidiPracticeTurn from(long tick) {
		return new MidiPracticeTurn(this.first, this.last, tick, this.rest, this.clicks, this.turn);
	}

	// the same pass played again from the tick after a pause: no break, a Count In of so many clicks
	MidiPracticeTurn again(long tick, int clicks) {
		return new MidiPracticeTurn(this.first, this.last, tick, 0, clicks, this.turn);
	}

	// whether the tick is in its break
	boolean rests(long tick) {
		return (tick >= this.start && tick < this.getCountIn());
	}

	// the song's tick its tick plays: its first measure's start all through its break and Count In
	public long songTick(long tick) {
		return Math.max(this.first.getStart(), tick - this.getMove());
	}

	// the microseconds from the tick to the one given, at the Percentage of Tempo, of what it plays between them: its break
	// and Count In at its first measure's tempo, each measure at its own
	long micros(long from, long to, int percent) {
		long measures = this.first.getStart() + this.getMove();
		long micros = overlap(this.start, measures, from, to) * MidiSequenceParser.usq(this.first.getTempo(), percent);
		for (int number = this.first.getNumber(); number <= this.last.getNumber(); number++) {
			TGMeasureHeader header = this.first.getSong().getMeasureHeader(number - 1);
			long at = header.getStart() + this.getMove();
			micros += overlap(at, at + header.getLength(), from, to) * MidiSequenceParser.usq(header.getTempo(), percent);
		}
		return micros / TGDuration.QUARTER_TIME;
	}

	// the ticks two ranges share (from their first tick to before their second)
	private static long overlap(long start, long end, long from, long to) {
		return Math.max(0, Math.min(end, to) - Math.max(start, from));
	}
}
