package app.tuxguitar.benchmarks;

import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGVoice;

/**
 * The songs the benchmarks play: a new song (one 4/4 track at 120 bpm) of so many measures, each a run of sixteenth
 * notes, two strings at once, so a measure is 32 notes: busier than most guitar parts, so the numbers err on the slow
 * side.
 */
final class BenchSongs {

	private static final int SIXTEENTHS = 16;

	private BenchSongs() {
	}

	static TGSong song(TGSongManager manager, int measures) {
		TGSong song = manager.newSong();
		grow(manager, song, measures);
		for (int index = 0; index < measures; index++) {
			fill(manager, song.getTrack(0).getMeasure(index));
		}
		return song;
	}

	private static void grow(TGSongManager manager, TGSong song, int measures) {
		while (song.countMeasureHeaders() < measures) {
			manager.addNewMeasureBeforeEnd(song);
		}
	}

	private static void fill(TGSongManager manager, TGMeasure measure) {
		TGFactory factory = manager.getFactory();
		long sixteenth = TGDuration.WHOLE_PRECISE_DURATION / SIXTEENTHS;
		for (int i = 0; i < SIXTEENTHS; i++) {
			TGBeat beat = factory.newBeat();
			beat.setPreciseStart(measure.getPreciseStart() + i * sixteenth);
			TGVoice voice = beat.getVoice(0);
			voice.setEmpty(false);
			voice.getDuration().setValue(TGDuration.SIXTEENTH);
			voice.addNote(note(factory, 1, i % 12));
			voice.addNote(note(factory, 2, (i + 3) % 12));
			manager.getMeasureManager().addBeat(measure, beat);
		}
		manager.getMeasureManager().orderBeats(measure);
	}

	private static TGNote note(TGFactory factory, int string, int fret) {
		TGNote note = factory.newNote();
		note.setString(string);
		note.setValue(fret);
		return note;
	}
}
