package app.tuxguitar.practice;

import java.util.Arrays;

/**
 * Orders of what is left of a cycle (each unit's remaining count, by unit of the pool) that keep "No consecutive
 * repeats": no unit twice in a row, the first not the unit before, the last not the unit the next cycle must start with.
 */
final class TGPracticeArrangement {

	// no unit: before the first turn, or a next cycle free to start with any unit
	static final int NONE = -1;

	private TGPracticeArrangement() {
	}

	static int total(int[] counts) {
		return Arrays.stream(counts).sum();
	}

	static int most(int[] counts) {
		return Arrays.stream(counts).max().getAsInt();
	}

	// the unit that holds more than half of the counts, else NONE
	static int majority(int[] counts) {
		int total = total(counts);
		for (int unit = 0; unit < counts.length; unit++) {
			if (2 * counts[unit] > total) {
				return unit;
			}
		}
		return NONE;
	}

	// whether the counts can be put in an order with no unit twice in a row, its first not the unit before and its last
	// not the unit given (NONE: any)
	static boolean feasible(int[] counts, int before, int last) {
		int left = total(counts);
		int majority = majority(counts);
		if (majority != NONE) {
			// it takes every other turn, the first and the last among them
			return (2 * counts[majority] == left + 1 && majority != before && majority != last);
		}
		int most = most(counts);
		return (2 * most < left || !shutOut(counts, before, last, most));
	}

	// with half of what is left (nothing left: none), a unit of that count takes the first or the last turn: one that may
	// take neither cannot be placed
	private static boolean shutOut(int[] counts, int before, int last, int most) {
		return (before == last && before != NONE && counts[before] == most);
	}
}
