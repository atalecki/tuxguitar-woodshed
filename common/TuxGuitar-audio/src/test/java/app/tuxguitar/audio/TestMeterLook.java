package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * How a meter looks: the shapes an app paints. A meter of one bar 6 high whose bar is 66 wide (1 per dB, from -60 at 0
 * to +6 at 66; 0 dB at 60), then a gap of 1 and its lamp, 6 by 6: 73 wide.
 */
public class TestMeterLook {

	private static final float WIDE = 73;
	private static final float HIGH = 6f;
	private static final float STEREO = 13f;

	// a reading of both sides at the peak and hold (dB), lit or not
	private static TGMeterReading reading(double peak, double hold, boolean clipped) {
		return new TGMeterReading(new double[] {peak, peak}, new double[] {hold, hold}, clipped, true);
	}

	// one bar when as high as one, else two
	private static String look(TGMeterReading reading, float high) {
		return TGMeterLook.shapes(reading, WIDE - HIGH + high, high, high > HIGH).stream().map(Object::toString).collect(Collectors.joining(" | "));
	}

	private static final double INF = Double.NEGATIVE_INFINITY;

	// each case: what it shows, the reading, the meter's height (6: one bar; 13: two bars), the shapes
	private static final Object[][] CASES = {
		{"a bar runs from the left to its peak, its hold line ending at the hold", reading(-6, -9, false), HIGH,
			"TRACK 0.0,0.0 66.0x6.0 | BAR 0.0,0.0 54.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | HOLD 49.0,0.0 2.0x6.0 | LAMP 67.0,0.0 6.0x6.0"},
		{"silence shows the empty track, the mark and the lamp", reading(INF, INF, false), HIGH,
			"TRACK 0.0,0.0 66.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | LAMP 67.0,0.0 6.0x6.0"},
		{"at 0 dB the bar reaches the mark, no stripes", reading(0, 0, false), HIGH,
			"TRACK 0.0,0.0 66.0x6.0 | BAR 0.0,0.0 60.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | HOLD 58.0,0.0 2.0x6.0 | LAMP 67.0,0.0 6.0x6.0"},
		{"above 0 dB the bar goes on striped (inside a chain the sound keeps it)", reading(3, 6, false), HIGH,
			"TRACK 0.0,0.0 66.0x6.0 | BAR 0.0,0.0 60.0x6.0 | OVER 60.0,0.0 3.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | HOLD 64.0,0.0 2.0x6.0 | LAMP 67.0,0.0 6.0x6.0"},
		{"a hold line at the floor stays inside", reading(-59, -59, false), HIGH,
			"TRACK 0.0,0.0 66.0x6.0 | BAR 0.0,0.0 1.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | HOLD 0.0,0.0 2.0x6.0 | LAMP 67.0,0.0 6.0x6.0"},
		{"a lit lamp is filled with a \"!\", its stroke and its dot a sixth of the lamp from its edges", reading(0, 0, true), HIGH,
			"TRACK 0.0,0.0 66.0x6.0 | BAR 0.0,0.0 60.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | HOLD 58.0,0.0 2.0x6.0 | LIT 67.0,0.0 6.0x6.0 | BANG 69.5,1.0 1.0x2.0 | BANG 69.5,4.0 1.0x1.0"},
		{"two bars, left above right, a gap between, one lamp as high as both",
			new TGMeterReading(new double[] {-6, INF}, new double[] {-6, -12}, false, true), STEREO,
			"TRACK 0.0,0.0 66.0x6.0 | BAR 0.0,0.0 54.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | HOLD 52.0,0.0 2.0x6.0 | "
			+ "TRACK 0.0,7.0 66.0x6.0 | ZERO 60.0,7.0 1.0x6.0 | HOLD 46.0,7.0 2.0x6.0 | LAMP 67.0,0.0 13.0x13.0"},
		{"one bar shows the louder side", new TGMeterReading(new double[] {-12, -6}, new double[] {-6, -12}, false, true), HIGH,
			"TRACK 0.0,0.0 66.0x6.0 | BAR 0.0,0.0 54.0x6.0 | ZERO 60.0,0.0 1.0x6.0 | HOLD 52.0,0.0 2.0x6.0 | LAMP 67.0,0.0 6.0x6.0"},
		{"a meter measuring nothing is an empty, greyed track, no lamp", TGMeterReading.OFF, HIGH, "GREYED 0.0,0.0 66.0x6.0"},
		{"the same, of two bars", TGMeterReading.OFF, STEREO, "GREYED 0.0,0.0 66.0x6.0 | GREYED 0.0,7.0 66.0x6.0"},
	};

	@Test
	void eachCaseLooksAsItShould() {
		String expected = Arrays.stream(CASES).map(row -> row[0] + ": " + row[3]).collect(Collectors.joining("\n"));
		String looked = Arrays.stream(CASES).map(row -> row[0] + ": " + look((TGMeterReading) row[1], (Float) row[2])).collect(Collectors.joining("\n"));
		assertEquals(expected, looked);
	}

	@Test
	void aMeterOfTwoBarsShowsItsHeldPeakUnderIt() {
		assertEquals("-0.4 dB | -inf dB | ", TGMeterLook.number(reading(-3, -0.4, false)) + " | " + TGMeterLook.number(reading(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, false)) + " | " + TGMeterLook.number(TGMeterReading.OFF));
	}

	// the louder side's peak and hold; "click to clear" while lit; a meter measuring nothing says so
	@Test
	void hoveringAMeterSaysWhatItRead() {
		String measured = "{0}: peak {1} dB, held {2} dB";
		String off = "{0}: not measured";
		TGMeterReading lit = new TGMeterReading(new double[] {-6.2, -9}, new double[] {-3.1, -4}, true, true);
		assertEquals("After Echo: peak -6.2 dB, held -3.1 dB Clipped: click to clear. | After Echo: peak -6.0 dB, held -9.0 dB | Track out: not measured",
			TGMeterLook.tip("After Echo", lit, measured, off, "Clipped: click to clear.") + " | "
			+ TGMeterLook.tip("After Echo", reading(-6, -9, false), measured, off, "Clipped") + " | "
			+ TGMeterLook.tip("Track out", TGMeterReading.OFF, measured, off, "Clipped"));
	}
}
