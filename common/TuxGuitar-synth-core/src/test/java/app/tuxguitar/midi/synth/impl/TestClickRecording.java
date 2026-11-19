package app.tuxguitar.midi.synth.impl;

import static app.tuxguitar.midi.synth.Signals.peak;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * A recording a click plays (a built-in one, or a WAV file of the user's own), evened as it is read so that every sound
 * clicks alike at the same volume: one channel (the file's averaged), no silence after it, at most 2 seconds, its peak
 * 0.5 (as the code-made Click's level). Played at the synthesizer's rate, and a number of semitones higher (beat 1
 * played accented).
 */
public class TestClickRecording {

	// a decaying sine of the frequency, at the level, so many samples long, then silence to the length
	private static float[] decay(double frequency, float level, int rate, int sounding, int length) {
		float[] samples = new float[length];
		for (int i = 0; i < sounding; i++) {
			samples[i] = (float) (level * Math.exp(-3.0 * i / sounding) * Math.sin(2 * Math.PI * frequency * i / rate));
		}
		return samples;
	}

	@Test
	public void itIsTheFilesChannelsAveraged() throws IOException {
		float[] left = decay(1000, 0.2f, 44100, 4410, 4410);
		float[] right = decay(1500, 0.6f, 44100, 4410, 4410);
		float[] average = new float[left.length];
		for (int i = 0; i < average.length; i++) {
			average[i] = (left[i] + right[i]) / 2;
		}

		assertArrayEquals(TGClickRecording.even(new float[][] {average}, 44100).samples(), TGClickRecording.even(new float[][] {left, right}, 44100).samples(), 1e-6f);
	}

	@Test
	public void itsPeakIsEvenedToHalfScale() throws IOException {
		float[] quiet = TGClickRecording.even(new float[][] {decay(800, 0.05f, 48000, 4800, 4800)}, 48000).samples();
		float[] loud = TGClickRecording.even(new float[][] {decay(800, 1.5f, 48000, 4800, 4800)}, 48000).samples();

		assertEquals("0.5 0.5", peak(quiet, 0, quiet.length) + " " + peak(loud, 0, loud.length));
		assertArrayEquals(quiet, loud, 1e-6f);
	}

	// below a thousandth of its peak (-60 dB) is silence
	@Test
	public void theSilenceAfterItIsTrimmed() throws IOException {
		float[] samples = decay(1000, 0.4f, 44100, 1000, 30000);
		samples[5] = 0.5f;
		samples[1500] = 0.0004f;
		samples[1600] = 0.5f * 0.001f;

		assertEquals(1601, TGClickRecording.even(new float[][] {samples}, 44100).samples().length);
	}

	// a metronome sound starts on the beat: the silence before it goes but a millisecond, faded in from nothing;
	// before it is below a hundredth of its peak (-40 dB)
	@Test
	public void theSilenceBeforeItGoesButAMillisecondFadedIn() throws IOException {
		float[] sound = decay(1000, 0.5f, 44100, 4410, 4410);
		sound[100] = 0.5f;
		float[] samples = new float[3000 + sound.length];
		java.util.Arrays.fill(samples, 0, 3000, 0.004f);
		System.arraycopy(sound, 1, samples, 3000, sound.length - 1);
		float[] evened = TGClickRecording.even(new float[][] {samples}, 44100).samples();

		assertEquals(44 + sound.length - 1, evened.length);
		assertEquals("0.0 0.002 " + sound[1], evened[0] + " " + evened[22] + " " + evened[44]);
	}

	// at a hundredth of its peak exactly, it has started
	@Test
	public void atAHundredthOfItsPeakItHasStarted() throws IOException {
		float[] samples = new float[1200];
		samples[1000] = 0.5f * 0.01f;
		samples[1100] = 0.5f;

		assertEquals(44 + 101, TGClickRecording.even(new float[][] {samples}, 44100).samples().length);
	}

	// one sounding at once keeps its first sample: nothing before it to fade in
	@Test
	public void oneSoundingAtOnceStartsAsItIs() throws IOException {
		float[] samples = decay(1000, 0.5f, 44100, 4410, 4410);
		samples[0] = 0.5f;

		assertEquals(0.5f, TGClickRecording.even(new float[][] {samples}, 44100).samples()[0]);
	}

	// counted from where it starts (a millisecond before it sounds): its loudest, 1.9 seconds in, is kept
	@Test
	public void atMostTwoSecondsAreKeptFromWhereItStarts() throws IOException {
		float[] samples = new float[22050 + 3 * 22050];
		System.arraycopy(decay(440, 0.5f, 22050, 3 * 22050, 3 * 22050), 1, samples, 22051, 3 * 22050 - 1);
		samples[22051 + 41895] = 1f;
		float[] evened = TGClickRecording.even(new float[][] {samples}, 22050).samples();

		assertEquals(2 * 22050 + " 0.5", evened.length + " " + evened[22 + 41895]);
	}

	@Test
	public void atMostTwoSecondsAreKept() throws IOException {
		assertEquals(2 * 22050, TGClickRecording.even(new float[][] {decay(440, 0.5f, 22050, 3 * 22050, 3 * 22050)}, 22050).samples().length);
	}

	@Test
	public void aSilentFileIsNoSound() {
		IOException refused = assertThrows(IOException.class, () -> TGClickRecording.even(new float[][] {new float[100]}, 44100));

		assertEquals("no sound in the file", refused.getMessage());
	}

	// how often a sound crosses zero going up, per second
	private static double frequency(float[] samples, int to, int rate) {
		int[] up = java.util.stream.IntStream.range(1, to).filter(i -> samples[i - 1] <= 0 && samples[i] > 0).toArray();
		return (up.length - 1) * (double) rate / (up[up.length - 1] - up[0]);
	}

	// at its own rate as it is; at another, as long and as high
	@Test
	public void itPlaysAtTheSynthesizersRate() throws IOException {
		TGClickRecording recording = TGClickRecording.even(new float[][] {decay(1000, 0.5f, 44100, 44100, 44100)}, 44100);
		float[] same = recording.at(44100, 0);
		float[] higher = recording.at(48000, 0);

		assertArrayEquals(recording.samples(), same);
		TGClickRecording odd = TGClickRecording.even(new float[][] {decay(1000, 0.5f, 22050, 2205, 2205)}, 22050);
		assertArrayEquals(odd.samples(), odd.at(22050, 0));
		assertEquals(48000, higher.length, 1);
		assertEquals(1000, frequency(higher, 24000, 48000), 2);
	}

	// two semitones higher: 2^(2/12) times the frequency, as much shorter
	@Test
	public void semitonesHigherItIsHigherAndShorter() throws IOException {
		TGClickRecording recording = TGClickRecording.even(new float[][] {decay(1000, 0.5f, 44100, 44100, 44100)}, 44100);
		float[] higher = recording.at(44100, 2);
		double ratio = Math.pow(2, 2 / 12.0);

		assertEquals(1000 * ratio, frequency(higher, 22050, 44100), 2);
		assertEquals(44100 / ratio, higher.length, 3);
	}

	// a WAV file, of any format the wave reader reads
	@Test
	public void itIsReadFromAWaveFile() throws IOException {
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		byte[] header = {'R', 'I', 'F', 'F', 40, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ', 16, 0, 0, 0, 1, 0, 1, 0, 0x44, (byte) 0xAC, 0, 0,
			(byte) 0x88, 0x58, 1, 0, 2, 0, 16, 0, 'd', 'a', 't', 'a', 4, 0, 0, 0, 0, 0x10, 0, (byte) 0xF0};
		file.write(header, 0, header.length);
		TGClickRecording recording = TGClickRecording.read(new ByteArrayInputStream(file.toByteArray()));

		assertEquals("44100 [0.5, -0.5]", recording.sampleRate() + " " + java.util.Arrays.toString(recording.samples()));
	}
}
