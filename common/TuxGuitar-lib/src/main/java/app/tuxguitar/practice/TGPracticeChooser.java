package app.tuxguitar.practice;

import java.util.SplittableRandom;

/**
 * Chooses a session's turns one by one. With "Complete cycle before reshuffle" the turns go in cycles, each holding
 * every unit of the pool as many times as its weight: a turn chooses among what is left of the cycle, in proportion
 * to it. Without, each turn chooses in proportion to the weights. With "No consecutive repeats" a turn never chooses
 * the unit before, nor (with cycles) one that would leave the rest of the cycle, or the next cycle's start, no order
 * that keeps the rule. A cycle reads the weights when it starts: a flag set during a cycle counts from the next.
 */
public final class TGPracticeChooser {

	private final TGPracticeSession session;
	// a generator whose streams for nearby seeds are unrelated (java.util.Random's first numbers for seeds 1, 2, 3...
	// are nearly the same, so a choice over many seeds would not be random)
	private final SplittableRandom random;
	// what is left of the cycle under way, by unit of the pool
	private int[] cycle;
	private int previous = TGPracticeArrangement.NONE;
	private int chosen;

	TGPracticeChooser(TGPracticeSession session, long seed) {
		this.session = session;
		this.random = new SplittableRandom(seed);
		this.cycle = new int[session.getPool().size()];
	}

	// the session whose turns it chooses
	public TGPracticeSession getSession() {
		return this.session;
	}

	// the unit of the next turn
	public TGPracticeUnit next() {
		int unit = (this.session.isCompleteCycle() ? this.nextInTheCycle() : this.draw(this.chancesOnTheirOwn()));
		this.previous = unit;
		this.chosen++;
		return this.session.getPool().get(unit);
	}

	// a cycle starts once the one before has chosen all it holds
	private int nextInTheCycle() {
		if (TGPracticeArrangement.total(this.cycle) == 0) {
			this.cycle = this.session.weights();
		}
		int unit = this.draw(this.chancesInTheCycle());
		this.cycle[unit]--;
		return unit;
	}

	// refuses a flag, set now, whose weights the next cycles could not play keeping "No consecutive repeats"; the cycle
	// under way keeps its turns
	public void checkFlag(int measure, int difficulty) throws TGPracticeRefusal {
		int after = this.turnsAfterTheCycle();
		if (this.session.isNoConsecutiveRepeats() && after > 0) {
			int[] next = this.session.weights(measure, difficulty);
			this.session.check(next, after);
			int last = (this.session.isCompleteCycle() ? TGPracticeArrangement.majority(next) : TGPracticeArrangement.NONE);
			if (!TGPracticeArrangement.feasible(this.cycle, this.previous, last)) {
				throw TGPracticeRule.refusal(this.session.getPool(), next);
			}
		}
	}

	// the turns of the session after the cycle under way (without cycles: after the turns chosen)
	private int turnsAfterTheCycle() {
		return this.session.getTurns() - this.chosen - TGPracticeArrangement.total(this.cycle);
	}

	private int[] chancesOnTheirOwn() {
		int[] chances = this.session.weights();
		if (this.session.isNoConsecutiveRepeats() && this.previous != TGPracticeArrangement.NONE) {
			chances[this.previous] = 0;
		}
		return chances;
	}

	// a unit that holds more than half of the next cycle must start it: the cycle under way may not end with it
	private int[] chancesInTheCycle() {
		int last = (this.turnsAfterTheCycle() > 0 ? TGPracticeArrangement.majority(this.session.weights()) : TGPracticeArrangement.NONE);
		int[] chances = new int[this.cycle.length];
		for (int unit = 0; unit < chances.length; unit++) {
			chances[unit] = (this.keepsTheRule(unit, last) ? this.cycle[unit] : 0);
		}
		return chances;
	}

	// whether choosing the unit (one left of the cycle) leaves the rest of the cycle an order that keeps the rule, ending
	// with another unit than the given one
	private boolean keepsTheRule(int unit, int last) {
		if (!this.session.isNoConsecutiveRepeats()) {
			return true;
		}
		if (this.cycle[unit] == 0 || unit == this.previous) {
			return false;
		}
		int[] left = this.cycle.clone();
		left[unit]--;
		return TGPracticeArrangement.feasible(left, unit, last);
	}

	// a unit, at random, in proportion to its chances
	private int draw(int[] chances) {
		int left = this.random.nextInt(TGPracticeArrangement.total(chances));
		int unit = 0;
		while (left >= chances[unit]) {
			left -= chances[unit];
			unit++;
		}
		return unit;
	}
}
