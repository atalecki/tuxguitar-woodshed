package app.tuxguitar.midi.synth.impl;

import static app.tuxguitar.midi.synth.Signals.peak;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.TGAudioBuffer;
import app.tuxguitar.midi.synth.TGMidiProcessor;
import app.tuxguitar.midi.synth.TGSynthManager;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiClickSound;
import app.tuxguitar.util.TGContext;

/**
 * The built-in click, the sound of Count In and the Metronome on the TuxGuitar Synthesizer's own drums, as the channel
 * plays it: notes sent, then blocks of 512 frames processed. A 1500 Hz sine from phase 0, rising over 0.2 ms and
 * falling by e every 5 ms, silent 30 ms after it starts, at 0.5 times the velocity / 127; the same left and right and
 * for every note; from the frame its note reaches it, with no latency.
 */
public class TestClickProcessor {

	private static final int FRAMES = TGAudioBuffer.BUFFER_SIZE / 2;
	private static final int BLOCKS = 4;
	// above this a sample is sounding: -60 dBFS
	private static final float SOUNDING = 0.001f;

	// a note on the frame, at the velocity
	private static final class Note {
		private final int key;
		private final int velocity;
		private final long frame;

		private Note(int key, int velocity, long frame) {
			this.key = key;
			this.velocity = velocity;
			this.frame = frame;
		}
	}

	private static Note note(int velocity, long frame) {
		return new Note(37, velocity, frame);
	}

	// left and right of the blocks the processor fills at the rate, the notes sent before the first: they click
	private static float[][] play(float sampleRate, Note... notes) {
		return play(new ClickProcessor(), sampleRate, notes);
	}

	// told the parameters first (a click's, see MidiClick.toParameters)
	private static float[][] told(Map<String, String> parameters, float sampleRate, Note... notes) {
		ClickProcessor click = new ClickProcessor();
		click.restoreParameters(parameters);
		return play(click, sampleRate, notes);
	}

	private static float[][] play(ClickProcessor click, float sampleRate, Note... notes) {
		click.setSampleRate(sampleRate);
		for (Note note : notes) {
			click.sendNoteOn(note.key, note.velocity, -1, false, note.frame);
		}
		float[][] sides = blocks(click);
		assertTrue(peak(sides[0], 0, sides[0].length) > SOUNDING, "the notes click");
		return sides;
	}

	// one block, filled again each time: the processor writes over what it held
	private static float[][] blocks(TGMidiProcessor processor) {
		float[][] sides = new float[2][BLOCKS * FRAMES];
		float[][] block = new float[2][FRAMES];
		for (int b = 0; b < BLOCKS; b++) {
			processor.process(block);
			copy(block, sides, b * FRAMES);
		}
		return sides;
	}

	// the block's sides into the sides, from the frame on
	private static void copy(float[][] block, float[][] sides, int at) {
		for (int side = 0; side < sides.length; side++) {
			System.arraycopy(block[side], 0, sides[side], at, block[side].length);
		}
	}

	private static float[] left(float sampleRate, Note... notes) {
		return play(sampleRate, notes)[0];
	}

	// its root mean square: a sine's, whatever its frequency, is its level over the square root of 2
	private static double rms(float[] samples, int from, int to) {
		return Math.sqrt(IntStream.range(from, to).mapToDouble(i -> samples[i] * samples[i]).sum() / (to - from));
	}

	// how often a sound crosses zero going up, per second, from its first crossing over the frames to its last
	private static double frequency(float[] samples, int from, int to, float sampleRate) {
		int[] up = IntStream.range(from + 1, to).filter(i -> samples[i - 1] <= 0 && samples[i] > 0).toArray();
		return (up.length - 1) * sampleRate / (up[up.length - 1] - up[0]);
	}

	private static float[] shifted(float[] samples, int by) {
		float[] shifted = new float[samples.length];
		System.arraycopy(samples, 0, shifted, by, samples.length - by);
		return shifted;
	}

	@Test
	public void aNoteIsSilentBeforeItsFrameAndClicksFromIt() {
		float[] left = left(44100, note(127, 300));

		assertEquals(0, peak(left, 0, 301), 0);
		assertTrue(peak(left, 301, 320) > 0.3, "sounds at once: " + peak(left, 301, 320));
	}

