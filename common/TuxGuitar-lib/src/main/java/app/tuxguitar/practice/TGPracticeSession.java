package app.tuxguitar.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;

import app.tuxguitar.song.models.TGSong;

/**
 * Interleaved practice of a song as it was started: its settings checked once, its pool (the units it chooses from, in
 * order), its number of turns and its rules. The units' weights are read from the song's difficulty flags when asked,
 * so a flag set during the session counts.
 */
public final class TGPracticeSession {

	private final TGSong song;
	private final List<TGPracticeUnit> pool;
	private final int turns;
	private final boolean noConsecutiveRepeats;
	private final boolean completeCycle;

	private TGPracticeSession(TGSong song, List<TGPracticeUnit> pool, TGPracticeSettings settings) {
		this.song = song;
		this.pool = Collections.unmodifiableList(pool);
		this.turns = settings.getTurns();
		this.noConsecutiveRepeats = settings.isNoConsecutiveRepeats();
		this.completeCycle = settings.isCompleteCycle();
	}

	// the session as set, or the reason it cannot be played
	public static TGPracticeSession start(TGPracticeSettings settings, TGSong song) throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(settings.getMeasuresPerLine(), settings.getLinesPerPage());
		List<TGPracticeUnit> pool = pool(grouping, settings, song.countMeasureHeaders());
		if (settings.getTurns() < 1) {
			throw new TGPracticeRefusal("A session needs at least 1 turn.");
		}
		TGPracticeSession session = new TGPracticeSession(song, pool, settings);
		session.check(session.weights(), session.turns);
		return session;
	}

	private static List<TGPracticeUnit> pool(TGPracticeGrouping grouping, TGPracticeSettings settings, int songMeasures) throws TGPracticeRefusal {
		TGPracticeUnitKind kind = settings.getUnit();
		int count = grouping.count(kind, songMeasures);
		if (settings.getPoolFrom() < 1) {
			throw new TGPracticeRefusal("The pool starts before " + kind.getName() + " 1.");
		}
		if (settings.getPoolFrom() > settings.getPoolTo()) {
			throw new TGPracticeRefusal("The pool is empty.");
		}
		if (settings.getPoolTo() > count) {
			throw new TGPracticeRefusal("The pool reaches past the song's " + count + " " + kind.getName(count) + ".");
		}
		List<TGPracticeUnit> pool = new ArrayList<TGPracticeUnit>();
		for (int number = settings.getPoolFrom(); number <= settings.getPoolTo(); number++) {
			pool.add(grouping.unit(kind, number, songMeasures));
		}
		return pool;
	}

	// refuses weights that cannot keep "No consecutive repeats" (when it is on) for that many turns
	void check(int[] weights, int turnCount) throws TGPracticeRefusal {
		if (this.noConsecutiveRepeats) {
			TGPracticeRule.check(this.pool, weights, turnCount, this.completeCycle);
		}
	}

	public List<TGPracticeUnit> getPool() {
		return this.pool;
	}

	public int getTurns() {
		return this.turns;
	}

	public boolean isNoConsecutiveRepeats() {
		return this.noConsecutiveRepeats;
	}

	public boolean isCompleteCycle() {
		return this.completeCycle;
	}

	// each unit's weight, by unit of the pool, as the song's flags are now
	public int[] weights() {
		return this.weights(TGPracticeUnit.difficulties(this.song));
	}

	// as weights(), the measure given (from 1) read as flagged that difficult
	int[] weights(int measure, int difficulty) {
		IntUnaryOperator flags = TGPracticeUnit.difficulties(this.song);
		return this.weights(number -> (number == measure ? difficulty : flags.applyAsInt(number)));
	}

	private int[] weights(IntUnaryOperator difficulties) {
		return this.pool.stream().mapToInt(unit -> unit.weight(difficulties)).toArray();
	}

	// chooses the session's turns, one by one, at random: the same seed, the same turns
	public TGPracticeChooser chooser(long seed) {
		return new TGPracticeChooser(this, seed);
	}
}
