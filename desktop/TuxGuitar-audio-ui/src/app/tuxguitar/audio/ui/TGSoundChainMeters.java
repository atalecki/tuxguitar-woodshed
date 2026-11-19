package app.tuxguitar.audio.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import app.tuxguitar.audio.TGChainMeters;
import app.tuxguitar.audio.TGLiveLevels;
import app.tuxguitar.audio.TGMeterReading;
import app.tuxguitar.midi.synth.TGLevels;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.util.TGContext;

/**
 * The Sound Chain panel's level meters: a meter under each button of the chain, then Track out and Master. While the
 * panel shows, the live levels are watched (measured) and read about 30 times a second (TGChainMeters decides what each
 * meter shows); hidden, nothing is measured or read. A click on a meter clears its lamp. Glue only; on the UI thread.
 */
final class TGSoundChainMeters {

	// about 30 readings a second
	private static final long TICK_MILLISECONDS = 33;

	private final TGContext context;
	private final UIControl panel;
	private final TGChainMeters meters = new TGChainMeters();
	// the meters shown, in the rows' order (the chain's elements, Track out), then Master; and what each is called
	private List<TGMeterCanvas> canvases = new ArrayList<TGMeterCanvas>();
	private List<String> names = new ArrayList<String>();
	// running while the panel shows
	private ScheduledExecutorService timer;

	TGSoundChainMeters(TGContext context, UIControl panel) {
		this.context = context;
		this.panel = panel;
	}

	// the meters of the rows followed now (TGSoundChainRows.getMeters), then Master's, and their names
	void follow(List<TGLevels.Key> rows, List<TGMeterCanvas> canvases, List<String> names) {
		this.meters.follow(rows);
		this.canvases = canvases;
		this.names = names;
		this.update();
	}

	// the panel shows or is hidden (or gone)
	void shown(boolean shown) {
		if (shown) {
			this.show();
		} else {
			this.hide();
		}
	}

	// the panel shows: the live levels are measured and read
	private void show() {
		if (this.timer == null) {
			TGLiveLevels.getInstance(this.context).watch();
			this.timer = TGAudioPlugin.scheduler("TuxGuitar levels");
			this.timer.scheduleAtFixedRate(() -> TGSoundChainButtons.later(this.context, this.panel, this::update), TICK_MILLISECONDS, TICK_MILLISECONDS, TimeUnit.MILLISECONDS);
		}
	}

	// the panel is hidden (or gone): nothing is measured or read
	private void hide() {
		if (this.timer != null) {
			this.timer.shutdownNow();
			this.timer = null;
			TGLiveLevels.getInstance(this.context).unwatch();
		}
	}

	// a click on the row's meter
	void clear(int row) {
		this.meters.clear(row);
		this.update();
	}

	private void update() {
		boolean playing = MidiPlayer.getInstance(this.context).isRunning();
		List<TGMeterReading> readings = this.meters.next(TGLiveLevels.getInstance(this.context).get(), System.currentTimeMillis(), playing);
		for (int meter = 0; meter < Math.min(readings.size(), this.canvases.size()); meter++) {
			this.canvases.get(meter).show(readings.get(meter), this.names.get(meter));
		}
	}
}
