package app.tuxguitar.practice;

import java.util.List;

/**
 * "No consecutive repeats" for a session's weights: with w the heaviest block's weight and L a cycle's length (the
 * weights added up), up to L turns need 2w <= L + 1, more turns 2w <= L. Without cycles each turn only leaves out the
 * block before, which any pool of two blocks or more can do.
 */
final class TGPracticeRule {

	private TGPracticeRule() {
	}

	// refuses weights of the pool's blocks that cannot keep the rule for that many turns
	static void check(List<TGPracticeBlock> pool, int[] weights, int turns, boolean completeCycle) throws TGPracticeRefusal {
		if (completeCycle ? !keeps(TGPracticeArrangement.most(weights), TGPracticeArrangement.total(weights), turns) : (pool.size() == 1 && turns > 1)) {
			throw refusal(pool, weights);
		}
	}

	private static boolean keeps(int heaviest, int cycle, int turns) {
		return (2 * heaviest <= (turns <= cycle ? cycle + 1 : cycle));
	}

	// why the weights cannot keep the rule, naming the block that holds more than half of a cycle (weights that cannot
	// keep it have one)
	static TGPracticeRefusal refusal(List<TGPracticeBlock> pool, int[] weights) {
		int heaviest = TGPracticeArrangement.majority(weights);
		TGPracticeBlock block = pool.get(heaviest);
		if (pool.size() == 1) {
			return new TGPracticeRefusal(block + " would come twice in a row.");
		}
		return new TGPracticeRefusal(block + " comes up " + weights[heaviest] + " times in a cycle of " + TGPracticeArrangement.total(weights)
			+ " turns and would come twice in a row.");
	}
}
