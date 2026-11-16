package app.tuxguitar.practice;

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.song.models.TGSong;

/**
 * A block of interleaved practice: what a turn plays and the random choice chooses, a run of so many units of the pool in
 * a row (units per block), from the first to the last (the same unit, for a block of one). The pool's blocks are every
 * such run, in score order, sliding by one unit (blocks: the one place that decides which units make a block); beats
 * run on through the barline, as the pool holds them. Two blocks are the same only when they are one block of the pool
 * (the same first and last unit): one that overlaps another is not it. Immutable.
 */
public final class TGPracticeBlock {

	private final List<TGPracticeUnit> units;

	private TGPracticeBlock(List<TGPracticeUnit> units) {
		this.units = units;
	}

	// every run of so many units of the pool in a row, in order; or the reason so many cannot make a block of the pool:
	// fewer than 1, or more than it holds
	static List<TGPracticeBlock> blocks(List<TGPracticeUnit> pool, int units) throws TGPracticeRefusal {
		TGPracticeUnitKind kind = pool.get(0).getKind();
		if (units < 1) {
			throw new TGPracticeRefusal("A block needs at least 1 " + kind.getName() + ".");
		}
		if (units > pool.size()) {
			throw new TGPracticeRefusal("A block of " + units + " " + kind.getName(units) + " does not fit in the pool's " + pool.size() + " " + kind.getName(pool.size()) + ".");
		}
		return IntStream.rangeClosed(0, pool.size() - units).mapToObj(first -> new TGPracticeBlock(pool.subList(first, first + units))).collect(Collectors.toList());
	}

	// its first unit
	public TGPracticeUnit getFrom() {
		return this.units.get(0);
	}

	// its last unit (the first, in a block of one)
	public TGPracticeUnit getTo() {
		return this.units.get(this.units.size() - 1);
	}

	// as difficult as its most difficult unit, so its most difficult measure
	public int weight(TGSong song) {
		return this.weight(TGPracticeUnit.difficulties(song));
	}

	// as weight(song), each measure as difficult as the function of its number (from 1) says
	int weight(IntUnaryOperator difficulties) {
		return this.units.stream().mapToInt(unit -> unit.weight(difficulties)).max().getAsInt();
	}

	// a block of one unit, as the unit: "Measure 5"; of more, by its first and last: "The block of measures 1-2", "The
	// block of measure 3 beat 4 to measure 4 beat 1"
	public String toString() {
		TGPracticeUnit from = this.getFrom();
		TGPracticeUnit to = this.getTo();
		if (from == to) {
			return from.toString();
		}
		if (from.getKind() == TGPracticeUnitKind.BEAT) {
			return "The block of " + from.name() + " to " + to.name();
		}
		return "The block of " + from.getKind().getName(2) + " " + from.getNumber() + "-" + to.getNumber();
	}
}
