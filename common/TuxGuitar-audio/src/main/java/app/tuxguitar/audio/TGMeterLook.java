package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * How a level meter looks, as shapes an app only paints: each a rectangle (in the app's units, from the meter's top
 * left) with the role that says how (TGMeterLook.Role). One bar, or two (left above right), from -60 dB at the left to
 * +6 dB at the right, linear in dB; a mark at 0 dB; the part of a bar above 0 dB striped; a hold line; a clip lamp at
 * the right end (a square as high as the meter), an outline while out, filled with a "!" (two shapes, so no font is
 * needed) once lit. Nothing is said by colour alone, so an app
 * paints the roles in its system colours and a high-contrast theme recolours the meter. A meter that measures nothing
 * is an empty, greyed track.
 */
public final class TGMeterLook {

	// the room between the bars and the lamp, and between two bars
	static final float GAP = 1f;
	// how wide the hold line and the 0 dB mark are
	static final float HOLD = 2f;
	static final float MARK = 1f;

	public enum Role {
		// the empty track under a bar
		TRACK,
		// the bar, up to 0 dB at most
		BAR,
		// the bar above 0 dB: striped
		OVER,
		// the 0 dB mark
		ZERO,
		// the hold line
		HOLD,
		// the clip lamp, out: an outline
		LAMP,
		// the clip lamp, lit: filled
		LIT,
		// the "!" on a lit lamp (its stroke, its dot): the colour the lamp is not filled with
		BANG,
		// the track of a meter that measures nothing
		GREYED
	}

	public static final class Shape {

		private final Role role;
		private final float x;
		private final float y;
		private final float width;
		private final float height;

		Shape(Role role, float x, float y, float width, float height) {
			this.role = role;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public Role getRole() {
			return this.role;
		}

		public float getX() {
			return this.x;
		}

		public float getY() {
			return this.y;
		}

		public float getWidth() {
			return this.width;
		}

		public float getHeight() {
			return this.height;
		}

		public String toString() {
			return this.role + " " + this.x + "," + this.y + " " + this.width + "x" + this.height;
		}
	}

	private TGMeterLook() {
	}

	// the shapes of the reading on a meter as wide and high as given: two bars (left, right) or one (the louder side)
	public static List<Shape> shapes(TGMeterReading reading, float width, float height, boolean stereo) {
		List<Shape> shapes = new ArrayList<Shape>();
		int bars = (stereo ? 2 : 1);
		float high = (height - GAP * (bars - 1)) / bars;
		// the lamp is as high as the meter
		float wide = width - GAP - height;
		for (int bar = 0; bar < bars; bar++) {
			shapes.addAll(side(reading, stereo, bar, bar * (high + GAP), wide, high));
		}
		shapes.addAll(reading.isOn() ? lamp(reading.isClipped(), wide + GAP, height) : new ArrayList<Shape>());
		return shapes;
	}

	// the bar of a side (of two bars) or of the louder side (of one), or greyed for a meter measuring nothing
	private static List<Shape> side(TGMeterReading reading, boolean stereo, int bar, float top, float wide, float high) {
		double peak = (stereo ? reading.getPeak(bar) : reading.getPeak());
		double hold = (stereo ? reading.getHold(bar) : reading.getHold());
		return (reading.isOn() ? bar(peak, hold, top, wide, high) : greyed(top, wide, high));
	}

	private static List<Shape> greyed(float top, float wide, float high) {
		List<Shape> shapes = new ArrayList<Shape>();
		shapes.add(new Shape(Role.GREYED, 0, top, wide, high));
		return shapes;
	}

	// the track, the bar to the peak (striped above 0 dB), the 0 dB mark, the hold line ending at the hold
	private static List<Shape> bar(double peak, double hold, float top, float wide, float high) {
		List<Shape> shapes = new ArrayList<Shape>();
		float zero = at(0, wide);
		float level = at(peak, wide);
		shapes.add(new Shape(Role.TRACK, 0, top, wide, high));
		if (level > 0) {
			shapes.add(new Shape(Role.BAR, 0, top, Math.min(level, zero), high));
		}
		if (level > zero) {
			shapes.add(new Shape(Role.OVER, zero, top, level - zero, high));
		}
		shapes.add(new Shape(Role.ZERO, zero, top, MARK, high));
		if (!Double.isInfinite(hold)) {
			shapes.add(new Shape(Role.HOLD, Math.max(0, at(hold, wide) - HOLD), top, HOLD, high));
		}
		return shapes;
	}

	// where the level sits along the bar
	private static float at(double decibels, float wide) {
		return (float) (TGMeterScale.position(decibels) * wide);
	}

	// an outline while out; lit, filled with a "!": its stroke and its dot, a sixth of the lamp from its edges
	private static List<Shape> lamp(boolean lit, float left, float size) {
		List<Shape> shapes = new ArrayList<Shape>();
		float sixth = size / 6;
		float middle = left + (size - sixth) / 2;
		shapes.add(new Shape(lit ? Role.LIT : Role.LAMP, left, 0, size, size));
		if (lit) {
			shapes.add(new Shape(Role.BANG, middle, sixth, sixth, size - 4 * sixth));
			shapes.add(new Shape(Role.BANG, middle, size - 2 * sixth, sixth, sixth));
		}
		return shapes;
	}

	// the number under a meter of two bars: the held peak ("-0.4 dB", "-inf dB"); none for a meter measuring nothing
	public static String number(TGMeterReading reading) {
		return (reading.isOn() ? TGMeterScale.number(reading.getHold()) + " dB" : "");
	}

	// what hovering a meter says, from the app's patterns: measured ("{0}: peak {1} dB, held {2} dB", the meter's name,
	// the louder side's peak and hold) or not ("{0}: not measured"), then, while its lamp is lit, the clipped sentence
	public static String tip(String name, TGMeterReading reading, String measured, String off, String clipped) {
		// as the app's messages are filled: each {n} replaced (no MessageFormat: a quote in a message stays as it is)
		String said = (reading.isOn() ? measured : off).replace("{0}", name).replace("{1}", TGMeterScale.number(reading.getPeak())).replace("{2}", TGMeterScale.number(reading.getHold()));
		return said + (reading.isClipped() ? " " + clipped : "");
	}
}
