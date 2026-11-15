package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.practice.TGPracticeUnitKind;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps on how interleaved practice chooses its turns, without playing them: the session started as set, then its
 * turns chosen for each random seed of a range. A turn reads back as the number of its unit; turns count from 1.
 */
public class ChoosingSteps {

	private static final Pattern RANGE = Pattern.compile("turns (\\d+) to (\\d+)");
	private static final Pattern CHOICE = Pattern.compile("measures? ((?:\\d+(?:, | and )?)+) (once|three times)");

	private final AudioWorld world;
	private final Practice practice;

	public ChoosingSteps(AudioWorld world, Practice practice) {
		this.world = world;
		this.practice = practice;
	}

	// "turns 1 to 4, turns 5 to 8 and turns 9 to 12": each range's first and last turn
	@ParameterType("turns \\d+ to \\d+(?:(?:, | and )turns \\d+ to \\d+)*")
	public List<int[]> turns(String text) {
		List<int[]> ranges = new ArrayList<int[]>();
		Matcher range = RANGE.matcher(text);
		while (range.find()) {
			ranges.add(new int[] {Integer.parseInt(range.group(1)), Integer.parseInt(range.group(2))});
		}
		return ranges;
	}

	// "measure 2 three times and measures 1, 3 and 4 once each": each measure as many times as it is chosen
	@ParameterType("measures? \\d+(?:(?:, | and )\\d+)* (?:once|three times)(?: each)?(?: and measures? \\d+(?:(?:, | and )\\d+)* (?:once|three times)(?: each)?)*")
	public List<Integer> choices(String text) {
		List<Integer> measures = new ArrayList<Integer>();
		Matcher choice = CHOICE.matcher(text);
		while (choice.find()) {
			for (String number : choice.group(1).split(", | and ")) {
				measures.addAll(Collections.nCopies(choice.group(2).equals("once") ? 1 : 3, Integer.valueOf(number)));
			}
		}
		return measures;
	}

	private List<Integer> choose(TGPracticeChooser chooser, int turns) {
		return IntStream.range(0, turns).mapToObj(turn -> chooser.next().getNumber()).collect(Collectors.toCollection(ArrayList::new));
	}

	@When("interleaved practice chooses {int} turn(s), with every random seed from {int} to {int}")
	public void chooseWithSeeds(int turns, int first, int last) {
		practice.settings().setTurns(turns);
		practice.start();
		for (int seed = first; seed <= last; seed++) {
			practice.runs().add(choose(practice.session().chooser(seed), turns));
		}
	}

	@When("interleaved practice chooses {int} turns, with random seed {int}")
	public void chooseWithSeed(int turns, int seed) {
		chooseWithSeeds(turns, seed, seed);
	}

	// the flag, set as the app sets it once the session does not refuse it, after the turn is chosen
	@When("interleaved practice chooses {int} turns with random seed {int}, while I flag measure {int} as {int}x difficult during turn {int}")
	public void chooseWhileFlagging(int turns, int seed, int measure, int difficulty, int during) {
		practice.settings().setTurns(turns);
		practice.start();
		TGPracticeChooser chooser = practice.session().chooser(seed);
		List<Integer> chosen = choose(chooser, during);
		try {
			chooser.checkFlag(measure, difficulty);
			practice.flag(measure, difficulty);
			assertTrue(practice.flagged());
		} catch (TGPracticeRefusal refusal) {
			world.setRefusal(refusal.getMessage());
		}
		chosen.addAll(choose(chooser, turns - during));
		practice.runs().add(chosen);
	}

	// each run's turns of the range, as unit numbers in order (the range from 1)
	private static List<Integer> sorted(List<Integer> run, int[] range) {
		List<Integer> part = new ArrayList<Integer>(run.subList(range[0] - 1, range[1]));
		Collections.sort(part);
		return part;
	}

	private void eachRangeChooses(List<int[]> ranges, List<Integer> expected) {
		Collections.sort(expected);
		for (List<Integer> run : practice.runs()) {
			for (int[] range : ranges) {
				assertEquals(expected, sorted(run, range), run.toString());
			}
		}
	}

