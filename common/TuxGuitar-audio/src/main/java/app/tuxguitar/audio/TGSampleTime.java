package app.tuxguitar.audio;

import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGTakeClip;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * How many samples a stretch of the song lasts. A tempo here is quarter notes per minute as the song sounds: the
 * measure's tempo at the tempo scale it plays at (the Percentage of Tempo, or a practice turn's). Recording, playing and the takes' clips all count samples here, so a
 * take played at the tempo it was recorded at lines up sample for sample.
 */
public final class TGSampleTime {

	private TGSampleTime() {
	}

	public static double tempo(TGMeasureHeader header, TGTempoScale scale) {
		return scale.exactBpm(header.getTempo().getQuarterValue());
	}

	public static int samples(long ticks, double tempo, int sampleRate) {
		return TGTakeClip.samples(ticks, tempo, sampleRate);
	}
}
