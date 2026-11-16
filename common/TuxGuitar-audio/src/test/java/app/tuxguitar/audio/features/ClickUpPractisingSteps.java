package app.tuxguitar.audio.features;

import static app.tuxguitar.audio.features.PractisingSteps.firstPass;
import static app.tuxguitar.audio.features.PractisingSteps.lastPass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.features.HeardNotes.Note;
import app.tuxguitar.practice.TGClickUpStep;
import app.tuxguitar.practice.TGTempoScale;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that practise interleaved click-up offline and measure its steps (README, Practice: Interleaved click-up). A
 * step is a practice turn: its passes are measured as PractisingSteps measures a turn (its notes on track 1, its Count
 * In the clicks before its first note), and it starts where its first pass does (TimedPractisingSteps). What a step
 * should play, its measures and its tempo, is the ladder's step of the same number (ClickUpSteps reads the ladder).
 */
public class ClickUpPractisingSteps {

	private final AudioWorld world;
	private final ClickUp clickUp;
	private final PractisingSteps practising;
	private final TimedPractisingSteps timed;
	private final ClickUpLiveSteps live;

	public ClickUpPractisingSteps(AudioWorld world, ClickUp clickUp, PractisingSteps practising, TimedPractisingSteps timed, ClickUpLiveSteps live) {
		this.world = world;
		this.clickUp = clickUp;
		this.practising = practising;
		this.timed = timed;
		this.live = live;
	}

	// "9" or "9 to 10": the first and last measure
	static int[] span(String text) {
		String[] ends = text.split(" to ");
		return new int[] {Integer.parseInt(ends[0]), Integer.parseInt(ends[ends.length - 1])};
	}

	// "9, 9, 9 to 10": the measures of each step
	@ParameterType("\\d+(?: to \\d+)?(?:, \\d+(?: to \\d+)?)*")
	public List<int[]> spans(String text) {
		return Arrays.stream(text.split(", ")).map(ClickUpPractisingSteps::span).collect(Collectors.toList());
	}

	@When("I practise interleaved click-up")
	public void practise() {
		clickUp.practise();
	}

	private List<TGClickUpStep> ladder() {
		return clickUp.ladder().getSteps();
	}

	// the step plays the measures so many times back to back, each pass each measure once and in order, each note on its
	// beat at the tempo scale
	private void assertStep(List<Integer> step, List<List<Note>> passes, int[] span, TGTempoScale scale, int times, String name) {
		timed.assertPasses(step, List.of(practising.notesOf(span[0], span[1])), times, name);
		step.forEach(pass -> PractisingSteps.assertOnTheBeat(passes.get(pass), scale, name + ", pass " + pass));
	}

	@Then("the output holds these steps, in order:")
	public void theseSteps(List<Map<String, String>> rows) {
		List<List<Integer>> steps = practising.practiceTurns();
		List<List<Note>> passes = practising.turns();
		assertEquals(rows.size(), steps.size(), "steps");
		for (int step = 0; step < rows.size(); step++) {
			Map<String, String> row = rows.get(step);
			String name = "step " + row.get("step");
			TGTempoScale scale = TGTempoScale.percent(Integer.parseInt(row.get("tempo").replace("%", "")));
			assertStep(steps.get(step), passes, span(row.get("measures")), scale, 1, name);
			long[] clicks = practising.countIn(passes, firstPass(steps.get(step)));
			assertTrue(clicks.length > 1, name + "'s Count In: " + Arrays.toString(clicks));
			practising.assertCountIn(clicks, clicks.length, Double.parseDouble(row.get("Count In clicks apart")), name);
		}
	}