	private static List<Integer> numbers(int from, int to) {
		return IntStream.rangeClosed(from, to).boxed().collect(Collectors.toCollection(ArrayList::new));
	}

	@Then("{turns} (each )choose {unit}s {int} to {int} once each")
	public void eachChooseOnceEach(List<int[]> ranges, TGPracticeUnitKind unit, int from, int to) {
		eachRangeChooses(ranges, numbers(from, to));
	}

	@Then("{turns} each choose measures {int} and {int} three times each and every other measure of the pool once")
	public void eachChooseTwoThreeTimes(List<int[]> ranges, int first, int second) {
		List<Integer> expected = pool();
		Collections.addAll(expected, first, first, second, second);
		eachRangeChooses(ranges, expected);
	}

	@Then("turns {int} to {int} choose {choices}")
	public void turnsChoose(int from, int to, List<Integer> measures) {
		eachRangeChooses(Collections.singletonList(new int[] {from, to}), measures);
	}

	@Then("no two turns in a row choose the same {unit}")
	public void noTwoTurnsInARow(TGPracticeUnitKind unit) {
		for (List<Integer> run : practice.runs()) {
			assertTrue(IntStream.range(1, run.size()).noneMatch(turn -> run.get(turn).equals(run.get(turn - 1))), run.toString());
		}
	}

	@Then("over the seeds, turns {int} to {int} take each of the {int} orders of {unit}s {int} to {int}")
	public void takeEachOrder(int from, int to, int orders, TGPracticeUnitKind unit, int first, int last) {
		Set<List<Integer>> taken = new HashSet<List<Integer>>();
		for (List<Integer> run : practice.runs()) {
			taken.add(run.subList(from - 1, to));
		}
		assertEquals(orders, taken.size());
		taken.forEach(order -> assertEquals(numbers(first, last), sorted(order, new int[] {1, order.size()})));
	}

	@Then("with at least one of the seeds, turns {int} and {int} choose the same {unit}")
	public void atLeastOneSeedRepeats(int first, int second, TGPracticeUnitKind unit) {
		assertTrue(practice.runs().stream().anyMatch(run -> run.get(first - 1).equals(run.get(second - 1))));
	}

	// the first run's turns that chose the unit
	private int chosen(int number) {
		return (int) practice.runs().get(0).stream().filter(chosen -> chosen == number).count();
	}

	private static void assertNear(int expected, int actual, int percent, String what) {
		assertTrue(Math.abs(actual - expected) * 100 <= expected * percent, what + ": " + actual + ", not " + expected + " within " + percent + "%");
	}

	@Then("each of {unit}s {int} to {int} is chosen for {int} turns, within {int}%")
	public void eachIsChosen(TGPracticeUnitKind unit, int from, int to, int turns, int percent) {
		numbers(from, to).forEach(number -> assertNear(turns, chosen(number), percent, unit.title() + " " + number));
	}

	@Then("measures {int} and {int} are each chosen for {int} turns, within {int}%")
	public void bothAreChosen(int first, int second, int turns, int percent) {
		assertNear(turns, chosen(first), percent, "Measure " + first);
		assertNear(turns, chosen(second), percent, "Measure " + second);
	}

	// every measure of the pool not flagged
	@Then("every other measure of the pool is chosen for {int} turns, within {int}%")
	public void everyOtherIsChosen(int turns, int percent) {
		List<Integer> others = unflagged();
		assertTrue(others.size() > 1);
		others.forEach(number -> assertNear(turns, chosen(number), percent, "Measure " + number));
	}

	@Then("measure {int} is chosen with {int} of the seeds, within {int}%")
	public void isChosenFirst(int number, int seeds, int percent) {
		int chosenFirst = (int) practice.runs().stream().filter(run -> run.get(0) == number).count();
		assertNear(seeds, chosenFirst, percent, "Measure " + number);
	}

	// the numbers of the pool's units
	private List<Integer> pool() {
		return practice.session().getPool().stream().map(TGPracticeUnit::getNumber).collect(Collectors.toCollection(ArrayList::new));
	}

	private List<Integer> unflagged() {
		return practice.session().getPool().stream().filter(unit -> unit.weight(world.song()) == 1).map(TGPracticeUnit::getNumber).collect(Collectors.toList());
	}
}
