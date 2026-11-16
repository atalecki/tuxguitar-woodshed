package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

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

	// turns the interval clock on: turns of 1 minute, breaks of 10 seconds, a session of 1 minute
	private void clockOn() {
		TestPracticeClock.turnOn(settings, 1, 10, 1);
	}

	private TGPracticeSchedule schedule(TGPracticeUnitKind kind, int from, int to, int clicks, int percent) throws TGPracticeRefusal {
		settings.setUnit(kind);
		settings.setPool(from, to);
		return TGPracticeSession.start(settings, song).schedule(header -> clicks, TGTempoScale.percent(percent));
	}

	// "29 passes, break 10" or, for the session's last turn, "29 passes, last" (no break after it)
	private static String plan(TGPracticePlan plan) {
		if (plan.isLast()) {
			return plan.getPasses() + " passes, last" + (plan.getBreakSeconds() == 0 ? "" : " and a break of " + plan.getBreakSeconds());
		}
		return plan.getPasses() + " passes, break " + plan.getBreakSeconds();
	}

	private String plans(TGPracticeSchedule schedule, TGPracticeBlock block, int turns) {
		StringBuilder plans = new StringBuilder();
		for (int turn = 0; turn < turns; turn++) {
			plans.append(plan(schedule.plan(block))).append("; ");
		}
		return plans.toString();
	}

	// the unit of the kind with that number, a block of its own
	private TGPracticeBlock unit(TGPracticeUnitKind kind, int number) throws TGPracticeRefusal {
		return block(TGPracticeGrouping.of(4, 4).unit(kind, number, 64));
	}

	private static TGPracticeBlock block(TGPracticeUnit unit) {
		return TGPracticeBlock.blocks(Collections.singletonList(unit)).get(0);
	}

	// so many turns of the first unit of the kind, planned by the schedule of a pool of units 1 to the one given
	private String planned(TGPracticeUnitKind kind, int to, int clicks, int percent, int turns) throws TGPracticeRefusal {
		return plans(schedule(kind, 1, to, clicks, percent), unit(kind, 1), turns);
	}

	@Test
	public void withoutTheClockATurnPlaysItsUnitOnceForTheTurnsSet() throws TGPracticeRefusal {
		settings.setTurns(3);
		assertEquals("1 passes, break 0; 1 passes, break 0; 1 passes, last; ", planned(TGPracticeUnitKind.MEASURE, 4, 4, 100, 3));
	}

	// with the clock a turn loops its unit for the turn's minutes and the session's time decides the last turn: each
	// case's clock (turn minutes, break seconds, session minutes), the pool's kind (units 1 to 4), the Count In's clicks,
	// the percent, and the plans of so many turns of unit 1
	@TestFactory
	public Stream<DynamicTest> withTheClockATurnLoopsItsUnitAndTheSessionsTimeDecidesTheLastTurn() {
		return Stream.of(
			timed("a measure loops for the turn, two turns in two minutes", "29 passes, break 10; 29 passes, last; ", new int[] {1, 10, 2}, TGPracticeUnitKind.MEASURE, 4, 100, 2),
			// turns of 66 seconds and breaks of 10: the third starts at 152 seconds, before the 3 minutes are up
			timed("the session's time counts every turn and break planned so far", "8 passes, break 10; 8 passes, break 10; 8 passes, last; ", new int[] {1, 10, 3},
				TGPracticeUnitKind.LINE, 4, 100, 3),
			timed("at half the tempo a pass and its Count In last twice as long", "4 passes, last; ", new int[] {1, 10, 1}, TGPracticeUnitKind.LINE, 4, 50, 1),
			timed("without a Count In a turn is its passes alone", "8 passes, break 0; 8 passes, last; ", new int[] {1, 0, 2}, TGPracticeUnitKind.LINE, 0, 100, 2));
	}

	private DynamicTest timed(String name, String plans, int[] clock, TGPracticeUnitKind kind, int clicks, int percent, int turns) {
		return DynamicTest.dynamicTest(name, () -> {
			TestPracticeClock.turnOn(settings, clock[0], clock[1], clock[2]);
			assertEquals(plans, planned(kind, 4, clicks, percent, turns));
		});
	}

	// line 1: measures 1 and 2 at 120 bpm, 3 and 4 at 60 bpm: 12 seconds a pass after a Count In of 2
	@Test
	public void aPassLastsAsLongAsItsMeasuresTakeAtTheirOwnTempos() throws TGPracticeRefusal {
		song.getMeasureHeader(2).getTempo().setQuarterValue(60);
		song.getMeasureHeader(3).getTempo().setQuarterValue(60);
		clockOn();
		assertEquals("5 passes, last; ", planned(TGPracticeUnitKind.LINE, 4, 4, 100, 1));
	}

	// the Count In is a beat of its first measure's time signature a click: 3 clicks of an eighth in 6/8 at 120 bpm
	// last 0.75 seconds, a measure of 6/8 1.5: 39 passes take 59.25 seconds, the 40th starts before the minute is up
	@Test
	public void theCountInLastsItsClicksAtItsFirstMeasuresBeat() throws TGPracticeRefusal {
		song.getMeasureHeader(0).getTimeSignature().setNumerator(6);
		song.getMeasureHeader(0).getTimeSignature().getDenominator().setValue(8);
		clockOn();
		assertEquals("40 passes, last; ", planned(TGPracticeUnitKind.MEASURE, 1, 3, 100, 1));
	}

	// line 2 is measures 5 to 8
	@Test
	public void aTurnIsPlannedToPlayItsUnitAtTheSessionsTempoScale() throws TGPracticeRefusal {
		TGPracticePlan plan = schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 50).plan(unit(TGPracticeUnitKind.LINE, 2));
		assertEquals("Line 2 Line 2 5-8 1/2", plan.getFrom() + " " + plan.getTo() + " " + plan.getFirstMeasure() + "-" + plan.getLastMeasure() + " " + plan.getScale());
	}

	@Test
	public void aTimedTurnIsPlannedToPlayItsUnit() throws TGPracticeRefusal {
		clockOn();
		TGPracticePlan plan = schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100).plan(unit(TGPracticeUnitKind.LINE, 3));
		assertEquals("Line 3 Line 3 9-12", plan.getFrom() + " " + plan.getTo() + " " + plan.getFirstMeasure() + "-" + plan.getLastMeasure());
	}

	// line 2 is measures 5 to 8 (ticks 16320 to 31680); beat 2 of measure 1 is ticks 1920 to 2880, its joint beat 3
	@Test
	public void aPlanSpansItsUnitsAndABeatsJoint() throws TGPracticeRefusal {
		TGPracticePlan line = schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100).plan(unit(TGPracticeUnitKind.LINE, 2));
		TGPracticePlan beat = schedule(TGPracticeUnitKind.BEAT, 1, 4, 4, 100).plan(block(TGPracticeUnit.beat(song.getMeasureHeader(0), 2)));
		assertEquals("16320 31680 31680 | 1920 2880 3840", spans(line) + " | " + spans(beat));
	}

	private String spans(TGPracticePlan plan) {
		TGPracticeSpan span = plan.span(song);
		return span.getStart() + " " + span.getJoint() + " " + span.getEnd();
	}

	// a timed turn of a beat (Count In 2 seconds) loops the beat and its joint: beat 2 of measure 1 and beat 3, 1 second;
	// beat 4 and measure 2's first, at 60 bpm, 1.5 seconds; the song's last beat alone, half a second
	@Test
	public void aTimedBeatsPassIsTheBeatAndItsJointEachAtItsMeasuresTempo() throws TGPracticeRefusal {
		song.getMeasureHeader(1).getTempo().setQuarterValue(60);
		assertEquals("58 passes, last | 39 passes, last | 116 passes, last", timedBeat(1, 2) + " | " + timedBeat(1, 4) + " | " + timedBeat(64, 4));
	}

	private String timedBeat(int measure, int beat) throws TGPracticeRefusal {
		clockOn();
		TGPracticeSchedule timed = schedule(TGPracticeUnitKind.BEAT, 1, 64, 4, 100);
		return plan(timed.plan(block(TGPracticeUnit.beat(song.getMeasureHeader(measure - 1), beat))));
	}

	// without a Count In before each turn a timed turn is its passes alone, the function's 4 clicks and "Count In before
	// every pass" taking no part: 8 passes of a line, not 6
	@Test
	public void withoutACountInBeforeEachTurnATimedTurnIsItsPassesAlone() throws TGPracticeRefusal {
		settings.setCountInEachTurn(false);
		settings.setCountInEveryPass(true);
		clockOn();
		TGPracticePlan plan = schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100).plan(unit(TGPracticeUnitKind.LINE, 1));
		assertEquals("8 passes, last false", plan(plan) + " " + plan.isCountInEveryPass());
	}

	@Test
	public void aPlanSaysWhetherACountInComesBeforeEveryPass() throws TGPracticeRefusal {
		settings.setCountInEveryPass(true);
		clockOn();
		TGPracticeSchedule timed = schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100);
		TGPracticePlan plan = timed.plan(unit(TGPracticeUnitKind.LINE, 1));
		settings.setIntervalClock(false);
		boolean untimed = schedule(TGPracticeUnitKind.LINE, 1, 4, 4, 100).plan(unit(TGPracticeUnitKind.LINE, 1)).isCountInEveryPass();
		assertEquals("6 passes, last true false", plan(plan) + " " + plan.isCountInEveryPass() + " " + untimed);
	}
}
