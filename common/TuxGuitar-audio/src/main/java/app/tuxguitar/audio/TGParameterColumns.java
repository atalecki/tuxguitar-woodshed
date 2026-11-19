package app.tuxguitar.audio;

/**
 * TuxGuitar's parameter rows (a row per parameter: its title, a slider, its value) laid out in the Sound Chain panel's
 * editor area as numbers: as many columns as the area's width holds, filled down each column first (parameters that
 * follow each other stay together, as a list reads), and the area as high as the longest column, at most the share of
 * TuxGuitar's window an editor may take (TGEditorArea.MAX_SHARE): beyond that the rows scroll inside the area. All in the
 * toolkit's units.
 */
public final class TGParameterColumns {

	private TGParameterColumns() {
	}

	// how many columns of rows cellWidth units wide fit width units: at least one, never more than there are rows
	public static int columns(int width, int cellWidth, int count) {
		return Math.max(1, Math.min(count, width / Math.max(1, cellWidth)));
	}

	// the rows in each column (the last may have fewer)
	public static int rows(int count, int columns) {
		return (count + columns - 1) / columns;
	}

	// the row (from 0) of the index-th parameter, a column holding rows of them
	public static int row(int index, int rows) {
		return index % rows;
	}

	// the column (from 0) of the index-th parameter
	public static int column(int index, int rows) {
		return index / rows;
	}

	// the area's height for that many rows rowHeight units high each, TuxGuitar's window windowHeight units high inside
	public static int height(int rows, int rowHeight, int windowHeight) {
		return Math.min(rows * rowHeight, (int) (windowHeight * TGEditorArea.MAX_SHARE));
	}

	// whether the rows scroll inside the area: they need more than its height (its scroll bar shows only then)
	public static boolean scrolls(int rows, int rowHeight, int windowHeight) {
		return (rows * rowHeight > height(rows, rowHeight, windowHeight));
	}
}
