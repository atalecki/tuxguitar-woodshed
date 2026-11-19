package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * How interleaved practice chooses its turns, for many random seeds: in cycles that hold each unit of the pool as many
 * times as its weight, or each turn on its own with chances in proportion to the weights; with "No consecutive
 * repeats", never the unit of the turn before. A flag set during a session counts from the next cycle on, and is
 * refused when the next cycles could not keep the rule. The pool is measures; a turn reads back as its measure.
 */
public class TestPracticeChooser {

	private TGSong song;
	private final TGPracticeSettings settings = new TGPracticeSettings();

	private void pool(int from, int to, int turns, int... flags) {
		song = PracticeSongs.song(8, flags);
		settings.setPool(from, to);
		settings.setTurns(turns);
	}

	private TGPracticeChooser chooser(int seed) throws TGPracticeRefusal {
		return TGPracticeSession.start(settings, song).chooser(seed);
	}

	private static List<Integer> choose(TGPracticeChooser chooser, int turns) {
		List<Integer> chosen = new ArrayList<Integer>();
		for (int turn = 0; turn < turns; turn++) {
			chosen.add(chooser.next().getFrom().getNumber());
		}
		return chosen;
	}

	// the turns chosen with every seed from 1 to the number given
	private List<List<Integer>> chooseWithSeeds(int seeds) throws TGPracticeRefusal {
		return withSeeds(seeds, chooser -> choose(chooser, settings.getTurns()));
	}

	private <T> List<T> withSeeds(int seeds, ChooserFunction<T> run) throws TGPracticeRefusal {
		List<T> results = new ArrayList<T>();
		for (int seed = 1; seed <= seeds; seed++) {
			results.add(run.apply(chooser(seed)));
		}
		return results;
	}

	private interface ChooserFunction<T> {
		T apply(TGPracticeChooser chooser) throws TGPracticeRefusal;
	}

	// the measures of turns from..to (from 1), sorted
	private static List<Integer> sorted(List<Integer> turns, int from, int to) {
		List<Integer> part = new ArrayList<Integer>(turns.subList(from - 1, to));
		part.sort(null);
		return part;
	}

	private static boolean repeats(List<Integer> turns) {
		for (int i = 1; i < turns.size(); i++) {
			if (turns.get(i).equals(turns.get(i - 1))) {
				return true;
			}
		}
		return false;
	}

	private static int count(List<Integer> turns, int measure) {
		return (int) turns.stream().filter(chosen -> chosen == measure).count();
	}

	private static void assertNear(int expected, int actual, int percent) {
		assertTrue(Math.abs(actual - expected) * 100 <= expected * percent, actual + " is not " + expected + " within " + percent + "%");
	}

	@Test
	public void everyCycleHoldsEachUnitAsOftenAsItsWeightNeverTwiceInARow() throws TGPracticeRefusal {
		pool(1, 4, 15, 2, 2);
		for (List<Integer> turns : chooseWithSeeds(300)) {
			for (int cycle = 0; cycle < 3; cycle++) {
				assertEquals(Arrays.asList(1, 2, 2, 3, 4), sorted(turns, cycle * 5 + 1, cycle * 5 + 5), turns.toString());
			}
			assertTrue(!repeats(turns), turns.toString());
		}
	}

	// a unit of half a cycle (2w = L) still never comes twice in a row, within or across cycles
	@Test
	public void theRuleHoldsAcrossCyclesEvenWhenItLeavesOneOrder() throws TGPracticeRefusal {
		pool(1, 3, 12, 1, 2);
		for (List<Integer> turns : chooseWithSeeds(300)) {
			assertTrue(!repeats(turns), turns.toString());
			assertEquals(Arrays.asList(1, 1, 2, 3), sorted(turns, 9, 12));
		}
	}

	@Test
	public void withoutTheRuleACycleMayRepeatAUnitInARow() throws TGPracticeRefusal {
		settings.setNoConsecutiveRepeats(false);
		pool(1, 2, 6, 1, 2);
		List<List<Integer>> runs = chooseWithSeeds(100);
		runs.forEach(turns -> assertEquals(Arrays.asList(1, 1, 2), sorted(turns, 4, 6)));
		assertTrue(runs.stream().anyMatch(TestPracticeChooser::repeats));
	}

	// the first turn of a cycle: in proportion to the weights
	@Test
	public void aCycleStartsWithAUnitInProportionToItsWeight() throws TGPracticeRefusal {
		pool(1, 3, 1, 1, 2);
		List<List<Integer>> runs = chooseWithSeeds(8000);
		assertNear(4000, (int) runs.stream().filter(turns -> turns.get(0) == 1).count(), 3);
		assertNear(2000, (int) runs.stream().filter(turns -> turns.get(0) == 3).count(), 5);
	}