	@Test
	public void itIsTheSameOnTheLeftAndTheRight() {
		float[][] sides = play(44100, note(95, 0));

		assertArrayEquals(sides[0], sides[1]);
	}

	// at 44100 and 48000 Hz alike: a 1500 Hz tone, sounding 25 ms on, silent from 30 ms
	@Test
	public void itIsA1500HzToneSilentAfter30Milliseconds() {
		for (float rate : new float[] {44100, 48000}) {
			float[] left = left(rate, note(127, 0));
			int ms25 = Math.round(rate * 0.025f);
			int ms30 = Math.round(rate * 0.030f);

			assertEquals(1500, frequency(left, 0, ms30, rate), 15, rate + " Hz");
			assertTrue(peak(left, ms25 - 50, ms25) > SOUNDING, rate + " Hz: sounds 25 ms on");
			assertEquals(0, peak(left, ms30, left.length), 0, rate + " Hz: silent from 30 ms");
		}
	}

	// its peak is 0.9 of its level, 0.5 at velocity 127; over 5 ms it falls by e
	@Test
	public void itPeaksAtNineTenthsOfHalfScaleAndFallsByEEvery5Milliseconds() {
		float[] left = left(44100, note(127, 0));
		float from5ms = peak(left, 221, 442);
		float from10ms = peak(left, 442, 663);

		assertEquals(0.45, peak(left, 0, left.length), 0.01);
		assertEquals(Math.E, from5ms / from10ms, 0.01);
	}

	// it rises over 0.2 ms (9 frames): its first frame is silent, and it reaches half its level 5 frames in
	@Test
	public void itRisesFromSilenceOverItsFirstFrames() {
		float[] left = left(44100, note(127, 0));

		assertEquals(0, left[0], 0);
		assertTrue(peak(left, 0, 5) < 0.25 && peak(left, 0, 6) > 0.225, Arrays.toString(Arrays.copyOf(left, 8)));
	}

	@Test
	public void itsLevelIsTheVelocityOver127() {
		float[] loud = left(44100, note(127, 0));
		float[] soft = left(44100, note(32, 0));
		for (int i = 0; i < loud.length; i++) {
			assertEquals(loud[i] * 32 / 127, soft[i], 0.000001, "at " + i);
		}
	}

	// whatever the note but the accented one (MidiClick.ACCENTED): the drum note is for other MIDI Ports
	@Test
	public void everyNoteButTheAccentedOneSoundsTheSameClick() {
		assertArrayEquals(left(44100, note(95, 0)), left(44100, new Note(42, 95, 0)));
		assertArrayEquals(left(44100, note(95, 0)), left(44100, new Note(MidiClick.PLAIN, 95, 0)));
	}

	// beat 1's click (MidiClick.ACCENTED): a 2000 Hz tone, 3 dB louder (root mean square), with the same rise and fall,
	// silent after 30 ms
	@Test
	public void theAccentedClickIsA2000HzTone3DecibelsLouderWithTheSameEnvelope() {
		for (float rate : new float[] {44100, 48000}) {
			float[] plain = left(rate, note(95, 0));
			float[] accented = left(rate, new Note(MidiClick.ACCENTED, 95, 0));
			int ms5 = Math.round(rate * 0.005f);
			int ms30 = Math.round(rate * 0.030f);

			assertEquals(2000, frequency(accented, 0, ms30, rate), 20, rate + " Hz");
			assertEquals(Math.pow(10, 3 / 20.0), rms(accented, 0, ms30) / rms(plain, 0, ms30), 0.01, rate + " Hz: 3 dB louder");
			assertEquals(Math.E, peak(accented, ms5, 2 * ms5) / peak(accented, 2 * ms5, 3 * ms5), 0.05, rate + " Hz: falls by e every 5 ms");
			assertEquals(0, peak(accented, ms30, accented.length), 0, rate + " Hz: silent from 30 ms");
		}
	}

	// as the drum tap gives it, at a velocity: what an accented note sounds from its frame
	@Test
	public void theAccentedClickAtAVelocityIsWhatAnAccentedNoteSounds() {
		float[] sound = ClickProcessor.sound(44100, 64, true);

		assertArrayEquals(Arrays.copyOf(left(44100, new Note(MidiClick.ACCENTED, 64, 0)), sound.length), sound);
	}

