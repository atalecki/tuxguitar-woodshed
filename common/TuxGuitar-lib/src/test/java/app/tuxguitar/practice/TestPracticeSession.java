package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * Starting interleaved practice: the settings, as the musician leaves or sets them, are checked once; a session that
 * cannot be played as set is refused, saying why. The song has 64 measures of 4/4.
 */
public class TestPracticeSession {

	private final TGSong song = PracticeSongs.song(64);
	private final TGPracticeSettings settings = new TGPracticeSettings();

	private TestPracticeSession flag(int measure, int difficulty) {
		song.getMeasureHeader(measure - 1).setDifficulty(difficulty);
		return this;
	}

	private TestPracticeSession set(TGPracticeUnitKind kind, int from, int to, int turns) {
		settings.setUnit(kind);
		settings.setPool(from, to);
		settings.setTurns(turns);
		return this;
	}

	private String start() {
		return PracticeRefusals.outcome(() -> TGPracticeSession.start(settings, song));
	}

	@Test
	public void theSettingsStartAsTheSpecSays() {
		assertEquals("MEASURE 1-1 1 turns 4/4 true true, Count In each turn true", settings.getUnit() + " " + settings.getPoolFrom() + "-"
			+ settings.getPoolTo() + " " + settings.getTurns() + " turns " + settings.getMeasuresPerLine() + "/" + settings.getLinesPerPage() + " "
			+ settings.isNoConsecutiveRepeats() + " " + settings.isCompleteCycle() + ", Count In each turn " + settings.isCountInEachTurn());
	}

	@Test
	public void aPoolMustLieInTheSong() {
		assertEquals("The pool reaches past the song's 64 measures.", set(TGPracticeUnitKind.MEASURE, 60, 70, 6).start());
		assertEquals("The pool reaches past the song's 4 pages.", set(TGPracticeUnitKind.PAGE, 1, 5, 6).start());
		assertEquals("The pool reaches past the song's 16 lines.", set(TGPracticeUnitKind.LINE, 16, 17, 6).start());
		assertEquals("The pool is empty.", set(TGPracticeUnitKind.LINE, 9, 8, 6).start());
		assertEquals("The pool starts before line 1.", set(TGPracticeUnitKind.LINE, 0, 2, 6).start());
		assertEquals("accepted accepted", set(TGPracticeUnitKind.MEASURE, 1, 64, 6).start() + " " + set(TGPracticeUnitKind.PAGE, 1, 4, 6).start());
	}

	@Test
	public void aSongOfOnePageSaysSo() {
		TGSong short1 = PracticeSongs.song(16);
		settings.setUnit(TGPracticeUnitKind.PAGE);
		settings.setPool(1, 2);
		assertEquals("The pool reaches past the song's 1 page.", PracticeRefusals.outcome(() -> TGPracticeSession.start(settings, short1)));
	}

	@Test
	public void aSessionNeedsATurnAndGroupsTheirParts() {
		assertEquals("A session needs at least 1 turn.", set(TGPracticeUnitKind.MEASURE, 1, 4, 0).start());
		settings.setMeasuresPerLine(0);
		settings.setLinesPerPage(4);
		assertEquals("A line needs at least 1 measure.", set(TGPracticeUnitKind.LINE, 1, 2, 6).start());
		settings.setMeasuresPerLine(4);
		settings.setLinesPerPage(0);
		assertEquals("A page needs at least 1 line.", set(TGPracticeUnitKind.PAGE, 1, 2, 6).start());
	}

