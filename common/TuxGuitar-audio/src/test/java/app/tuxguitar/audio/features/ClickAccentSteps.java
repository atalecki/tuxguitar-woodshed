package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.Meters;
import app.tuxguitar.midi.synth.impl.ClickProcessor;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiClickSound;
import io.cucumber.java.en.Then;

/**
 * Steps on the accent of the built-in click on beat 1. A plain click is, sample for sample, the code-made Click at the
 * click volume (dB); an accented one is measured: its frequency over its first 20 ms, and its level (root mean square over its
 * 30 ms) against the plain click's (3 dB above: 10^(3/20) times). Offline the drum tap is measured from each click's sample (the clicks are where
 * TuxGuitar sends their notes to the drums), and is silent between them; live, the output.
 */
public class ClickAccentSteps {

	private static final double PLAIN = 1500;
	private static final double ACCENTED = 2000;
	private static final double LOUDER = Math.pow(10, 3 / 20.0);
	// how much of a click its frequency is measured over, in seconds
	private static final double MEASURED = 0.02;

	private final AudioWorld world;

	public ClickAccentSteps(AudioWorld world) {
		this.world = world;
	}

	private Heard heard() {
		return PlaySteps.heard(world);
	}

	@Then("the Count In clicks sound the built-in click at {int} dB, accented on click {int} only")
	public void countInAccentedOn(int volume, int click) {
		assertDrums(heard().timeline().getCountIn().positions(), volume, index -> index == click - 1);
	}

	@Then("the Count In clicks sound the built-in click at {int} dB, accented on clicks {int} and {int} only")
	public void countInAccentedOn(int volume, int click, int other) {
		assertDrums(heard().timeline().getCountIn().positions(), volume, index -> index == click - 1 || index == other - 1);
	}

	@Then("the Count In clicks sound the built-in click at {int} dB, accented on no click")
	public void countInAccentedOnNone(int volume) {
		assertDrums(heard().timeline().getCountIn().positions(), volume, index -> false);
	}

	// beat 1 of each measure, where the song plays it
	@Then("the Metronome clicks sound the built-in click at {int} dB, accented on beat 1 of measures {int} to {int} only")
	public void metronomeAccentedOn(int volume, int first, int last) {
		Set<Long> beats = IntStream.rangeClosed(first, last).mapToObj(measure -> (long) heard().sampleAt(SongPosition.parse("measure " + measure + " beat 1"), 0)).collect(Collectors.toSet());
		long[] clicks = heard().metronome();
		assertDrums(clicks, volume, index -> beats.contains(clicks[index]));
	}

	// from the first click to the end of the last, on both sides of the drum tap; the plain click at the volume, as the
	// drums play it (a full velocity's click times the volume's gain)
	private void assertDrums(long[] clicks, int volume, IntPredicate accented) {
		int rate = heard().timeline().getSampleRate();
		float[] plain = ClickProcessor.sound(rate, 127);
		float gain = (float) MidiClick.sameAccented(MidiClickSound.CLICK, volume).getGain();
		for (int i = 0; i < plain.length; i++) {
			plain[i] *= gain;
		}
		for (int side = 0; side < 2; side++) {
			assertSide(heard().drums(side), clicks, plain, accented, rate);
		}
	}

	private static void assertSide(float[] drums, long[] clicks, float[] plain, IntPredicate accented, int rate) {
		float[] rest = Arrays.copyOf(drums, drums.length);
		for (int index = 0; index < clicks.length; index++) {
			int from = (int) clicks[index];
			int to = Math.min(from + plain.length, drums.length);
			assertClick(drums, from, to, plain, accented.test(index), rate, "click " + (index + 1) + " at " + from);
			Arrays.fill(rest, from, to, 0f);
		}
		assertEquals(0f, Meters.peak(rest, (int) clicks[0], (int) Math.min(clicks[clicks.length - 1] + plain.length, rest.length)), "silent between the clicks");
	}

	private static void assertClick(float[] drums, int from, int to, float[] plain, boolean accented, int rate, String which) {
		float[] heard = Arrays.copyOfRange(drums, from, to);
		if (!accented) {
			assertArrayEquals(Arrays.copyOf(plain, to - from), heard, which + ": the plain click");
			return;
		}
		assertSounds(heard, rms(plain), ACCENTED, LOUDER, rate, which + ": accented");
	}

	// at the frequency within 1%, its level the times the reference's within 1%
	private static void assertSounds(float[] click, double reference, double frequency, double times, int rate, String which) {
		assertEquals(frequency, Meters.frequency(click, 0, (int) Math.min(click.length, MEASURED * rate), rate), frequency / 100, which);
		assertEquals(reference * times, rms(click), reference * times / 100, which);
	}

	// root mean square
	private static double rms(float[] samples) {
		return Math.sqrt(IntStream.range(0, samples.length).mapToDouble(i -> samples[i] * samples[i]).sum() / samples.length);
	}

	// the clicks in order on every beat, from measure 1 beat 1: each measure's first is beat 1
	@Then("the output's left side holds {int} clicks, those on beat 1 of each measure 2000 Hz and 3 dB above the others, at 1500 Hz")
	public void liveAccented(int count) {
		float[] left = world.live()[0];
		List<Integer> clicks = Meters.clicks(left, 0, left.length);
		assertEquals(count, clicks.size(), "clicks " + clicks);
		Set<Integer> beats = this.firstBeats();
		int length = (int) (0.03 * TestInterface.SAMPLE_RATE);
		double plain = rms(Arrays.copyOfRange(left, clicks.get(1), clicks.get(1) + length));
		for (int index = 0; index < count; index++) {
			int from = clicks.get(index);
			assertLive(Arrays.copyOfRange(left, from, Math.min(from + length, left.length)), plain, beats.contains(index), "click " + (index + 1) + " at " + from);
		}
	}

	private static void assertLive(float[] click, double plain, boolean accented, String which) {
		if (accented) {
			assertSounds(click, plain, ACCENTED, LOUDER, TestInterface.SAMPLE_RATE, which + ": accented");
		} else {
			assertSounds(click, plain, PLAIN, 1, TestInterface.SAMPLE_RATE, which + ": plain");
		}
	}

	// the indexes of the clicks on beat 1, the song's measures one after the other
	private Set<Integer> firstBeats() {
		Set<Integer> beats = new java.util.HashSet<Integer>();
		int beat = 0;
		for (int measure = 0; measure < world.song().countMeasureHeaders(); measure++) {
			beats.add(beat);
			beat += world.song().getMeasureHeader(measure).getTimeSignature().getNumerator();
		}
		return beats;
	}
}
