package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * TuxGuitar's parameter rows in the panel's editor area: as many columns as its width holds, filled down each column
 * first, the area as high as the longest column but never more than half of TuxGuitar's window (the rows scroll beyond).
 */
public class TestParameterColumns {

	// width, a column's width, rows -> columns: as many as fit, one when not even one fits, never more than the rows
	@Test
	public void asManyColumnsAsTheWidthHoldsAtLeastOneAtMostOnePerRow() {
		String columns = TGParameterColumns.columns(1000, 300, 10) + " " + TGParameterColumns.columns(900, 300, 10) + " " + TGParameterColumns.columns(899, 300, 10)
			+ " " + TGParameterColumns.columns(1000, 300, 2) + " " + TGParameterColumns.columns(200, 300, 10) + " " + TGParameterColumns.columns(1000, 300, 0);
		assertEquals("3 3 2 2 1 1", columns);
	}

	// width, columns, a column's width with the narrowest slider (240) -> the slider: one row in a wide area 400, not
	// stretched across it; what a column spares widens it (240 + 110); never narrower than 240 when nothing is spared; in
	// three columns each spares a third (240 + 10), up to 400 again
	@Test
	public void eachSliderTakesWhatItsColumnSparesFromTheNarrowestUpToTheWidest() {
		String sliders = TGParameterColumns.slider(1800, 1, 490) + " " + TGParameterColumns.slider(600, 1, 490) + " " + TGParameterColumns.slider(400, 1, 490)
			+ " " + TGParameterColumns.slider(1500, 3, 490) + " " + TGParameterColumns.slider(3000, 3, 490);
		assertEquals("400 350 240 250 400", sliders);
	}

	@Test
	public void eachColumnHoldsItsShareOfTheRowsTheLastOneFewer() {
		String rows = TGParameterColumns.rows(10, 3) + " " + TGParameterColumns.rows(9, 3) + " " + TGParameterColumns.rows(1, 1) + " " + TGParameterColumns.rows(0, 1);
		assertEquals("4 3 1 0", rows);
	}

	// ten parameters in three columns of four: down the first column, then the next
	@Test
	public void theParametersGoDownEachColumnFirst() {
		String places = IntStream.range(0, 10).mapToObj(index -> TGParameterColumns.row(index, 4) + "," + TGParameterColumns.column(index, 4)).collect(Collectors.joining(" "));
		assertEquals("0,0 1,0 2,0 3,0 0,1 1,1 2,1 3,1 0,2 1,2", places);
	}

	// rows of 30 units in a window 800 high inside: as high as the rows, at most 400 (a part of a unit left out)
	@Test
	public void theAreaIsAsHighAsTheLongestColumnAtMostHalfTheWindow() {
		String heights = TGParameterColumns.height(4, 30, 800) + " " + TGParameterColumns.height(13, 30, 800) + " " + TGParameterColumns.height(20, 20, 800)
			+ " " + TGParameterColumns.height(20, 30, 801) + " " + TGParameterColumns.height(0, 30, 800);
		assertEquals("120 390 400 400 0", heights);
	}

	// beyond that height the rows scroll (the scroll bar shows), not before: 13 rows fit, 14 do not
	@Test
	public void theRowsScrollOnlyWhenTheyNeedMoreThanTheArea() {
		String scrolls = TGParameterColumns.scrolls(13, 30, 800) + " " + TGParameterColumns.scrolls(14, 30, 800) + " " + TGParameterColumns.scrolls(20, 20, 800) + " " + TGParameterColumns.scrolls(0, 30, 800);
		assertEquals("false true false false", scrolls);
	}
}
