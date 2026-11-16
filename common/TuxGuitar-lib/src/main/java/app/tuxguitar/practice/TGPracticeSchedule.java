package app.tuxguitar.practice;

import java.util.function.ToIntFunction;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * Plans a session's turns, one by one as they are chosen: the one place that decides how many passes a turn plays and
 * which turn is the last. Without the interval clock a turn plays its block once, for the number of turns set. With it
 * (TGPracticeClock), a turn's Count In and pass (TGPracticeSpan: a beat's with its joint) last as long as their
 * measures take at their own tempos, at the tempo scale the session plays at, and the session's time counts every turn
 * and break planned so far. One per play of a session, planned on the one thread that lays the turns.
 */
public final class TGPracticeSchedule {

	private final TGPracticeSession session;
	private final TGSong song;
	private final ToIntFunction<TGMeasureHeader> clicks;
	private final TGTempoScale scale;
	private int planned;
	// with the clock, when the next turn starts, from the session's first turn's start (microseconds)
	private long next;

	TGPracticeSchedule(TGPracticeSession session, TGSong song, ToIntFunction<TGMeasureHeader> clicks, TGTempoScale scale) {
		this.session = session;
		this.song = song;
		this.clicks = clicks;
		this.scale = scale;
	}

	// the next turn, which plays the block at the session's tempo scale
	public TGPracticePlan plan(TGPracticeBlock block) {
		this.planned++;
		return this.session.getClock().map(clock -> this.timed(clock, block)).orElseGet(() -> this.counted(block));
	}

	private TGPracticePlan counted(TGPracticeBlock block) {
		return new TGPracticePlan(block.getFrom(), block.getTo(), this.scale, 1, false, 0, this.planned >= this.session.getTurns());
	}

	private TGPracticePlan timed(TGPracticeClock clock, TGPracticeBlock block) {
		TGPracticeSpan span = TGPracticeSpan.of(this.song, block.getFrom(), block.getTo());
		TGMeasureHeader first = span.getFirst();
		long countIn = this.clicks(first) * this.micros(first, first.getTimeSignature().getDenominator().getTime());
		long pass = span.headers().stream().mapToLong(header -> this.micros(header, span.ticksIn(header))).sum();
		long end = this.next + clock.length(countIn, pass);
		boolean last = !clock.follows(end);
		this.next = clock.after(end);
		return new TGPracticePlan(block.getFrom(), block.getTo(), this.scale, clock.passes(countIn, pass), clock.isCountInEveryPass(), (last ? 0 : clock.getBreakSeconds()), last);
	}

	// the Count In clicks before a turn whose first measure is the header: with Count In before each turn on, as the
	// function given says now; none with it off
	int clicks(TGMeasureHeader header) {
		return (this.session.isCountInEachTurn() ? this.clicks.applyAsInt(header) : 0);
	}

	// how long so many ticks of the measure take at its tempo and the session's tempo scale (microseconds)
	private long micros(TGMeasureHeader header, long ticks) {
		return this.scale.micros(ticks, header.getTempo());
	}
}
