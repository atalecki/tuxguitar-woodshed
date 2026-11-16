package app.tuxguitar.practice;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * Interleaved click-up's course (TGClickUpLadder.course): a turn per step of the ladder, in its order, walked with a
 * cursor. Each plays the measures of the step's sections at its tempo scale, the ladder's passes, with no break; the
 * last step is the last turn. A step counts in with the click-up's Count In before each step on, whether or not the
 * player's Count In is. It counts its steps and is not timed. A flag changes none of them (it chooses nothing a
 * flag could weigh), so it takes any. Its turns are its steps (getSteps): Repeat step and Previous step move the cursor
 * back. One per play, planned on the one thread that lays the turns, ended on any.
 */
final class TGClickUpCourse implements TGPracticeCourse, TGPracticeSteps {

	private final TGClickUpLadder ladder;
	// the clicks of a Count In before the header's measure, the player's Count In on or off
	private final ToIntFunction<TGMeasureHeader> clicks;
	// the step the next turn plays (from 0)
	private int cursor;
	// whether it has ended: set once, on the thread that ends it, read on any
	private volatile boolean ended;

	TGClickUpCourse(TGClickUpLadder ladder, ToIntFunction<TGMeasureHeader> clicks) {
		this.ladder = ladder;
		this.clicks = clicks;
	}

	public TGPracticePlan ahead() {
		List<TGClickUpStep> steps = this.ladder.getSteps();
		TGClickUpStep step = steps.get(this.cursor++);
		return new TGPracticePlan(step, this.ladder.getPasses(), this.ladder.isCountInEveryPass(), this.cursor == steps.size());
	}

	// the order of the steps does not hang on when one starts
	public void started() {
	}

	public TGPracticePlan next() {
		return this.ahead();
	}

	public int clicks(TGMeasureHeader first) {
		return (this.ladder.isCountInEachStep() ? this.clicks.applyAsInt(first) : 0);
	}

	public void checkFlag(int measure, int difficulty) {
	}

	public String getModeName() {
		return "Interleaved click-up";
	}

	public boolean isTimed() {
		return false;
	}

	public int getTurns() {
		return this.ladder.getSteps().size();
	}

	public boolean isEnded() {
		return this.ended;
	}

	public void end() {
		this.ended = true;
	}

	public Optional<TGPracticeSteps> getSteps() {
		return Optional.of(this);
	}

	public int repeat(int turn) {
		return this.planNext(turn);
	}

	public int previous(int turn) {
		return this.planNext(Math.max(1, turn - 1));
	}

	// the step (from 1) is planned next; its turn is its number
	private int planNext(int step) {
		this.cursor = step - 1;
		return step;
	}
}
