package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

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
		try {
			TGPracticeSession.start(settings, song);
			return "accepted";
		} catch (TGPracticeRefusal refusal) {
			return refusal.getMessage();
		}
	}

	@Test
	public void theSettingsStartAsTheSpecSays() {
		assertEquals("MEASURE 1-1 1 turns 4/4 true true", settings.getUnit() + " " + settings.getPoolFrom() + "-" + settings.getPoolTo()
			+ " " + settings.getTurns() + " turns " + settings.getMeasuresPerLine() + "/" + settings.getLinesPerPage()
			+ " " + settings.isNoConsecutiveRepeats() + " " + settings.isCompleteCycle());
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
		try {
			TGPracticeSession.start(settings, short1);
		} catch (TGPracticeRefusal refusal) {
			assertEquals("The pool reaches past the song's 1 page.", refusal.getMessage());
			return;
		}
		throw new AssertionError("accepted");
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

	// with w the heaviest unit's weight and L the weights added up: up to L turns need 2w <= L + 1, more 2w <= L
	@Test
	public void noConsecutiveRepeatsRefusesWeightsThatCannotKeepIt() {
		assertEquals("Measure 5 would come twice in a row.", set(TGPracticeUnitKind.MEASURE, 5, 5, 2).start());
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 5, 5, 1).start());
		flag(1, 3);
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 1, 3, 5).start());
		assertEquals("Measure 1 comes up 3 times in a cycle of 5 turns and would come twice in a row.", set(TGPracticeUnitKind.MEASURE, 1, 3, 6).start());
		assertEquals("accepted", set(TGPracticeUnitKind.MEASURE, 1, 4, 12).start());
		assertEquals("Measure 1 comes up 3 times in a cycle of 4 turns and would come twice in a row.", set(TGPracticeUnitKind.MEASURE, 1, 2, 2).start());
	}

	@Test
	public void aLineThatCannotKeepTheRuleIsNamed() {
		flag(6, 4).flag(7, 4);
		assertEquals("Line 2 comes up 4 times in a cycle of 5 turns and would come twice in a row.", set(TGPracticeUnitKind.LINE, 1, 2, 2).start());
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
		assertEquals("3 2 1", TGPracticeGrouping.of(4, 4).unit(TGPracticeUnitKind.LINE, 3, 64).weight(song)
			+ " " + TGPracticeGrouping.of(1, 1).unit(TGPracticeUnitKind.LINE, 12, 64).weight(song)
			+ " " + TGPracticeGrouping.of(1, 1).unit(TGPracticeUnitKind.LINE, 13, 64).weight(song));
	}

	// a session reads the flags when asked: a flag set after it started counts
	@Test
	public void aSessionReadsTheFlagsAsTheyAre() throws TGPracticeRefusal {
		set(TGPracticeUnitKind.MEASURE, 1, 3, 1);
		TGPracticeSession session = TGPracticeSession.start(settings, song);
		flag(2, 4);
		assertEquals("[1, 4, 1] [1, 4, 5]", Arrays.toString(session.weights()) + " " + Arrays.toString(session.weights(3, 5)));
	}
}
