package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

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
		return PracticeRefusals.outcome(() -> TGPracticeGrouping.of(perLine, perPage));
	}

	// "Measure 3 beat 1 (3-3), ...": each unit and the measures it holds
	private static String units(TGSong song, TGPracticeUnitKind kind, int from, int to) throws TGPracticeRefusal {
		return TGPracticeGrouping.of(4, 4).units(kind, from, to, song, "pool").stream().map(unit -> unit + " (" + unit.getFirst() + "-" + unit.getLast() + ")")
			.collect(Collectors.joining(", "));
	}

	// a beat is the unit of its measure's time signature: a measure of 6/8 has 6; a range of beats is a range of measures,
	// each of their beats in score order, a beat holding its measure alone
	@Test
	public void theBeatsOfARangeOfMeasuresAreTheirBeatsInScoreOrder() throws TGPracticeRefusal {
		TGSong song = PracticeSongs.song(SONG);
		PracticeSongs.signature(song, 4, 6, 8);
		assertEquals("Measure 3 beat 1 (3-3), Measure 3 beat 2 (3-3), Measure 3 beat 3 (3-3), Measure 3 beat 4 (3-3), Measure 4 beat 1 (4-4), "
			+ "Measure 4 beat 2 (4-4), Measure 4 beat 3 (4-4), Measure 4 beat 4 (4-4), Measure 4 beat 5 (4-4), Measure 4 beat 6 (4-4)",
			units(song, TGPracticeUnitKind.BEAT, 3, 4));
	}

	@Test
	public void theOtherUnitsOfARangeAreItsUnits() throws TGPracticeRefusal {
		TGSong song = PracticeSongs.song(SONG);
		assertEquals("Line 2 (5-8), Line 3 (9-12) | Measure 64 (64-64)", units(song, TGPracticeUnitKind.LINE, 2, 3) + " | " + units(song, TGPracticeUnitKind.MEASURE, 64, 64));
	}

	private static String beatsRefusal(int from, int to) {
		return PracticeRefusals.outcome(() -> TGPracticeGrouping.of(4, 4).units(TGPracticeUnitKind.BEAT, from, to, PracticeSongs.song(SONG), "pool"));
	}

	// the range of beats is given in measures, and so are its refusals
	@Test
	public void aRangeOfBeatsIsRefusedInMeasures() {
		assertEquals("The pool reaches past the song's 64 measures. | The pool starts before measure 1. | The pool is empty. | accepted",
			beatsRefusal(60, 70) + " | " + beatsRefusal(0, 2) + " | " + beatsRefusal(9, 8) + " | " + beatsRefusal(1, 64));
	}

	// the form bounds a pool of beats by the song's measures
	@Test
	public void aPoolOfBeatsReachesNoFurtherThanTheSongsLastMeasure() throws TGPracticeRefusal {
		assertEquals("64 64", TGPracticeGrouping.poolLimit(TGPracticeUnitKind.BEAT, 5, 3, SONG) + " " + TGPracticeGrouping.of(5, 3).count(TGPracticeUnitKind.BEAT, SONG));
	}

	// how many units a form's pool of the kind holds, counted without building them, for each pool (from, to, from,
	// to...), with lines of so many measures and pages of so many lines
	private static String poolUnits(TGSong song, TGPracticeUnitKind kind, int measuresPerLine, int linesPerPage, int... pools) {
		TGPracticeSettings form = new TGPracticeSettings();
		form.setUnit(kind);
		form.setMeasuresPerLine(measuresPerLine);
		form.setLinesPerPage(linesPerPage);
		return IntStream.range(0, pools.length / 2).mapToObj(pool -> {
			form.setPool(pools[2 * pool], pools[2 * pool + 1]);
			return String.valueOf(TGPracticeGrouping.poolUnits(form, song));
		}).collect(Collectors.joining(" "));
	}

	// measures 2-5, lines 2-3, pages 1-4 of 64 measures; the beats of measures 3-4, 4 of 4/4 and 6 of 6/8
	@Test
	public void aFormsPoolHoldsItsUnitsOrItsMeasuresBeats() {
		TGSong song = PracticeSongs.song(SONG);
		PracticeSongs.signature(song, 4, 6, 8);
		assertEquals("4 2 4 10", poolUnits(song, TGPracticeUnitKind.MEASURE, 4, 4, 2, 5) + " " + poolUnits(song, TGPracticeUnitKind.LINE, 4, 4, 2, 3) + " "
			+ poolUnits(song, TGPracticeUnitKind.PAGE, 4, 4, 1, 4) + " " + poolUnits(song, TGPracticeUnitKind.BEAT, 4, 4, 3, 4));
	}

	// a pool refused when the session starts holds the part of it in the song (from before measure 1, to past the
	// song's end, beats past it too), none when it is empty
	@Test
	public void aFormsPoolHoldsItsPartInTheSongNoneWhenEmpty() {
		TGSong song = PracticeSongs.song(SONG);
		assertEquals("3 5 0 | 20", poolUnits(song, TGPracticeUnitKind.MEASURE, 4, 4, 0, 3, 60, 70, 5, 4) + " | " + poolUnits(song, TGPracticeUnitKind.BEAT, 4, 4, 60, 70));
	}

	// a group size of 0 counts as 1 would: a line of one measure, a page of one line
	@Test
	public void aGroupSizeOfNoneCountsAsOneWould() {
		TGSong song = PracticeSongs.song(SONG);
		assertEquals("64 64", poolUnits(song, TGPracticeUnitKind.LINE, 0, 4, 1, 99) + " " + poolUnits(song, TGPracticeUnitKind.PAGE, 1, 0, 1, 99));
	}

	private static String beat(TGMeasureHeader header, int beat) {
		try {
			TGPracticeUnit unit = TGPracticeUnit.beat(header, beat);
			return unit.getKind() + " " + unit.getNumber() + " " + unit.getFirst() + "-" + unit.getLast();
		} catch (IllegalArgumentException refused) {
			return refused.getMessage();
		}
	}

	// a beat from 1 to its measure's numerator, numbered in its measure
	@Test
	public void aBeatLiesInItsMeasure() {
		TGSong song = PracticeSongs.song(SONG);
		PracticeSongs.signature(song, 7, 3, 4);
		TGMeasureHeader header = song.getMeasureHeader(6);
		assertEquals("Measure 7 has no beat 0. | BEAT 1 7-7 | BEAT 3 7-7 | Measure 7 has no beat 4.", beat(header, 0) + " | " + beat(header, 1) + " | "
			+ beat(header, 3) + " | " + beat(header, 4));
	}

	@Test
	public void aKindOfUnitIsGivenInRangesOfItsOwnButBeatsInMeasures() {
		assertEquals("[MEASURE, LINE, PAGE, MEASURE] beat beats", Arrays.stream(TGPracticeUnitKind.values()).map(TGPracticeUnitKind::rangeKind)
			.collect(Collectors.toList()) + " " + TGPracticeUnitKind.BEAT.getName() + " " + TGPracticeUnitKind.BEAT.getName(2));
	}

	// line 3 of measures 9 to 12, built again; then with another kind, number, first or last measure, and a measure
	@Test
	public void aUnitIsTheSameAsAnotherOfTheSameKindNumberAndMeasures() {
		TGPracticeUnit line = new TGPracticeUnit(TGPracticeUnitKind.LINE, 3, 9, 12);
		Object[] others = {new TGPracticeUnit(TGPracticeUnitKind.LINE, 3, 9, 12), new TGPracticeUnit(TGPracticeUnitKind.PAGE, 3, 9, 12),
			new TGPracticeUnit(TGPracticeUnitKind.LINE, 4, 9, 12), new TGPracticeUnit(TGPracticeUnitKind.LINE, 3, 8, 12),
			new TGPracticeUnit(TGPracticeUnitKind.LINE, 3, 9, 11), "Line 3"};
		assertEquals("[true, false, false, false, false, false] true", Arrays.stream(others).map(line::equals).collect(Collectors.toList()) + " "
			+ (line.hashCode() == others[0].hashCode()));
	}
}