	// a note far into a later block, and one that sounds on across a block's end, click as on frame 0
	@Test
	public void aClickSoundsOnItsFrameAcrossBlocks() {
		float[] first = left(44100, note(95, 0));

		assertArrayEquals(shifted(first, 1000), left(44100, note(95, 1000)));
		assertArrayEquals(shifted(first, 500), left(44100, note(95, 500)));
	}

	// a second note while the first still sounds: both, added
	@Test
	public void clicksThatOverlapAdd() {
		float[] one = left(44100, note(95, 0));
		float[] two = left(44100, note(95, 0), note(95, 100));
		float[] later = shifted(one, 100);
		for (int i = 0; i < one.length; i++) {
			assertEquals(one[i] + later[i], two[i], 0.000001, "at " + i);
		}
	}

	// an untimed note (Count In's timer, a note entered) clicks at the start of the next block
	@Test
	public void anUntimedNoteClicksAtTheStartOfTheNextBlock() {
		ClickProcessor click = new ClickProcessor();
		click.sendNoteOn(37, 95, -1, false);

		assertArrayEquals(play(44100, note(95, 0)), blocks(click));
	}

	// a note off, a bend or a control (All Notes Off included) leaves a click to sound out
	@Test
	public void aClickSoundsOutWhateverFollowsIt() {
		ClickProcessor click = new ClickProcessor();
		click.sendNoteOn(37, 95, -1, false, 0);
		click.sendNoteOff(37, 95, -1, false, 1);
		click.sendPitchBend(0, -1, false, 2);
		click.sendControlChange(123, 0, 3);

		assertArrayEquals(play(44100, note(95, 0)), blocks(click));
	}

	// through 16-bit buffers of a block each, the same click within a 16-bit step
	@Test
	public void itFillsBuffersWithTheClick() {
		ClickProcessor click = new ClickProcessor();
		click.sendNoteOn(37, 127, -1, false, 0);
		float[] expected = left(44100, note(127, 0));
		for (int b = 0; b < 2; b++) {
			TGAudioBuffer buffer = new TGAudioBuffer();
			click.fillBuffer(buffer);
			float[][] block = new float[2][FRAMES];
			buffer.read(block);
			assertArrayEquals(Arrays.copyOfRange(expected, b * FRAMES, (b + 1) * FRAMES), block[1], 1f / Short.MAX_VALUE, "block " + b);
		}
	}

	// told the default sounds, the code-made Click accented on beat 1, at 0 dB: bit for bit what it played before it was
	// told any sounds, at 44100 and 48000 Hz
	@Test
	public void theDefaultSoundsAt0DecibelsAreTheClickItAlwaysPlayed() {
		Map<String, String> click = MidiClick.sameAccented(MidiClickSound.CLICK, 0).toParameters(null);
		for (float rate : new float[] {44100, 48000}) {
			Note[] notes = {note(95, 0), new Note(MidiClick.ACCENTED, 64, 700), new Note(MidiClick.PLAIN, 127, 1500)};
			assertArrayEquals(play(rate, notes), told(click, rate, notes));
		}
	}

	// the recording's samples at the rate, so many semitones higher, at the gain: what a note at velocity 127 sounds
	private static float[] recording(MidiClickSound sound, float rate, int semitones, double gain) {
		float[] samples = TGClickSounds.recording(sound, null).at((int) rate, semitones);
		for (int i = 0; i < samples.length; i++) {
			samples[i] *= (float) gain;
		}
		return samples;
	}

	// the first so many samples of the left
	private static float[] first(float[][] sides, int length) {
		return Arrays.copyOf(sides[0], length);
	}

	// the first samples of the left played are the recording's at the rate, as it is, at velocity 127 and 0 dB
	private static void assertRecording(MidiClickSound sound, float rate, float[][] played) {
		assertArrayEquals(Arrays.copyOf(recording(sound, rate, 0, 1), 2048), first(played, 2048), 1e-6f, sound + " at " + rate + " Hz");
	}

