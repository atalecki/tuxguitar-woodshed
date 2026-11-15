package app.tuxguitar.audio;

import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * A measure as the song plays it, from one tick to another of it (in ticks from its start), in a pass (from 0) at the
 * tempo scale of that pass (its Percentage of Tempo, or a practice turn's). It starts a stretch when song time jumps to it: a repeat, an alternate ending, the
 * jump back of a loop. Immutable.
 */
public final class TGPlayedMeasure {

	private final TGMeasureHeader header;
	private final long from;
	private final long to;
	private final boolean jump;
	private final int pass;
	private final TGTempoScale scale;
	private final int step;

	// the step of a measure no pass reaches (interleaved practice plays it)
	public static final int NO_STEP = -1;

	// step: where it comes in its pass, from 0; NO_STEP for a measure no pass reaches
	public TGPlayedMeasure(TGMeasureHeader header, long from, long to, boolean jump, int pass, TGTempoScale scale, int step) {
		this.header = header;
		this.from = from;
		this.to = to;
		this.jump = jump;
		this.pass = pass;
		this.scale = scale;
		this.step = step;
	}

	// played only up to the tick
	public TGPlayedMeasure until(long tick) {
		return new TGPlayedMeasure(this.header, this.from, tick, this.jump, this.pass, this.scale, this.step);
	}

	public int getStep() {
		return this.step;
	}

	public TGMeasureHeader getHeader() {
		return this.header;
	}

	public long getFrom() {
		return this.from;
	}

	public long getTo() {
		return this.to;
	}

	public boolean isJump() {
		return this.jump;
	}

	public int getPass() {
		return this.pass;
	}

	public TGTempoScale getScale() {
		return this.scale;
	}

	// quarter notes per minute as it sounds
	public double getTempo() {
		return TGSampleTime.tempo(this.header, this.scale);
	}
}
