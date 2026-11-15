package app.tuxguitar.practice;

import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;

/** Songs for the practice tests. */
final class PracticeSongs {

	private PracticeSongs() {
	}

	// a new song of that many measures, the given ones flagged: measure, difficulty, measure, difficulty...
	static TGSong song(int measures, int... flags) {
		TGSongManager manager = new TGSongManager();
		TGSong song = manager.newSong();
		while (song.countMeasureHeaders() < measures) {
			manager.addNewMeasureBeforeEnd(song);
		}
		for (int i = 0; i < flags.length; i += 2) {
			song.getMeasureHeader(flags[i] - 1).setDifficulty(flags[i + 1]);
		}
		return song;
	}
}
