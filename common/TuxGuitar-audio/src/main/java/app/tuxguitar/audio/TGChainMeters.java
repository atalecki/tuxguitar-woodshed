package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import app.tuxguitar.midi.synth.TGLevelTap;
import app.tuxguitar.midi.synth.TGLevels;

/**
 * The meters a sound chain shows (the Sound Chain panel's): one per row it follows (its elements, then the track's
 * output: TGSoundChainRows.getMeters), then the master. Each tick it reads the live levels' taps (never the processors,
 * so never their locks) and moves each meter (TGMeterBallistics). A row measuring nothing (an effect switched off, a
 * point not running yet, no live TuxGuitar Synthesizer) reads OFF. The state of each tap's meter is kept across rows
 * followed, so another track's lamp shows its clips when it is picked; every Play starting clears every lamp, and a
 * click clears its row's.
 */
public final class TGChainMeters {

	private List<TGLevels.Key> rows = Collections.emptyList();
	// the meter of each tap read, by the tap: kept across the rows followed
	private final Map<TGLevelTap, TGTapMeter> meters = new IdentityHashMap<TGLevelTap, TGTapMeter>();
	// the levels read last
	private TGLevels levels;
	// whether the player played at the last reading: shown while it plays, no Play has started yet
	private boolean playing = true;

	// the rows followed: the points read (null: none)
	public void follow(List<TGLevels.Key> rows) {
		this.rows = new ArrayList<TGLevels.Key>(rows);
	}

	// a reading per row followed, then the master's, at the time (milliseconds); the player playing or not. The live
	// levels, or null without a live TuxGuitar Synthesizer
	public List<TGMeterReading> next(TGLevels levels, long now, boolean playing) {
		this.levels = levels;
		if (playing && !this.playing) {
			this.clearAll();
		}
		this.playing = playing;
		return this.taps().stream().map(tap -> (tap != null ? this.meter(tap).read(now) : TGMeterReading.OFF)).collect(Collectors.toList());
	}

	// the row's lamp goes out (a click on its meter; the master's row is the last)
	public void clear(int row) {
		List<TGLevelTap> taps = this.taps();
		TGLevelTap tap = (row >= 0 && row < taps.size() ? taps.get(row) : null);
		if (tap != null) {
			this.meter(tap).clear();
		}
	}

	// Play started: every lamp goes out, those of rows not followed now too; the meters of taps gone go
	private void clearAll() {
		Collection<TGLevelTap> taps = (this.levels != null ? this.levels.taps() : Collections.<TGLevelTap>emptyList());
		this.meters.keySet().retainAll(taps);
		taps.forEach(tap -> this.meter(tap).clear());
	}

	// the tap of each row, then the master's; null for none
	private List<TGLevelTap> taps() {
		List<TGLevelTap> taps = this.rows.stream().map(this::tap).collect(Collectors.toList());
		taps.add(this.levels != null ? this.levels.master() : null);
		return taps;
	}

	private TGLevelTap tap(TGLevels.Key key) {
		return (this.levels != null && key != null ? this.levels.tap(key) : null);
	}

	private TGTapMeter meter(TGLevelTap tap) {
		return this.meters.computeIfAbsent(tap, key -> new TGTapMeter(key, key::getClips));
	}
}
