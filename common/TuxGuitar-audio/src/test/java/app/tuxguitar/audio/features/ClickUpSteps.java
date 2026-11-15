package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;

import app.tuxguitar.practice.TGClickUpSettings;
import app.tuxguitar.practice.TGClickUpTempoMode;
import app.tuxguitar.practice.TGPracticeUnitKind;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that set and start interleaved click-up (README, Practice: Interleaved click-up) and read its steps; starting
 * checks the settings and works the steps out, it plays nothing. A rung of the tempo ladder is given in percent of the
 * written tempo ("60%") or in bpm ("60 bpm"); a ladder's start, goal and increment are given in the same unit.
 */
public class ClickUpSteps {

	private static final String EVERY_PASS = "Count In before every pass";

	private final ClickUp clickUp;

	public ClickUpSteps(ClickUp clickUp) {
		this.clickUp = clickUp;
	}

	/** A tempo on the ladder, as given: its value in its unit. */
	static final class Rung {
		final int value;
		final TGClickUpTempoMode mode;

		Rung(int value, TGClickUpTempoMode mode) {
			this.value = value;
			this.mode = mode;
		}
	}

	@ParameterType("(\\d+)(%| bpm)")
	public Rung rung(String value, String unit) {
		return new Rung(Integer.parseInt(value), "%".equals(unit) ? TGClickUpTempoMode.PERCENT : TGClickUpTempoMode.BPM);
	}

	@When("I set interleaved click-up to {unit}s over {pool}")
	public void setTo(TGPracticeUnitKind unit, int[] passage) {
		clickUp.settings().setUnit(unit);
		clickUp.settings().setPassageFrom(passage[0]);
		clickUp.settings().setPassageTo(passage[1]);
	}

	@Given("I have set interleaved click-up to {unit}s over {pool}, from {rung} to {rung} by {rung}")
	public void setToFrom(TGPracticeUnitKind unit, int[] passage, Rung start, Rung goal, Rung increment) {
		setTo(unit, passage);
		assertEquals(start.mode + " " + start.mode, goal.mode + " " + increment.mode, "the ladder's unit");
		clickUp.settings().setTempoMode(start.mode);
		clickUp.settings().setStart(start.value);
		clickUp.settings().setGoal(goal.value);
		clickUp.settings().setIncrement(increment.value);
	}

	@Given("I have set interleaved click-up to {unit}s over {pool}, from {rung} to {rung} by {rung}, with lines of {int} measures")
	public void setToFromWithLines(TGPracticeUnitKind unit, int[] passage, Rung start, Rung goal, Rung increment, int measures) {
		setToFrom(unit, passage, start, goal, increment);
		clickUp.settings().setMeasuresPerLine(measures);
	}

	@Given("I have set interleaved click-up to {unit}s over {pool}, from {rung} to {rung} by {rung}, with lines of {int} measures and pages of {int} lines")
	public void setToFromWithLinesAndPages(TGPracticeUnitKind unit, int[] passage, Rung start, Rung goal, Rung increment, int measures, int lines) {
		setToFromWithLines(unit, passage, start, goal, increment, measures);
		clickUp.settings().setLinesPerPage(lines);
	}

	@Given("I have set interleaved click-up to {unit}s over {pool}, from {rung} to {rung} by {rung}, with {int} passes a step")
	public void setToFromWithPasses(TGPracticeUnitKind unit, int[] passage, Rung start, Rung goal, Rung increment, int passes) {
		setToFrom(unit, passage, start, goal, increment);
		clickUp.settings().setPasses(passes);
	}

	@Given("I have turned interleaved click-up's {string} {word}")
	public void turnEveryPass(String option, String onOrOff) {
		assertEquals(EVERY_PASS, option);
		clickUp.settings().setCountInEveryPass("on".equals(onOrOff));
	}

	@When("I start interleaved click-up of {unit}s over {pool}, from {rung} to {rung} by {rung}")
	public void start(TGPracticeUnitKind unit, int[] passage, Rung start, Rung goal, Rung increment) {
		setToFrom(unit, passage, start, goal, increment);
		clickUp.start();
	}

	@When("I start interleaved click-up of {unit}s over {pool}, from {rung} to {rung} by {rung}, with {int} passes a step, with lines of {int} measures and pages of {int} lines")
	public void startWithPassesLinesAndPages(TGPracticeUnitKind unit, int[] passage, Rung start, Rung goal, Rung increment, int passes, int measures, int lines) {
		setToFromWithLinesAndPages(unit, passage, start, goal, increment, measures, lines);
		clickUp.settings().setPasses(passes);
		clickUp.start();
	}

	@Then("interleaved click-up's ladder runs from {int}% to {int}% by {int}%, with {int} pass(es) a step")
	public void theLadderRuns(int start, int goal, int increment, int passes) {
		TGClickUpSettings settings = clickUp.settings();
		assertEquals(TGClickUpTempoMode.PERCENT + " " + start + " " + goal + " " + increment + " " + passes,
			settings.getTempoMode() + " " + settings.getStart() + " " + settings.getGoal() + " " + settings.getIncrement() + " " + settings.getPasses());
	}

	@Then("interleaved click-up's {string} is off")
	public void everyPassIsOff(String option) {
		assertEquals(EVERY_PASS + " off", option + (clickUp.settings().isCountInEveryPass() ? " on" : " off"));
	}

	// "1@60%, 1@65%, 1-2@60%, ..."
	@Then("interleaved click-up's steps are {string}")
	public void theStepsAre(String steps) {
		assertEquals(steps, clickUp.ladder().getSteps().stream().map(String::valueOf).collect(Collectors.joining(", ")));
	}
}
