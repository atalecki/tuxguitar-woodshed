package app.tuxguitar.audio;

/**
 * How one shown level meter moves, by wall time: its peak rises at once and falls back FALL dB a second; a hold line
 * stays at the highest peak for HOLD, then falls at the same rate; each side on its own. Its clip lamp is lit while
 * its point has clipped more often than this meter acknowledged (cleared).
 */
public final class TGMeterBallistics {

	// dB a second
	public static final double FALL = 20;
	// milliseconds
	public static final long HOLD = 2000;

	private static final int SIDES = 2;

	// per side: the peak, and the peak held (from when), in dB; minus infinity before any sound
	private final double[] peaks = TGMeterReading.silence();
	private final double[] holds = TGMeterReading.silence();
	private final long[] heldAt = new long[SIDES];
	// the time of the last reading
	private long last;
	// the clips its point had counted at the last reading, and those acknowledged
	private long seen;
	private long acknowledged;

	// the reading after the newest peaks of each side (amplitudes, since the last reading) and the clips its point has
	// counted, at the time (milliseconds)
	public TGMeterReading next(float[] peaks, long clips, long now) {
		double fall = falling(now - this.last);
		this.last = now;
		this.seen = clips;
		double[] shownPeaks = new double[SIDES];
		double[] shownHolds = new double[SIDES];
		for (int side = 0; side < SIDES; side++) {
			double level = TGMeterScale.decibels(peaks[side]);
			this.peaks[side] = Math.max(level, this.peaks[side] - fall);
			if (level >= this.holding(side, now)) {
				this.holds[side] = level;
				this.heldAt[side] = now;
			}
			shownPeaks[side] = shown(this.peaks[side]);
			shownHolds[side] = shown(this.holding(side, now));
		}
		return new TGMeterReading(shownPeaks, shownHolds, (clips > this.acknowledged), true);
	}

	// how far a level falls over the milliseconds (none backwards)
	private static double falling(long millis) {
		return FALL * Math.max(0, millis) / 1000.0;
	}

	// where the side's hold is at the time: where it was held until HOLD has passed, then falling
	private double holding(int side, long now) {
		return this.holds[side] - falling(now - this.heldAt[side] - HOLD);
	}

	// below the scale's floor a meter shows nothing
	private static double shown(double level) {
		return (level < TGMeterScale.FLOOR ? Double.NEGATIVE_INFINITY : level);
	}

	// the lamp goes out: the clips counted so far are acknowledged
	public void clear() {
		this.acknowledged = this.seen;
	}
}
