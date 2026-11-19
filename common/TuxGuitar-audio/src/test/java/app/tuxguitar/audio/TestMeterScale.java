package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The one scale of every level meter: decibels, where a level sits, and how it is written.
 */
public class TestMeterScale {

	@Test
	void fullScaleIsZeroDecibelsAndHalfOfItMinusSix() {
		assertEquals(0, TGMeterScale.decibels(1), 1e-9);
		assertEquals(-6.0206, TGMeterScale.decibels(0.5), 1e-4);
		assertEquals(6.0206, TGMeterScale.decibels(2), 1e-4);
	}

	@Test
	void silenceIsMinusInfinity() {
		assertEquals(Double.NEGATIVE_INFINITY, TGMeterScale.decibels(0));
	}

	// linear in dB from -60 (empty) to +6 (full)
	@Test
	void aLevelSitsLinearlyFromTheFloorToTheTop() {
		assertEquals(0, TGMeterScale.position(-60), 1e-9);
		assertEquals(0.5, TGMeterScale.position(-27), 1e-9);
		assertEquals(1, TGMeterScale.position(6), 1e-9);
	}

	@Test
	void aLevelOutsideTheScaleIsHeldAtItsEnds() {
		assertEquals(0, TGMeterScale.position(-80), 1e-9);
		assertEquals(0, TGMeterScale.position(Double.NEGATIVE_INFINITY), 1e-9);
		assertEquals(1, TGMeterScale.position(12), 1e-9);
	}

	@Test
	void zeroDecibelsSitsTenEleventhsUp() {
		assertEquals(60.0 / 66, TGMeterScale.zero(), 1e-9);
	}

	// an input's meter: -60 to 0 dB, rounded
	@Test
	void anInputMeterIsAPercentFromTheFloorToFullScale() {
		assertEquals(0, TGMeterScale.percent(Double.NEGATIVE_INFINITY));
		assertEquals(0, TGMeterScale.percent(-60));
		assertEquals(50, TGMeterScale.percent(-30));
		assertEquals(51, TGMeterScale.percent(-29.5));
		assertEquals(100, TGMeterScale.percent(0));
		assertEquals(100, TGMeterScale.percent(6));
	}

	@Test
	void aLevelIsWrittenToOneDecimal() {
		assertEquals("-6.0", TGMeterScale.number(-6.0206));
		assertEquals("3.5", TGMeterScale.number(3.46));
		assertEquals("-inf", TGMeterScale.number(Double.NEGATIVE_INFINITY));
	}
}
