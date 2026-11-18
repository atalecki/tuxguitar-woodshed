package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sound Chain panel's action line wraps: its groups of buttons (as wide as the app measured them, in pixels) go on
 * as few lines as fit the width there is, in their order (never reordered), a gap between two groups on one line; a
 * group wider than the width goes alone on its line. The panel's lines are laid out from it (place).
 */
public final class TGSoundChainStrip {

	private TGSoundChainStrip() {
	}

	// the groups' indexes on each line
	public static List<List<Integer>> lines(float[] groupWidths, float available, float gap) {
		List<List<Integer>> lines = new ArrayList<List<Integer>>();
		if (groupWidths.length == 0) {
			return lines;
		}
		lines.add(new ArrayList<Integer>(List.of(0)));
		float width = groupWidths[0];
		for (int group = 1; group < groupWidths.length; group++) {
			float wider = width + gap + groupWidths[group];
			if (wider > available) {
				lines.add(new ArrayList<Integer>());
				wider = groupWidths[group];
			}
			lines.get(lines.size() - 1).add(group);
			width = wider;
		}
		return lines;
	}

	// each item (as wide and high as the app measured it) placed on its line, from the top left: a line as high as its
	// highest item, each item centred on it, a gap between items and between lines
	public static Placement place(float[] widths, float[] heights, float available, float gap) {
		Placement placed = new Placement(widths.length);
		for (List<Integer> line : lines(widths, available, gap)) {
			placed.line(line, widths, heights, gap);
		}
		return placed;
	}

	// where each item goes, and the size they all take (the widest line, the lines' height)
	public static final class Placement {

		private final float[] x;
		private final float[] y;
		private float width;
		private float height;
		private boolean placed;

		Placement(int items) {
			this.x = new float[items];
			this.y = new float[items];
		}

		private void line(List<Integer> line, float[] widths, float[] heights, float gap) {
			float top = (this.placed ? this.height + gap : 0);
			float high = (float) line.stream().mapToDouble(item -> heights[item]).max().getAsDouble();
			float left = 0;
			for (int item : line) {
				this.x[item] = left;
				this.y[item] = top + (high - heights[item]) / 2;
				left += widths[item] + gap;
			}
			this.width = Math.max(this.width, left - gap);
			this.height = top + high;
			this.placed = true;
		}

		public float getX(int item) {
			return this.x[item];
		}

		public float getY(int item) {
			return this.y[item];
		}

		public float getWidth() {
			return this.width;
		}

		public float getHeight() {
			return this.height;
		}
	}
}
