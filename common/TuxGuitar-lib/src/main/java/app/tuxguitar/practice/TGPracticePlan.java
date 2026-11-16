package app.tuxguitar.practice;

import java.util.Optional;

import app.tuxguitar.song.models.TGSong;

/**
 * A turn as its session's schedule plans it: what it plays (its units, from one to another: the measures from the
 * first's first to the last's last), the tempo scale it plays at, how many passes of them it plays, whether a Count In comes before
 * every pass or only the first (when Count In is on), and whether it is the session's last turn or, if not, how many
 * seconds of break come before the next; and the interleaved click-up step it plays, if it plays one. Immutable.
 */
public final class TGPracticePlan {

	private final TGPracticeUnit from;
	private final TGPracticeUnit to;
	private final TGTempoScale scale;
	private final int passes;
	private final boolean countInEveryPass;
	private final int breakSeconds;
	private final boolean last;
	// null: no click-up step (a turn of interleaved practice)
	private final TGClickUpStep step;

	TGPracticePlan(TGPracticeUnit from, TGPracticeUnit to, TGTempoScale scale, int passes, boolean countInEveryPass, int breakSeconds, boolean last) {
		this(from, to, scale, passes, countInEveryPass, breakSeconds, last, null);
	}

	// the click-up step's turn: its sections at its tempo scale, so many passes, no break
	TGPracticePlan(TGClickUpStep step, int passes, boolean countInEveryPass, boolean last) {
		this(step.from, step.to, step.getScale(), passes, countInEveryPass, 0, last, step);
	}

	private TGPracticePlan(TGPracticeUnit from, TGPracticeUnit to, TGTempoScale scale, int passes, boolean countInEveryPass, int breakSeconds, boolean last,
		TGClickUpStep step) {
		this.step = step;
		this.from = from;
		this.to = to;
		this.scale = scale;
		this.passes = passes;
		this.countInEveryPass = countInEveryPass;
		this.breakSeconds = breakSeconds;
		this.last = last;
	}

	// the first unit it plays
	public TGPracticeUnit getFrom() {
		return this.from;
	}

	// the last unit it plays (the first, when it plays one)
	public TGPracticeUnit getTo() {
		return this.to;
	}

	// the number of the first measure it plays (from 1)
	public int getFirstMeasure() {
		return this.from.getFirst();
	}

	// the number of the last measure it plays (from 1)
	public int getLastMeasure() {
		return this.to.getLast();
	}

	// what it plays of the song, in its ticks (see TGPracticeSpan)
	public TGPracticeSpan span(TGSong song) {
		return TGPracticeSpan.of(song, this.from, this.to);
	}

	public TGTempoScale getScale() {
		return this.scale;
	}

	public int getPasses() {
		return this.passes;
	}

	public boolean isCountInEveryPass() {
		return this.countInEveryPass;
	}

	// the break before the next turn (0: none, also after the last turn)
	public int getBreakSeconds() {
		return this.breakSeconds;
	}

	public boolean isLast() {
		return this.last;
	}

	// the click-up step it plays; none for a turn of interleaved practice
	public Optional<TGClickUpStep> getStep() {
		return Optional.ofNullable(this.step);
	}
}
