package app.tuxguitar.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.tuxguitar.song.models.TGSong;

/**
 * Lines and pages as fixed groups, counted from measure 1, set for the session whatever the score's layout shows: a
 * line of so many measures, a page of so many lines. The last line or page of a song holds what is left.
 */
public final class TGPracticeGrouping {

	private final int measuresPerLine;
	private final int linesPerPage;

	private TGPracticeGrouping(int measuresPerLine, int linesPerPage) {
		this.measuresPerLine = measuresPerLine;
		this.linesPerPage = linesPerPage;
	}

	public static TGPracticeGrouping of(int measuresPerLine, int linesPerPage) throws TGPracticeRefusal {
		if (measuresPerLine < 1) {
			throw new TGPracticeRefusal("A line needs at least 1 measure.");
		}
		if (linesPerPage < 1) {
			throw new TGPracticeRefusal("A page needs at least 1 line.");
		}
		return new TGPracticeGrouping(measuresPerLine, linesPerPage);
	}

	// the measures a unit of the kind holds, the last one of a song aside
	private int measures(TGPracticeUnitKind kind) {
		switch (kind) {
			case LINE:
				return this.measuresPerLine;
			case PAGE:
				return this.measuresPerLine * this.linesPerPage;
			default:
				return 1;
		}
	}

	// the units of the kind in a song of that many measures
	public int count(TGPracticeUnitKind kind, int songMeasures) {
		return (songMeasures + this.measures(kind) - 1) / this.measures(kind);
	}

	// the last unit a pool of the kind may reach in a song of that many measures, for a form still being filled in: a
	// group size below 1 (refused when the session starts) bounds it as 1 would
	public static int poolLimit(TGPracticeUnitKind kind, int measuresPerLine, int linesPerPage, int songMeasures) {
		return new TGPracticeGrouping(Math.max(1, measuresPerLine), Math.max(1, linesPerPage)).count(kind, songMeasures);
	}

	// the units of the kind in a range from one number to another (from 1) of the kind's range kind, in score order (the
	// beats of measures from and to); or the reason the range, named as the session calls it ("pool"), cannot be played
	public List<TGPracticeUnit> units(TGPracticeUnitKind kind, int from, int to, TGSong song, String range) throws TGPracticeRefusal {
		TGPracticeUnitKind ranged = kind.rangeKind();
		int count = this.count(ranged, song.countMeasureHeaders());
		if (from < 1) {
			throw new TGPracticeRefusal("The " + range + " starts before " + ranged.getName() + " 1.");
		}
		if (from > to) {
			throw new TGPracticeRefusal("The " + range + " is empty.");
		}
		if (to > count) {
			throw new TGPracticeRefusal("The " + range + " reaches past the song's " + count + " " + ranged.getName(count) + ".");
		}
		List<TGPracticeUnit> units = new ArrayList<TGPracticeUnit>();
		for (int number = from; number <= to; number++) {
			units.addAll(this.unitsOf(kind, number, song));
		}
		return units;
	}

	// the units of the kind that the range's unit with that number holds: itself, or a measure's beats
	private List<TGPracticeUnit> unitsOf(TGPracticeUnitKind kind, int number, TGSong song) {
		if (kind == TGPracticeUnitKind.BEAT) {
			return TGPracticeUnit.beats(song.getMeasureHeader(number - 1));
		}
		return Collections.singletonList(this.unit(kind, number, song.countMeasureHeaders()));
	}

	// the unit of the kind with that number (from 1), in a song of that many measures
	public TGPracticeUnit unit(TGPracticeUnitKind kind, int number, int songMeasures) {
		int measures = this.measures(kind);
		return new TGPracticeUnit(kind, number, (number - 1) * measures + 1, Math.min(number * measures, songMeasures));
	}
}
