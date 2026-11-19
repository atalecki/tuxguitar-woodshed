package app.tuxguitar.midi.synth.impl;

import java.io.IOException;
import java.io.InputStream;

import app.tuxguitar.midi.synth.TGResampler;
import app.tuxguitar.midi.synth.TGWaveReader;

/**
 * A recording a click plays (a built-in sound, or a WAV file of the user's own, see MidiClickSound), evened as it is
 * read so that every sound clicks alike at the same volume: one channel (the file's channels averaged), the silence
 * after it trimmed (below a thousandth of its peak, -60 dB), at most LONGEST seconds, its peak PEAK (the code-made
 * Click's level, see ClickProcessor). Immutable. The built-in recordings ship evened (see the README of click-sounds):
 * evening them again changes nothing but the last bit.
 */
public final class TGClickRecording {

	public static final float PEAK = 0.5f;
	public static final int LONGEST = 2;
	// below this part of its peak a sample is silence
	private static final float SILENCE = 0.001f;
	// a pitched rate is rounded to this many Hz, so that its ratio to the synthesizer's reduces (see TGResampler)
	private static final int ROUNDING = 100;

	private final float[] samples;
	private final int sampleRate;

	private TGClickRecording(float[] samples, int sampleRate) {
		this.samples = samples;
		this.sampleRate = sampleRate;
	}

	// the recording of a WAV file, evened
	public static TGClickRecording read(InputStream in) throws IOException {
		TGWaveReader.Format format = TGWaveReader.readFormat(in);
		return even(TGWaveReader.readSamples(in, format), format.getSampleRate());
	}

	// the channels at the rate, evened; refused when silent
	public static TGClickRecording even(float[][] channels, int sampleRate) throws IOException {
		float[] mono = mix(channels, Math.min(channels[0].length, LONGEST * sampleRate));
		float peak = peak(mono);
		if (peak == 0) {
			throw new IOException("no sound in the file");
		}
		float[] evened = new float[sounding(mono, peak * SILENCE)];
		for (int i = 0; i < evened.length; i++) {
			evened[i] = mono[i] * PEAK / peak;
		}
		return new TGClickRecording(evened, sampleRate);
	}

	// the first so many frames of the channels, added: evened, the same as averaged
	private static float[] mix(float[][] channels, int frames) {
		float[] mono = new float[frames];
		for (float[] channel : channels) {
			for (int i = 0; i < frames; i++) {
				mono[i] += channel[i];
			}
		}
		return mono;
	}

	private static float peak(float[] samples) {
		float peak = 0;
		for (float sample : samples) {
			peak = Math.max(peak, Math.abs(sample));
		}
		return peak;
	}

	// how many samples there are up to the last one above silence
	private static int sounding(float[] samples, float silence) {
		int length = samples.length;
		while (Math.abs(samples[length - 1]) < silence) {
			length--;
		}
		return length;
	}

	public float[] samples() {
		return this.samples.clone();
	}

	public int sampleRate() {
		return this.sampleRate;
	}

	// played at the rate, so many semitones higher (and as much shorter)
	public float[] at(int rate, int semitones) {
		return TGResampler.resample(new float[][] {this.samples}, pitched(semitones), rate)[0].clone();
	}

	// the rate it plays so many semitones higher at, as if recorded at a lower rate: its own for none
	private int pitched(int semitones) {
		if (semitones == 0) {
			return this.sampleRate;
		}
		return (int) Math.round(this.sampleRate * Math.pow(2, semitones / 12.0) / ROUNDING) * ROUNDING;
	}
}
