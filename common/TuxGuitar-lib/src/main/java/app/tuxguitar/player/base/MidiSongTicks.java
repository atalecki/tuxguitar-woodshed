package app.tuxguitar.player.base;

import java.util.function.BiPredicate;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * The player's ticks and the song's: the player plays the measures one after the other as the repeats say, from its
 * loop start, so a tick of the player is a tick of the song as written (a "start") in the measure that pass plays.
 * Moved from the desktop's MidiTickUtil (which the caret and the transport map the player's position through).
 */
public final class MidiSongTicks {

	private MidiSongTicks() {
	}

	// the song's tick the player's tick plays; before the loop start, the loop start; past the end, the player's tick
	// of the last measure played. During an interleaved practice session the player's tick is the song's (see
	// MidiPlayer.getTickPosition)
	public static long getStart(MidiPlayer player, TGSong song, long tick) {
		return (player.isPractising() ? tick : played(player, song, tick));
	}

	private static long played(MidiPlayer player, TGSong song, long tick) {
		long startPoint = startPoint(player);
		Played played = find(player, song, startPoint, (header, at) -> tick >= at && tick < at + header.getLength());
		if (played.found) {
			return played.header.getStart() + (tick - played.at);
		}
		return (tick < startPoint ? startPoint : played.at);
	}

	// the player's tick that plays the song's tick, on the first pass that plays its measure; before the loop start,
	// the loop start; in no measure played, the player's tick of the last measure played
	public static long getTick(MidiPlayer player, TGSong song, long start) {
		long startPoint = startPoint(player);
		Played played = find(player, song, startPoint, (header, at) -> start >= header.getStart() && start < header.getStart() + header.getLength());
		return (played.found || start >= startPoint ? played.at : startPoint);
	}

	private static long startPoint(MidiPlayer player) {
		player.updateLoop(false);
		return player.getLoopSPosition();
	}

	// the first measure played (from the loop's, as the repeats say) the test accepts, with the player's tick it starts
	// on; else the last one played
	private static Played find(MidiPlayer player, TGSong song, long startPoint, BiPredicate<TGMeasureHeader, Long> accepts) {
		Played played = new Played(startPoint);
		MidiRepeatController controller = new MidiRepeatController(song, player.getLoopSHeader(), player.getLoopEHeader());
		while (!controller.finished() && !played.found) {
			TGMeasureHeader header = song.getMeasureHeader(controller.getIndex());
			controller.process();
			if (controller.shouldPlay()) {
				played.next(header, accepts);
			}
		}
		return played;
	}

	private static final class Played {
		private long at;
		private long length;
		private TGMeasureHeader header;
		private boolean found;

		Played(long startPoint) {
			this.at = startPoint;
		}

		// the measure plays after the one before
		void next(TGMeasureHeader played, BiPredicate<TGMeasureHeader, Long> accepts) {
			this.at += this.length;
			this.length = played.getLength();
			this.header = played;
			this.found = accepts.test(played, this.at);
		}
	}
}
