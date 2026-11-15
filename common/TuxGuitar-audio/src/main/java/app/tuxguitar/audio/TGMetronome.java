package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * The Metronome's clicks over a timeline: one on each beat (the time signature's denominator) of each measure played,
 * at the sample where its note reaches the TuxGuitar Synthesizer's General MIDI drums (outside every sound chain, see
 * TGDrumTap), each held a beat at its measure's tempo.
 */
public final class TGMetronome {

	private TGMetronome() {
	}

	public static TGClicks of(TGTimeline timeline) {
		List<Long> positions = new ArrayList<Long>();
		List<Long> holds = new ArrayList<Long>();
		for (int index = 0; index < timeline.count(); index++) {
			TGPlayedMeasure measure = timeline.get(index);
			long beat = measure.getHeader().getTimeSignature().getDenominator().getTime();
			// the first beat from where the measure starts playing
			for (long tick = (measure.getFrom() + beat - 1) / beat * beat; tick < measure.getTo(); tick += beat) {
				positions.add(timeline.sampleAt(index, tick));
				holds.add(timeline.sampleAt(index, tick + beat) - timeline.sampleAt(index, tick));
			}
		}
		return new TGClicks(positions, holds);
	}
}
