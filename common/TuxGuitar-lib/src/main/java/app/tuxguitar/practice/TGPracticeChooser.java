package app.tuxguitar.practice;

import java.util.SplittableRandom;

/**
 * Chooses a session's turns one by one. With "Complete cycle before reshuffle" the turns go in cycles, each holding
 * every block of the pool as many times as its weight: a turn chooses among what is left of the cycle, in proportion
 * to it. Without, each turn chooses in proportion to the weights. With "No consecutive repeats" a turn never chooses
 * the block before, nor (with cycles) one that would leave the rest of the cycle, or the next cycle's start, no order
 * that keeps the rule. A cycle reads the weights when its first turn starts playing: a flag set during a cycle counts
 * from the next one, even when that one's first turn was chosen ahead (ahead) and has not started yet. One chooser per
 * session; the player chooses on its thread while an edit checks a flag on another, so each call holds the chooser.
 */
public final class TGPracticeChooser {

	private final TGPracticeSession session;
	// a generator whose streams for nearby seeds are unrelated (java.util.Random's first numbers for seeds 1, 2, 3...
	// are nearly the same, so a choice over many seeds would not be random)
	private final SplittableRandom random;
	// what is left of the cycle under way, by block of the pool
	private int[] cycle;
	private int previous = TGPracticeArrangement.NONE;
	private int chosen;
	// whether the turn chosen last opened its cycle and has not started: the cycle takes the song's weights when it starts
	private boolean opened;

	TGPracticeChooser(TGPracticeSession session, long seed) {
		this.session = session;
		this.random = new SplittableRandom(seed);
		this.cycle = new int[session.getPool().size()];
	}

	// the session whose turns it chooses
	public TGPracticeSession getSession() {
		return this.session;
	}

	// the block of the next turn, which starts playing now
	public synchronized TGPracticeBlock next() {
		TGPracticeBlock block = this.ahead();
		this.started();
		return block;
	}

	// the block of the next turn, chosen ahead of playing it, once the turn before has started; started() says when it
	// starts
	public synchronized TGPracticeBlock ahead() {
		this.started();
		int block = (this.session.isCompleteCycle() ? this.nextInTheCycle() : this.draw(this.chancesOnTheirOwn()));
		this.previous = block;
		this.chosen++;
		return this.session.getPool().get(block);
	}

	// the turn chosen last has started playing: a cycle it opened holds the weights the song has now, less that turn
	public synchronized void started() {
		if (this.opened) {
			this.cycle = this.session.weights();
			this.cycle[this.previous]--;
			this.opened = false;
		}
	}

	// a cycle starts once the one before has chosen all it holds
	private int nextInTheCycle() {
		boolean opens = (TGPracticeArrangement.total(this.cycle) == 0);
		if (opens) {
			this.cycle = this.session.weights();
		}
		int block = this.draw(this.chancesInTheCycle());
		this.cycle[block]--;
		this.opened = opens;
		return block;
	}

	// refuses a flag, set now, whose weights the next cycles could not play keeping "No consecutive repeats"; the cycle
	// under way (the last one a turn of which has started) keeps its turns
	public synchronized void checkFlag(int measure, int difficulty) throws TGPracticeRefusal {
		int after = this.turnsAfterTheCycle();
		if (this.session.isNoConsecutiveRepeats() && after > 0) {
			int[] next = this.session.weights(measure, difficulty);
			this.session.check(next, after);
			if (!(this.opened ? this.opens(next, after) : this.ends(next))) {
				throw TGPracticeRule.refusal(this.session.getPool(), next);
			}
		}
	}

	// whether what is left of the cycle under way has an order that keeps the rule before a cycle of the weights
	private boolean ends(int[] next) {
		int last = (this.session.isCompleteCycle() ? TGPracticeArrangement.majority(next) : TGPracticeArrangement.NONE);
		return TGPracticeArrangement.feasible(this.cycle, this.previous, last);
	}

	// whether a cycle of the weights, opened by the turn chosen ahead, has an order that keeps the rule (before the
	// cycle after it, when turns follow)
	private boolean opens(int[] next, int after) {
		int[] left = next.clone();
		left[this.previous]--;
		int last = (after > TGPracticeArrangement.total(next) ? TGPracticeArrangement.majority(next) : TGPracticeArrangement.NONE);
		return TGPracticeArrangement.feasible(left, this.previous, last);
	}

	// the turns of the session after the cycle under way (without cycles: after the turns chosen); a turn chosen ahead
	// that opened its cycle is not under way
	private int turnsAfterTheCycle() {
		int under = (this.opened ? 1 : -TGPracticeArrangement.total(this.cycle));
		return this.session.getTurns() - this.chosen + under;
	}

	private int[] chancesOnTheirOwn() {
		int[] chances = this.session.weights();
		if (this.session.isNoConsecutiveRepeats() && this.previous != TGPracticeArrangement.NONE) {
			chances[this.previous] = 0;
		}
		return chances;
	}

	// a block that holds more than half of the next cycle must start it: the cycle under way may not end with it
	private int[] chancesInTheCycle() {
		int last = (this.turnsAfterTheCycle() > 0 ? TGPracticeArrangement.majority(this.session.weights()) : TGPracticeArrangement.NONE);
		int[] chances = new int[this.cycle.length];
		for (int block = 0; block < chances.length; block++) {
			chances[block] = (this.keepsTheRule(block, last) ? this.cycle[block] : 0);
		}
		return chances;
	}

	// whether choosing the block (one left of the cycle) leaves the rest of the cycle an order that keeps the rule, ending
	// with another block than the given one
	private boolean keepsTheRule(int block, int last) {
		if (!this.session.isNoConsecutiveRepeats()) {
			return true;
		}
		if (this.cycle[block] == 0 || block == this.previous) {
			return false;
		}
		int[] left = this.cycle.clone();
		left[block]--;
		return TGPracticeArrangement.feasible(left, block, last);
	}

	// a block, at random, in proportion to its chances
	private int draw(int[] chances) {
		int left = this.random.nextInt(TGPracticeArrangement.total(chances));
		int block = 0;
		while (left >= chances[block]) {
			left -= chances[block];
			block++;
		}
		return block;
	}
}
