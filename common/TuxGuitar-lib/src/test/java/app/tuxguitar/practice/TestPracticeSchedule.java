package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * A session's schedule plans its turns one by one as they are chosen: how many passes each plays, and whether another
 * follows it, after a break of so many seconds. Without the interval clock a turn plays its unit once, for the number
 * of turns set; with it, a turn loops its unit for the turn's time, as long as its measures take at their tempos and
 * the Percentage of Tempo, and the session's time decides the last turn. The song has 64 measures of 4/4 at 120 bpm (2
 * seconds a measure), and a Count In is 4 clicks (2 seconds).
 */
public class TestPracticeSchedule {

	private final TGSong song = PracticeSongs.song(64);
	private final TGPracticeSettings settings = new TGPracticeSettings();

	private TestPracticeSchedule clock(int turnMinutes, int breakSeconds, int sessionMinutes) {
		settings.setIntervalClock(true);
		settings.setTurnMinutes(turnMinutes);
		settings.setBreakSeconds(breakSeconds);
		settings.setSessionMinutes(sessionMinutes);
		return this;
	}

	private TGPracticeSchedule schedule(TGPracticeUnitKind kind, int from, int to, int clicks, int percent) throws TGPracticeRefusal {
		settings.setUnit(kind);
		settings.setPool(from, to);
		return TGPracticeSession.start(settings, song).schedule(header -> clicks, percent);
	}

	// "29 passes, break 10" or, for the session's last turn, "29 passes, last" (no break after it)
	private static String plan(TGPracticePlan plan) {
		if (plan.isLast()) {
			return plan.getPasses() + " passes, last" + (plan.getBreakSeconds() == 0 ? "" : " and a break of " + plan.getBreakSeconds());
		}
		return plan.getPasses() + " passes, break " + plan.getBreakSeconds();
	}

	private String plans(TGPracticeSchedule schedule, TGPracticeUnit unit, int turns) {
		StringBuilder plans = new StringBuilder();
		for (int turn = 0; turn < turns; turn++) {
			plans.append(plan(schedule.plan(unit))).append("; ");
		}
		return plans.toString();
	}

	private TGPracticeUnit unit(TGPracticeUnitKind kind, int number) throws TGPracticeRefusal {
		return TGPracticeGrouping.of(4, 4).unit(kind, number, 64);
	}

	@Test
	public void withoutTheClockATurnPlaysItsUnitOnceForTheTurnsSet() throws TGPracticeRefusal {
		settings.setTurns(3);
		TGPracticeSchedule schedule = schedule(TGPracticeUnitKind.MEASURE, 1, 4, 4, 100);
		assertEquals("1 passes, break 0; 1 passes, break 0; 1 passes, last; ", plans(schedule, unit(TGPracticeUnitKind.MEASURE, 1), 3));
	}

	@Test
	public void withTheClockATurnLoopsItsUnitAndTheSessionsTimeDecidesTheLastTurn() throws TGPracticeRefusal {
		TGPracticeSchedule schedule = clock(1, 10, 2).schedule(TGPracticeUnitKind.MEASURE, 1, 4, 4, 100);
		assertEquals("29 passes, break 10; 29 passes, last; ", plans(schedule, unit(TGPracticeUnitKind.MEASURE, 1), 2));
	}

	// turns of 66 seconds and breaks of 10: the third starts at 152 seconds, before the 3 minutes are up
	@Test
	public void theSessionsTimeCountsEveryTurnAndBreakPlannedSoFar() throws TGPracticeRefusal {
		TGPracticeSchedule schedule = clock(1, 10, 3).schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100);
		assertEquals("8 passes, break 10; 8 passes, break 10; 8 passes, last; ", plans(schedule, unit(TGPracticeUnitKind.LINE, 1), 3));
	}

	// line 1: measures 1 and 2 at 120 bpm, 3 and 4 at 60 bpm: 12 seconds a pass after a Count In of 2
	@Test
	public void aPassLastsAsLongAsItsMeasuresTakeAtTheirOwnTempos() throws TGPracticeRefusal {
		song.getMeasureHeader(2).getTempo().setQuarterValue(60);
		song.getMeasureHeader(3).getTempo().setQuarterValue(60);
		TGPracticeSchedule schedule = clock(1, 10, 1).schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100);
		assertEquals("5 passes, last; ", plans(schedule, unit(TGPracticeUnitKind.LINE, 1), 1));
	}

	// the Count In is a beat of its first measure's time signature a click: 3 clicks of an eighth in 6/8 at 120 bpm
	// last 0.75 seconds, a measure of 6/8 1.5: 39 passes take 59.25 seconds, the 40th starts before the minute is up
	@Test
	public void theCountInLastsItsClicksAtItsFirstMeasuresBeat() throws TGPracticeRefusal {
		song.getMeasureHeader(0).getTimeSignature().setNumerator(6);
		song.getMeasureHeader(0).getTimeSignature().getDenominator().setValue(8);
		TGPracticeSchedule schedule = clock(1, 10, 1).schedule(TGPracticeUnitKind.MEASURE, 1, 1, 3, 100);
		assertEquals("40 passes, last; ", plans(schedule, unit(TGPracticeUnitKind.MEASURE, 1), 1));
	}

	@Test
	public void atHalfTheTempoAPassAndItsCountInLastTwiceAsLong() throws TGPracticeRefusal {
		TGPracticeSchedule schedule = clock(1, 10, 1).schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 50);
		assertEquals("4 passes, last; ", plans(schedule, unit(TGPracticeUnitKind.LINE, 1), 1));
	}

	@Test
	public void withoutACountInATurnIsItsPassesAlone() throws TGPracticeRefusal {
		TGPracticeSchedule schedule = clock(1, 0, 2).schedule(TGPracticeUnitKind.LINE, 1, 4, 0, 100);
		assertEquals("8 passes, break 0; 8 passes, last; ", plans(schedule, unit(TGPracticeUnitKind.LINE, 1), 2));
	}

	@Test
	public void aPlanSaysWhetherACountInComesBeforeEveryPass() throws TGPracticeRefusal {
		settings.setCountInEveryPass(true);
		TGPracticeSchedule timed = clock(1, 10, 1).schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100);
		TGPracticePlan plan = timed.plan(unit(TGPracticeUnitKind.LINE, 1));
		settings.setIntervalClock(false);
		boolean untimed = schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100).plan(unit(TGPracticeUnitKind.LINE, 1)).isCountInEveryPass();
		assertEquals("6 passes, last true false", plan(plan) + " " + plan.isCountInEveryPass() + " " + untimed);
	}
}
