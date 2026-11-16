package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * Interleaved click-up's steps, worked out once when it starts. The tempo ladder runs from its start to its goal by its
 * increment, its last rung the goal itself. Sections come into play one at a time; with N in play step i (from 0) plays
 * at rung i the set P_N[i mod length]: P_1 = 1; P_2 = 1-2, 2; P_N = 1-N, N, (N-1)-N, N, ..., 2-N, N. When the rungs run
 * out before the steps with N in play end on 1-N, they go on at the goal until they do (the ladder's tail). A step reads
 * back as its sections and its rung, "1-2@60%", one section alone as "2@65%". The song has 64 measures of 4/4 at 120 bpm.
 */
public class TestClickUpLadder {

	private final TGSong song = PracticeSongs.song(64);
	private final TGClickUpSettings settings = new TGClickUpSettings();

	private TestClickUpLadder passage(TGPracticeUnitKind kind, int from, int to) {
		settings.setUnit(kind);
		settings.setPassageFrom(from);
		settings.setPassageTo(to);
		return this;
	}

	private TestClickUpLadder ladder(TGClickUpTempoMode mode, int start, int goal, int increment) {
		settings.setTempoMode(mode);
		settings.setStart(start);
		settings.setGoal(goal);
		settings.setIncrement(increment);
		return this;
	}

	private String steps(Function<TGClickUpStep, String> reading) throws TGPracticeRefusal {
		return TGClickUpLadder.of(settings, song).getSteps().stream().map(reading).collect(Collectors.joining(", "));
	}

	// "3 sections, 60 to 80 by 5: <the steps>", the sections measures 1 to 3, the ladder in percent
	private String ladder(int sections, int start, int goal, int increment) throws TGPracticeRefusal {
		passage(TGPracticeUnitKind.MEASURE, 1, sections).ladder(TGClickUpTempoMode.PERCENT, start, goal, increment);
		return sections + " sections, " + start + " to " + goal + " by " + increment + ": " + steps(String::valueOf);
	}

	private String start() {
		return PracticeRefusals.outcome(() -> TGClickUpLadder.of(settings, song));
	}

	@Test
	public void theSettingsStartAsTheSpecSays() {
		assertEquals("MEASURE 1-1 4/4 PERCENT 60-100 by 5, 1 passes, Count In each step true every pass false", settings.getUnit() + " " + settings.getPassageFrom()
			+ "-" + settings.getPassageTo() + " " + settings.getMeasuresPerLine() + "/" + settings.getLinesPerPage() + " " + settings.getTempoMode() + " "
			+ settings.getStart() + "-" + settings.getGoal() + " by " + settings.getIncrement() + ", " + settings.getPasses() + " passes, Count In each step "
			+ settings.isCountInEachStep() + " every pass " + settings.isCountInEveryPass());
	}

	// one section climbs the ladder alone; a second plays with the first, then alone, in turn; a third alone every other
	// step, between runs reaching back one section further (the owner's example); with 3 in play over 7 rungs the runs
	// reach back to 1-3 again, and the ladder ends on 2-3, so its tail plays 3 and 1-3 at the goal; with 4 over 3 rungs
	// the tail plays 4, 2-4, 4 and 1-4. The last rung is the goal where the increment would pass it (72); an increment
	// past the goal leaves the start and the goal; a goal that is the start leaves one rung
	@Test
	public void theStepsAddTheSectionsOneAtATimeAndEndWithAllOfThemAtTheGoal() throws TGPracticeRefusal {
		assertEquals(String.join("\n",
			"1 sections, 60 to 80 by 5: 1@60%, 1@65%, 1@70%, 1@75%, 1@80%",
			"2 sections, 60 to 80 by 5: 1@60%, 1@65%, 1@70%, 1@75%, 1@80%, 1-2@60%, 2@65%, 1-2@70%, 2@75%, 1-2@80%",
			"3 sections, 60 to 80 by 5: 1@60%, 1@65%, 1@70%, 1@75%, 1@80%, 1-2@60%, 2@65%, 1-2@70%, 2@75%, 1-2@80%, 1-3@60%, 3@65%, 2-3@70%, 3@75%, 1-3@80%",
			"3 sections, 60 to 90 by 5: 1@60%, 1@65%, 1@70%, 1@75%, 1@80%, 1@85%, 1@90%, 1-2@60%, 2@65%, 1-2@70%, 2@75%, 1-2@80%, 2@85%, 1-2@90%, "
				+ "1-3@60%, 3@65%, 2-3@70%, 3@75%, 1-3@80%, 3@85%, 2-3@90%, 3@90%, 1-3@90%",
			"4 sections, 60 to 70 by 5: 1@60%, 1@65%, 1@70%, 1-2@60%, 2@65%, 1-2@70%, 1-3@60%, 3@65%, 2-3@70%, 3@70%, 1-3@70%, "
				+ "1-4@60%, 4@65%, 3-4@70%, 4@70%, 2-4@70%, 4@70%, 1-4@70%",
			"2 sections, 60 to 75 by 5: 1@60%, 1@65%, 1@70%, 1@75%, 1-2@60%, 2@65%, 1-2@70%, 2@75%, 1-2@75%",
			"2 sections, 60 to 72 by 5: 1@60%, 1@65%, 1@70%, 1@72%, 1-2@60%, 2@65%, 1-2@70%, 2@72%, 1-2@72%",
			"2 sections, 60 to 100 by 50: 1@60%, 1@100%, 1-2@60%, 2@100%, 1-2@100%",
			"3 sections, 80 to 80 by 5: 1@80%, 1-2@80%, 1-3@80%"),
			String.join("\n", ladder(1, 60, 80, 5), ladder(2, 60, 80, 5), ladder(3, 60, 80, 5), ladder(3, 60, 90, 5), ladder(4, 60, 70, 5),
				ladder(2, 60, 75, 5), ladder(2, 60, 72, 5), ladder(2, 60, 100, 50), ladder(3, 80, 80, 5)));
	}

	private String scales() throws TGPracticeRefusal {
		return steps(step -> step + " " + step.getScale());
	}

	private void writtenTempo(int fromMeasure, int bpm) {
		for (int measure = fromMeasure; measure <= 64; measure++) {
			song.getMeasureHeader(measure - 1).getTempo().setQuarterValue(bpm);
		}
	}

	// a step in percent plays the song at that fraction of its written tempo; in bpm at its bpm over the written tempo of
	// the passage's first measure: all of the song at 90 bpm, 45 is half of it; measure 1 at 120 and the rest at 60, a
	// passage from measure 1 is read against 120
	@Test
	public void aStepPlaysAtItsRungOverTheWrittenTempoOfThePassagesFirstMeasure() throws TGPracticeRefusal {
		passage(TGPracticeUnitKind.MEASURE, 1, 1).ladder(TGClickUpTempoMode.PERCENT, 50, 65, 15);
		String percent = scales();
		writtenTempo(1, 90);
		passage(TGPracticeUnitKind.MEASURE, 9, 9).ladder(TGClickUpTempoMode.BPM, 45, 90, 30);
		String bpm = scales();
		writtenTempo(1, 120);
		writtenTempo(2, 60);
		passage(TGPracticeUnitKind.MEASURE, 1, 2).ladder(TGClickUpTempoMode.BPM, 60, 120, 60);
		assertEquals("1@50% 1/2, 1@65% 13/20 | 1@45 bpm 1/2, 1@75 bpm 5/6, 1@90 bpm 1/1 | 1@60 bpm 1/2, 1@120 bpm 1/1, 1-2@60 bpm 1/2, 2@120 bpm 1/1, 1-2@120 bpm 1/1",
			percent + " | " + bpm + " | " + scales());
	}

	private String tempo(TGClickUpTempoMode mode, int writtenBpm) {
		settings.setTempo(mode, writtenBpm);
		return settings.getTempoMode() + " " + settings.getStart() + "-" + settings.getGoal() + " by " + settings.getIncrement();
	}

	// the ladder knows the written tempo it is read against: the passage's first measure's (84 from measure 9 on, 120
	// before); the ladder given anew in a unit starts at 60% of the written tempo and climbs to it by 5, in that unit:
	// 60% to 100%, at 84 bpm 50 bpm (50.4 rounded) to 84 bpm, at 1 bpm 1 to 1
	@Test
	public void aLadderGivenAnewInAUnitClimbsFromSixtyPercentOfTheWrittenTempoToItByFive() throws TGPracticeRefusal {
		writtenTempo(9, 84);
		passage(TGPracticeUnitKind.MEASURE, 9, 10).ladder(TGClickUpTempoMode.BPM, 30, 40, 2);
		int written = TGClickUpLadder.of(settings, song).getWrittenBpm();
		assertEquals("84 | PERCENT 60-100 by 5 | BPM 50-84 by 5 | BPM 72-120 by 5 | BPM 1-1 by 5", written + " | " + tempo(TGClickUpTempoMode.PERCENT, written) + " | "
			+ tempo(TGClickUpTempoMode.BPM, written) + " | " + tempo(TGClickUpTempoMode.BPM, 120) + " | " + tempo(TGClickUpTempoMode.BPM, 1));
	}

	@Test
	public void aStepKnowsItsSectionsInPlayItsSectionsAndItsRung() throws TGPracticeRefusal {
		passage(TGPracticeUnitKind.MEASURE, 1, 3).ladder(TGClickUpTempoMode.BPM, 60, 80, 20);
		assertEquals("1 1-1 60 BPM, 1 1-1 80 BPM, 2 1-2 60 BPM, 2 2-2 80 BPM, 2 1-2 80 BPM, 3 1-3 60 BPM, 3 3-3 80 BPM, 3 2-3 80 BPM, 3 3-3 80 BPM, 3 1-3 80 BPM",
			steps(step -> step.getSectionsInPlay() + " " + step.getFirstSection() + "-" + step.getLastSection() + " " + step.getRung() + " " + step.getTempoMode()));
	}

	private String measures(TGPracticeUnitKind kind, int from, int to) throws TGPracticeRefusal {
		return passage(kind, from, to).ladder(TGClickUpTempoMode.PERCENT, 50, 100, 50).steps(step -> step.getFirstMeasure() + "-" + step.getLastMeasure());
	}

	// lines of 3 and pages of 2 lines; then lines of 5, whose line 13 holds what is left, measures 61 to 64
	@Test
	public void aStepPlaysTheMeasuresOfItsSections() throws TGPracticeRefusal {
		settings.setMeasuresPerLine(3);
		settings.setLinesPerPage(2);
		String measures = measures(TGPracticeUnitKind.MEASURE, 9, 10) + " | " + measures(TGPracticeUnitKind.LINE, 2, 3) + " | " + measures(TGPracticeUnitKind.PAGE, 1, 2);
		settings.setMeasuresPerLine(5);
		assertEquals("9-9, 9-9, 9-10, 10-10, 9-10 | 4-6, 4-6, 4-9, 7-9, 4-9 | 1-6, 1-6, 1-12, 7-12, 1-12 | 56-60, 56-60, 56-64, 61-64, 56-64",
			measures + " | " + measures(TGPracticeUnitKind.LINE, 12, 13));
	}

	// "passes, Count In before each step, before every pass"
	private String countIns() throws TGPracticeRefusal {
		TGClickUpLadder ladder = TGClickUpLadder.of(settings, song);
		return ladder.getPasses() + " " + ladder.isCountInEachStep() + " " + ladder.isCountInEveryPass();
	}

	// before every pass only with a Count In before each step: without one there is none to put before every pass
	@Test
	public void theLadderCarriesThePassesAndTheCountIns() throws TGPracticeRefusal {
		settings.setPasses(3);
		settings.setCountInEveryPass(true);
		String eachStep = countIns();
		settings.setCountInEachStep(false);
		assertEquals("3 true true | 3 false false", eachStep + " | " + countIns());
	}

	@Test
	public void theStepsCannotBeChanged() throws TGPracticeRefusal {
		List<TGClickUpStep> steps = TGClickUpLadder.of(settings, song).getSteps();
		assertThrows(UnsupportedOperationException.class, () -> steps.remove(0));
	}

	@Test
	public void aPassageMustLieInTheSong() {
		assertEquals("The passage reaches past the song's 64 measures.", passage(TGPracticeUnitKind.MEASURE, 60, 70).start());
		assertEquals("The passage reaches past the song's 4 pages.", passage(TGPracticeUnitKind.PAGE, 1, 5).start());
		assertEquals("The passage is empty.", passage(TGPracticeUnitKind.LINE, 9, 8).start());
		assertEquals("The passage starts before line 1.", passage(TGPracticeUnitKind.LINE, 0, 2).start());
		assertEquals("accepted", passage(TGPracticeUnitKind.PAGE, 1, 4).start());
	}

	// a goal that is the start is one rung, and accepted
	@Test
	public void aLadderStartsAtOneOrMoreAndClimbsByOneOrMoreToAGoalNotBelowItsStart() {
		assertEquals(String.join("\n", "The ladder starts below 1%.", "The ladder starts below 1 bpm.", "accepted", "The ladder's goal is below its start.",
			"accepted", "The ladder climbs by less than 1%.", "The ladder climbs by less than 1 bpm.", "accepted"),
			String.join("\n", ladder(TGClickUpTempoMode.PERCENT, 0, 100, 5).start(), ladder(TGClickUpTempoMode.BPM, 0, 120, 5).start(),
				ladder(TGClickUpTempoMode.PERCENT, 1, 100, 5).start(), ladder(TGClickUpTempoMode.PERCENT, 80, 79, 5).start(),
				ladder(TGClickUpTempoMode.PERCENT, 80, 80, 5).start(), ladder(TGClickUpTempoMode.PERCENT, 60, 100, 0).start(),
				ladder(TGClickUpTempoMode.BPM, 60, 120, 0).start(), ladder(TGClickUpTempoMode.PERCENT, 60, 100, 1).start()));
	}

	@Test
	public void aStepNeedsAPassAndGroupsTheirParts() {
		settings.setPasses(0);
		String passes = start();
		settings.setPasses(1);
		settings.setMeasuresPerLine(0);
		String lines = passage(TGPracticeUnitKind.LINE, 1, 2).start();
		settings.setMeasuresPerLine(4);
		settings.setLinesPerPage(0);
		assertEquals("A step needs at least 1 pass. A line needs at least 1 measure. A page needs at least 1 line.",
			passes + " " + lines + " " + passage(TGPracticeUnitKind.PAGE, 1, 2).start());
	}
}
