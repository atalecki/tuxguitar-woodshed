package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * The words the app shows for practice, as keys of its messages and their arguments. Here the messages read back as the
 * key and its arguments, "key(a, b)", so that each test reads what the app looks up. A turn of interleaved practice is
 * worded by its unit's kind and number ("Turn 3 of 12: Line 5"), a beat by its measure and its beat in it; a click-up
 * step by its sections and its tempo, a beat again by measure and beat. A field of a range of units is labelled as it is,
 * or, when the range is given in another kind (beats in measures), in that kind. The song has 16 measures of 4/4.
 */
public class TestPracticeWording {

	private static final TGPracticeWording.Messages MESSAGES = (key, arguments) -> key + "(" + String.join(", ", arguments) + ")";

	private final TGSong song = PracticeSongs.song(16);

	private TGPracticeUnit line(int number) {
		return new TGPracticeUnit(TGPracticeUnitKind.LINE, number, 4 * number - 3, 4 * number);
	}

	private TGPracticeUnit beat(int measure, int beat) {
		return TGPracticeUnit.beat(song.getMeasureHeader(measure - 1), beat);
	}

	private static TGPracticePlan plan(TGPracticeUnit unit) {
		return new TGPracticePlan(unit, unit, TGTempoScale.percent(100), 1, false, 0, false);
	}

	// a block of the units from and to
	private static TGPracticePlan plan(TGPracticeUnit from, TGPracticeUnit to) {
		return new TGPracticePlan(from, to, TGTempoScale.percent(100), 1, false, 0, false);
	}

	private static String turn(TGPracticeTurn turn) {
		return TGPracticeWording.turn(turn, MESSAGES);
	}

	// step 5 of 11 of from to, at that rung of the tempo mode
	private static String step(TGPracticeUnit from, TGPracticeUnit to, int rung, TGClickUpTempoMode mode) {
		TGClickUpStep step = new TGClickUpStep(from, to, 1, 2, rung, mode, TGTempoScale.percent(100));
		return turn(new TGPracticeTurn(5, 11, new TGPracticePlan(step, 1, false, false)));
	}

	// the sections of step 5 of 11 of from to at 70%: "line(2)", "beat(9, 2)", "sections.line(2, 3)"
	private static String sections(TGPracticeUnit from, TGPracticeUnit to) {
		String read = step(from, to, 70, TGClickUpTempoMode.PERCENT);
		return read.substring("transport.click-up.step(5, 11, transport.click-up.".length(), read.length() - ", transport.click-up.tempo.percent(70))".length());
	}

	@Test
	public void aCountedTurnNamesItsUnitsKindAndNumber() {
		assertEquals("transport.practice.turn(3, 12, transport.practice.turn.line(), 5)", turn(new TGPracticeTurn(3, 12, plan(line(5)))));
	}

	@Test
	public void aCountedTurnOfABeatNamesItsMeasureAndBeat() {
		assertEquals("transport.practice.beat-turn(3, 12, 2, 3)", turn(new TGPracticeTurn(3, 12, plan(beat(2, 3)))));
	}

	@Test
	public void aTimedTurnSaysItsMinutesAndSecondsLeft() {
		assertEquals("transport.practice.timed-turn(3, transport.practice.turn.line(), 5, 1:42)", turn(TGPracticeTurn.timed(3, plan(line(5)), 102)));
	}

	@Test
	public void aTimedTurnOfABeatNamesItsMeasureAndBeat() {
		assertEquals("transport.practice.beat-timed-turn(3, 2, 3, 10:05)", turn(TGPracticeTurn.timed(3, plan(beat(2, 3)), 605)));
	}

	@Test
	public void aBreakNamesTheNextTurnsUnit() {
		assertEquals("transport.practice.break(4, transport.practice.turn.line(), 7, 0:12)", turn(TGPracticeTurn.inBreak(4, plan(line(7)), 12)));
	}

	@Test
	public void aBreakBeforeABeatNamesItsMeasureAndBeat() {
		assertEquals("transport.practice.beat-break(4, 9, 1, 0:00)", turn(TGPracticeTurn.inBreak(4, plan(beat(9, 1)), 0)));
	}

	@Test
	public void aCountedTurnOfABlockNamesItsFirstAndLastUnitAsARun() {
		assertEquals("transport.practice.block-turn(3, 12, transport.click-up.sections.line(4, 5)) | "
			+ "transport.practice.block-turn(3, 12, transport.click-up.sections.beat(3, 2, 4, 1))", turn(new TGPracticeTurn(3, 12, plan(line(4), line(5)))) + " | "
				+ turn(new TGPracticeTurn(3, 12, plan(beat(3, 2), beat(4, 1)))));
	}

	@Test
	public void aTimedTurnOfABlockNamesItsRunAndTheTimeLeft() {
		assertEquals("transport.practice.block-timed-turn(3, transport.click-up.sections.line(4, 5), 1:42)", turn(TGPracticeTurn.timed(3, plan(line(4), line(5)), 102)));
	}

	@Test
	public void aBreakBeforeABlockNamesItsRun() {
		assertEquals("transport.practice.block-break(4, transport.click-up.sections.beat(3, 2, 4, 1), 0:12)", turn(TGPracticeTurn.inBreak(4, plan(beat(3, 2), beat(4, 1)), 12)));
	}

	@Test
	public void aStepNamesItsNumberItsSectionsAndItsTempoInItsMode() {
		assertEquals("transport.click-up.step(5, 11, transport.click-up.section.line(2), transport.click-up.tempo.bpm(84)) "
			+ "transport.click-up.step(5, 11, transport.click-up.section.line(2), transport.click-up.tempo.percent(70))",
			step(line(2), line(2), 84, TGClickUpTempoMode.BPM) + " " + step(line(2), line(2), 70, TGClickUpTempoMode.PERCENT));
	}

	// a run from a beat to the same beat of the next measure is a run, not one section
	@Test
	public void aStepNamesOneSectionOrARunsFirstAndLastABeatByMeasureAndBeat() {
		assertEquals("section.line(2) sections.line(2, 3) section.beat(9, 2) sections.beat(9, 1, 9, 3) sections.beat(9, 4, 10, 4)",
			sections(line(2), line(2)) + " " + sections(line(2), line(3)) + " " + sections(beat(9, 2), beat(9, 2)) + " " + sections(beat(9, 1), beat(9, 3)) + " "
				+ sections(beat(9, 4), beat(10, 4)));
	}

	@Test
	public void aRangeOfTheUnitsOwnKindIsLabelledAsItIs() {
		assertEquals("transport.practice.pool-from() transport.practice.pool-from()",
			TGPracticeWording.rangeField("transport.practice.pool-from", TGPracticeUnitKind.MEASURE, MESSAGES) + " "
				+ TGPracticeWording.rangeField("transport.practice.pool-from", TGPracticeUnitKind.LINE, MESSAGES));
	}

	@Test
	public void theUnitsPerBlockAreLabelledByTheUnitsKind() {
		assertEquals("transport.practice.units-per-block.measure() transport.practice.units-per-block.beat()",
			TGPracticeWording.blockField(TGPracticeUnitKind.MEASURE, MESSAGES) + " " + TGPracticeWording.blockField(TGPracticeUnitKind.BEAT, MESSAGES));
	}

	@Test
	public void aRangeOfBeatsIsLabelledInMeasures() {
		assertEquals("transport.practice.range.measure(transport.click-up.passage-to())",
			TGPracticeWording.rangeField("transport.click-up.passage-to", TGPracticeUnitKind.BEAT, MESSAGES));
	}
}
