package app.tuxguitar.practice;

/**
 * Interleaved practice as the musician sets it, checked only when a session starts (TGPracticeSession.start): what a
 * turn plays, the pool (from and to, unit numbers from 1), the number of turns, the groups, and the two rules, both on
 * until turned off.
 */
public class TGPracticeSettings {

	public static final int DEFAULT_GROUP = 4;

	private TGPracticeUnitKind unit = TGPracticeUnitKind.MEASURE;
	private int poolFrom = 1;
	private int poolTo = 1;
	private int turns = 1;
	private int measuresPerLine = DEFAULT_GROUP;
	private int linesPerPage = DEFAULT_GROUP;
	private boolean noConsecutiveRepeats = true;
	private boolean completeCycle = true;

	public TGPracticeUnitKind getUnit() {
		return this.unit;
	}

	public void setUnit(TGPracticeUnitKind unit) {
		this.unit = unit;
	}

	public int getPoolFrom() {
		return this.poolFrom;
	}

	public int getPoolTo() {
		return this.poolTo;
	}

	public void setPool(int from, int to) {
		this.poolFrom = from;
		this.poolTo = to;
	}

	public int getTurns() {
		return this.turns;
	}

	public void setTurns(int turns) {
		this.turns = turns;
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

	public boolean isNoConsecutiveRepeats() {
		return this.noConsecutiveRepeats;
	}

	public void setNoConsecutiveRepeats(boolean noConsecutiveRepeats) {
		this.noConsecutiveRepeats = noConsecutiveRepeats;
	}

	public boolean isCompleteCycle() {
		return this.completeCycle;
	}

	public void setCompleteCycle(boolean completeCycle) {
		this.completeCycle = completeCycle;
	}
}
