package app.tuxguitar.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

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
			throw new TGPracticeRefusal("transport.practice.refusal.measures-per-line");
		}
		if (linesPerPage < 1) {
			throw new TGPracticeRefusal("transport.practice.refusal.lines-per-page");
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

	// the groups of a form still being filled in: a group size below 1 (refused when the session starts) as 1
	private static TGPracticeGrouping ofForm(int measuresPerLine, int linesPerPage) {
		return new TGPracticeGrouping(Math.max(1, measuresPerLine), Math.max(1, linesPerPage));
	}

	// the last unit a pool of the kind may reach in a song of that many measures, for a form still being filled in
	public static int poolLimit(TGPracticeUnitKind kind, int measuresPerLine, int linesPerPage, int songMeasures) {
		return ofForm(measuresPerLine, linesPerPage).count(kind, songMeasures);
	}

	// how many units the pool of a form still being filled in holds, counted without building them: 1 for each unit of
	// its range, or each measure's beats for beats; the numbers before 1 or past the song's end left out; 0 when that
	// leaves none
	static int poolUnits(TGPracticeSettings form, TGSong song) {
		TGPracticeUnitKind kind = form.getUnit();
		TGPracticeGrouping grouping = ofForm(form.getMeasuresPerLine(), form.getLinesPerPage());
		int last = Math.min(form.getPoolTo(), grouping.count(kind.rangeKind(), song.countMeasureHeaders()));
		return IntStream.rangeClosed(Math.max(1, form.getPoolFrom()), last)
			.map(number -> (kind == TGPracticeUnitKind.BEAT ? TGPracticeUnit.beatCount(song.getMeasureHeader(number - 1)) : 1)).sum();
	}

	// the units of the kind in a range from one number to another (from 1) of the kind's range kind, in score order (the
	// beats of measures from and to); or the reason the range cannot be played, in the messages of the range's key
	// ("transport.practice.refusal.pool": its -starts-before, -empty and -past-end)
	public List<TGPracticeUnit> units(TGPracticeUnitKind kind, int from, int to, TGSong song, String range) throws TGPracticeRefusal {
		TGPracticeUnitKind ranged = kind.rangeKind();
		int count = this.count(ranged, song.countMeasureHeaders());
		if (from < 1) {
			throw new TGPracticeRefusal(range + "-starts-before", TGPracticeWording.noun(ranged, 1, TGPracticeWording.APP));
		}
		if (from > to) {
			throw new TGPracticeRefusal(range + "-empty");
		}
		if (to > count) {
			throw new TGPracticeRefusal(range + "-past-end", String.valueOf(count), TGPracticeWording.noun(ranged, count, TGPracticeWording.APP));
		}
		return this.unitsIn(kind, from, to, song);
	}

	// the units of the kind that the range's units with the numbers from and to hold, in score order
	private List<TGPracticeUnit> unitsIn(TGPracticeUnitKind kind, int from, int to, TGSong song) {
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
