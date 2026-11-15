package app.tuxguitar.practice;

/**
 * A turn of an interleaved practice session as the app shows it: its number in the session (from 1) and the units its
 * plan plays, from one to another (their kind and numbers; the same unit, when it plays one); without the interval clock, the session's number of turns; with it (timed), whether it is
 * the break before the turn, and the seconds left to the turn's end or in the break, rounded up. The app words it in its
 * own language. Immutable.
 */
public final class TGPracticeTurn {

	// a timed session does not know its number of turns
	private static final int TIMED = 0;

	private final int turn;
	private final int turns;
	private final TGPracticeUnit from;
	private final TGPracticeUnit to;
	private final boolean inBreak;
	private final int secondsLeft;

	// a turn of a session of so many turns, as planned
	public TGPracticeTurn(int turn, int turns, TGPracticePlan plan) {
		this(turn, turns, plan, false, 0);
	}

	private TGPracticeTurn(int turn, int turns, TGPracticePlan plan, boolean inBreak, int secondsLeft) {
		this.turn = turn;
		this.turns = turns;
		this.from = plan.getFrom();
		this.to = plan.getTo();
		this.inBreak = inBreak;
		this.secondsLeft = secondsLeft;
	}

	// a turn of a timed session, as planned, so many seconds before its end
	public static TGPracticeTurn timed(int turn, TGPracticePlan plan, int secondsLeft) {
		return new TGPracticeTurn(turn, TIMED, plan, false, secondsLeft);
	}

	// the break before a turn of a timed session, as planned, so many seconds before its end
	public static TGPracticeTurn inBreak(int turn, TGPracticePlan plan, int secondsLeft) {
		return new TGPracticeTurn(turn, TIMED, plan, true, secondsLeft);
	}

	public int getTurn() {
		return this.turn;
	}

	// the session's number of turns; 0 when it is timed
	public int getTurns() {
		return this.turns;
	}

	// the first unit it plays
	public TGPracticeUnit getFrom() {
		return this.from;
	}

	// the last unit it plays (the first, when it plays one)
	public TGPracticeUnit getTo() {
		return this.to;
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
