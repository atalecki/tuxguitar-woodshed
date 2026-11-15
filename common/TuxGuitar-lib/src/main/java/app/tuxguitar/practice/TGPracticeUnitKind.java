package app.tuxguitar.practice;

/**
 * What a turn of interleaved practice plays: a measure, a line (a fixed group of measures) or a page (a fixed group
 * of lines).
 */
public enum TGPracticeUnitKind {

	MEASURE("measure"), LINE("line"), PAGE("page");

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
}
