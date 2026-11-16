package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.practice.TGClickUpSettings;
import app.tuxguitar.practice.TGClickUpStep;
import app.tuxguitar.practice.TGPracticeBlock;
import app.tuxguitar.practice.TGPracticeGrouping;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSettings;
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

	private static final String NO_REPEATS = "No consecutive repeats";
	private static final String CYCLE = "Complete cycle before reshuffle";
	private static final String EACH_TURN = "Count In before each turn";
	// interleaved practice's rules and options, by name, as the settings set and read them
	private static final Map<String, BiConsumer<TGPracticeSettings, Boolean>> SET = Map.of(NO_REPEATS, TGPracticeSettings::setNoConsecutiveRepeats, CYCLE,
		TGPracticeSettings::setCompleteCycle, EACH_TURN, TGPracticeSettings::setCountInEachTurn);
	private static final Map<String, Predicate<TGPracticeSettings>> IS = Map.of(NO_REPEATS, TGPracticeSettings::isNoConsecutiveRepeats, CYCLE,
		TGPracticeSettings::isCompleteCycle, EACH_TURN, TGPracticeSettings::isCountInEachTurn);

	private final AudioWorld world;
	private final Practice practice;
	private final ClickUp clickUp;

	public PracticeSteps(AudioWorld world, Practice practice, ClickUp clickUp) {
		this.world = world;
		this.practice = practice;
		this.clickUp = clickUp;
	}

	@ParameterType("measure|line|page|beat")
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

	// interleaved practice or interleaved click-up: whether it is the click-up
	@ParameterType("practice|click-up")
	public boolean mode(String name) {
		return "click-up".equals(name);
	}

	// an option of interleaved practice's or click-up's, by name, as its settings set it
	@Given("I have turned interleaved {mode}'s {string} {word}")
	public void turnOption(boolean ofClickUp, String option, String onOrOff) {
		boolean on = "on".equals(onOrOff);
		if (ofClickUp) {
			ClickUpSteps.SET.get(option).accept(clickUp.settings(), on);
		} else {
			SET.get(option).accept(practice.settings(), on);
		}
	}

	@Then("interleaved {mode}'s {string} is {word}")
	public void optionIs(boolean ofClickUp, String option, String onOrOff) {
		boolean on = (ofClickUp ? ClickUpSteps.IS.get(option).test(clickUp.settings()) : IS.get(option).test(practice.settings()));
		assertEquals(option + " " + onOrOff, option + (on ? " on" : " off"));
	}

	@When("I start interleaved practice of {unit}s over {pool} for {int} turn(s)")
	public void start(TGPracticeUnitKind unit, int[] pool, int turns) {
		setTo(unit, pool);
		practice.settings().setTurns(turns);
		practice.start();
	}

	@When("I start interleaved practice of {unit}s over {pool} for {int} turn(s), with lines of {int} measures and pages of {int} lines")
	public void start(TGPracticeUnitKind unit, int[] pool, int turns, int measures, int lines) {
		practice.settings().setMeasuresPerLine(measures);
		practice.settings().setLinesPerPage(lines);
		start(unit, pool, turns);
	}

	// interleaved practice's pool holds every block of the units from and to (as many as those units less the units per
	// block but one), or interleaved click-up's last step every unit from and to
	@Then("starting is accepted")
	public void startingIsAccepted() {
		assertNull(world.refusal());
		if (clickUp.isStarted()) {
			TGClickUpSettings settings = clickUp.settings();
			List<TGClickUpStep> steps = clickUp.ladder().getSteps();
			assertEquals(unitsIn(settings.getUnit(), settings.getPassageFrom(), settings.getPassageTo()), steps.get(steps.size() - 1).getLastSection());
		} else {
			TGPracticeSettings settings = practice.settings();
			assertEquals(unitsIn(settings.getUnit(), settings.getPoolFrom(), settings.getPoolTo()) - settings.getUnitsPerBlock() + 1, practice.session().getPool().size());
		}
	}

	// how many units of the kind a range from and to holds: a range of beats is one of measures, and holds their beats
	private int unitsIn(TGPracticeUnitKind kind, int from, int to) {
		if (kind != TGPracticeUnitKind.BEAT) {
			return to - from + 1;
		}
		return IntStream.rangeClosed(from, to).map(measure -> practice.header(measure).getTimeSignature().getNumerator()).sum();
	}

	// its blocks as they read (README, Block reading): a block of one beat as the beat
	@Then("interleaved practice's pool is the {int} beats {string}")
	@Then("interleaved practice's pool is the {int} blocks {string}")
	public void thePoolIsTheBlocks(int count, String blocks) {
		List<TGPracticeBlock> pool = practice.session().getPool();
		assertEquals(count + " " + blocks, pool.size() + " " + Practice.blocks(pool));
	}

	@Given("I have set interleaved practice's units per block to {int}")
	public void setUnitsPerBlock(int units) {
		practice.settings().setUnitsPerBlock(units);
	}

	@Then("interleaved practice's units per block is {int}")
	public void unitsPerBlockIs(int units) {
		assertEquals(units, practice.settings().getUnitsPerBlock());
	}

	// "1x, 1x, 3x": each block of the pool's weight, in order
	@Then("interleaved practice's beats weigh {string}")
	@Then("interleaved practice's blocks weigh {string}")
	public void theBeatsWeigh(String weights) {
		assertEquals(weights, IntStream.of(practice.session().weights()).mapToObj(weight -> weight + "x").collect(Collectors.joining(", ")));
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

	// the weight of the block of the session started as set (one unit per block) that starts at the unit
	@Then("interleaved practice's {unit} {int} weighs {int}x")
	public void theUnitWeighs(TGPracticeUnitKind kind, int number, int weight) {
		practice.start();
		List<TGPracticeBlock> pool = practice.session().getPool();
		int block = IntStream.range(0, pool.size()).filter(index -> pool.get(index).getFrom().getNumber() == number).findFirst().getAsInt();
		assertEquals(kind + " " + number + " " + weight, pool.get(block).getFrom().getKind() + " " + number + " " + practice.session().weights()[block]);
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