	// with w the heaviest unit's weight and L the weights added up: up to L turns need 2w <= L + 1, more 2w <= L; the
	// refusal names the unit, a line as a line
	@Test
	public void noConsecutiveRepeatsRefusesWeightsThatCannotKeepIt() {
		assertEquals("Measure 5 would come twice in a row.", set(TGPracticeUnitKind.MEASURE, 5, 5, 2).start());
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 5, 5, 1).start());
		flag(1, 3);
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 1, 3, 5).start());
		assertEquals("Measure 1 comes up 3 times in a cycle of 5 turns and would come twice in a row.", set(TGPracticeUnitKind.MEASURE, 1, 3, 6).start());
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 1, 4, 12).start());
		assertEquals("Measure 1 comes up 3 times in a cycle of 4 turns and would come twice in a row.", set(TGPracticeUnitKind.MEASURE, 1, 2, 2).start());
		flag(1, 1).flag(6, 4).flag(7, 4);
		assertEquals("Line 2 comes up 4 times in a cycle of 5 turns and would come twice in a row.", set(TGPracticeUnitKind.LINE, 1, 2, 2).start());
	}

	private TestPracticeSession clock(int turnMinutes, int breakSeconds, int sessionMinutes) {
		TestPracticeClock.turnOn(settings, turnMinutes, breakSeconds, sessionMinutes);
		return this;
	}

	// the interval clock times the session: the rule counts the most turns it can hold, its time over a turn's time
	// plus a break, rounded up; the number of turns set takes no part
	@Test
	public void aTimedSessionKeepsTheRuleForTheMostTurnsItCanHold() {
		flag(1, 2);
		set(TGPracticeUnitKind.MEASURE, 1, 2, 0);
		String refused = "Measure 1 comes up 2 times in a cycle of 3 turns and would come twice in a row.";
		assertEquals("accepted", clock(1, 0, 3).start());
		assertEquals(refused, clock(1, 0, 4).start());
		assertEquals("accepted", clock(1, 30, 4).start());
		assertEquals(refused, clock(1, 10, 4).start());
	}

	@Test
	public void aTimedSessionHoldsAtMostItsClocksTurns() throws TGPracticeRefusal {
		set(TGPracticeUnitKind.MEASURE, 1, 4, 9);
		TGPracticeSession session = TGPracticeSession.start(clock(1, 10, 4).settings, song);
		assertEquals("4 10", session.getTurns() + " " + session.getClock().get().getBreakSeconds());
	}

	// with the clock a pool of one unit plays it every turn: the rule cannot apply, and a flag is never refused
	@Test
	public void aTimedPoolOfOneUnitIsNotHeldToTheRule() throws TGPracticeRefusal {
		TGPracticeSession session = TGPracticeSession.start(clock(1, 0, 30).set(TGPracticeUnitKind.MEASURE, 5, 5, 0).settings, song);
		TGPracticeChooser chooser = session.chooser(1);
		chooser.checkFlag(5, 5);
		assertEquals("Measure 5 Measure 5 Measure 5 false", chooser.next() + " " + chooser.next() + " " + chooser.next() + " " + session.isNoConsecutiveRepeats());
	}

	@Test
	public void aSessionWithoutTheClockHasNone() throws TGPracticeRefusal {
		assertEquals("false", Boolean.toString(TGPracticeSession.start(set(TGPracticeUnitKind.MEASURE, 1, 4, 9).settings, song).getClock().isPresent()));
	}

	@Test
	public void aClockThatCannotTimeTheSessionRefusesIt() {
		set(TGPracticeUnitKind.MEASURE, 1, 4, 6);
		assertEquals("A turn needs at least 1 minute.", clock(0, 10, 4).start());
	}

	@Test
	public void withoutTheRepeatRuleAnyWeightsPlay() {
		settings.setNoConsecutiveRepeats(false);
		flag(1, 5);
		assertEquals("accepted accepted", set(TGPracticeUnitKind.MEASURE, 5, 5, 6).start() + " " + set(TGPracticeUnitKind.MEASURE, 1, 4, 6).start());
	}

	// without cycles each turn leaves out only the unit before: any pool of two units or more can keep the rule
	@Test
	public void withoutCyclesOnlyAPoolOfOneUnitCannotKeepTheRule() {
		settings.setCompleteCycle(false);
		flag(1, 5);
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 1, 2, 9).start());
		assertEquals("Measure 1 would come twice in a row.", set(TGPracticeUnitKind.MEASURE, 1, 1, 2).start());
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 1, 1, 1).start());
	}

	@Test
	public void aSessionHoldsThePoolsUnitsInOrder() throws TGPracticeRefusal {
		settings.setMeasuresPerLine(5);
		settings.setLinesPerPage(3);
		set(TGPracticeUnitKind.LINE, 12, 13, 1);
		assertEquals("[Line 12, Line 13]", TGPracticeSession.start(settings, song).getPool().toString());
	}

	// a line or page weighs as much as its most difficult measure
	@Test
	public void aUnitWeighsAsMuchAsItsMostDifficultMeasure() throws TGPracticeRefusal {
		flag(11, 3).flag(12, 2);
		set(TGPracticeUnitKind.LINE, 1, 4, 1);
		assertEquals("[1, 1, 3, 1]", Arrays.toString(TGPracticeSession.start(settings, song).weights()));
		set(TGPracticeUnitKind.PAGE, 1, 4, 1);
		assertEquals("[3, 1, 1, 1]", Arrays.toString(TGPracticeSession.start(settings, song).weights()));
		IntUnaryOperator flags = TGPracticeUnit.difficulties(song);
		assertEquals("3 2 1", TGPracticeGrouping.of(4, 4).unit(TGPracticeUnitKind.LINE, 3, 64).weight(flags)
			+ " " + TGPracticeGrouping.of(1, 1).unit(TGPracticeUnitKind.LINE, 12, 64).weight(flags)
			+ " " + TGPracticeGrouping.of(1, 1).unit(TGPracticeUnitKind.LINE, 13, 64).weight(flags));
	}

	// a pool of beats holds every beat of its measures, each as difficult as its measure
	@Test
	public void aPoolOfBeatsHoldsTheBeatsOfItsMeasuresEachAsDifficultAsItsMeasure() throws TGPracticeRefusal {
		flag(4, 3);
		TGPracticeSession session = TGPracticeSession.start(set(TGPracticeUnitKind.BEAT, 3, 4, 1).settings, song);
		assertEquals("[Measure 3 beat 1, Measure 3 beat 2, Measure 3 beat 3, Measure 3 beat 4, Measure 4 beat 1, Measure 4 beat 2, Measure 4 beat 3, "
			+ "Measure 4 beat 4] [1, 1, 1, 1, 3, 3, 3, 3]", session.getPool() + " " + Arrays.toString(session.weights()));
	}

	// a measure of one beat (1/4) flagged 5x holds 5 of a cycle of 9 beats; the pool is refused in measures
	@Test
	public void aPoolOfBeatsIsRefusedNamingTheBeatOrTheMeasures() {
		PracticeSongs.signature(song, 2, 1, 4);
		String alone = set(TGPracticeUnitKind.BEAT, 2, 2, 2).start();
		flag(2, 5);
		assertEquals("Measure 2 beat 1 would come twice in a row. | accepted | Measure 2 beat 1 comes up 5 times in a cycle of 9 turns and would come twice in a row. | "
			+ "The pool reaches past the song's 64 measures.", alone + " | " + set(TGPracticeUnitKind.BEAT, 1, 2, 9).start() + " | "
				+ set(TGPracticeUnitKind.BEAT, 1, 2, 10).start() + " | " + set(TGPracticeUnitKind.BEAT, 60, 70, 1).start());
	}

	// a session reads the flags when asked: a flag set after it started counts
	@Test
	public void aSessionReadsTheFlagsAsTheyAre() throws TGPracticeRefusal {
		set(TGPracticeUnitKind.MEASURE, 1, 3, 1);
		TGPracticeSession session = TGPracticeSession.start(settings, song);
		flag(2, 4);
		assertEquals("[1, 4, 1] [1, 4, 5]", Arrays.toString(session.weights()) + " " + Arrays.toString(session.weights(3, 5)));
	}

	// a session plays until it ends (Stop, the last turn, moving the position, an edit): it starts under way, and ends
	// once, whatever ends it again
	@Test
	public void aSessionIsUnderWayUntilItEnds() throws TGPracticeRefusal {
		TGPracticeSession session = TGPracticeSession.start(set(TGPracticeUnitKind.MEASURE, 1, 4, 6).settings, song);
		boolean before = session.isEnded();
		session.end();
		boolean once = session.isEnded();
		session.end();

		assertEquals("false true true", before + " " + once + " " + session.isEnded());
	}
}
