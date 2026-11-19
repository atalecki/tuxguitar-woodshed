package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.impl.ClickProcessor;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiClickSound;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerCountDown;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.properties.TGPropertiesManager;

/**
 * The drum tap over a timeline: from its first sample, the built-in click on each Count In click's sample (and the
 * Metronome's), with the click's sounds at its volume, as live, whatever their drum notes, beat 1's sound on beat 1 (the
 * Count In's first click, the Metronome's first of each measure); silence elsewhere, and without a Count In. The
 * song has 2 measures of 4/4 at 120 bpm, played from measure 2 at 44100 Hz.
 */
public class TestDrumTap {

	private final TGContext context = new TGContext();
	private final TGSongManager songManager = new TGSongManager();
	private final TGSong song = songManager.newSong();
	private final MidiPlayerCountDown countDown = MidiPlayer.getInstance(context).getCountDown();

	public TestDrumTap() {
		TGPropertiesManager.getInstance(context).setPropertiesFactory(MapProperties::new);
		songManager.addNewMeasureBeforeEnd(song);
	}

	private TGTimeline fromMeasure2() {
		List<TGPlayedMeasure> played = TGSongTime.play(song, new MidiPlayerMode(), song.getMeasureHeader(1).getStart(), Long.MAX_VALUE, 1);
		return new TGTimeline(song, played, 44100, TGCountIn.before(countDown, played, 44100));
	}

	// the code-made Click at the volume (dB) on each sample (cut at the timeline's end), left and right alike, accented
	// on the samples of beat 1
	private static float[] clicks(TGTimeline timeline, int volume, java.util.Set<Long> accented, long... positions) {
		float[] expected = new float[(int) timeline.length()];
		float gain = (float) Math.pow(10, volume / 20.0);
		for (long position : positions) {
			float[] click = ClickProcessor.sound(44100, 127, accented.contains(position));
			for (int i = 0; i < Math.min(click.length, expected.length - position); i++) {
				expected[(int) position + i] = gain * click[i];
			}
		}
		return expected;
	}

	private static void assertSides(float[] expected, float[][] tap) {
		assertTrue(Meters.peak(expected, 0, expected.length) > 0.1, "the expected clicks sound");
		assertArrayEquals(expected, tap[0]);
		assertArrayEquals(expected, tap[1]);
	}

	// one Count In tick: one click, from the first sample on (no latency); plain, the beat before beat 1 being beat 4
	@Test
	public void isTheBuiltInClickOnTheCountInsClick() {
		countDown.setEnabled(true);
		countDown.setTickCount(1);
		TGTimeline timeline = fromMeasure2();

		assertSides(clicks(timeline, -3, java.util.Set.of(), 0), TGDrumTap.render(timeline, MidiClick.DEFAULT));
	}

	// the volume sets its level
	@Test
	public void isTheBuiltInClickAtTheClickVolume() {
		countDown.setEnabled(true);
		countDown.setTickCount(1);
		TGTimeline timeline = fromMeasure2();

		assertSides(clicks(timeline, 0, java.util.Set.of(), 0), TGDrumTap.render(timeline, MidiClick.sameAccented(MidiClickSound.CLICK, 0)));
	}

	// a recording sounds as long as it is: the wood block still sounds 0.3 s on, long after the code-made Click's 30 ms
	@Test
	public void playsTheClicksRecordings() {
		countDown.setEnabled(true);
		countDown.setTickCount(1);
		TGTimeline timeline = fromMeasure2();
		float[][] woodBlock = TGDrumTap.render(timeline, MidiClick.sameAccented(MidiClickSound.WOOD_BLOCK, 0));
		float[][] click = TGDrumTap.render(timeline, MidiClick.DEFAULT);

		assertEquals("true 0.0", (Meters.peak(woodBlock[0], 13000, 14000) > 0.001) + " " + Meters.peak(click[0], 13000, 14000));
	}

	// the user's own sound from the folder of own sounds; with no folder it cannot be read, and the Click plays
	@Test
	public void playsTheUsersOwnSoundFromTheirFolder(@org.junit.jupiter.api.io.TempDir java.io.File folder) throws Exception {
		try (java.io.InputStream clap = ClickProcessor.class.getResourceAsStream("click-sounds/clap.wav")) {
			java.nio.file.Files.copy(clap, new java.io.File(folder, "Mine.wav").toPath());
		}
		countDown.setEnabled(true);
		countDown.setTickCount(1);
		TGTimeline timeline = fromMeasure2();
		MidiClick own = MidiClick.sameAccented(MidiClickSound.own("Mine.wav"), 0);
		float[][] mine = TGDrumTap.render(timeline, own, folder.getPath(), TGClicks.NONE);

		assertEquals("true", Boolean.toString(Meters.peak(mine[0], 1800, 2500) > 0.1));
		assertSides(clicks(timeline, 0, java.util.Set.of(), 0), TGDrumTap.render(timeline, own, null, TGClicks.NONE));
	}

	// stopped 1000 samples in, just after the Count In's click: the tap ends there, holding what the click sounds by then
	@Test
	public void aClickSoundsToTheLastSample() {
		countDown.setEnabled(true);
		countDown.setTickCount(1);
		TGTimeline timeline = fromMeasure2().until(1000);

		assertEquals(1000, timeline.length());
		assertSides(clicks(timeline, -3, java.util.Set.of(), 0), TGDrumTap.render(timeline, MidiClick.DEFAULT));
	}

	// as many Count In clicks as Metronome clicks (4 each, a beat of 22050 samples apart): the Metronome's on measure 2's
	// beats
	@Test
	public void clicksTheCountInThenTheMetronome() {
		countDown.setEnabled(true);
		countDown.setTickCount(4);
		TGTimeline timeline = fromMeasure2();
		long[] beats = TGMetronome.of(timeline).positions();

		assertEquals("[88200, 110250, 132300, 154350]", Arrays.toString(beats));
		assertSides(clicks(timeline, -3, java.util.Set.of(0L, 88200L), 0, 22050, 44100, 66150, 88200, 110250, 132300, 154350), TGDrumTap.render(timeline, MidiClick.DEFAULT, TGMetronome.of(timeline)));
	}

	@Test
	public void isSilentWithoutACountIn() {
		float[][] tap = TGDrumTap.render(fromMeasure2(), MidiClick.DEFAULT);

		assertEquals(0, Meters.peak(tap[0], 0, tap[0].length) + Meters.peak(tap[1], 0, tap[1].length), 0);
	}
}
