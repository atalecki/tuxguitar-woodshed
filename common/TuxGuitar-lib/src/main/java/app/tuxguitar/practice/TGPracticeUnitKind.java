package app.tuxguitar.practice;

/**
 * What a turn of interleaved practice plays: a measure, a line (a fixed group of measures), a page (a fixed group of
 * lines) or a beat (one beat of a measure, the unit of its time signature), whose range is given in measures.
 */
public enum TGPracticeUnitKind {

	MEASURE("measure"), LINE("line"), PAGE("page"), BEAT("beat");

	private final String name;

	TGPracticeUnitKind(String name) {
		this.name = name;
	}

	// "line" for one, "lines" for any other count
	public String getName(int count) {
		return (count == 1 ? this.name : this.name + "s");
	}

	public String getName() {
		return this.name;
	}

	// "Line"
	public String title() {
		return Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1);
	}

	// the kind a range of units of this kind is given in: its own, but measures for beats
	public TGPracticeUnitKind rangeKind() {
		return (this == BEAT ? MEASURE : this);
	}
}
