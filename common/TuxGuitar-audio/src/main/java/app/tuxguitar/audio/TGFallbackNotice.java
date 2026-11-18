package app.tuxguitar.audio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The quiet hint that a default instrument did not play (TGFallback): a fallback is told the first time it happens for
 * the song (and again once the defaults have changed), for MILLIS after or until its cause no longer holds, whichever
 * comes first; the last one told is read. Any thread may tell or read.
 */
final class TGFallbackNotice {

	// how long a fallback is told
	static final long MILLIS = 5000;

	// the song the fallbacks told were of (a song opened is another one), and those told
	private Object song;
	private final Set<TGFallback> told = new HashSet<TGFallback>();
	// the last one told, and when (milliseconds); null when none is
	private TGFallback latest;
	private long at;

	// the fallback happened playing the song at the time (milliseconds): true when it is told now, that is when it was
	// not yet for that song since the defaults changed
	synchronized boolean fellBack(TGFallback fallback, Object song, long now) {
		if (song != this.song) {
			this.song = song;
			this.told.clear();
		}
		if (!this.told.add(fallback)) {
			return false;
		}
		this.latest = fallback;
		this.at = now;
		return true;
	}

	// the defaults changed: each fallback is told again when it happens
	synchronized void defaultsChanged() {
		this.told.clear();
		this.latest = null;
	}

	// the last fallback told, said, at the time with these defaults and this scan; null for none
	synchronized String getText(long now, TGDefaultInstruments defaults, List<TGVst3Plugin> scanned) {
		boolean told = (this.latest != null && now - this.at < MILLIS && this.latest.holds(defaults, scanned));
		return (told ? this.latest.say() : null);
	}
}
