package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * Interleaved click-up's course (TGClickUpLadder.course): a turn per step of the ladder, in its order, each the measures
 * of the step's sections at its tempo scale, the ladder's passes, with no break, the last step the last turn. It counts
 * its steps, is not timed, and takes any flag without changing a step. Each play has a course of its own, which ends as
 * a session does. The song has 64 measures of 4/4 at 120 bpm; the passage is lines 2 to 3 of 4 measures, 50% to 100% by
 * 50%: 2@50%, 2@100%, 2-3@50%, 3@100%, 2-3@100%.
 */
public class TestClickUpCourse {

	private final TGClickUpSettings settings = new TGClickUpSettings();

	public TestClickUpCourse() {
		settings.setUnit(TGPracticeUnitKind.LINE);
		settings.setPassageFrom(2);
		settings.setPassageTo(3);
		settings.setStart(50);
		settings.setGoal(100);
		settings.setIncrement(50);
	}

	private TGPracticeCourse course() throws TGPracticeRefusal {
		return TGClickUpLadder.of(settings, PracticeSongs.song(64)).course();
	}

	// "Line 2-Line 3 5-12@1/2 x2 every pass true, break 0, last false"
	private static String reading(TGPracticePlan plan) {
		return plan.getFrom() + "-" + plan.getTo() + " " + plan.getFirstMeasure() + "-" + plan.getLastMeasure() + "@" + plan.getScale() + " x" + plan.getPasses()
			+ " every pass " + plan.isCountInEveryPass() + ", break " + plan.getBreakSeconds() + ", last " + plan.isLast();
	}

	private static List<String> plans(int count, Supplier<TGPracticePlan> plan) {
		List<String> plans = new ArrayList<String>();
		for (int turn = 0; turn < count; turn++) {
			plans.add(reading(plan.get()));
		}
		return plans;
	}

	@Test
	public void eachTurnPlaysTheNextStepOfTheLadder() throws TGPracticeRefusal {
		settings.setPasses(2);
		settings.setCountInEveryPass(true);
		assertEquals(List.of("Line 2-Line 2 5-8@1/2 x2 every pass true, break 0, last false", "Line 2-Line 2 5-8@1/1 x2 every pass true, break 0, last false",
			"Line 2-Line 3 5-12@1/2 x2 every pass true, break 0, last false", "Line 3-Line 3 9-12@1/1 x2 every pass true, break 0, last false",
			"Line 2-Line 3 5-12@1/1 x2 every pass true, break 0, last true"),
			plans(5, course()::next));
	}

	// planned ahead, live, the steps come in the same order
	@Test
	public void aTurnPlannedAheadIsTheNextStep() throws TGPracticeRefusal {
		TGPracticeCourse course = course();
		List<String> plans = plans(1, course::next);
		plans.addAll(plans(1, course::ahead));
		course.started();
		plans.addAll(plans(1, course::next));
		assertEquals(List.of("Line 2-Line 2 5-8@1/2 x1 every pass false, break 0, last false", "Line 2-Line 2 5-8@1/1 x1 every pass false, break 0, last false",
			"Line 2-Line 3 5-12@1/2 x1 every pass false, break 0, last false"), plans);
	}

	@Test
	public void aCourseCountsItsStepsAndIsNotTimed() throws TGPracticeRefusal {
		TGPracticeCourse course = course();
		assertEquals("false 5", course.isTimed() + " " + course.getTurns());
	}

	// a click-up chooses nothing a flag could weigh
	@Test
	public void aFlagIsTakenAndTheStepsStayAsTheyWere() throws TGPracticeRefusal {
		TGPracticeCourse course = course();
		course.next();
		course.checkFlag(9, 3);
		assertEquals(List.of("Line 2-Line 2 5-8@1/1 x1 every pass false, break 0, last false", "Line 2-Line 3 5-12@1/2 x1 every pass false, break 0, last false"),
			plans(2, course::next));
	}

	// a course of its own per play: ending one ends neither another's steps nor it
	@Test
	public void aCourseEndsAndStaysEndedAndEachPlayHasItsOwn() throws TGPracticeRefusal {
		TGClickUpLadder ladder = TGClickUpLadder.of(settings, PracticeSongs.song(64));
		TGPracticeCourse course = ladder.course();
		course.next();
		boolean before = course.isEnded();
		course.end();
		course.end();
		TGPracticeCourse other = ladder.course();
		assertEquals("false true false Line 2-Line 2 5-8@1/2 x1 every pass false, break 0, last false",
			before + " " + course.isEnded() + " " + other.isEnded() + " " + reading(other.next()));
	}
}
