package app.tuxguitar.audio;

import java.util.Locale;

/**
 * The scale of every level meter, in one place: decibels (of full scale) from a sample's amplitude, where a level sits
 * on a meter (linear in dB from FLOOR, empty, to TOP, full; 0 dB at zero()), and how a level is written.
 */
public final class TGMeterScale {

	// the quietest level a meter shows: anything quieter is empty
	public static final double FLOOR = -60;
	// the loudest: inside a sound chain the sound may go above full scale (0 dB)
	public static final double TOP = 6;

	private TGMeterScale() {
	}

	// minus infinity for silence
	public static double decibels(double amplitude) {
		return 20 * Math.log10(amplitude);
	}

	// where the level sits between FLOOR (0) and TOP (1), held at both ends
	public static double position(double decibels) {
		return within(FLOOR, TOP, decibels);
	}

	// where 0 dB (full scale) sits
	public static double zero() {
		return position(0);
	}

	// how full a meter from FLOOR to 0 dB is, 0 to 100 (an input's meter: an input cannot go above full scale)
	public static int percent(double decibels) {
		return (int) Math.round(100 * within(FLOOR, 0, decibels));
	}

	// the level to one decimal ("-6.0"), "-inf" for silence
	public static String number(double decibels) {
		return (Double.isInfinite(decibels) ? "-inf" : String.format(Locale.ROOT, "%.1f", decibels));
	}

	// where the level sits between the quietest (0) and the loudest (1), held at both ends
	private static double within(double quietest, double loudest, double decibels) {
		return Math.min(1, Math.max(0, (decibels - quietest) / (loudest - quietest)));
	}
}
