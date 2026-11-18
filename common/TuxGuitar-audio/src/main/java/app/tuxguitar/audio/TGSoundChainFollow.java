package app.tuxguitar.audio;

import java.util.Arrays;
import java.util.Objects;

import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGContext;

/**
 * A sound chain editor that follows the caret's track (the Sound Chain panel): it reads the rows again (which asks what
 * the default instrument plays, TGDefaultInstrumentSource.play: that touches the disk) only when what it shows changed,
 * told by a key read cheaply on each update of the app: the song and the track (its position, its name), the track's
 * channel and that channel's instrument (its kind decides the default), the chain as its names read (TGSoundChain.read:
 * not the plugins' states, which an editor's gestures change), and the default instruments' stamp. After reading again,
 * another track picks its source; the same track keeps the row picked while it is there, as the Sound Chain dialog keeps
 * it.
 */
public final class TGSoundChainFollow {

	private TGSoundChainFollow() {
	}

	// what an editor shows, read at one moment; NONE: no track
	public static final class Key {

		public static final Key NONE = new Key(null, null, new Object[0]);

		private final TGTrack track;
		private final TGChannel channel;
		// what it is compared by
		private final Object[] shown;

		private Key(TGTrack track, TGChannel channel, Object[] shown) {
			this.track = track;
			this.channel = channel;
			this.shown = shown;
		}

		// the track's name; none's for no track
		public String title(String none) {
			return (this.track != null ? this.track.getName() : none);
		}

		// the rows of the track's chain, a default source saying what plays and why (this touches the disk)
		public TGSoundChainRows rows(TGContext context) {
			return (this.channel != null ? TGSoundChainRows.of(this.channel, TGDefaultInstrumentSource.install(context).play(this.channel)) : TGSoundChainRows.NONE);
		}

		private boolean isTrack(Key other) {
			return Arrays.equals(Arrays.copyOf(this.shown, 3), Arrays.copyOf(other.shown, 3));
		}
	}

	public static Key key(TGTrack track, TGChannel channel, long stamp) {
		TGSong song = track.getSong();
		return new Key(track, channel, new Object[] {new Identity(song), track.getNumber(), channel.getChannelId(), track.getName(), channel.getBank(), channel.getProgram(), TGSoundChain.read(channel), stamp});
	}

	// the caret's track (null: none) as the app's song has it now
	public static Key key(TGContext context, TGTrack track) {
		return (track != null ? key(track, channel(context, track), TGDefaultInstrumentSource.install(context).getStamp()) : Key.NONE);
	}

	private static TGChannel channel(TGContext context, TGTrack track) {
		TGDocumentManager document = TGDocumentManager.getInstance(context);
		return document.getSongManager().getChannel(document.getSong(), track.getChannelId());
	}

	public static boolean changed(Key shown, Key now) {
		return !Arrays.equals(shown.shown, now.shown);
	}

	// the row picked once now is shown (-1: none): the source on another track (on none, none); on the same song,
	// track and channel the one picked before, while the chain has that row
	public static int picked(Key shown, Key now, Integer before, int rows) {
		return (shown.isTrack(now) ? kept(TGSoundChainRows.row(before), rows) : first(rows));
	}

	private static int kept(int row, int rows) {
		return (row < rows ? row : -1);
	}

	private static int first(int rows) {
		return (rows > 0 ? 0 : -1);
	}

	// a song compared as itself (another song is another key, even one equal to it)
	private static final class Identity {

		private final Object object;

		Identity(Object object) {
			this.object = object;
		}

		public boolean equals(Object other) {
			return (other instanceof Identity && ((Identity) other).object == this.object);
		}

		public int hashCode() {
			return Objects.hashCode(System.identityHashCode(this.object));
		}
	}
}
