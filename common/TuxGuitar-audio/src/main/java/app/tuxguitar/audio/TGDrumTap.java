package app.tuxguitar.audio;

import java.util.List;

import app.tuxguitar.midi.synth.impl.ClickProcessor;
import app.tuxguitar.player.base.MidiClick;

/**
 * The drum tap: what the TuxGuitar Synthesizer's own drums sound over a timeline, outside every sound chain, where they
 * join the output mix, left and right: the built-in click (ClickProcessor), as live. They click the Count In: each
 * click reaches the drums on its sample at the click sound's velocity (its drum note is for other MIDI Ports) and
 * sounds from it, adding no latency. The Metronome clicks here too (TGMetronome).
 */
public final class TGDrumTap {

	private TGDrumTap() {
	}

	public static float[][] render(TGTimeline timeline, MidiClick click) {
		return render(timeline, click, TGClicks.NONE);
	}

	// the Count In's clicks, then the Metronome's
	public static float[][] render(TGTimeline timeline, MidiClick click, TGClicks metronome) {
		float[][] tap = new float[2][(int) timeline.length()];
		ClickProcessor drums = new ClickProcessor();
		drums.setSampleRate(timeline.getSampleRate());
		for (TGClicks clicks : List.of(timeline.getCountIn(), metronome)) {
			for (long position : clicks.positions()) {
				drums.click(click.getVelocity(), position);
			}
		}
		float[][] block = new float[2][TGBlocks.FRAMES];
		for (long start = 0; start < timeline.length(); start += TGBlocks.FRAMES) {
			drums.process(block);
			TGBlocks.place(block, tap, start);
		}
		drums.close();
		return tap;
	}
}
