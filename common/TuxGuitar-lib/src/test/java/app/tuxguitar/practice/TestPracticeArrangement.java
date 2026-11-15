package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Whether what is left of a cycle (each unit's remaining count) can still be put in an order with no unit twice in a
 * row, starting with another unit than the one before and ending with another than the one the next cycle must start
 * with (-1: no such unit). The rule is checked against every order, tried one by one, for up to four units of up to
 * three turns each.
 */
public class TestPracticeArrangement {

	private static final int UNITS = 4;
	private static final int MOST = 3;

	@Test
	public void theRuleAgreesWithTryingEveryOrder() {
		List<String> disagreements = new ArrayList<String>();
		int[] counts = new int[UNITS];
		int cases = 0;
		do {
			for (int previous = -1; previous < UNITS; previous++) {
				for (int last = -1; last < UNITS; last++) {
					boolean tried = canOrder(counts.clone(), previous, last);
					if (TGPracticeArrangement.feasible(counts.clone(), previous, last) != tried) {
						disagreements.add(Arrays.toString(counts) + " after " + previous + " before " + last + ": " + tried);
					}
					cases++;
				}
			}
		} while (nextCounts(counts));
		assertEquals("[] in 6400 cases", disagreements + " in " + cases + " cases");
	}

	@Test
	public void theCountsAreLeftAsTheyWere() {
		int[] counts = {2, 1, 0};
		TGPracticeArrangement.feasible(counts, 1, 0);
		assertEquals("[2, 1, 0]", Arrays.toString(counts));
	}

	// the next counts, as digits of a number in base MOST + 1; false after the last
	private static boolean nextCounts(int[] counts) {
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] < MOST) {
				counts[i]++;
				return true;
			}
			counts[i] = 0;
		}
		return false;
	}

	private static boolean canOrder(int[] counts, int previous, int last) {
		if (Arrays.stream(counts).sum() == 0) {
			return (last < 0 || previous != last);
		}
		for (int unit = 0; unit < counts.length; unit++) {
			if (counts[unit] > 0 && unit != previous) {
				counts[unit]--;
				boolean can = canOrder(counts, unit, last);
				counts[unit]++;
				if (can) {
					return true;
				}
			}
		}
		return false;
	}
}
