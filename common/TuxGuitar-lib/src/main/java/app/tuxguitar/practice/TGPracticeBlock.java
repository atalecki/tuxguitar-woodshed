package app.tuxguitar.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A block of interleaved practice: what a turn plays and the random choice chooses, a run of so many units of the pool in
 * a row (units per block), from the first to the last (the same unit, for a block of one). The pool's blocks are every
 * such run, in score order, sliding by one unit (blocks: the one place that decides which units make a block); beats
 * run on through the barline, as the pool holds them. The blocks of a pool share one copy of its units, taken when they
 * are cut, each knowing where its first and last unit lie in it, so a change to the list they were cut from leaves
 * them as they were. Two blocks are the same only when they are one block of the pool (the same first and last unit):
 * one that overlaps another is not it. Immutable.
 */
public final class TGPracticeBlock {

	private final List<TGPracticeUnit> pool;
	private final int first;
	private final int last;

	private TGPracticeBlock(List<TGPracticeUnit> pool, int first, int last) {
		this.pool = pool;
		this.first = first;
		this.last = last;
	}

	// every run of so many units of the pool in a row, in order; or the reason so many cannot make a block of the pool:
	// fewer than 1, or more than it holds
	static List<TGPracticeBlock> blocks(List<TGPracticeUnit> pool, int units) throws TGPracticeRefusal {
		TGPracticeUnitKind kind = pool.get(0).getKind();
		if (units < 1) {
			throw new TGPracticeRefusal("transport.practice.refusal.units-per-block", TGPracticeWording.noun(kind, 1, TGPracticeWording.APP));
		}
		if (units > pool.size()) {
			throw new TGPracticeRefusal("transport.practice.refusal.block-too-big", String.valueOf(units), TGPracticeWording.noun(kind, units, TGPracticeWording.APP),
				String.valueOf(pool.size()), TGPracticeWording.noun(kind, pool.size(), TGPracticeWording.APP));
		}
		List<TGPracticeUnit> copy = Collections.unmodifiableList(new ArrayList<TGPracticeUnit>(pool));
		return IntStream.rangeClosed(0, pool.size() - units).mapToObj(first -> new TGPracticeBlock(copy, first, first + units - 1)).collect(Collectors.toList());
	}

	// the weight of each of the blocks of one pool (one result of blocks), each measure as difficult as the function of
	// its number (from 1) says: a block as difficult as its most difficult unit, so its most difficult measure; each
	// unit of the pool weighed once, however many blocks hold it
	static int[] weights(List<TGPracticeBlock> blocks, IntUnaryOperator difficulties) {
		int[] units = blocks.get(0).pool.stream().mapToInt(unit -> unit.weight(difficulties)).toArray();
		return blocks.stream().mapToInt(block -> IntStream.rangeClosed(block.first, block.last).map(index -> units[index]).max().getAsInt()).toArray();
	}

	// its first unit
	public TGPracticeUnit getFrom() {
		return this.pool.get(this.first);
	}

	// its last unit (the first, in a block of one)
	public TGPracticeUnit getTo() {
		return this.pool.get(this.last);
	}

	// a reading for tests and logs, in English: a block of one unit as the unit, "Measure 5"; of more, its first and
	// last, "Measure 1 to Measure 2". The app names a block in its own language (TGPracticeWording)
	public String toString() {
		return (this.first == this.last ? this.getFrom().toString() : this.getFrom() + " to " + this.getTo());
	}
}
