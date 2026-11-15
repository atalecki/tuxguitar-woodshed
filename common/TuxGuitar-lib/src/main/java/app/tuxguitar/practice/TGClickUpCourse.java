package app.tuxguitar.practice;

import java.util.List;

/**
 * Interleaved click-up's course (TGClickUpLadder.course): a turn per step of the ladder, in its order, walked with a
 * cursor. Each plays the measures of the step's sections at its tempo scale, the ladder's passes, with no break; the
 * last step is the last turn. It counts its steps and is not timed. A flag changes none of them (it chooses nothing a
 * flag could weigh), so it takes any. One per play, planned on the one thread that lays the turns, ended on any.
 */
final class TGClickUpCourse implements TGPracticeCourse {

	private final TGClickUpLadder ladder;
	// the step the next turn plays (from 0)
	private int cursor;
	// whether it has ended: set once, on the thread that ends it, read on any
	private volatile boolean ended;

	TGClickUpCourse(TGClickUpLadder ladder) {
		this.ladder = ladder;
	}

	public TGPracticePlan ahead() {
		List<TGClickUpStep> steps = this.ladder.getSteps();
		TGClickUpStep step = steps.get(this.cursor++);
		return new TGPracticePlan(step.from, step.to, step.getScale(), this.ladder.getPasses(), this.ladder.isCountInEveryPass(), 0, this.cursor == steps.size());
	}

	// the order of the steps does not hang on when one starts
	public void started() {
	}

	public TGPracticePlan next() {
		return this.ahead();
	}

	public void checkFlag(int measure, int difficulty) {
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
}
