package app.tuxguitar.practice;

/**
 * A turn as its session's schedule plans it: how many passes of its unit it plays, whether a Count In comes before
 * every pass or only the first (when Count In is on), and whether it is the session's last turn or, if not, how many
 * seconds of break come before the next. Immutable.
 */
public final class TGPracticePlan {

	private final int passes;
	private final boolean countInEveryPass;
	private final int breakSeconds;
	private final boolean last;

	TGPracticePlan(int passes, boolean countInEveryPass, int breakSeconds, boolean last) {
		this.passes = passes;
		this.countInEveryPass = countInEveryPass;
		this.breakSeconds = breakSeconds;
		this.last = last;
	}

	public int getPasses() {
		return this.passes;
	}

	public boolean isCountInEveryPass() {
		return this.countInEveryPass;
	}

	// the break before the next turn (0: none, also after the last turn)
	public int getBreakSeconds() {
		return this.breakSeconds;
	}

	public boolean isLast() {
		return this.last;
	}
}
