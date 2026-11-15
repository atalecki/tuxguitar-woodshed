package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import app.tuxguitar.practice.TGClickUpStep;
import app.tuxguitar.practice.TGPracticeGrouping;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.practice.TGPracticeUnitKind;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that set and start interleaved practice and flag measures as difficult (README, Practice). Starting checks the
 * settings and makes the session; it plays nothing yet.
 */
public class PracticeSteps {

	private final AudioWorld world;
	private final Practice practice;
	private final ClickUp clickUp;

	public PracticeSteps(AudioWorld world, Practice practice, ClickUp clickUp) {
		this.world = world;
		this.practice = practice;
		this.clickUp = clickUp;
	}

	@ParameterType("measure|line|page")
	public TGPracticeUnitKind unit(String name) {
		return TGPracticeUnitKind.valueOf(name.toUpperCase(Locale.ROOT));
	}

	// "lines 1 to 4": the unit numbers from and to; the kind is the unit's
	@ParameterType("(?:measures|lines|pages) (\\d+) to (\\d+)")
	public int[] pool(String from, String to) {
		return new int[] {Integer.parseInt(from), Integer.parseInt(to)};
	}

	@Given("I have set interleaved practice to {unit}s over {pool}")
	@When("I set interleaved practice to {unit}s over {pool}")
	public void setTo(TGPracticeUnitKind unit, int[] pool) {
		practice.settings().setUnit(unit);
		practice.settings().setPool(pool[0], pool[1]);
	}

	@Given("I have set interleaved practice to {unit}s over {pool}, with lines of {int} measures")
	public void setToWithLines(TGPracticeUnitKind unit, int[] pool, int measures) {
		setTo(unit, pool);
		practice.settings().setMeasuresPerLine(measures);
	}

	@Given("I have set interleaved practice to {unit}s over {pool}, with lines of {int} measures and pages of {int} lines")
	@When("I set interleaved practice to {unit}s over {pool}, with lines of {int} measures and pages of {int} lines")
	public void setToWithLinesAndPages(TGPracticeUnitKind unit, int[] pool, int measures, int lines) {
		setToWithLines(unit, pool, measures);
		practice.settings().setLinesPerPage(lines);
	}

	@Given("I have turned interleaved practice's {string} {word}")
	public void turnTheRule(String rule, String onOrOff) {
		boolean on = "on".equals(onOrOff);
		if ("No consecutive repeats".equals(rule)) {
			practice.settings().setNoConsecutiveRepeats(on);
		} else {
			assertEquals("Complete cycle before reshuffle", rule);
			practice.settings().setCompleteCycle(on);
		}
	}

	@Then("interleaved practice's {string} is on")
	public void theRuleIsOn(String rule) {
		assertTrue("No consecutive repeats".equals(rule) ? practice.settings().isNoConsecutiveRepeats() : practice.settings().isCompleteCycle(), rule);
	}

	@When("I start interleaved practice of {unit}s over {pool} for {int} turns, with lines of {int} measures and pages of {int} lines")
	public void start(TGPracticeUnitKind unit, int[] pool, int turns, int measures, int lines) {
		setToWithLinesAndPages(unit, pool, measures, lines);
		practice.settings().setTurns(turns);
		practice.start();
	}

	// interleaved practice's pool, or interleaved click-up's last step, holds every unit from and to
	@Then("starting is accepted")
	public void startingIsAccepted() {
		assertNull(world.refusal());
		if (clickUp.isStarted()) {
			List<TGClickUpStep> steps = clickUp.ladder().getSteps();
			assertEquals(clickUp.settings().getPassageTo() - clickUp.settings().getPassageFrom() + 1, steps.get(steps.size() - 1).getLastSection());
		} else {
			assertEquals(practice.settings().getPoolTo() - practice.settings().getPoolFrom() + 1, practice.session().getPool().size());
		}
	}

	private TGPracticeUnit unitOfTheSong(TGPracticeUnitKind kind, int number) throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(practice.settings().getMeasuresPerLine(), practice.settings().getLinesPerPage());
		return grouping.unit(kind, number, world.song().countMeasureHeaders());
	}

	@Then("interleaved practice's {unit} {int} is measures {int} to {int}")
	public void theUnitIsMeasures(TGPracticeUnitKind kind, int number, int first, int last) throws TGPracticeRefusal {
		TGPracticeUnit unit = unitOfTheSong(kind, number);
		assertEquals(first + " to " + last, unit.getFirst() + " to " + unit.getLast());
	}

	@Then("interleaved practice's {unit} {int} weighs {int}x")
	public void theUnitWeighs(TGPracticeUnitKind kind, int number, int weight) throws TGPracticeRefusal {
		assertEquals(weight, unitOfTheSong(kind, number).weight(world.song()));
	}

	@When("I flag measure {int} as {int}x difficult")
	public void flag(int measure, int difficulty) {
		practice.flag(measure, difficulty);
	}

	@Given("I have flagged measure {int} as {int}x difficult")
	public void haveFlagged(int measure, int difficulty) {
		flag(measure, difficulty);
		assertTrue(practice.flagged());
		measureIsFlagged(measure, difficulty);
	}

	@Then("measure {int} is flagged {int}x difficult")
	public void measureIsFlagged(int measure, int difficulty) {
		assertEquals(difficulty, practice.header(measure).getDifficulty());
	}

	@Then("once I have opened it again, measure {int} is flagged {int}x difficult, measure {int} {int}x and measure {int} {int}x")
	public void onceOpenedAgainTheMeasuresAreFlagged(int first, int firstFlag, int second, int secondFlag, int third, int thirdFlag) {
		world.open(world.saved());
		assertEquals(firstFlag + " " + secondFlag + " " + thirdFlag, practice.header(first).getDifficulty() + " "
			+ practice.header(second).getDifficulty() + " " + practice.header(third).getDifficulty());
	}
}