	// a processor told the sound for every beat (beat 1 accented), at 0 dB
	private static ClickProcessor toldOnly(MidiClickSound sound) {
		ClickProcessor click = new ClickProcessor();
		click.restoreParameters(MidiClick.sameAccented(sound, 0).toParameters(null));
		return click;
	}

	// the volume: 10^(dB / 20) times the sound, whatever the velocity
	@Test
	public void theClickIsPlayedAtItsVolume() {
		float[] loud = left(44100, note(64, 0));
		float[] soft = told(MidiClick.sameAccented(MidiClickSound.CLICK, -6).toParameters(null), 44100, note(64, 0))[0];
		for (int i = 0; i < loud.length; i++) {
			assertEquals(loud[i] * Math.pow(10, -6 / 20.0), soft[i], 1e-6, "at " + i);
		}
	}

	// the other beats' recording, at the synthesizer's rate, for every note but the accented one
	@Test
	public void theOtherBeatsPlayTheirRecordingAtTheSynthesizersRate() {
		Map<String, String> woodBlock = MidiClick.sameAccented(MidiClickSound.WOOD_BLOCK, 0).toParameters(null);
		for (float rate : new float[] {44100, 48000}) {
			assertRecording(MidiClickSound.WOOD_BLOCK, rate, told(woodBlock, rate, new Note(MidiClick.PLAIN, 127, 0)));
			assertRecording(MidiClickSound.WOOD_BLOCK, rate, told(woodBlock, rate, note(127, 0)));
		}
	}

	// beat 1 the other beats' recording, accented: 2 semitones higher, 3 dB louder
	@Test
	public void beat1AccentedIsTheRecording2SemitonesHigherAnd3DecibelsLouder() {
		float[][] played = told(MidiClick.sameAccented(MidiClickSound.CLAVES, 0).toParameters(null), 48000, new Note(MidiClick.ACCENTED, 127, 0));

		assertArrayEquals(Arrays.copyOf(recording(MidiClickSound.CLAVES, 48000, 2, Math.pow(10, 3 / 20.0)), 2048), first(played, 2048), 1e-6f);
	}

	// beat 1 a sound of its own, as it is: the code-made Click its plain 1500 Hz click
	@Test
	public void beat1OfItsOwnIsItsSoundAsItIs() {
		float[][] clap = told(new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, 0).toParameters(null), 44100, new Note(MidiClick.ACCENTED, 127, 0));
		float[][] click = told(new MidiClick(MidiClickSound.CLICK, MidiClickSound.SNARE, 0).toParameters(null), 44100, new Note(MidiClick.ACCENTED, 127, 0));

