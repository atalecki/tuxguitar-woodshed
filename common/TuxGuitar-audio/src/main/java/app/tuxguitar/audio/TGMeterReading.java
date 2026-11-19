package app.tuxguitar.audio;

import java.util.Arrays;

/**
 * What a level meter shows at one moment: per side the peak and the held peak in dB (minus infinity below the scale's
 * floor), whether its clip lamp is lit, and whether it measures at all (a meter that does not, an effect switched off,
 * a sound not made by the TuxGuitar Synthesizer, shows an empty, greyed bar). Two equal readings draw the same.
 */
public final class TGMeterReading {

	// a meter that measures nothing
	public static final TGMeterReading OFF = new TGMeterReading(silence(), silence(), false, false);

	private final double[] peaks;
	private final double[] holds;
	private final boolean clipped;
	private final boolean on;

	TGMeterReading(double[] peaks, double[] holds, boolean clipped, boolean on) {
		this.peaks = peaks;
		this.holds = holds;
		this.clipped = clipped;
		this.on = on;
	}

	// minus infinity on both sides
	static double[] silence() {
		double[] silence = new double[2];
		Arrays.fill(silence, Double.NEGATIVE_INFINITY);
		return silence;
	}

	public double getPeak(int side) {
		return this.peaks[side];
	}

	public double getHold(int side) {
		return this.holds[side];
	}

	// the louder side's: what a meter of one bar shows
	public double getPeak() {
		return Math.max(this.peaks[0], this.peaks[1]);
	}

	public double getHold() {
		return Math.max(this.holds[0], this.holds[1]);
	}

	public boolean isClipped() {
		return this.clipped;
	}

	public boolean isOn() {
		return this.on;
	}

	public boolean equals(Object other) {
		if(!(other instanceof TGMeterReading)) {
			return false;
		}
		TGMeterReading reading = (TGMeterReading) other;
		return (Arrays.equals(this.peaks, reading.peaks) && Arrays.equals(this.holds, reading.holds) && this.clipped == reading.clipped && this.on == reading.on);
	}

	public int hashCode() {
		return Arrays.hashCode(this.peaks) * 31 + Arrays.hashCode(this.holds);
	}

	public String toString() {
		return "peak " + Arrays.toString(this.peaks) + " hold " + Arrays.toString(this.holds) + (this.clipped ? " clipped" : "") + (this.on ? "" : " off");
	}
}
