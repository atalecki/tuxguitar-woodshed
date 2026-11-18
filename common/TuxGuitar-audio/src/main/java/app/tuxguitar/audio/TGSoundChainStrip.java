package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * The Sound Chain panel's action line wraps: its groups of buttons (as wide as the app measured them, in pixels) go on
 * as few lines as fit the width there is, in their order (never reordered), a gap between two groups on one line; a
 * group wider than the width goes alone on its line.
 */
public final class TGSoundChainStrip {

	private TGSoundChainStrip() {
	}

	// the groups' indexes on each line
	public static List<List<Integer>> lines(int[] groupWidths, int available, int gap) {
		List<List<Integer>> lines = new ArrayList<List<Integer>>();
		if (groupWidths.length == 0) {
			return lines;
		}
		lines.add(new ArrayList<Integer>(List.of(0)));
		int width = groupWidths[0];
		for (int group = 1; group < groupWidths.length; group++) {
			int wider = width + gap + groupWidths[group];
			if (wider > available) {
				lines.add(new ArrayList<Integer>());
				wider = groupWidths[group];
			}
			lines.get(lines.size() - 1).add(group);
			width = wider;
		}
		return lines;
	}
}
