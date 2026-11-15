package app.tuxguitar.audio.features;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.audio.Meters;

/**
 * The notes a track sounds with Test Sine (a sine at each note's pitch, from phase 0 on its first sample), beat by beat,
 * turn by turn, as the features README measures them: a beat's note is the MIDI note nearest the tone it sounds over
 * the beat (clear of its first and last EDGE samples), when within 1% of it; it starts where a sine at that note's
 * pitch fits the FIT samples after the beat's first EDGE best (Meters.sineStart, NEAR samples around the beat).
 */
final class HeardNotes {

	static final int NONE = -1;

	private static final int EDGE = 64;
	private static final int FIT = 2048;
	private static final int NEAR = 8;

	private HeardNotes() {
	}

	// a beat as the track sounds it
	static final class Note {
		// where the beat that played sounds, how long it lasts, at what Percentage of Tempo
		final Heard.PlayedBeat beat;
		// the MIDI note heard (NONE when no tone is within 1% of one), where its sine starts (Integer.MIN_VALUE: nowhere
		// near the beat)
		final int note;
		final int start;

		Note(Heard.PlayedBeat beat, int note, int start) {
			this.beat = beat;
			this.note = note;
			this.start = start;
		}

		// where the next beat would start: the end of this one, from where it starts
		int end() {
			return this.start + this.beat.length;
		}

		public String toString() {
			return this.note + "@" + this.start;
		}
	}

	// the notes of each turn, a list per turn
	static List<List<Note>> of(Heard heard, float[] tap) {
		List<List<Note>> turns = new ArrayList<List<Note>>();
		for (List<Heard.PlayedBeat> beats : heard.turnBeats()) {
			List<Note> notes = new ArrayList<Note>();
			beats.forEach(beat -> notes.add(note(tap, beat)));
			turns.add(notes);
		}
		return turns;
	}

	private static Note note(float[] tap, Heard.PlayedBeat beat) {
		double frequency = Meters.frequency(tap, beat.sample + EDGE, beat.sample + beat.length - EDGE, TestInterface.SAMPLE_RATE);
		int note = nearest(frequency);
		int start = (note == NONE ? Integer.MIN_VALUE
			: Meters.sineStart(tap, beat.sample + EDGE, beat.sample + EDGE + FIT, pitch(note), TestInterface.SAMPLE_RATE, beat.sample, NEAR));
		return new Note(beat, note, start);
	}

	// the MIDI note within 1% of the frequency; NONE when none is (no tone)
	static int nearest(double frequency) {
		if (frequency <= 0) {
			return NONE;
		}
		int note = (int) Math.round(69 + 12 * Math.log(frequency / 440) / Math.log(2));
		return (Math.abs(frequency - pitch(note)) <= pitch(note) / 100 ? note : NONE);
	}

	// the frequency of the MIDI note, equal-tempered from A4 at 440 Hz
	static double pitch(int note) {
		return 440 * Math.pow(2, (note - 69) / 12.0);
	}

	// where a sine at the note's pitch starts, so many samples around near, fitting the samples from..to
	static int sineStart(float[] tap, int note, int from, int to, int near) {
		return Meters.sineStart(tap, from, to, pitch(note), TestInterface.SAMPLE_RATE, near, NEAR);
	}
}
