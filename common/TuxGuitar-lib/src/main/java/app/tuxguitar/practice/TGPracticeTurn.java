package app.tuxguitar.practice;

/**
 * A turn of an interleaved practice session as the app shows it: its number in the session (from 1) and the unit it
 * plays (its kind and number); without the interval clock, the session's number of turns; with it (timed), whether it is
 * the break before the turn, and the seconds left to the turn's end or in the break, rounded up. The app words it in its
 * own language. Immutable.
 */
public final class TGPracticeTurn {

	// a timed session does not know its number of turns
	private static final int TIMED = 0;

	private final int turn;
	private final int turns;
	private final TGPracticeUnit unit;
	private final boolean inBreak;
	private final int secondsLeft;

	// a turn of a session of so many turns
	public TGPracticeTurn(int turn, int turns, TGPracticeUnit unit) {
		this(turn, turns, unit, false, 0);
	}

	private TGPracticeTurn(int turn, int turns, TGPracticeUnit unit, boolean inBreak, int secondsLeft) {
		this.turn = turn;
		this.turns = turns;
		this.unit = unit;
		this.inBreak = inBreak;
		this.secondsLeft = secondsLeft;
	}

	// a turn of a timed session, so many seconds before its end
	public static TGPracticeTurn timed(int turn, TGPracticeUnit unit, int secondsLeft) {
		return new TGPracticeTurn(turn, TIMED, unit, false, secondsLeft);
	}

	// the break before a turn of a timed session, so many seconds before its end
	public static TGPracticeTurn inBreak(int turn, TGPracticeUnit unit, int secondsLeft) {
		return new TGPracticeTurn(turn, TIMED, unit, true, secondsLeft);
	}

	public int getTurn() {
		return this.turn;
	}

	// the session's number of turns; 0 when it is timed
	public int getTurns() {
		return this.turns;
	}

	public TGPracticeUnit getUnit() {
		return this.unit;
	}

	// whether the interval clock times the session
	public boolean isTimed() {
		return this.turns == TIMED;
	}

	// whether it is the break before the turn (timed only)
	public boolean isBreak() {
		return this.inBreak;
	}

	// the seconds left to the turn's end, or in the break before it, rounded up (timed only; else 0)
	public int getSecondsLeft() {
		return this.secondsLeft;
	}
}
