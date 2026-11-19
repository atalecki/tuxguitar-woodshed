package app.tuxguitar.audio;

import java.util.function.LongSupplier;

import app.tuxguitar.midi.synth.TGLevelTap;

/**
 * A tap as one shown meter reads it: where it read up to, and how the meter moves (TGMeterBallistics). Its lamp counts
 * the clips the supplier gives: the tap's own (the Track panel's meters), or those since a reset kept elsewhere
 * (an armed track's input, TGInputLevel). On the thread that shows it; the tap is read without a lock.
 */
public final class TGTapMeter {

	private final TGLevelTap tap;
	private final LongSupplier clips;
	private final TGMeterBallistics ballistics = new TGMeterBallistics();
	private final float[] peaks = new float[TGLevelTap.SIDES];
	private long cursor;

	public TGTapMeter(TGLevelTap tap, LongSupplier clips) {
		this.tap = tap;
		this.clips = clips;
	}

	// the reading at the time (milliseconds): the blocks measured since the last one
	public TGMeterReading read(long now) {
		this.cursor = this.tap.read(this.cursor, this.peaks);
		return this.ballistics.next(this.peaks, this.clips.getAsLong(), now);
	}

	// the lamp goes out: the clips counted so far are acknowledged
	public void clear() {
		this.ballistics.clear(this.clips.getAsLong());
	}
}
