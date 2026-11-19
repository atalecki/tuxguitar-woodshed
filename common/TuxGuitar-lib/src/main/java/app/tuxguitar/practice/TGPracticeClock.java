package app.tuxguitar.practice;

/**
 * The interval clock of a session, times in microseconds: a turn loops its unit, pass after pass, until the turn's time
 * is up, counted from the start of its Count In, and the pass under way plays to its end, so a turn lasts its time or a
 * little more; a break follows every turn but the last; another turn follows only when a turn's end plus a break comes
 * before the session's time is up, counted from the session's start. With a Count In before every pass (only with
 * one before each turn), each pass counts its Count In; else only the first does. Immutable.
 */
public final class TGPracticeClock {

	private static final long SECOND = 1000000L;
	private static final long MINUTE = 60 * SECOND;

	private final long turn;
	private final int breakSeconds;
	private final long session;
	private final boolean countInEveryPass;

	private TGPracticeClock(TGPracticeSettings settings) {
		this.turn = settings.getTurnMinutes() * MINUTE;
		this.breakSeconds = settings.getBreakSeconds();
		this.session = settings.getSessionMinutes() * MINUTE;
		this.countInEveryPass = settings.isCountInEachTurn() && settings.isCountInEveryPass();
	}

	// the clock as set, or the reason it cannot time a session
	public static TGPracticeClock of(TGPracticeSettings settings) throws TGPracticeRefusal {
		if (settings.getTurnMinutes() < 1) {
			throw new TGPracticeRefusal("transport.practice.refusal.turn-minutes");
		}
		if (settings.getBreakSeconds() < 0) {
			throw new TGPracticeRefusal("transport.practice.refusal.break-seconds");
		}
		if (settings.getSessionMinutes() < settings.getTurnMinutes()) {
			throw new TGPracticeRefusal("transport.practice.refusal.session-minutes");
		}
		return new TGPracticeClock(settings);
	}

	// the passes a turn plays, its Count In (0: none) and its pass lasting so long: as many as start before its time is
	// up, at least one
	public int passes(long countIn, long pass) {
		return (int) Math.max(1, (this.countInEveryPass ? ceil(this.turn, countIn + pass) : ceil(this.turn - countIn, pass)));
	}

	// how long a turn lasts, its Count In (0: none) and its pass lasting so long
	public long length(long countIn, long pass) {
		int passes = this.passes(countIn, pass);
		return passes * pass + (this.countInEveryPass ? passes : 1) * countIn;
	}

	// whether another turn follows a turn that ends so long after the session's start
	public boolean follows(long end) {
		return (this.after(end) < this.session);
	}

	// when the turn after one that ends so long after the session's start starts: after a break
	public long after(long end) {
		return end + this.breakSeconds * SECOND;
	}

	public boolean isCountInEveryPass() {
		return this.countInEveryPass;
	}

	public int getBreakSeconds() {
		return this.breakSeconds;
	}

	// the most turns the session can hold: each but the last lasts at least a turn's time plus a break, and each starts
	// before the session's time is up
	public int mostTurns() {
		return (int) ceil(this.session, this.turn + this.breakSeconds * SECOND);
	}

	private static long ceil(long dividend, long divisor) {
		return (dividend + divisor - 1) / divisor;
	}
}
