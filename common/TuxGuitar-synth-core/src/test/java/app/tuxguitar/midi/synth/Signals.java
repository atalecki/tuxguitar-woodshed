package app.tuxguitar.midi.synth;

/**
 * What a test measures of samples.
 */
public final class Signals {

	private Signals() {
	}

	// the largest magnitude of the samples from one to another
	public static float peak(float[] samples, int from, int to) {
		float peak = 0;
		for (int i = from; i < to; i++) {
			peak = Math.max(peak, Math.abs(samples[i]));
		}
		return peak;
	}
}
