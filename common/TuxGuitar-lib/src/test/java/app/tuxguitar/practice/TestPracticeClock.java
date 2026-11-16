package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * The interval clock as the musician sets it: a turn loops its unit, pass after pass, until the turn's time is up
 * (counted from the start of its Count In), the pass under way playing to its end; another turn follows only when a
 * turn's end plus a break comes before the session's time is up. Times read back in seconds.
 */
public class TestPracticeClock {

	private static final long SECOND = 1000000;

	private final TGPracticeSettings settings = new TGPracticeSettings();

	// turns the interval clock on in the settings, as set (the session's and the schedule's tests too)
	static void turnOn(TGPracticeSettings settings, int turnMinutes, int breakSeconds, int sessionMinutes) {
		settings.setIntervalClock(true);
		settings.setTurnMinutes(turnMinutes);
		settings.setBreakSeconds(breakSeconds);
		settings.setSessionMinutes(sessionMinutes);
	}

	private TGPracticeClock clock(int turnMinutes, int breakSeconds, int sessionMinutes, boolean countInEveryPass) throws TGPracticeRefusal {
		turnOn(settings, turnMinutes, breakSeconds, sessionMinutes);
		settings.setCountInEveryPass(countInEveryPass);
		return TGPracticeClock.of(settings);
	}

	// "passes, seconds" of each turn whose Count In and pass last so many seconds, given in pairs, joined by " | "
	private static String turns(TGPracticeClock clock, long... countInsAndPasses) {
		return IntStream.range(0, countInsAndPasses.length / 2).mapToObj(turn -> turn(clock, countInsAndPasses[2 * turn], countInsAndPasses[2 * turn + 1]))
			.collect(Collectors.joining(" | "));
	}

	private static String turn(TGPracticeClock clock, long countIn, long pass) {
		return clock.passes(countIn * SECOND, pass * SECOND) + ", " + clock.length(countIn * SECOND, pass * SECOND) / SECOND;
	}

	@Test
	public void theClockStartsOffTimingTurnsOf3MinutesBreaksOf30SecondsAndSessionsOf30Minutes() {
		assertEquals("false 3 30 30 false", settings.isIntervalClock() + " " + settings.getTurnMinutes() + " " + settings.getBreakSeconds()
			+ " " + settings.getSessionMinutes() + " " + settings.isCountInEveryPass());
	}

	@Test
	public void aTurnLoopsItsUnitUntilItsTimeIsUpThePassUnderWayPlayingToItsEnd() throws TGPracticeRefusal {
		assertEquals("29, 60 | 8, 66 | 8, 64 | 2, 66 | 4, 68 | 1, 102", turns(clock(1, 10, 1, false), 2, 2, 2, 8, 0, 8, 2, 32, 4, 16, 2, 100));
	}

	// a Count In as long as the turn's time (many clicks at a slow tempo) still leaves the turn one pass
	@Test
	public void aTurnPlaysAtLeastOnePass() throws TGPracticeRefusal {
		assertEquals("1, 62 1, 72", turn(clock(1, 10, 1, false), 60, 2) + " " + turn(clock(1, 10, 1, false), 70, 2));
	}

	@Test
	public void withCountInBeforeEveryPassEachPassCountsItsCountIn() throws TGPracticeRefusal {
		assertEquals("6, 60 | 6, 60 | 8, 64 | 1, 102", turns(clock(1, 10, 1, true), 2, 8, 5, 5, 0, 8, 2, 100));
	}

	// before every pass only with a Count In before each turn: without one there is none to put before every pass
	@Test
	public void aCountInComesBeforeEveryPassOnlyWithOneBeforeEachTurn() throws TGPracticeRefusal {
		boolean eachTurn = clock(1, 10, 1, true).isCountInEveryPass();
		settings.setCountInEachTurn(false);
		assertEquals("true false", eachTurn + " " + clock(1, 10, 1, true).isCountInEveryPass());
	}

	@Test
	public void anotherTurnFollowsOnlyWhenTheTurnsEndPlusABreakComesBeforeTheSessionsTimeIsUp() throws TGPracticeRefusal {
		TGPracticeClock clock = clock(1, 10, 2, false);
		assertEquals("true false false", clock.follows(66 * SECOND) + " " + clock.follows(110 * SECOND) + " " + clock.follows(142 * SECOND));
		TGPracticeClock noBreak = clock(1, 0, 2, false);
		assertEquals("true false", noBreak.follows(60 * SECOND + 999999) + " " + noBreak.follows(120 * SECOND));
	}

	@Test
	public void theBreakIsSoManySeconds() throws TGPracticeRefusal {
		assertEquals("10 0", clock(1, 10, 2, false).getBreakSeconds() + " " + clock(1, 0, 2, false).getBreakSeconds());
	}

	// a turn lasts at least its time and a break follows every turn but the last: the session's time over a turn's
	// time plus a break, rounded up
	@Test
	public void aSessionHoldsAtMostItsTimeOverATurnsTimePlusABreakRoundedUpTurns() throws TGPracticeRefusal {
		assertEquals("3 4 3 4 1", clock(1, 0, 3, false).mostTurns() + " " + clock(1, 0, 4, false).mostTurns() + " " + clock(1, 30, 4, false).mostTurns()
			+ " " + clock(1, 10, 4, false).mostTurns() + " " + clock(2, 10, 2, false).mostTurns());
	}

	@Test
	public void aClockThatCannotTimeASessionIsRefusedSayingWhy() {
		assertEquals("A turn needs at least 1 minute.", refusal(0, 10, 4));
		assertEquals("A break cannot be shorter than 0 seconds.", refusal(1, -1, 4));
		assertEquals("A session needs at least the minutes of one turn.", refusal(2, 10, 1));
		assertEquals("accepted", refusal(2, 10, 2));
	}

	private String refusal(int turnMinutes, int breakSeconds, int sessionMinutes) {
		try {
			clock(turnMinutes, breakSeconds, sessionMinutes, false);
			return "accepted";
		} catch (TGPracticeRefusal refusal) {
			return refusal.getMessage();
		}
	}
}
