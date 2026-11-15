package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;

/**
 * What a render keeps: a track's tap, its left side unless asked for another; where the player reports the song is,
 * the render's latency and the audio device's output latency behind the clock. The song is one measure of 4/4 at 120
 * bpm, played whole (88200 samples at 44100 Hz).
 */
public class TestRender {

	private final TGSongManager songManager = new TGSongManager();
	private final TGSong song = songManager.newSong();
	private final TGTrack track = song.getTrack(0);
	private final TGTimeline timeline = new TGTimeline(song, TGSongTime.play(song, new MidiPlayerMode(), song.getMeasureHeader(0).getStart(), Long.MAX_VALUE, 1), 44100);

	// latency 600, the track's tap 0.25 on the left and 0.5 on the right
	private TGRender render() {
		Map<Integer, float[][]> taps = new HashMap<Integer, float[][]>();
		taps.put(track.getChannelId(), new float[][] {{0.25f}, {0.5f}});
		return new TGRender(timeline, taps, new float[2][1], new float[2][1], 600, 0);
	}

	@Test
	public void aTracksTapIsItsLeftSideUnlessAskedForAnother() {
		assertEquals("0.25 0.5", render().getTap(track)[0] + " " + render().getTap(track, 1)[0]);
	}

	// with 1024 samples of output latency: 1624 behind the clock, after each block and at each sample
	@Test
	public void thePositionReportedIsTheLatenciesBehindTheClock() {
		TGReportedPositions behind = TGReportedPositions.at(timeline, 1624);

		assertEquals(behind.tickAt(20000) + " " + behind.tickAt(2048), render().getReportedAt(1024).tickAt(20000) + " " + render().getReported(1024)[3]);
	}
}
