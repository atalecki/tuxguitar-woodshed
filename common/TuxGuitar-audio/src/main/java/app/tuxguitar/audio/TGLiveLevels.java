package app.tuxguitar.audio;

import java.util.function.BooleanSupplier;

import app.tuxguitar.midi.synth.TGLevels;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The levels of the synthesizer heard live: the TuxGuitar Synthesizer opened last, while it is the MIDI Port (offline
 * renders have their own, which nobody watches). Whoever shows meters watches them here: the synthesizer's levels are
 * measured while anyone does, those of a synthesizer opened later too.
 */
public final class TGLiveLevels {

	// whether the TuxGuitar Synthesizer is the MIDI Port
	private final BooleanSupplier heard;
	private TGLevels levels;
	// how many show meters; the live levels are watched once while anyone does
	private int watching;

	public static TGLiveLevels getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGLiveLevels.class.getName(), key -> new TGLiveLevels(() -> MidiPlayer.getInstance(key).isOutputPortOpen(TGAudioSession.SYNTHESIZER_PORT_KEY)));
	}

	TGLiveLevels(BooleanSupplier heard) {
		this.heard = heard;
	}

	// the synthesizer whose levels these are opens: it plays live while it is the MIDI Port
	public synchronized void play(TGLevels levels) {
		this.watched(false);
		this.levels = levels;
		this.watched(true);
	}

	// the live levels (if any) watched or no longer, while anyone shows meters
	private void watched(boolean on) {
		if (this.levels != null && this.watching > 0) {
			if (on) {
				this.levels.watch();
			} else {
				this.levels.unwatch();
			}
		}
	}

	// the live levels, or null while no TuxGuitar Synthesizer plays (another MIDI Port)
	public synchronized TGLevels get() {
		return (this.heard.getAsBoolean() ? this.levels : null);
	}

	// someone shows meters
	public synchronized void watch() {
		this.watched(false);
		this.watching ++;
		this.watched(true);
	}

	// one who showed meters no longer does
	public synchronized void unwatch() {
		this.watched(false);
		this.watching = Math.max(0, this.watching - 1);
		this.watched(true);
	}
}
