package app.tuxguitar.practice;

import java.util.Optional;

/**
 * Interleaved practice's course: each turn's unit chosen at random by the session's chooser (TGPracticeChooser), then
 * planned by its schedule (TGPracticeSchedule), at the tempo scale the session plays at. It is counted or timed as the
 * session is, and ends with it.
 */
final class TGPracticeRandomCourse implements TGPracticeCourse {

	private final TGPracticeChooser chooser;
	private final TGPracticeSchedule schedule;

	TGPracticeRandomCourse(TGPracticeChooser chooser, TGPracticeSchedule schedule) {
		this.chooser = chooser;
		this.schedule = schedule;
	}

	public TGPracticePlan ahead() {
		return this.schedule.plan(this.chooser.ahead());
	}

	public void started() {
		this.chooser.started();
	}

	public TGPracticePlan next() {
		return this.schedule.plan(this.chooser.next());
	}

	public void checkFlag(int measure, int difficulty) throws TGPracticeRefusal {
		this.chooser.checkFlag(measure, difficulty);
	}

	public boolean isTimed() {
		return this.session().getClock().isPresent();
	}

	public int getTurns() {
		return this.session().getTurns();
	}

	public boolean isEnded() {
		return this.session().isEnded();
	}

	public void end() {
		this.session().end();
	}

	// a turn chosen at random is no step to go back to
	public Optional<TGPracticeSteps> getSteps() {
		return Optional.empty();
	}

	private TGPracticeSession session() {
		return this.chooser.getSession();
	}
}
