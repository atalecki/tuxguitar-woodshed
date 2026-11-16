package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * Interleaved click-up's course (TGClickUpLadder.course): a turn per step of the ladder, in its order, each the measures
 * of the step's sections at its tempo scale, the ladder's passes, with no break, the last step the last turn. It counts
 * its steps, is not timed, and takes any flag without changing a step. Each play has a course of its own, which ends as
 * a session does. Repeat step and Previous step go back over its steps: the step chosen is planned next, then the steps
 * after it. The song has 64 measures of 4/4 at 120 bpm; the passage is lines 2 to 3 of 4 measures, 50% to 100% by
 * 50%: 2@50%, 2@100%, 2-3@50%, 3@100%, 2-3@100%.
 */
public class TestClickUpCourse {

	// a Count In of a click more than the measure's beats, as the player's Count In could have
	private static final ToIntFunction<TGMeasureHeader> CLICKS = header -> header.getTimeSignature().getNumerator() + 1;

	private final TGClickUpSettings settings = new TGClickUpSettings();
	private final TGSong song = PracticeSongs.song(64);

	public TestClickUpCourse() {
		settings.setUnit(TGPracticeUnitKind.LINE);
		settings.setPassageFrom(2);
		settings.setPassageTo(3);
		settings.setStart(50);
		settings.setGoal(100);
		settings.setIncrement(50);
	}

	private TGPracticeCourse course() throws TGPracticeRefusal {
		return TGClickUpLadder.of(settings, song).course(CLICKS);
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

	// its steps are there to go back over: Repeat step during step 3 plans step 3
	@Test
	public void aCourseCountsItsStepsIsNotTimedAndHasStepsToGoBackOver() throws TGPracticeRefusal {
		TGPracticeCourse course = course();
		TGPracticeSteps steps = course.getSteps().get();
		assertEquals("false 5 3", course.isTimed() + " " + course.getTurns() + " " + steps.repeat(3));
	}

	// the click-up's own Count In before each step (on until turned off), whatever the function's Count In: as many clicks
	// as it says before a step's first measure (here measure 5's), or none
	@Test
	public void aStepCountsInAsTheFunctionSaysWithCountInBeforeEachStepOn() throws TGPracticeRefusal {
		TGMeasureHeader first = song.getMeasureHeader(4);
		int eachStep = course().clicks(first);
		settings.setCountInEachStep(false);
		assertEquals("5 0", eachStep + " " + course().clicks(first));
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
		TGClickUpLadder ladder = TGClickUpLadder.of(settings, song);
		TGPracticeCourse course = ladder.course(CLICKS);
		course.next();
		boolean before = course.isEnded();
		course.end();
		course.end();
		TGPracticeCourse other = ladder.course(CLICKS);
		assertEquals("false true false Line 2-Line 2 5-8@1/2 x1 every pass false, break 0, last false",
			before + " " + course.isEnded() + " " + other.isEnded() + " " + reading(other.next()));
	}

	// "5-12@1/2", " last" after the last step
	private static String measures(TGPracticePlan plan) {
		return plan.getFirstMeasure() + "-" + plan.getLastMeasure() + "@" + plan.getScale() + (plan.isLast() ? " last" : "");
	}

	// the course has planned steps 1 to 4 (step 4 ahead, while step 3 plays), then the step is chosen during the turn:
	// "the step's turn | the plans of the turns after it, so many"
	private String chosen(ToIntBiFunction<TGPracticeSteps, Integer> choice, int turn, int after) throws TGPracticeRefusal {
		TGPracticeCourse course = course();
		plans(4, course::next);
		int step = choice.applyAsInt(course.getSteps().get(), turn);
		List<String> plans = new ArrayList<String>();
		for (int plan = 0; plan < after; plan++) {
			plans.add(measures(course.next()));
		}
		return step + " | " + String.join(", ", plans);
	}

	// Repeat step during step 3: step 3, then 4 and 5; Previous step during step 3: step 2, then 3; during step 1: step 1,
	// then 2; Repeat step during step 5: step 5, the last
	@Test
	public void theStepChosenIsPlannedNextThenTheStepsAfterIt() throws TGPracticeRefusal {
		assertEquals(List.of("3 | 5-12@1/2, 9-12@1/1, 5-12@1/1 last", "2 | 5-8@1/1, 5-12@1/2", "1 | 5-8@1/2, 5-8@1/1", "5 | 5-12@1/1 last"),
			List.of(chosen(TGPracticeSteps::repeat, 3, 3), chosen(TGPracticeSteps::previous, 3, 2), chosen(TGPracticeSteps::previous, 1, 2),
				chosen(TGPracticeSteps::repeat, 5, 1)));
	}
}
