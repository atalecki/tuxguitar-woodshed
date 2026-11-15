package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Lines and pages are fixed groups of measures, counted from measure 1, the last one holding what is left; a measure
 * is a unit of its own. A unit reads back as "<first>-<last>".
 */
public class TestPracticeGrouping {

	private static final int SONG = 64;

	private static String unit(TGPracticeGrouping grouping, TGPracticeUnitKind kind, int number) {
		TGPracticeUnit unit = grouping.unit(kind, number, SONG);
		return unit.getFirst() + "-" + unit.getLast();
	}

	private static String units(int perLine, int perPage, int line, int page) throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(perLine, perPage);
		return unit(grouping, TGPracticeUnitKind.LINE, line) + " " + unit(grouping, TGPracticeUnitKind.PAGE, page);
	}

	@Test
	public void linesAndPagesAreFixedGroupsTheLastHoldingWhatIsLeft() throws TGPracticeRefusal {
		assertEquals("9-12 17-32", units(4, 4, 3, 2));
		assertEquals("61-64 49-64", units(4, 4, 16, 4));
		assertEquals("61-64 61-64", units(5, 3, 13, 5));
		assertEquals("64-64 64-64", units(1, 1, 64, 64));
	}

	@Test
	public void aMeasureIsAUnitOfItsOwn() throws TGPracticeRefusal {
		assertEquals("7-7", unit(TGPracticeGrouping.of(5, 3), TGPracticeUnitKind.MEASURE, 7));
	}

	@Test
	public void aSongHoldsAsManyUnitsAsItsMeasuresFill() throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(5, 3);
		assertEquals("64 13 5", grouping.count(TGPracticeUnitKind.MEASURE, SONG) + " " + grouping.count(TGPracticeUnitKind.LINE, SONG)
			+ " " + grouping.count(TGPracticeUnitKind.PAGE, SONG));
	}

	// what a form lets the pool reach: the song's last unit, as the group sizes set so far make it
	@Test
	public void thePoolReachesNoFurtherThanTheSongsLastUnit() {
		assertEquals("64 13 5", TGPracticeGrouping.poolLimit(TGPracticeUnitKind.MEASURE, 5, 3, SONG) + " "
			+ TGPracticeGrouping.poolLimit(TGPracticeUnitKind.LINE, 5, 3, SONG) + " " + TGPracticeGrouping.poolLimit(TGPracticeUnitKind.PAGE, 5, 3, SONG));
	}

	// a group size of 0 is refused when the session starts; until then it bounds the pool as 1 would
	@Test
	public void aGroupSizeOfNoneBoundsThePoolAsOneWould() {
		assertEquals("64 16", TGPracticeGrouping.poolLimit(TGPracticeUnitKind.LINE, 0, 4, SONG) + " "
			+ TGPracticeGrouping.poolLimit(TGPracticeUnitKind.PAGE, 4, 0, SONG));
	}

	@Test
	public void aUnitIsNamedByItsKindAndNumber() throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(4, 4);
		assertEquals("Measure 5, Line 3, Page 2", grouping.unit(TGPracticeUnitKind.MEASURE, 5, SONG) + ", "
			+ grouping.unit(TGPracticeUnitKind.LINE, 3, SONG) + ", " + grouping.unit(TGPracticeUnitKind.PAGE, 2, SONG));
	}

	@Test
	public void aLineNeedsAMeasureAndAPageALine() {
		assertEquals("A line needs at least 1 measure. A page needs at least 1 line.", refusal(0, 4) + " " + refusal(4, 0));
	}

	private static String refusal(int perLine, int perPage) {
		try {
			TGPracticeGrouping.of(perLine, perPage);
			return "accepted";
		} catch (TGPracticeRefusal refusal) {
			return refusal.getMessage();
		}
	}
}
