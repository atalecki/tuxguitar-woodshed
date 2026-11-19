package app.tuxguitar.audio;

import java.util.List;

import app.tuxguitar.midi.synth.impl.ClickProcessor;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiSampleClock;

/**
 * The drum tap: what the TuxGuitar Synthesizer's own drums sound over a timeline, outside every sound chain, where they
 * join the output mix, left and right: the built-in click (ClickProcessor), as live, told the click (its sounds and
 * volume, the user's own sounds from their folder). They click the Count In: each click reaches the drums on its sample
 * as live (MidiClick.getVelocity on the drums; the drum notes are for other MIDI Ports), beat 1's sound or the other
 * beats', and sounds from it, adding no latency. The Metronome clicks here too (TGMetronome).
 */
public final class TGDrumTap {

	private TGDrumTap() {
	}

	public static float[][] render(TGTimeline timeline, MidiClick click) {
		return render(timeline, click, TGClicks.NONE);
	}

	public static float[][] render(TGTimeline timeline, MidiClick click, TGClicks metronome) {
		return render(timeline, click, null, metronome);
	}

	// the Count In's clicks, then the Metronome's; the user's own sounds in the folder
	public static float[][] render(TGTimeline timeline, MidiClick click, String folder, TGClicks metronome) {
		float[][] tap = new float[2][(int) timeline.length()];
		ClickProcessor drums = new ClickProcessor();
		drums.setSampleRate(timeline.getSampleRate());
		drums.restoreParameters(click.toParameters(folder));
		for (TGClicks clicks : List.of(timeline.getCountIn(), metronome)) {
			long[] positions = clicks.positions();
			boolean[] accents = clicks.accents();
			for (int index = 0; index < positions.length; index++) {
				drums.click(click.getVelocity(MidiSampleClock.DRUMS, accents[index]), positions[index], accents[index]);
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
