package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.stream.IntStream;

/**
 * How one signal fits N times another (README, Measuring): in time to the sample, and, for a profile taking over from
 * another, the change from one N to the other.
 */
final class Fit {

	// the lags searched either way, in samples
	static final int LAGS = 64;
	// below this, the other signal is too quiet to divide by
	private static final double QUIET = 0.01;
	// how near a sample is to N times the other's
	private static final double WITHIN = 0.001;

	private Fit() {
	}

	// over the samples from to to (clear of LAGS at either end), the lag at which the signal fits N times the other best
	// (least root sum of squares of what is left) is 0, and what is left there is within the percent of N times the
	// other's root sum of squares
	static void assertInTime(float[] signal, float[] other, float times, int from, int to, int percent) {
		int start = from + LAGS;
		int end = to - LAGS;
		int best = IntStream.rangeClosed(-LAGS, LAGS).boxed().min(Comparator.comparingDouble(lag -> residual(signal, other, times, lag, start, end))).get();
		double scale = Math.sqrt(IntStream.range(start, end).mapToDouble(i -> times * other[i] * times * other[i]).sum());
		assertEquals(0, best, "lag of the best fit");
		assertTrue(scale > 0, "the other signal sounds nothing");
		assertTrue(residual(signal, other, times, 0, start, end) <= scale * percent / 100, "left " + residual(signal, other, times, 0, start, end) / scale);
	}

	// the root sum of squares of the signal minus N times the other lag samples earlier
	static double residual(float[] signal, float[] other, float times, int lag, int start, int end) {
		double sum = 0;
		for (int i = start; i < end; i++) {
			double left = signal[i] - times * other[i - lag];
			sum += left * left;
		}
		return Math.sqrt(sum);
	}

	// the signal is N times the other, sample for sample within 0.001, N going from first to then: between the two at
	// every sample (so it never drops out), first at its first sample and then at its last, the change (from the first
	// sample that is not first times the other to the last that is not then times it) lasting the milliseconds, within
	// the tolerance; measured where the other sounds above QUIET
	static void assertChangeover(float[] signal, float[] other, double first, double then, double millis, double within, int rate) {
		int[] heard = assertBetween(signal, other, first, then);
		int left = IntStream.of(heard).filter(i -> !times(signal[i], other[i], first)).findFirst().orElse(-1);
		int reached = IntStream.of(heard).filter(i -> !times(signal[i], other[i], then)).max().orElse(-1);
		assertTrue(left >= 0 && times(signal[heard[heard.length - 1]], other[heard[heard.length - 1]], then), "first " + left + ", then at the end");
		assertEquals(millis, (reached - left + 1) * 1000.0 / rate, within, "the change, from sample " + left);
	}

	// where the other sounds above QUIET, the signal is between first and then times it at every sample, within 0.001;
	// those samples
	static int[] assertBetween(float[] signal, float[] other, double first, double then) {
		int[] heard = IntStream.range(0, Math.min(signal.length, other.length)).filter(i -> Math.abs(other[i]) > QUIET).toArray();
		int outside = IntStream.of(heard).filter(i -> !between(signal[i], other[i], first, then)).findFirst().orElse(-1);
		assertEquals(-1, outside, "outside at sample " + outside);
		assertTrue(heard.length > 0, "the other signal sounds nothing");
		return heard;
	}

	private static boolean times(float sample, float other, double times) {
		return Math.abs(sample - times * other) <= WITHIN;
	}

	private static boolean between(float sample, float other, double first, double then) {
		double one = first * other;
		double two = then * other;
		return sample >= Math.min(one, two) - WITHIN && sample <= Math.max(one, two) + WITHIN;
	}
}
