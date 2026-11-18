package app.tuxguitar.audio;

import static app.tuxguitar.editor.action.SongActionFixture.reads;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The Sound Chain panel's action line: its groups of buttons (edit, effect, instrument, the dialog's button), as wide as
 * the app measured them, on as few lines as fit the width there is, in order, a gap between two groups on a line; a
 * group wider than the width goes alone on its line. Read as the groups' indexes per line.
 */
class TestSoundChainStrip {

	private static String lines(int available, float... widths) {
		return TGSoundChainStrip.lines(widths, available, 10).toString();
	}

	// each item's place "x,y", then the size the items take "<width>x<height>"
	private static String place(float available, float[] widths, float[] heights) {
		TGSoundChainStrip.Placement placed = TGSoundChainStrip.place(widths, heights, available, 4);
		StringBuilder read = new StringBuilder();
		for (int item = 0; item < widths.length; item++) {
			read.append(placed.getX(item)).append(",").append(placed.getY(item)).append(" ");
		}
		return read.append("| ").append(placed.getWidth()).append("x").append(placed.getHeight()).toString();
	}

	// the panel's lines laid out: a line as high as its highest item, each item centred on it (a label next to a
	// button), the gap between lines too; the items take the widest line's width
	@TestFactory
	Stream<DynamicTest> eachItemIsPlacedOnItsLine() {
		return Stream.of(
			reads("two lines", "0.0,2.5 104.0,0.0 0.0,34.0 | 184.0x54.0", () -> place(200, new float[] {100, 80, 60}, new float[] {25, 30, 20})),
			reads("one line", "0.0,2.5 104.0,0.0 | 184.0x30.0", () -> place(500, new float[] {100, 80}, new float[] {25, 30})),
			reads("nothing", "| 0.0x0.0", () -> place(500, new float[0], new float[0])));
	}

	@TestFactory
	Stream<DynamicTest> theGroupsGoOnAsFewLinesAsFitInOrder() {
		return Stream.of(
			reads("all fit on one line", "[[0, 1, 2]]", () -> lines(300, 100, 80, 60)),
			reads("they fit exactly", "[[0, 1, 2]]", () -> lines(260, 100, 80, 60)),
			reads("one pixel short: the last wraps", "[[0, 1], [2]]", () -> lines(259, 100, 80, 60)),
			reads("narrow: one group per line", "[[0], [1], [2]]", () -> lines(140, 100, 80, 60)),
			reads("a later group fits after a wrap", "[[0], [1, 2]]", () -> lines(150, 100, 80, 50)),
			reads("a group wider than the width goes alone", "[[0], [1], [2, 3]]", () -> lines(120, 50, 400, 50, 50)),
			reads("no width at all", "[[0], [1]]", () -> lines(0, 50, 50)),
			reads("no groups", "[]", () -> lines(300)));
	}
}
