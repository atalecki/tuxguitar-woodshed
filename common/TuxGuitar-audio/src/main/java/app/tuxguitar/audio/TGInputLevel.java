package app.tuxguitar.audio;

import app.tuxguitar.midi.synth.TGLevelTap;

/**
 * The input level of an armed track, measured on a level tap: what its input channel(s) delivered, a block at a time
 * (the audio thread is the tap's only writer). Its level is the peak of the newest blocks the tap keeps (about two
 * thirds of a second of 512-frame blocks), the higher side of a pair. At -0.1 dBFS or above the input is clipping; it
 * shows it has clipped until the clip indicator is reset, which acknowledges the clips counted so far (only a reset
 * writes that): no flag is written by two threads.
 */
public final class TGInputLevel {

	private final TGLevelTap tap = new TGLevelTap();
	// the clips counted when the indicator was last reset: written by the reset only
	private volatile long acknowledged;

	// a block of what the input delivered, one array per side, all of the same length
	public void deliver(float[][] sides) {
		this.tap.measure(sides);
	}

	public TGLevelTap getTap() {
		return this.tap;
	}

	// minus infinity for silence
	public double getLevel() {
		return TGMeterScale.decibels(this.peak());
	}

	public boolean isClipping() {
		return this.peak() >= TGLevelTap.CLIPPING;
	}

	// the higher side's peak over the newest blocks
	private float peak() {
		float[] peak = new float[TGLevelTap.SIDES];
		this.tap.read(0, peak);
		return Math.max(peak[0], peak[1]);
	}

	// the clips counted since the clip indicator was last reset
	public long getClipsSinceReset() {
		return this.tap.getClips() - this.acknowledged;
	}

	public boolean hasClipped() {
		return this.getClipsSinceReset() > 0;
	}

	public void resetClipIndicator() {
		this.acknowledged = this.tap.getClips();
	}
}
