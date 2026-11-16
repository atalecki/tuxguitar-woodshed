package app.tuxguitar.practice;

import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * A unit of the song: its kind, its number (from 1) and the measures it holds, first to last (from 1). A beat's number
 * is its beat in its measure, the one measure it holds. Two units are the same when all four are.
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

	// how many beats the measure the header heads has: as many as its time signature's numerator
	static int beatCount(TGMeasureHeader header) {
		return header.getTimeSignature().getNumerator();
	}

	// the beat (from 1) of the measure the header heads
	static TGPracticeUnit beat(TGMeasureHeader header, int beat) {
		if (beat < 1 || beat > beatCount(header)) {
			throw new IllegalArgumentException("Measure " + header.getNumber() + " has no beat " + beat + ".");
		}
		return new TGPracticeUnit(TGPracticeUnitKind.BEAT, beat, header.getNumber(), header.getNumber());
	}

	// every beat of the measure the header heads, in order
	static List<TGPracticeUnit> beats(TGMeasureHeader header) {
		return IntStream.rangeClosed(1, beatCount(header)).mapToObj(beat -> beat(header, beat)).collect(Collectors.toList());
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

	// as difficult as its most difficult measure, each measure as difficult as the function of its number (from 1) says
	int weight(IntUnaryOperator difficulties) {
		return IntStream.rangeClosed(this.first, this.last).map(difficulties).max().getAsInt();
	}

	// each measure's difficulty flag in the song, by its number (from 1)
	static IntUnaryOperator difficulties(TGSong song) {
		return number -> song.getMeasureHeader(number - 1).getDifficulty();
	}

	// the same unit: of the same kind and number, holding the same measures
	public boolean equals(Object other) {
		if (!(other instanceof TGPracticeUnit)) {
			return false;
		}
		TGPracticeUnit unit = (TGPracticeUnit) other;
		return (this.kind == unit.kind && this.number == unit.number && this.first == unit.first && this.last == unit.last);
	}

	public int hashCode() {
		return Objects.hash(this.kind, this.number, this.first, this.last);
	}

	// "line 3", a beat "measure 2 beat 3"
	String name() {
		if (this.kind == TGPracticeUnitKind.BEAT) {
			return TGPracticeUnitKind.MEASURE.getName() + " " + this.first + " " + this.kind.getName() + " " + this.number;
		}
		return this.kind.getName() + " " + this.number;
	}

	// "Line 3", a beat "Measure 2 beat 3"
	public String toString() {
		String name = this.name();
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
