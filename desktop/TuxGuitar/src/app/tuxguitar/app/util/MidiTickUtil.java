package app.tuxguitar.app.util;

import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiSongTicks;
import app.tuxguitar.util.TGContext;

// the player's ticks and the song's, as lib's MidiSongTicks maps them (during a practice session the player's tick is
// the song's)
public class MidiTickUtil {

	public static long getStart(TGContext context, long tick){
		return MidiSongTicks.getStart(MidiPlayer.getInstance(context), TGDocumentManager.getInstance(context).getSong(), tick);
	}

	public static long getTick(TGContext context, long start){
		return MidiSongTicks.getTick(MidiPlayer.getInstance(context), TGDocumentManager.getInstance(context).getSong(), start);
	}

	public static int getSHeader(TGContext context) {
		return MidiPlayer.getInstance(context).getLoopSHeader();
	}

	public static int getEHeader(TGContext context) {
		return MidiPlayer.getInstance(context).getLoopEHeader();
	}
}