	// each step as the ladder's step of its number: its measures, at its tempo scale
	private void assertTheLaddersSteps(int times) {
		List<List<Integer>> steps = practising.practiceTurns();
		List<List<Note>> passes = practising.turns();
		List<TGClickUpStep> ladder = ladder();
		assertEquals(ladder.size(), steps.size(), "steps");
		for (int step = 0; step < steps.size(); step++) {
			TGClickUpStep planned = ladder.get(step);
			assertStep(steps.get(step), passes, new int[] {planned.getFirstMeasure(), planned.getLastMeasure()}, planned.getScale(), times, "step " + (step + 1));
		}
	}

	// played live, as ClickUpLiveSteps hears it
	@Then("each step sounds the notes of its measures once each and in order, each within 1 sample of its beat at the step's tempo")
	public void eachStepSoundsItsMeasures() {
		if (world.live() != null) {
			live.assertEachBeatOnTime();
		} else {
			assertTheLaddersSteps(1);
		}
	}

	@Then("each step sounds the notes of its measures {int} times back to back, each measure once and in order every time, each within 1 sample of its beat at the step's tempo")
	public void eachStepSoundsItsMeasuresTimes(int times) {
		assertTheLaddersSteps(times);
	}

	@Then("the steps sound measures {spans}, in order, each measure once a step and within 1 sample of its beat at the step's tempo")
	public void theStepsSoundMeasures(List<int[]> spans) {
		List<List<Integer>> steps = practising.practiceTurns();
		List<List<Note>> passes = practising.turns();
		assertEquals(spans.size(), steps.size(), "steps");
		for (int step = 0; step < steps.size(); step++) {
			assertStep(steps.get(step), passes, spans.get(step), ladder().get(step).getScale(), 1, "step " + (step + 1));
		}
	}

	// the samples a beat of the step's first measure lasts at its tempo
	private double beat(TGClickUpStep step) {
		return TestInterface.SAMPLE_RATE * 60 / step.getScale().exactBpm(world.song().getMeasureHeader(step.getFirstMeasure() - 1).getTempo().getQuarterValue());
	}

	@Then("before each step the output holds {int} Count In clicks, the last one a beat at the step's tempo before its first measure, within 1 sample")
	public void beforeEachStepTheCountIn(int count) {
		List<List<Integer>> steps = practising.practiceTurns();
		List<List<Note>> passes = practising.turns();
		for (int step = 0; step < steps.size(); step++) {
			List<Note> first = passes.get(firstPass(steps.get(step)));
			long[] clicks = practising.countIn(passes, firstPass(steps.get(step)));
			double beat = beat(ladder().get(step));
			practising.assertCountIn(clicks, count, beat, "step " + (step + 1));
			assertEquals(beat, PractisingSteps.first(first).start - clicks[clicks.length - 1], 1, "step " + (step + 1) + "'s last Count In click");
		}
	}

	@Then("before step {int} the output holds {int} Count In clicks, {double} samples apart within 1 sample")
	public void beforeStepTheCountIn(int step, int count, double apart) {
		List<List<Note>> passes = practising.turns();
		practising.assertCountIn(practising.countIn(passes, firstPass(practising.practiceTurns().get(step - 1))), count, apart, "step " + step);
	}

	// each of the steps (from 1) starts where the step before ends: with its first Count In click, when it has one
	private void assertEachStartsAtTheEndOfTheStepBefore(int from, int to) {
		List<List<Integer>> steps = practising.practiceTurns();
		List<List<Note>> passes = practising.turns();
		assertTrue(to <= steps.size(), steps.size() + " steps");
		for (int step = from - 1; step < to; step++) {
			long end = TimedPractisingSteps.end(passes, lastPass(steps.get(step - 1)));
			assertEquals(end, timed.start(passes, firstPass(steps.get(step))), 1, "step " + (step + 1));
		}
	}

	@Then("the first Count In click of each step but the first sounds at the end of the step before, within 1 sample")
	public void theFirstClickAtTheEndOfTheStepBefore() {
		List<List<Integer>> steps = practising.practiceTurns();
		List<List<Note>> passes = practising.turns();
		steps.forEach(step -> assertTrue(practising.countIn(passes, firstPass(step)).length > 0, "a step with no Count In: " + step));
		assertEachStartsAtTheEndOfTheStepBefore(2, steps.size());
	}

