package app.tuxguitar.practice;

/**
 * How interleaved click-up's tempo ladder is given: in percent of the written tempo, or in bpm, read against the written
 * tempo of the passage's first measure.
 */
public enum TGClickUpTempoMode {

	PERCENT("percent", "%"), BPM("bpm", " bpm");

	private final String name;
	private final String unit;

	TGClickUpTempoMode(String name, String unit) {
		this.name = name;
		this.unit = unit;
	}

	// "percent", "bpm": the app's messages name the mode by it
	public String getName() {
		return this.name;
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
