package app.tuxguitar.util.log;

/**
 * Lets one message through per interval, and counts the ones held back, for code that runs many times a second (the
 * audio thread): {@code if (LOG.isTraceEnabled() && RATE.allow(System.nanoTime())) LOG.trace(...)}. It allocates
 * nothing. One thread uses each.
 */
public final class TGLogRate {

	private final long intervalNanos;
	private boolean started;
	private long next;
	private int heldBack;

	public TGLogRate(long intervalMillis) {
		this.intervalNanos = intervalMillis * 1000000L;
	}

	// true for the first call, then for the first call an interval or more after the last one let through
	public boolean allow(long nowNanos) {
		if (this.started && nowNanos - this.next < 0) {
			this.heldBack++;
			return false;
		}
		this.started = true;
		this.next = nowNanos + this.intervalNanos;
		return true;
	}

	// the calls refused since this was last asked
	public int takeHeldBack() {
		int count = this.heldBack;
		this.heldBack = 0;
		return count;
	}
}
