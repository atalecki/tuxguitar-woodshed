package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * How a shown meter moves over a fake clock (milliseconds): instant rise, a fall of 20 dB a second, a hold line for 2 s,
 * and a clip lamp lit until cleared.
 */
public class TestMeterBallistics {

	private static final double DB = 1e-6;
	private static final double INF = Double.NEGATIVE_INFINITY;
	// full scale, and 10 dB under it
	private static final float FULL = 1f;
	private static final float MINUS_TEN = (float) Math.pow(10, -10 / 20.0);

	private final TGMeterBallistics meter = new TGMeterBallistics();
	private long clips;

	// the reading at the time, after the peaks (both sides at one level)
	private TGMeterReading at(long now, float level) {
		return this.at(now, level, level);
	}

	private TGMeterReading at(long now, float left, float right) {
		return this.meter.next(new float[] {left, right}, this.clips, now);
	}

	// the peak and the hold of the left side, in dB
	private static String left(TGMeterReading reading) {
		return String.format(java.util.Locale.ROOT, "%.3f/%.3f", reading.getPeak(0), reading.getHold(0));
	}

	@Test
	void aPeakShowsAtOnce() {
		TGMeterReading reading = this.at(0, 0.5f);
		assertEquals(-6.0206, reading.getPeak(0), 1e-4);
		assertEquals(-6.0206, reading.getHold(0), 1e-4);
		assertTrue(reading.isOn());
	}

	@Test
	void aPeakFallsTwentyDecibelsASecond() {
		this.at(0, FULL);
		assertEquals(-10, this.at(500, 0f).getPeak(0), DB);
		assertEquals(-20, this.at(1000, 0f).getPeak(0), DB);
	}

	@Test
	void aLouderPeakLiftsAFallingOneAtOnce() {
		this.at(0, MINUS_TEN);
		this.at(1000, 0f);
		assertEquals(0, this.at(1100, FULL).getPeak(0), DB);
	}

	@Test
	void aQuieterPeakDoesNotStopTheFall() {
		this.at(0, FULL);
		assertEquals(-10, this.at(500, (float) Math.pow(10, -15 / 20.0)).getPeak(0), DB);
	}

	// a fresh meter given peaks (pairs of a time in ms and a level in dB, both sides), read in silence at the time: the
	// left side's "peak/hold"
	private static String after(long readAt, double... timesAndLevels) {
		TGMeterBallistics meter = new TGMeterBallistics();
		for (int i = 0; i < timesAndLevels.length; i += 2) {
			float level = (float) Math.pow(10, timesAndLevels[i + 1] / 20);
			meter.next(new float[] {level, level}, 0, (long) timesAndLevels[i]);
		}
		return left(meter.next(new float[] {0f, 0f}, 0, readAt));
	}

	@Test
	void theHoldLineStaysTwoSecondsAtTheHighestPeakThenFalls() {
		// held 2 s, then falling 20 dB a second
		assertEquals("-40.000/0.000", after(2000, 0, 0));
		assertEquals("-50.000/-10.000", after(2500, 0, 0));
		// a louder peak takes the hold and restarts it
		assertEquals("-20.000/0.000", after(2500, 0, -10, 1500, 0));
		assertEquals("-40.000/0.000", after(3500, 0, -10, 1500, 0));
		// a quieter peak during the hold leaves it
		assertEquals("-40.000/-10.000", after(2500, 0, 0, 1000, -10));
		// once the hold falls, a peak louder than where it is now (if quieter than where it was) holds
		assertEquals("-15.000/-5.000", after(3000, 0, 0, 2500, -5));
	}

	// silence shows nothing, from the start and once a peak has fallen below the floor (-60 dB), and reads the same
	// each time; a peak at the floor still shows
	@Test
	void silenceShowsNothing() {
		assertEquals("-Infinity/-Infinity", left(this.at(0, 0f)));
		this.at(1, FULL);
		TGMeterReading floor = this.at(6001, 0f);
		assertEquals("-Infinity/-Infinity", left(floor));
		assertEquals(floor, this.at(7000, 0f));
		assertEquals(-60, this.at(8000, 0.001f).getPeak(0), 1e-4);
	}

	@Test
	void eachSideMovesOnItsOwn() {
		this.at(0, FULL, 0f);
		TGMeterReading reading = this.at(500, 0f, MINUS_TEN);
		assertEquals(-10, reading.getPeak(0), DB);
		assertEquals(-10, reading.getPeak(1), DB);
		assertEquals(0, reading.getHold(0), DB);
		assertEquals(-10, reading.getHold(1), DB);
	}

	// a meter of one bar shows the louder side
	@Test
	void theLouderSideIsTheOneBarsReading() {
		TGMeterReading reading = this.at(0, MINUS_TEN, FULL);
		assertEquals(0, reading.getPeak(), DB);
		assertEquals(0, reading.getHold(), DB);
		reading = this.at(0, FULL, MINUS_TEN);
		assertEquals(0, reading.getPeak(), DB);
		assertEquals(0, reading.getHold(), DB);
	}

	@Test
	void theLampLightsOnAClipAndStaysLit() {
		assertFalse(this.at(0, FULL).isClipped());
		this.clips = 1;
		assertTrue(this.at(100, 0f).isClipped());
		assertTrue(this.at(5000, 0f).isClipped());
	}

	@Test
	void clearingPutsTheLampOutUntilTheNextClip() {
		this.clips = 1;
		this.at(0, FULL);
		this.meter.clear(this.clips);
		assertFalse(this.at(100, 0f).isClipped());
		this.clips = 2;
		assertTrue(this.at(200, FULL).isClipped());
	}

	// a lamp lit or out reads differently, so it is drawn again
	@Test
	void readingsDifferWhenTheLampDoes() {
		this.at(0, 0f);
		TGMeterReading out = this.at(100, 0f);
		this.clips = 1;
		assertNotEquals(out, this.at(200, 0f));
		assertNotEquals(TGMeterReading.OFF, out);
	}

	@Test
	void anOffMeterIsEmptyAndGreyed() {
		assertFalse(TGMeterReading.OFF.isOn());
		assertFalse(TGMeterReading.OFF.isClipped());
		assertEquals(INF, TGMeterReading.OFF.getPeak());
		assertEquals(INF, TGMeterReading.OFF.getHold());
	}
}
