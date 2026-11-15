package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.player.base.MidiPracticeTurn;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGSong;

/**
 * An interleaved practice session as it plays live: the turns its sequence holds so far (MidiPracticeTurn: one after the
 * other in the sequence's ticks, from its first), played as offline (TGTurn.unit: each its Count In, then its unit's
 * measures), on one timeline from the session's first turn. The transport tells where the sequence plays, in its
 * quarter notes; a turn moves its measures there from where the song has them, so in its Count In the song's quarter
 * note runs back from its first measure, as the timeline tells it.
 */
final class TGLiveTurns {

	private TGLiveTurns() {
	}

	// the turns played at the percent, from the timeline's sample 0
	static TGTimeline timeline(TGSong song, List<MidiPracticeTurn> laid, int percent, int sampleRate) {
		List<TGTurn> turns = new ArrayList<TGTurn>();
		for (int turn = 0; turn < laid.size(); turn++) {
			MidiPracticeTurn played = laid.get(turn);
			turns.add(TGTurn.unit(song, played.getFirst().getNumber(), played.getLast().getNumber(), turn, percent, played.getClicks(), sampleRate));
		}
		return TGTimeline.of(song, turns, sampleRate);
	}

	// the song's quarter note the sequence's plays (without turns, the song plays: the same one)
	static double songQuarters(List<MidiPracticeTurn> laid, double quarters) {
		return quarters - (laid.isEmpty() ? 0 : laid.get(turnAt(laid, quarters)).getMove() / (double) TGDuration.QUARTER_TIME);
	}

	// the timeline's sample (see timeline) the sequence's quarter note plays on: in the turn's measure that holds the
	// song's tick it plays, or before its first, in its Count In
	static long sampleAt(TGTimeline timeline, List<MidiPracticeTurn> laid, double quarters) {
		int turn = turnAt(laid, quarters);
		double tick = (songQuarters(laid, quarters) + 1) * TGDuration.QUARTER_TIME;
		int index = timeline.firstOf(turn);
		while (index + 1 < timeline.firstOf(turn + 1) && timeline.get(index + 1).getHeader().getStart() <= tick) {
			index++;
		}
		TGPlayedMeasure measure = timeline.get(index);
		double ticks = tick - measure.getHeader().getStart() - measure.getFrom();
		return timeline.start(index) + Math.round(ticks * 60.0 * timeline.getSampleRate() / (TGDuration.QUARTER_TIME * measure.getTempo()));
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
