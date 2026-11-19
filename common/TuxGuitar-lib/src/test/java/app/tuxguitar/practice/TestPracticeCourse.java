package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * Interleaved practice's course (TGPracticeSession.course): each turn plays the unit the session's chooser chooses from
 * the seed, planned by its schedule at the tempo scale, the last one the schedule's last; it refuses a flag as the
 * chooser does; it is counted or timed as the session is, and ends with it, the session with it. The pool is measures
 * of an 8-measure song, for 6 turns.
 */
public class TestPracticeCourse {

	private static final long SEED = 7;

	private final TGPracticeSettings settings = new TGPracticeSettings();
	private TGPracticeSession session;

	public TestPracticeCourse() {
		settings.setPool(1, 4);
		settings.setTurns(6);
	}

	// the course of a new session as set, at 80%
	private TGPracticeCourse course() throws TGPracticeRefusal {
		session = TGPracticeSession.start(settings, PracticeSongs.song(8));
		return session.course(SEED, header -> 0, TGTempoScale.percent(80));
	}

	// "Measure 3@4/5", " last" after the last
	@Test
	public void eachTurnPlaysTheUnitTheChooserChoosesFromTheSeedAtTheScale() throws TGPracticeRefusal {
		TGPracticeCourse course = course();
		TGPracticeChooser chooser = session.chooser(SEED);
		List<String> expected = new ArrayList<String>();
		List<String> played = new ArrayList<String>();
		for (int turn = 1; turn <= 6; turn++) {
			TGPracticePlan plan = course.next();
			played.add(plan.getFrom() + "@" + plan.getScale() + (plan.isLast() ? " last" : ""));
			expected.add(chooser.next() + "@4/5" + (turn == 6 ? " last" : ""));
		}
		assertEquals(expected, played);
	}

	// a cycle of 2 measures, then measure 2 flagged 3x: a cycle of 4 turns could not keep "No consecutive repeats"
	@Test
	public void aFlagTheTurnsToComeCouldNotPlayIsRefused() throws TGPracticeRefusal {
		settings.setPool(1, 2);
		TGPracticeCourse course = course();
		course.next();
		assertEquals("Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row.", PracticeRefusals.outcome(() -> course.checkFlag(2, 3)));
	}

	// interleaved practice's own Count In before each turn (on until turned off), whatever the function's Count In: as
	// many clicks as it says before a turn's first measure (here measure 3's), or none
	@Test
	public void aTurnCountsInAsTheFunctionSaysWithCountInBeforeEachTurnOn() throws TGPracticeRefusal {
		int eachTurn = clicksBeforeMeasure3();
		settings.setCountInEachTurn(false);
		assertEquals("5 0", eachTurn + " " + clicksBeforeMeasure3());
	}

	// the clicks a new session's course counts in before measure 3, the function saying 2 more than its number
	private int clicksBeforeMeasure3() throws TGPracticeRefusal {
		TGSong song = PracticeSongs.song(8);
		return TGPracticeSession.start(settings, song).course(SEED, header -> header.getNumber() + 2, TGTempoScale.percent(80)).clicks(song.getMeasureHeader(2));
	}

	// a turn chosen at random is no step to go back to: Repeat step and Previous step have nothing to go back over
	@Test
	public void aCourseOfRandomTurnsHasNoSteps() throws TGPracticeRefusal {
		assertEquals("no steps", course().getSteps().map(steps -> "steps").orElse("no steps"));
	}

	@Test
	public void aCountedCourseHasTheSessionsTurns() throws TGPracticeRefusal {
		TGPracticeCourse course = course();
		assertEquals("false 6", course.isTimed() + " " + course.getTurns());
	}

	// a 1-minute session of 1-minute turns: the most turns it can hold
	@Test
	public void aTimedCourseHoldsTheTurnsItsSessionCan() throws TGPracticeRefusal {
		settings.setIntervalClock(true);
		settings.setTurnMinutes(1);
		settings.setBreakSeconds(10);
		settings.setSessionMinutes(1);
		TGPracticeCourse course = course();
		assertEquals("true " + session.getTurns(), course.isTimed() + " " + course.getTurns());
	}

	@Test
	public void aCourseEndsWithItsSessionAndTheSessionWithIt() throws TGPracticeRefusal {
		course().end();
		TGPracticeSession ended = session;
		TGPracticeCourse other = course();
		boolean before = other.isEnded();
		session.end();
		assertEquals("true false true", ended.isEnded() + " " + before + " " + other.isEnded());
	}
}