		assertRecording(MidiClickSound.CLAP, 44100, clap);
		assertArrayEquals(left(44100, note(127, 0)), click[0]);
	}

	// the user's own sound, from the folder the parameters name; one that cannot be read plays the code-made Click
	@Test
	public void anOwnSoundIsReadFromTheFolderToldAndOneUnreadablePlaysTheClick(@org.junit.jupiter.api.io.TempDir java.io.File folder) throws Exception {
		try (java.io.InputStream clap = TGClickSounds.open(MidiClickSound.CLAP, null)) {
			java.nio.file.Files.copy(clap, new java.io.File(folder, "Mine.wav").toPath());
		}
		float[][] own = told(MidiClick.sameAccented(MidiClickSound.own("Mine.wav"), 0).toParameters(folder.getPath()), 44100, note(127, 0));
		float[][] gone = told(MidiClick.sameAccented(MidiClickSound.own("Gone.wav"), 0).toParameters(folder.getPath()), 44100, note(127, 0), new Note(MidiClick.ACCENTED, 127, 100));

		assertRecording(MidiClickSound.CLAP, 44100, own);
		assertArrayEquals(left(44100, note(127, 0), new Note(MidiClick.ACCENTED, 127, 100)), gone[0]);
	}

	// every sample of a recording accented is louder, from its first (a file of the user's that starts at full scale)
	@Test
	public void aRecordingAccentedIsLouderFromItsFirstSample(@org.junit.jupiter.api.io.TempDir java.io.File folder) throws Exception {
		byte[] wave = {'R', 'I', 'F', 'F', 40, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ', 16, 0, 0, 0, 1, 0, 1, 0, 0x44, (byte) 0xAC, 0, 0,
			(byte) 0x88, 0x58, 1, 0, 2, 0, 16, 0, 'd', 'a', 't', 'a', 4, 0, 0, 0, 0, 0x40, 0, (byte) 0xC0};
		java.nio.file.Files.write(new java.io.File(folder, "Loud.wav").toPath(), wave);
		MidiClickSound loud = MidiClickSound.own("Loud.wav");
		float[] higher = TGClickSounds.recording(loud, folder.getPath()).at(44100, 2);
		float[] expected = new float[FRAMES * BLOCKS];
		for (int i = 0; i < higher.length; i++) {
			expected[i] = higher[i] * (float) Math.pow(10, 3 / 20.0);
		}

		assertArrayEquals(expected, told(MidiClick.sameAccented(loud, 0).toParameters(folder.getPath()), 44100, new Note(MidiClick.ACCENTED, 127, 0))[0]);
	}

	// a recording sounds as long as it is (the snare's 1.3 s), not the code-made Click's 30 ms
	@Test
	public void aRecordingSoundsAsLongAsItIs() {
		ClickProcessor click = toldOnly(MidiClickSound.SNARE);
		click.sendNoteOn(MidiClick.PLAIN, 127, -1, false, 0);
		float[][] block = new float[2][FRAMES];
		float late = 0;
		for (int b = 0; b < 100; b++) {
			click.process(block);
			late = (b == 99 ? peak(block[0], 0, FRAMES) : late);
		}

		assertTrue(late > 0.0001, "sounds 1.1 s on: " + late);
	}

	// a click sounds the sounds the processor was told when its note reached it, to its end, whatever it is told after
	@Test
	public void aClickKeepsTheSoundsOfWhenItsNoteCame() {
		ClickProcessor click = toldOnly(MidiClickSound.HI_HAT);
		click.sendNoteOn(MidiClick.PLAIN, 127, -1, false, 0);
		click.restoreParameters(MidiClick.DEFAULT.toParameters(null));

		assertRecording(MidiClickSound.HI_HAT, 44100, blocks(click));
	}

	// parameters that tell no click (another processor's) leave the sounds as they were
	@Test
	public void parametersOfNoClickLeaveTheSounds() {
		ClickProcessor click = toldOnly(MidiClickSound.COWBELL);
		click.restoreParameters(Map.of("gervil.program", "128:0"));

		assertRecording(MidiClickSound.COWBELL, 44100, play(click, 44100, note(127, 0)));
	}

	// told the sounds, then a new rate: the sounds at that rate
	@Test
	public void toldTheSoundsThenARateItPlaysThemAtThatRate() {
		assertRecording(MidiClickSound.COWBELL, 48000, play(toldOnly(MidiClickSound.COWBELL), 48000, note(127, 0)));
	}

	// no latency, no parameters; open until closed, then silent
	@Test
	public void itHasNoLatencyNorParametersAndIsSilentOnceClosed() {
		ClickProcessor click = new ClickProcessor();
		Map<String, String> parameters = new HashMap<String, String>();
		click.restoreParameters(Map.of("gervil.program", "128:0"));
		click.storeParameters(parameters);
		boolean open = click.isOpen();
		click.sendNoteOn(37, 127, -1, false, 0);
		click.close();
		click.finalize();
		float[][] closed = blocks(click);

		assertEquals("0 {} true false false 0.0", click.getLatency() + " " + parameters + " " + open + " " + click.isOpen() + " " + click.isBusy() + " " + peak(closed[0], 0, closed[0].length));
	}

	// the synthesizer finds the built-in click by its type, with no plugin installed, but does not offer it as a source a
	// track can choose
	@Test
	public void theSynthesizerFindsItsFactoryButDoesNotOfferIt() {
		TGSynthManager manager = TGSynthManager.getInstance(new TGContext());

		assertEquals("click true []", ClickProcessorFactory.TYPE + " " + (manager.findMidiProcessorFactory(ClickProcessorFactory.TYPE).createProcessor() instanceof ClickProcessor)
				+ " " + manager.getAllSupportedMidiTypes());
	}
}
