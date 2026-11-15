package app.tuxguitar.practice;

/**
 * A turn of an interleaved practice session as the app shows it: its number in the session (from 1), the session's
 * number of turns, and the unit it plays (its kind and number). The app words it in its own language. Immutable.
 */
public final class TGPracticeTurn {

	private final int turn;
	private final int turns;
	private final TGPracticeUnit unit;

	public TGPracticeTurn(int turn, int turns, TGPracticeUnit unit) {
		this.turn = turn;
		this.turns = turns;
		this.unit = unit;
	}

	public int getTurn() {
		return this.turn;
	}

	public int getTurns() {
		return this.turns;
	}

	public TGPracticeUnit getUnit() {
		return this.unit;
	}
}
