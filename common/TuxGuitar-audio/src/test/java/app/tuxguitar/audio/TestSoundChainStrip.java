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

	private static String lines(int available, int... widths) {
		return TGSoundChainStrip.lines(widths, available, 10).toString();
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
