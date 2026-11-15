package app.tuxguitar.player.base;

import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * A turn of interleaved practice in the sequence the player plays: from its tick, its Count In (so many clicks, a beat
 * of its first measure's time signature apart; none with Count In off), then its unit's measures, first to last, each
 * once in score order, one after the other. Immutable.
 */
public final class MidiPracticeTurn {

	private final TGMeasureHeader first;
	private final TGMeasureHeader last;
	private final long start;
	private final int clicks;

	public MidiPracticeTurn(TGMeasureHeader first, TGMeasureHeader last, long start, int clicks) {
		this.first = first;
		this.last = last;
		this.start = start;
		this.clicks = clicks;
	}

	public TGMeasureHeader getFirst() {
		return this.first;
	}

	public TGMeasureHeader getLast() {
		return this.last;
	}

	// where its Count In starts (its first measure, without one)
	public long getStart() {
		return this.start;
	}

	public int getClicks() {
		return this.clicks;
	}

	// ticks between two clicks: the unit of its first measure's time signature
	public long getBeat() {
		return this.first.getTimeSignature().getDenominator().getTime();
	}

	// how far its measures are moved from where the song starts them
	public long getMove() {
		return this.start + this.clicks * this.getBeat() - this.first.getStart();
	}

	// where the turn after it starts
	public long getEnd() {
		return this.last.getStart() + this.last.getLength() + this.getMove();
	}

	// the song's tick its tick plays: its first measure's start all through its Count In
	public long songTick(long tick) {
		return Math.max(this.first.getStart(), tick - this.getMove());
	}
}
