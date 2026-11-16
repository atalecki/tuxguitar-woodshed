package app.tuxguitar.practice;

import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * Plans a session's turns, one by one as they are chosen: the one place that decides how many passes a turn plays and
 * which turn is the last. Without the interval clock a turn plays its unit once, for the number of turns set. With it
 * (TGPracticeClock), a turn's Count In and pass last as long as their measures take at their own tempos, at the tempo
 * scale the session plays at, and the session's time counts every turn and break planned so far. One per play of a
 * session, planned on the one thread that lays the turns.
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

	// the next turn, which plays the unit at the session's tempo scale
	public TGPracticePlan plan(TGPracticeUnit unit) {
		this.planned++;
		return this.session.getClock().map(clock -> this.timed(clock, unit)).orElseGet(() -> this.counted(unit));
	}

	private TGPracticePlan counted(TGPracticeUnit unit) {
		return new TGPracticePlan(unit, unit, this.scale, 1, false, 0, this.planned >= this.session.getTurns());
	}

	private TGPracticePlan timed(TGPracticeClock clock, TGPracticeUnit unit) {
		TGMeasureHeader first = this.header(unit.getFirst());
		long countIn = this.clicks(first) * this.micros(first, first.getTimeSignature().getDenominator().getTime());
		long pass = IntStream.rangeClosed(unit.getFirst(), unit.getLast()).mapToLong(number -> this.micros(this.header(number), this.header(number).getLength())).sum();
		long end = this.next + clock.length(countIn, pass);
		boolean last = !clock.follows(end);
		this.next = clock.after(end);
		return new TGPracticePlan(unit, unit, this.scale, clock.passes(countIn, pass), clock.isCountInEveryPass(), (last ? 0 : clock.getBreakSeconds()), last);
	}

	// the Count In clicks before a turn whose first measure is the header, as the function given says now
	int clicks(TGMeasureHeader header) {
		return this.clicks.applyAsInt(header);
	}

	private TGMeasureHeader header(int number) {
		return this.song.getMeasureHeader(number - 1);
	}

	// how long so many ticks of the measure take at its tempo and the session's tempo scale (microseconds)
	private long micros(TGMeasureHeader header, long ticks) {
		return this.scale.micros(ticks, header.getTempo());
	}
}
