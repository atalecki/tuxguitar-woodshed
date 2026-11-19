package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.TGLevelTap;

/**
 * The input level: the peak of the newest blocks only, the higher side of a pair, clipping from -0.1 dBFS, and the clip
 * indicator held until reset; and a meter of it (the Recording window's) whose lamp follows the clip indicator.
 */
public class TestInputLevel {

	private final TGInputLevel input = new TGInputLevel();

	private static float[][] level(int samples, float... sides) {
		float[][] block = new float[sides.length][samples];
		for (int side = 0; side < sides.length; side++) {
			Arrays.fill(block[side], sides[side]);
		}
		return block;
	}

	@Test
	public void silenceReadsMinusInfinity() {
		assertEquals(Double.NEGATIVE_INFINITY, this.input.getLevel());
	}

	// a peak counts while its block is among the newest a reader reads
	@Test
	public void aPeakCountsWhileItsBlockIsAmongTheNewest() {
		this.input.deliver(level(1, -0.5f));
		for (int block = 1; block < TGLevelTap.NEWEST; block++) {
			this.input.deliver(level(3, 0.1f));
		}
		assertEquals(20 * Math.log10(0.5), this.input.getLevel(), 1e-6);
		this.input.deliver(level(1, 0.1f));
		assertEquals(20 * Math.log10(0.1f), this.input.getLevel(), 1e-6);
	}

	@Test
	public void aPairReadsItsHigherSide() {
		this.input.deliver(level(2, 0.25f, -0.5f));
		assertEquals(20 * Math.log10(0.5), this.input.getLevel(), 1e-6);
	}

	@Test
	public void theInputIsClippingFromMinusOneTenthOfADecibel() {
		this.input.deliver(level(1, (float) Math.pow(10, -0.1001 / 20)));
		assertFalse(this.input.isClipping());
		assertFalse(this.input.hasClipped());

		this.input.deliver(level(1, TGLevelTap.CLIPPING));
		assertTrue(this.input.isClipping());
		assertTrue(this.input.hasClipped());
	}

	@Test
	public void theClipIndicatorHoldsUntilReset() {
		this.input.deliver(level(1, 1f));
		for (int block = 0; block < TGLevelTap.NEWEST; block++) {
			this.input.deliver(level(8, 0f));
		}
		assertFalse(this.input.isClipping());
		assertTrue(this.input.hasClipped());

		this.input.resetClipIndicator();
		assertFalse(this.input.hasClipped());
		this.input.deliver(level(1, 1f));
		assertEquals(1, this.input.getClipsSinceReset());
	}

	// the Recording window's meter: its lamp is the clip indicator, so Reset clip puts it out
	@Test
	public void aMeterOfTheInputLightsUntilTheIndicatorIsReset() {
		TGTapMeter meter = new TGTapMeter(this.input.getTap(), this.input::getClipsSinceReset);
		this.input.deliver(level(1, 1f));
		TGMeterReading first = meter.read(0);
		assertEquals(0, first.getPeak(), 1e-6);
		assertTrue(first.isClipped());
		this.input.resetClipIndicator();
		assertFalse(meter.read(33).isClipped());
	}
}
