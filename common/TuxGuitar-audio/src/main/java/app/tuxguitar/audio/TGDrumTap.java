package app.tuxguitar.audio;

import java.util.List;

import app.tuxguitar.midi.synth.impl.GervillDrums;
import app.tuxguitar.midi.synth.impl.GervillProcessor;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.util.TGContext;

/**
 * The drum tap: what the TuxGuitar Synthesizer's General MIDI drums sound over a timeline, outside every sound chain,
 * where they join the output mix, left and right. They click the Count In: each click's note reaches the drums on its
 * sample, is heard from it (Gervill's latency is taken out, see TGBlocks) and is held a beat. The Metronome clicks
 * here too (TGMetronome).
 */
public final class TGDrumTap {

	private TGDrumTap() {
	}

	public static float[][] render(TGContext context, TGTimeline timeline, MidiClick click) {
		return render(context, timeline, click, TGClicks.NONE);
	}

	// the Count In's clicks, then the Metronome's
	public static float[][] render(TGContext context, TGTimeline timeline, MidiClick click, TGClicks metronome) {
		float[][] tap = new float[2][(int) timeline.length()];
		List<TGClicks> all = List.of(timeline.getCountIn(), metronome);
		// no drums to open when nothing clicks
		if (all.stream().allMatch(clicks -> clicks.positions().length == 0)) {
			return tap;
		}
		GervillDrums drums = new GervillDrums(context, timeline.getSampleRate());
		for (TGClicks clicks : all) {
			long[] positions = clicks.positions();
			for (int each = 0; each < positions.length; each++) {
				drums.click(click, positions[each], clicks.hold(each));
			}
		}
		float[][] block = new float[2][TGBlocks.FRAMES];
		int latency = latency(timeline);
		for (long start = 0; start < timeline.length() + latency; start += TGBlocks.FRAMES) {
			drums.fill(block);
			TGBlocks.place(block, tap, start - latency);
		}
		drums.close();
		return tap;
	}

	// the drums' latency, Gervill's at the timeline's rate, whether or not they click
	public static int latency(TGTimeline timeline) {
		return GervillProcessor.latency(timeline.getSampleRate());
	}
}
