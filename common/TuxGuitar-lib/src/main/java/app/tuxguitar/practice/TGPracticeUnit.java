package app.tuxguitar.practice;

import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import app.tuxguitar.song.models.TGSong;

/**
 * A unit of the song: its kind, its number (from 1) and the measures it holds, first to last (from 1).
 */
public final class TGPracticeUnit {

	private final TGPracticeUnitKind kind;
	private final int number;
	private final int first;
	private final int last;

	TGPracticeUnit(TGPracticeUnitKind kind, int number, int first, int last) {
		this.kind = kind;
		this.number = number;
		this.first = first;
		this.last = last;
	}

	public TGPracticeUnitKind getKind() {
		return this.kind;
	}

	public int getNumber() {
		return this.number;
	}

	public int getFirst() {
		return this.first;
	}

	public int getLast() {
		return this.last;
	}

	// as difficult as its most difficult measure
	public int weight(TGSong song) {
		return this.weight(difficulties(song));
	}

	// as weight(song), each measure as difficult as the function of its number (from 1) says
	int weight(IntUnaryOperator difficulties) {
		return IntStream.rangeClosed(this.first, this.last).map(difficulties).max().getAsInt();
	}

	// each measure's difficulty flag in the song, by its number (from 1)
	static IntUnaryOperator difficulties(TGSong song) {
		return number -> song.getMeasureHeader(number - 1).getDifficulty();
	}

	// "Line 3"
	public String toString() {
		return this.kind.title() + " " + this.number;
	}
}
