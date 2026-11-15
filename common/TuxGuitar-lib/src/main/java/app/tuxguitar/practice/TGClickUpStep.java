package app.tuxguitar.practice;

/**
 * A step of interleaved click-up: with so many sections in play, it plays a run of them that ends with the newest
 * (first to last, numbered from 1 in the passage), the measures they hold (first to last, from 1), at a rung of the
 * tempo ladder, in the unit the ladder is given in, and the tempo scale that rung plays the song at.
 */
public final class TGClickUpStep {

	// the first and last section it plays, as units of the song
	final TGPracticeUnit from;
	final TGPracticeUnit to;
	private final int firstSection;
	private final int lastSection;
	private final int rung;
	private final TGClickUpTempoMode tempoMode;
	private final TGTempoScale scale;

	TGClickUpStep(TGPracticeUnit first, TGPracticeUnit last, int firstSection, int lastSection, int rung, TGClickUpTempoMode tempoMode, TGTempoScale scale) {
		this.from = first;
		this.to = last;
		this.firstSection = firstSection;
		this.lastSection = lastSection;
		this.rung = rung;
		this.tempoMode = tempoMode;
		this.scale = scale;
	}

	// the sections in play: the newest is the step's last section
	public int getSectionsInPlay() {
		return this.lastSection;
	}

	public int getFirstSection() {
		return this.firstSection;
	}

	public int getLastSection() {
		return this.lastSection;
	}

	public int getFirstMeasure() {
		return this.from.getFirst();
	}

	public int getLastMeasure() {
		return this.to.getLast();
	}

	// the rung, in the ladder's unit
	public int getRung() {
		return this.rung;
	}

	public TGClickUpTempoMode getTempoMode() {
		return this.tempoMode;
	}

	public TGTempoScale getScale() {
		return this.scale;
	}

	// "1-2@60%", one section alone "2@65 bpm"
	public String toString() {
		String sections = (this.firstSection == this.lastSection ? "" : this.firstSection + "-") + this.lastSection;
		return sections + "@" + this.tempoMode.reading(this.rung);
	}
}
