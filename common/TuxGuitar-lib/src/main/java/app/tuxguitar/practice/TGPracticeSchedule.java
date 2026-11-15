package app.tuxguitar.practice;

import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * Plans a session's turns, one by one as they are chosen: the one place that decides how many passes a turn plays and
 * which turn is the last. Without the interval clock a turn plays its unit once, for the number of turns set. With it
 * (TGPracticeClock), a turn's Count In and pass last as long as their measures take at their own tempos, at the
 * Percentage of Tempo, and the session's time counts every turn and break planned so far. One per play of a session,
 * planned on the one thread that lays the turns.
 */
public final class TGPracticeSchedule {

	private static final long MINUTE = 60000000L;
	private static final long PERCENT = 100;

	private final TGPracticeSession session;
	private final TGSong song;
	private final ToIntFunction<TGMeasureHeader> clicks;
	private final int percent;
	private int planned;
	// with the clock, when the next turn starts, from the session's first turn's start (microseconds)
	private long next;

	TGPracticeSchedule(TGPracticeSession session, TGSong song, ToIntFunction<TGMeasureHeader> clicks, int percent) {
		this.session = session;
		this.song = song;
		this.clicks = clicks;
		this.percent = percent;
	}

	// the next turn, which plays the unit
	public TGPracticePlan plan(TGPracticeUnit unit) {
		this.planned++;
		return this.session.getClock().map(clock -> this.timed(clock, unit)).orElseGet(this::counted);
	}

	private TGPracticePlan counted() {
		return new TGPracticePlan(1, false, 0, this.planned >= this.session.getTurns());
	}

	private TGPracticePlan timed(TGPracticeClock clock, TGPracticeUnit unit) {
		TGMeasureHeader first = this.header(unit.getFirst());
		long countIn = this.clicks.applyAsInt(first) * this.micros(first, first.getTimeSignature().getDenominator().getTime());
		long pass = IntStream.rangeClosed(unit.getFirst(), unit.getLast()).mapToLong(number -> this.micros(this.header(number), this.header(number).getLength())).sum();
		long end = this.next + clock.length(countIn, pass);
		boolean last = !clock.follows(end);
		this.next = clock.after(end);
		return new TGPracticePlan(clock.passes(countIn, pass), clock.isCountInEveryPass(), (last ? 0 : clock.getBreakSeconds()), last);
	}

	private TGMeasureHeader header(int number) {
		return this.song.getMeasureHeader(number - 1);
	}

	// how long so many ticks of the measure take at its tempo and the Percentage of Tempo (microseconds)
	private long micros(TGMeasureHeader header, long ticks) {
		return ticks * MINUTE * PERCENT / (TGDuration.QUARTER_TIME * header.getTempo().getQuarterValue() * this.percent);
	}
}
