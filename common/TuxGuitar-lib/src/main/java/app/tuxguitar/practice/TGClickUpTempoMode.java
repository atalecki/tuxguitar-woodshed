package app.tuxguitar.practice;

/**
 * How interleaved click-up's tempo ladder is given: in percent of the written tempo, or in bpm, read against the written
 * tempo of the passage's first measure.
 */
public enum TGClickUpTempoMode {

	PERCENT("%"), BPM(" bpm");

	private final String unit;

	TGClickUpTempoMode(String unit) {
		this.unit = unit;
	}

	// the tempo a rung plays the song at, against the written bpm of the passage's first measure
	TGTempoScale scale(int rung, int writtenBpm) {
		return (this == PERCENT ? TGTempoScale.percent(rung) : TGTempoScale.bpm(rung, writtenBpm));
	}

	// "60%", "60 bpm"
	public String reading(int value) {
		return value + this.unit;
	}
}
