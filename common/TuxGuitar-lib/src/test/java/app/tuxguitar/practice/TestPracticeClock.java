package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The interval clock as the musician sets it: a turn loops its unit, pass after pass, until the turn's time is up
 * (counted from the start of its Count In), the pass under way playing to its end; another turn follows only when a
 * turn's end plus a break comes before the session's time is up. Times read back in seconds.
 */
public class TestPracticeClock {

	private static final long SECOND = 1000000;

	private final TGPracticeSettings settings = new TGPracticeSettings();

	private TGPracticeClock clock(int turnMinutes, int breakSeconds, int sessionMinutes, boolean countInEveryPass) throws TGPracticeRefusal {
		settings.setIntervalClock(true);
		settings.setTurnMinutes(turnMinutes);
		settings.setBreakSeconds(breakSeconds);
		settings.setSessionMinutes(sessionMinutes);
		settings.setCountInEveryPass(countInEveryPass);
		return TGPracticeClock.of(settings);
	}

	// "passes, seconds" of a turn whose Count In and pass last so many seconds
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
		TGPracticeClock clock = clock(1, 10, 1, false);
		assertEquals("29, 60", turn(clock, 2, 2));
		assertEquals("8, 66", turn(clock, 2, 8));
		assertEquals("8, 64", turn(clock, 0, 8));
		assertEquals("2, 66", turn(clock, 2, 32));
		assertEquals("4, 68", turn(clock, 4, 16));
		assertEquals("1, 102", turn(clock, 2, 100));
	}

	// a Count In as long as the turn's time (many clicks at a slow tempo) still leaves the turn one pass
	@Test
	public void aTurnPlaysAtLeastOnePass() throws TGPracticeRefusal {
		assertEquals("1, 62 1, 72", turn(clock(1, 10, 1, false), 60, 2) + " " + turn(clock(1, 10, 1, false), 70, 2));
	}

	@Test
	public void withCountInBeforeEveryPassEachPassCountsItsCountIn() throws TGPracticeRefusal {
		TGPracticeClock clock = clock(1, 10, 1, true);
		assertEquals("6, 60", turn(clock, 2, 8));
		assertEquals("6, 60", turn(clock, 5, 5));
		assertEquals("8, 64", turn(clock, 0, 8));
		assertEquals("1, 102", turn(clock, 2, 100));
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
