package app.tuxguitar.practice;

/**
 * Interleaved practice as the musician sets it, checked only when a session starts (TGPracticeSession.start): what a
 * turn plays (a block of so many units in a row, TGPracticeBlock: 1 until changed), the pool (from and to, unit
 * numbers from 1), the number of turns, the groups, the two rules and whether
 * each turn has a Count In (its own, whether or not the player's Count In is on), all three on until turned off; and the
 * interval clock (off until turned on), which times the session instead of counting its turns (TGPracticeClock): a
 * turn's minutes, a break's seconds, a session's minutes, and whether a Count In comes before every pass of a turn or
 * only its first.
 */
public class TGPracticeSettings {

	public static final int DEFAULT_GROUP = 4;
	public static final int DEFAULT_TURN_MINUTES = 3;
	public static final int DEFAULT_BREAK_SECONDS = 30;
	public static final int DEFAULT_SESSION_MINUTES = 30;

	private TGPracticeUnitKind unit = TGPracticeUnitKind.MEASURE;
	private int unitsPerBlock = 1;
	private int poolFrom = 1;
	private int poolTo = 1;
	private int turns = 1;
	private int measuresPerLine = DEFAULT_GROUP;
	private int linesPerPage = DEFAULT_GROUP;
	private boolean noConsecutiveRepeats = true;
	private boolean completeCycle = true;
	private boolean countInEachTurn = true;
	private boolean intervalClock;
	private int turnMinutes = DEFAULT_TURN_MINUTES;
	private int breakSeconds = DEFAULT_BREAK_SECONDS;
	private int sessionMinutes = DEFAULT_SESSION_MINUTES;
	private boolean countInEveryPass;

	public TGPracticeUnitKind getUnit() {
		return this.unit;
	}

	public void setUnit(TGPracticeUnitKind unit) {
		this.unit = unit;
	}

	// how many units of the pool in a row a turn plays
	public int getUnitsPerBlock() {
		return this.unitsPerBlock;
	}

	public void setUnitsPerBlock(int unitsPerBlock) {
		this.unitsPerBlock = unitsPerBlock;
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

	public boolean isCountInEachTurn() {
		return this.countInEachTurn;
	}

	public void setCountInEachTurn(boolean countInEachTurn) {
		this.countInEachTurn = countInEachTurn;
	}

	public boolean isIntervalClock() {
		return this.intervalClock;
	}

	public void setIntervalClock(boolean intervalClock) {
		this.intervalClock = intervalClock;
	}

	public int getTurnMinutes() {
		return this.turnMinutes;
	}

	public void setTurnMinutes(int turnMinutes) {
		this.turnMinutes = turnMinutes;
	}

	public int getBreakSeconds() {
		return this.breakSeconds;
	}

	public void setBreakSeconds(int breakSeconds) {
		this.breakSeconds = breakSeconds;
	}

	public int getSessionMinutes() {
		return this.sessionMinutes;
	}

	public void setSessionMinutes(int sessionMinutes) {
		this.sessionMinutes = sessionMinutes;
	}

	public boolean isCountInEveryPass() {
		return this.countInEveryPass;
	}

	public void setCountInEveryPass(boolean countInEveryPass) {
		this.countInEveryPass = countInEveryPass;
	}
}