	// within a cycle, a turn chooses in proportion to what is left of each unit: after measure 2, measure 1 (two left)
	// comes twice as often as measure 3 (one left)
	@Test
	public void aTurnChoosesInProportionToWhatIsLeftOfTheCycle() throws TGPracticeRefusal {
		pool(1, 4, 2, 1, 2);
		List<List<Integer>> afterTwo = new ArrayList<List<Integer>>(chooseWithSeeds(20000));
		afterTwo.removeIf(turns -> turns.get(0) != 2);
		assertNear(afterTwo.size() / 2, (int) afterTwo.stream().filter(turns -> turns.get(1) == 1).count(), 5);
		assertNear(afterTwo.size() / 4, (int) afterTwo.stream().filter(turns -> turns.get(1) == 3).count(), 7);
	}

	// a turn that would leave the rest of the cycle no order is not chosen: after measure 2, measure 3 would leave
	// measure 1 twice in a row
	@Test
	public void aTurnThatWouldLeaveTheCycleNoOrderIsNotChosen() throws TGPracticeRefusal {
		pool(1, 3, 2, 1, 2);
		List<List<Integer>> afterTwo = new ArrayList<List<Integer>>(chooseWithSeeds(400));
		afterTwo.removeIf(turns -> turns.get(0) != 2);
		assertTrue(afterTwo.size() > 50 && afterTwo.stream().allMatch(turns -> turns.get(1) == 1), afterTwo.toString());
	}

	@Test
	public void withoutCyclesEachTurnLeavesOutTheOneBeforeTheOthersInProportionToTheirWeights() throws TGPracticeRefusal {
		settings.setCompleteCycle(false);
		pool(1, 3, 30000, 1, 2);
		List<Integer> turns = chooseWithSeeds(1).get(0);
		assertTrue(!repeats(turns));
		int afterThree = 0;
		int oneAfterThree = 0;
		for (int i = 1; i < turns.size(); i++) {
			afterThree += (turns.get(i - 1) == 3 ? 1 : 0);
			oneAfterThree += (turns.get(i - 1) == 3 && turns.get(i) == 1 ? 1 : 0);
		}
		assertNear(afterThree * 2 / 3, oneAfterThree, 3);
	}

	@Test
	public void withoutCyclesOrTheRuleEachTurnChoosesInProportionToTheWeights() throws TGPracticeRefusal {
		settings.setCompleteCycle(false);
		settings.setNoConsecutiveRepeats(false);
		pool(1, 2, 20000, 1, 3);
		List<Integer> turns = chooseWithSeeds(1).get(0);
		assertNear(15000, count(turns, 1), 2);
		assertTrue(repeats(turns));
	}

	// the flag set after the turn: the steps that follow see it, as the app sets it once it is not refused
	private String flagDuring(TGPracticeChooser chooser, int measure, int difficulty) {
		String outcome = PracticeRefusals.outcome(() -> chooser.checkFlag(measure, difficulty));
		if (!outcome.equals("accepted")) {
			return outcome;
		}
		song.getMeasureHeader(measure - 1).setDifficulty(difficulty);
		return "flagged";
	}

	@Test
	public void aFlagDuringASessionCountsFromTheNextCycleOn() throws TGPracticeRefusal {
		pool(1, 4, 10);
		for (List<Integer> turns : withSeeds(200, chooser -> {
			List<Integer> chosen = choose(chooser, 2);
			assertEquals("flagged", flagDuring(chooser, 2, 3));
			chosen.addAll(choose(chooser, 8));
			song.getMeasureHeader(1).setDifficulty(1);
			return chosen;
		})) {
			assertEquals(Arrays.asList(1, 2, 3, 4), sorted(turns, 1, 4));
			assertEquals(Arrays.asList(1, 2, 2, 2, 3, 4), sorted(turns, 5, 10));
			assertTrue(!repeats(turns), turns.toString());
		}
	}

	// refused, the flag leaves the session as it was: its turns are those of the same seed without the flag
	@Test
	public void aFlagTheNextCyclesCouldNotPlayIsRefused() throws TGPracticeRefusal {
		pool(1, 2, 6);
		TGPracticeChooser flagged = chooser(1);
		List<Integer> turns = choose(flagged, 1);
		assertEquals("Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row.", flagDuring(flagged, 2, 3));
		turns.addAll(choose(flagged, 5));
		assertEquals(choose(chooser(1), 6) + " 1", turns + " " + song.getMeasureHeader(1).getDifficulty());
	}

	@Test
	public void aFlagIsNotRefusedWhenNoCycleFollowsOrTheRuleIsOff() throws TGPracticeRefusal {
		pool(1, 2, 2);
		TGPracticeChooser last = chooser(1);
		choose(last, 1);
		settings.setNoConsecutiveRepeats(false);
		pool(1, 2, 6);
		TGPracticeChooser free = chooser(1);
		choose(free, 1);
		settings.setNoConsecutiveRepeats(true);
		settings.setCompleteCycle(false);
		TGPracticeChooser noCycles = chooser(1);
		choose(noCycles, 1);
		assertEquals("flagged flagged flagged", flagDuring(last, 2, 5) + " " + flagDuring(free, 2, 5) + " " + flagDuring(noCycles, 1, 5));
	}

