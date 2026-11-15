package app.tuxguitar.practice;

import java.util.Objects;

import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGTempo;

/**
 * How fast a step of practice plays the song: an exact fraction of its written tempo, above 0. p% is p/100 of it; b bpm
 * is b over the written bpm it is read against, so a tempo change in the song plays in proportion. Fractions that are
 * equal are the same scale (50% is 60 bpm against 120).
 */
public final class TGTempoScale {

	private static final long MINUTE = 60000000L;

	private final long num;
	private final long den;

	private TGTempoScale(long num, long den) {
		if (num < 1 || den < 1) {
			throw new IllegalArgumentException("A tempo scale is above 0: " + num + "/" + den);
		}
		long gcd = gcd(num, den);
		this.num = num / gcd;
		this.den = den / gcd;
	}

	private static long gcd(long a, long b) {
		return (b == 0 ? a : gcd(b, a % b));
	}

	// p% of the written tempo
	public static TGTempoScale percent(int percent) {
		return new TGTempoScale(percent, 100);
	}

	// the bpm over the written bpm it is read against
	public static TGTempoScale bpm(int bpm, int writtenBpm) {
		return new TGTempoScale(bpm, writtenBpm);
	}

	// the microseconds a quarter note of the written tempo lasts at this scale, rounded down: for p% the very value the
	// player's sequence gives the Percentage of Tempo p (MidiSequenceParser.usq)
	public int usq(TGTempo written) {
		return (int) (written.getInUSQ() * (double) this.den / this.num);
	}

	// the bpm a written bpm is heard at, at this scale, rounded to the nearest (a half up)
	public int bpm(int writtenBpm) {
		return (int) ((2 * writtenBpm * this.num + this.den) / (2 * this.den));
	}

	// the bpm a written bpm is heard at, at this scale, not rounded: for p% the very value the audio's sample time gives
	// the Percentage of Tempo p
	public double exactBpm(int writtenBpm) {
		return writtenBpm * this.num / (double) this.den;
	}

	// the microseconds so many ticks of the written tempo last at this scale, rounded down once, at the end (not per
	// quarter note, as usq is)
	public long micros(long ticks, TGTempo written) {
		return ticks * MINUTE * this.den / (TGDuration.QUARTER_TIME * written.getQuarterValue() * this.num);
	}

	public boolean equals(Object other) {
		return (other instanceof TGTempoScale && ((TGTempoScale) other).num == this.num && ((TGTempoScale) other).den == this.den);
	}

	public int hashCode() {
		return Objects.hash(this.num, this.den);
	}

	// "13/20"
	public String toString() {
		return this.num + "/" + this.den;
	}
}
