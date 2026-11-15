package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.player.base.MidiPracticeTurn;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * An interleaved practice session as it plays live: the passes its sequence holds so far (MidiPracticeTurn: one after the
 * other in the sequence's ticks, from its first), played as offline (TGTurn.unit: each its Count In, then its unit's
 * measures; a turn's first pass after the break before it, the others continuing it), on one timeline from the
 * session's first turn. The transport tells where the sequence plays, in its
 * quarter notes; a turn moves its measures there from where the song has them, so in its Count In the song's quarter
 * note runs back from its first measure, as the timeline tells it.
 */
final class TGLiveTurns {

	private TGLiveTurns() {
	}

	// the passes played, each at its tempo scale, from the timeline's sample 0
	static TGTimeline timeline(TGSong song, List<MidiPracticeTurn> laid, int sampleRate) {
		List<TGTurn> turns = new ArrayList<TGTurn>();
		for (int index = 0; index < laid.size(); index++) {
			turns.add(pass(song, laid, index, sampleRate));
		}
		return TGTimeline.of(song, turns, sampleRate);
	}

	// the pass (by index) as a turn of the timeline: one that continues the turn of the pass before it, or one that opens
	// a turn after its break, as long as the sequence's ticks of it last at its first measure's tempo
	private static TGTurn pass(TGSong song, List<MidiPracticeTurn> laid, int index, int sampleRate) {
		MidiPracticeTurn played = laid.get(index);
		TGTurn unit = TGTurn.unit(song, played.getFirst().getNumber(), played.getLast().getNumber(), index, played.getScale(), header -> played.getClicks(), sampleRate);
		if (index > 0 && laid.get(index - 1).getTurn() == played.getTurn()) {
			return TGTurn.pass(unit.getCountIn(), unit.getMeasures());
		}
		long rest = TGSampleTime.samples(played.getRest(), TGSampleTime.tempo(played.getFirst(), played.getScale()), sampleRate);
		return TGTurn.afterBreak(rest, unit.getCountIn(), unit.getMeasures());
	}

	// the song's quarter note the sequence's plays (without turns, the song plays: the same one)
	static double songQuarters(List<MidiPracticeTurn> laid, double quarters) {
		return quarters - (laid.isEmpty() ? 0 : laid.get(turnAt(laid, quarters)).getMove() / (double) TGDuration.QUARTER_TIME);
	}

	// the timeline's sample (see timeline) the sequence's quarter note plays on: from where its turn's first measure
	// starts, back by the song's ticks before it (its Count In), on by those of each of the turn's measures it has
	// reached (whole measures, in score order), each at its measure's tempo
	static long sampleAt(TGTimeline timeline, List<MidiPracticeTurn> laid, double quarters) {
		int turn = turnAt(laid, quarters);
		double tick = (songQuarters(laid, quarters) + 1) * TGDuration.QUARTER_TIME;
		int first = timeline.firstOf(turn);
		long sample = timeline.start(first) + samples(timeline, first, Math.min(0, tick - timeline.get(first).getHeader().getStart()));
		for (int index = first; index < timeline.firstOf(turn + 1); index++) {
			TGMeasureHeader header = timeline.get(index).getHeader();
			sample += samples(timeline, index, Math.max(0, Math.min(header.getLength(), tick - header.getStart())));
		}
		return sample;
	}

	// the samples the ticks last at the played measure's tempo
	private static long samples(TGTimeline timeline, int index, double ticks) {
		return Math.round(ticks * 60.0 * timeline.getSampleRate() / (TGDuration.QUARTER_TIME * timeline.get(index).getTempo()));
	}

	// the turn the sequence's quarter note is in: the last one that starts there or before (the first, before them all)
	private static int turnAt(List<MidiPracticeTurn> laid, double quarters) {
		double tick = (quarters + 1) * TGDuration.QUARTER_TIME;
		int turn = 0;
		while (turn + 1 < laid.size() && laid.get(turn + 1).getStart() <= tick) {
			turn++;
		}
		return turn;
	}
}