	// measures 1 to 3, then measure 1 flagged 3x: the next and last cycle (1 x 1 y 1) must start with measure 1, so the
	// cycle under way may not end with it, and a flag that would leave it no other end is refused
	// flagged during turn 1 or 2 of a session of 4 turns (the next cycle plays only its first turn, and is still kept)
	// or 8
	@Test
	public void aFlagThatLeavesTheNextCycleOneStartKeepsTheCycleUnderWayFromEndingWithIt() throws TGPracticeRefusal {
		for (int[] turnsDuring : new int[][] {{4, 1}, {4, 2}, {8, 1}, {8, 2}}) {
			pool(1, 3, turnsDuring[0]);
			for (String outcome : withSeeds(200, chooser -> {
				List<Integer> chosen = choose(chooser, turnsDuring[1]);
				String flagged = flagDuring(chooser, 1, 3);
				chosen.addAll(choose(chooser, turnsDuring[0] - turnsDuring[1]));
				song.getMeasureHeader(0).setDifficulty(1);
				return check(chosen, turnsDuring[1], flagged);
			})) {
				assertEquals("ok", outcome);
			}
		}
	}

	private static String check(List<Integer> turns, int during, String flagged) {
		boolean leftOnlyMeasureOne = (during == 2 && !turns.subList(0, 2).contains(1));
		if (leftOnlyMeasureOne) {
			return (flagged.equals("Measure 1 comes up 3 times in a cycle of 5 turns and would come twice in a row.") ? "ok" : turns + " " + flagged);
		}
		boolean oneStartsTheNext = true;
		for (int turn = 3; turn < turns.size(); turn += 2) {
			oneStartsTheNext &= (turns.get(turn) == 1);
		}
		return (flagged.equals("flagged") && oneStartsTheNext && !repeats(turns) ? "ok" : turns + " " + flagged);
	}

	// one turn after the cycle under way is enough: the next cycle, as for a session, must be one that can be played;
	// so is the session's last turn chosen ahead, opening a cycle of its own
	@Test
	public void aFlagIsRefusedWhenOnlyOneTurnOfTheNextCyclePlays() throws TGPracticeRefusal {
		pool(1, 2, 3);
		TGPracticeChooser after = chooser(1);
		choose(after, 1);
		TGPracticeChooser ahead = chooser(1);
		choose(ahead, 2);
		ahead.ahead();
		String refusal = "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row.";
		assertEquals(refusal + " | " + refusal, flagDuring(after, 2, 3) + " | " + flagDuring(ahead, 2, 3));
	}

	// the player chooses a turn ahead, while the turn before plays: a flag set during a cycle's last turn, after the next
	// cycle's first turn was chosen and before it starts, counts from that cycle (none of its turns has started)
	@Test
	public void aFlagBeforeACyclesFirstTurnStartsCountsFromThatCycle() throws TGPracticeRefusal {
		pool(1, 4, 10);
		for (List<Integer> turns : withSeeds(200, chooser -> {
			List<Integer> chosen = choose(chooser, 4);
			chosen.add(chooser.ahead().getFrom().getNumber());
			assertEquals("flagged", flagDuring(chooser, 2, 3));
			chooser.started();
			chosen.addAll(choose(chooser, 5));
			song.getMeasureHeader(1).setDifficulty(1);
			return chosen;
		})) {
			assertEquals(Arrays.asList(1, 2, 2, 2, 3, 4), sorted(turns, 5, 10));
			assertTrue(!repeats(turns), turns.toString());
		}
	}

	// measures 1 to 3, the next cycle's first turn chosen ahead: measure 1 flagged 3x makes that cycle 1 x 1 y 1, which
	// only a first turn of measure 1 can start; with another one chosen the flag is refused
	@Test
	public void aFlagTheCycleOfATurnChosenAheadCouldNotPlayIsRefused() throws TGPracticeRefusal {
		pool(1, 3, 8);
		for (String outcome : withSeeds(100, chooser -> {
			choose(chooser, 3);
			int ahead = chooser.ahead().getFrom().getNumber();
			String flagged = flagDuring(chooser, 1, 3);
			song.getMeasureHeader(0).setDifficulty(1);
			return (ahead == 1 ? "flagged" : "Measure 1 comes up 3 times in a cycle of 5 turns and would come twice in a row.").equals(flagged) ? "ok" : ahead + " " + flagged;
		})) {
			assertEquals("ok", outcome);
		}
	}

	@Test
	public void aTurnIsAUnitOfThePool() throws TGPracticeRefusal {
		settings.setUnit(TGPracticeUnitKind.LINE);
		settings.setMeasuresPerLine(2);
		settings.setLinesPerPage(4);
		pool(2, 3, 2);
		IntFunction<String> first = seed -> {
			try {
				TGPracticeUnit unit = chooser(seed).next().getFrom();
				return unit + " " + unit.getFirst() + "-" + unit.getLast();
			} catch (TGPracticeRefusal e) {
				throw new IllegalStateException(e);
			}
		};
		List<String> firsts = new ArrayList<String>();
		for (int seed = 1; seed <= 20; seed++) {
			firsts.add(first.apply(seed));
		}
		assertEquals("[Line 2 3-4, Line 3 5-6]", firsts.stream().distinct().sorted().collect(Collectors.toList()).toString());
	}
}
