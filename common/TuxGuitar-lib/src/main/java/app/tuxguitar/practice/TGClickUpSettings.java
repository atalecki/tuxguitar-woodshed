package app.tuxguitar.practice;

/**
 * Interleaved click-up as the musician sets it, checked only when it starts (TGClickUpLadder.of): what a section is,
 * the passage (from and to, unit numbers from 1), the groups, the tempo ladder (how it is given, its start, its goal and
 * its increment), the passes of each step, whether each step has a Count In (its own, whether or not the player's Count
 * In is on) and, if so, whether one comes before every pass or only the first.
 */
public class TGClickUpSettings {

	public static final int DEFAULT_START = 60;
	public static final int DEFAULT_GOAL = 100;
	public static final int DEFAULT_INCREMENT = 5;

	private TGPracticeUnitKind unit = TGPracticeUnitKind.MEASURE;
	private int passageFrom = 1;
	private int passageTo = 1;
	private int measuresPerLine = TGPracticeSettings.DEFAULT_GROUP;
	private int linesPerPage = TGPracticeSettings.DEFAULT_GROUP;
	private TGClickUpTempoMode tempoMode = TGClickUpTempoMode.PERCENT;
	private int start = DEFAULT_START;
	private int goal = DEFAULT_GOAL;
	private int increment = DEFAULT_INCREMENT;
	private int passes = 1;
	private boolean countInEachStep = true;
	private boolean countInEveryPass;

	public TGPracticeUnitKind getUnit() {
		return this.unit;
	}

	public void setUnit(TGPracticeUnitKind unit) {
		this.unit = unit;
	}

	public int getPassageFrom() {
		return this.passageFrom;
	}

	public void setPassageFrom(int passageFrom) {
		this.passageFrom = passageFrom;
	}

	public int getPassageTo() {
		return this.passageTo;
	}

	public void setPassageTo(int passageTo) {
		this.passageTo = passageTo;
	}

	public int getMeasuresPerLine() {
		return this.measuresPerLine;
	}

	public void setMeasuresPerLine(int measuresPerLine) {
		this.measuresPerLine = measuresPerLine;
	}

	public int getLinesPerPage() {
		return this.linesPerPage;
	}

	public void setLinesPerPage(int linesPerPage) {
		this.linesPerPage = linesPerPage;
	}

	public TGClickUpTempoMode getTempoMode() {
		return this.tempoMode;
	}

	public void setTempoMode(TGClickUpTempoMode tempoMode) {
		this.tempoMode = tempoMode;
	}

	public int getStart() {
		return this.start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getGoal() {
		return this.goal;
	}

	public void setGoal(int goal) {
		this.goal = goal;
	}

	public int getIncrement() {
		return this.increment;
	}

	public void setIncrement(int increment) {
		this.increment = increment;
	}

	public int getPasses() {
		return this.passes;
	}

	public void setPasses(int passes) {
		this.passes = passes;
	}

	// the ladder given anew in the mode: from 60% of the written tempo (rounded) to it, by 5, in the mode's unit; in
	// percent 60% to 100%, in bpm against the written bpm it is read against (see TGClickUpLadder.getWrittenBpm)
	public void setTempo(TGClickUpTempoMode tempoMode, int writtenBpm) {
		int written = (tempoMode == TGClickUpTempoMode.PERCENT ? DEFAULT_GOAL : writtenBpm);
		this.tempoMode = tempoMode;
		this.start = (written * DEFAULT_START + DEFAULT_GOAL / 2) / DEFAULT_GOAL;
		this.goal = written;
		this.increment = DEFAULT_INCREMENT;
	}

	public boolean isCountInEachStep() {
		return this.countInEachStep;
	}

	public void setCountInEachStep(boolean countInEachStep) {
		this.countInEachStep = countInEachStep;
	}

	public boolean isCountInEveryPass() {
		return this.countInEveryPass;
	}

	public void setCountInEveryPass(boolean countInEveryPass) {
		this.countInEveryPass = countInEveryPass;
	}
}