	@Then("steps {int} to {int} start at the end of the step before, within 1 sample")
	public void stepsStartAtTheEndOfTheStepBefore(int from, int to) {
		assertEachStartsAtTheEndOfTheStepBefore(from, to);
	}

	// from where its first pass starts (its first Count In click, or its first note) to where its last pass ends
	private long length(List<List<Note>> passes, List<Integer> step) {
		return TimedPractisingSteps.end(passes, lastPass(step)) - timed.start(passes, firstPass(step));
	}

	@Then("step {int} lasts {int} samples and step {int} {int}, within 1 sample")
	public void stepsLast(int one, int samples, int other, int otherSamples) {
		List<List<Integer>> steps = practising.practiceTurns();
		List<List<Note>> passes = practising.turns();
		assertEquals(samples, length(passes, steps.get(one - 1)), 1, "step " + one);
		assertEquals(otherSamples, length(passes, steps.get(other - 1)), 1, "step " + other);
	}

	// the step (one pass) sounds the notes of the beats of the measure from one to the other, then the joint's, each on its
	// beat at the step's tempo
	@Then("step {int} sounds the notes of m{int}b{int} to m{int}b{int}, then the joint's note on m{int}b{int}, each within 1 sample of its beat at the step's tempo")
	public void stepSoundsItsBeatsThenTheJoint(int step, int measure, int from, int lastMeasure, int to, int jointMeasure, int joint) {
		List<Note> pass = practising.turns().get(firstPass(practising.practiceTurns().get(step - 1)));
		assertEquals(measure, lastMeasure, "a run of beats of one measure");
		List<String> expected = new ArrayList<String>();
		IntStream.rangeClosed(from, to).forEach(beat -> expected.add(heard(measure, beat)));
		expected.add(heard(jointMeasure, joint));
		assertEquals(expected, pass.stream().map(note -> note.note + "@" + note.beat.tick).collect(Collectors.toList()), "step " + step);
		PractisingSteps.assertOnTheBeat(pass, ladder().get(step - 1).getScale(), "step " + step);
	}

	// the Background's note of the beat, at its tick: "note@tick"
	private String heard(int measure, int beat) {
		return (PractisingSteps.BASE + measure) + "@" + SongPosition.parse("m" + measure + "b" + beat).tick(world.song());
	}

	@Then("step {int} lasts {int} samples, within 1 sample")
	public void stepLasts(int step, int samples) {
		assertEquals(samples, length(practising.turns(), practising.practiceTurns().get(step - 1)), 1, "step " + step);
	}

	@Then("step {int} sounds measure {int}'s beats {int} samples apart, within 1 sample")
	public void stepSoundsTheBeatsApart(int step, int measure, int apart) {
		List<List<Note>> passes = practising.turns();
		List<Note> beats = practising.practiceTurns().get(step - 1).stream().flatMap(pass -> passes.get(pass).stream())
			.filter(note -> note.note == PractisingSteps.BASE + measure).collect(Collectors.toList());
		assertTrue(beats.size() > 1, "measure " + measure + " in step " + step + ": " + beats);
		for (int beat = 1; beat < beats.size(); beat++) {
			assertEquals(apart, beats.get(beat).start - beats.get(beat - 1).start, 1, "measure " + measure + ": " + beats);
		}
	}

	@Then("during each step's Count In TuxGuitar reports beat 1 of the step's first measure")
	public void duringEachCountInTheFirstBeat() {
		List<List<Note>> passes = practising.turns();
		for (List<Integer> step : practising.practiceTurns()) {
			long[] clicks = practising.countIn(passes, firstPass(step));
			assertTrue(clicks.length > 0, "a step with no Count In: " + step);
			practising.assertReportsTheFirstTick(passes.get(firstPass(step)), clicks[0], "step " + step);
		}
	}
}
