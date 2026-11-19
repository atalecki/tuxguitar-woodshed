package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.song.models.TGTimeSignature;

/**
 * The Metronome's clicks over a timeline: one on each beat (the time signature's denominator) of each measure played,
 * at the sample where its note reaches the TuxGuitar Synthesizer's own drums (the built-in click, outside every sound
 * chain, see TGDrumTap); beat 1 of each measure accented (MidiClick.isAccented), as live.
 */
public final class TGMetronome {

	private TGMetronome() {
	}

	public static TGClicks of(TGTimeline timeline) {
		List<Long> positions = new ArrayList<Long>();
		List<Boolean> accents = new ArrayList<Boolean>();
		for (int index = 0; index < timeline.count(); index++) {
			TGPlayedMeasure measure = timeline.get(index);
			TGTimeSignature signature = measure.getHeader().getTimeSignature();
			long beat = signature.getDenominator().getTime();
			// the first beat from where the measure starts playing
			for (long tick = (measure.getFrom() + beat - 1) / beat * beat; tick < measure.getTo(); tick += beat) {
				positions.add(timeline.sampleAt(index, tick));
				accents.add(MidiClick.isAccented((int) (tick / beat), signature.getNumerator()));
			}
		}
		return new TGClicks(positions, accents);
	}
}
