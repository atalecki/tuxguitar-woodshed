package app.tuxguitar.midi.synth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The level taps of a synthesizer: one after each element of each channel's sound chain, one at each channel's output
 * (after its Volume and Balance, what joins the mix) and one at the master. A chain's source is tapped by its role, so
 * the default instrument and the kept source are one point; an effect by its element id, so a moved effect keeps its
 * tap. Taps outlive a channel's closing (they stop receiving blocks), so a chain that reopens keeps its clip counts;
 * those of effects no longer in a chain go when that chain opens again.
 *
 * The thread that opens chains makes the taps (a copy-on-write map); the audio thread measures into them only while
 * someone watches; any thread reads them without a lock.
 */
public final class TGLevels {

	public enum Point { SOURCE, EFFECT, OUT }

	// one point of a channel's sound
	public static final class Key {
		private final int channel;
		private final Point point;
		private final String effect;

		private Key(int channel, Point point, String effect) {
			this.channel = channel;
			this.point = point;
			this.effect = effect;
		}

		public static Key source(int channel) {
			return new Key(channel, Point.SOURCE, null);
		}

		public static Key effect(int channel, String effect) {
			return new Key(channel, Point.EFFECT, effect);
		}

		public static Key out(int channel) {
			return new Key(channel, Point.OUT, null);
		}

		public boolean equals(Object other) {
			if(!(other instanceof Key)) {
				return false;
			}
			Key key = (Key) other;
			return (this.channel == key.channel && this.point == key.point && Objects.equals(this.effect, key.effect));
		}

		// an effect of the channel, not among the effects
		private boolean isDropped(int channel, Collection<String> effects) {
			return (this.channel == channel && this.point == Point.EFFECT && !effects.contains(this.effect));
		}

		public int hashCode() {
			return Objects.hash(this.channel, this.point, this.effect);
		}
	}

	private final TGLevelTap master = new TGLevelTap();
	// replaced whole, never changed: a reader takes the map as it is
	private volatile Map<Key, TGLevelTap> taps = Collections.emptyMap();
	private final AtomicInteger watching = new AtomicInteger();

	public TGLevelTap master() {
		return this.master;
	}

	// the point's tap; null while that point has never been part of an open chain
	public TGLevelTap tap(Key key) {
		return this.taps.get(key);
	}

	// every tap there is now, the master's too
	public Collection<TGLevelTap> taps() {
		List<TGLevelTap> taps = new ArrayList<TGLevelTap>(this.taps.values());
		taps.add(this.master);
		return taps;
	}

	// the point's tap, made when missing: by the thread that opens chains, never by the audio thread
	public synchronized TGLevelTap open(Key key) {
		TGLevelTap tap = this.taps.get(key);
		if( tap == null ) {
			tap = new TGLevelTap();
			Map<Key, TGLevelTap> taps = new HashMap<Key, TGLevelTap>(this.taps);
			taps.put(key, tap);
			this.taps = Collections.unmodifiableMap(taps);
		}
		return tap;
	}

	// the channel's chain now has these effects: the taps of its other effects go
	synchronized void keep(int channel, Collection<String> effects) {
		Map<Key, TGLevelTap> taps = new HashMap<Key, TGLevelTap>(this.taps);
		taps.keySet().removeIf(key -> key.isDropped(channel, effects));
		this.taps = Collections.unmodifiableMap(taps);
	}

	// someone shows the levels: they are measured while anyone does
	public void watch() {
		this.watching.incrementAndGet();
	}

	// one who watched no longer does; never below nobody
	public void unwatch() {
		this.watching.updateAndGet(watching -> Math.max(0, watching - 1));
	}

	// one volatile read: the audio thread asks it every block
	public boolean isOn() {
		return (this.watching.get() > 0);
	}
}
