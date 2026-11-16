package app.tuxguitar.practice;

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.song.models.TGSong;

/**
 * A block of interleaved practice: what a turn plays and the random choice chooses, a run of units of the pool in a row,
 * from the first to the last (the same unit, for a block of one). The pool's blocks (blocks) are the one place that
 * decides which units make a block. Two blocks are the same only when they are one block of the pool. Immutable.
 */
public final class TGPracticeBlock {

	private final List<TGPracticeUnit> units;

	private TGPracticeBlock(List<TGPracticeUnit> units) {
		this.units = units;
	}

	// the pool's blocks, in order: each unit its own
	static List<TGPracticeBlock> blocks(List<TGPracticeUnit> pool) {
		return IntStream.range(0, pool.size()).mapToObj(first -> new TGPracticeBlock(pool.subList(first, first + 1))).collect(Collectors.toList());
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

	// a block of one unit, as the unit: "Measure 5"
	public String toString() {
		return this.getFrom().toString();
	}
}
