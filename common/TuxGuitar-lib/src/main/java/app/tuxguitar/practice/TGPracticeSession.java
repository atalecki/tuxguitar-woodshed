package app.tuxguitar.practice;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * Interleaved practice of a song as it was started: its settings checked once, its pool (the units it chooses from, in
 * order), its number of turns (or its interval clock, which times it), its rules and whether each turn has a Count In
 * (its own, whether or not the player's Count In is on); under way until it ends. The units' weights are read from the song's
 * difficulty flags when asked, so a flag set during the session counts.
 */
public final class TGPracticeSession {

	private final TGSong song;
	private final List<TGPracticeUnit> pool;
	private final int turns;
	private final TGPracticeClock clock;
	private final boolean noConsecutiveRepeats;
	private final boolean completeCycle;
	private final boolean countInEachTurn;
	// whether it has ended: set once, on the thread that ends it (the player's or the app's), read on any
	private volatile boolean ended;

	private TGPracticeSession(TGSong song, List<TGPracticeUnit> pool, TGPracticeSettings settings, TGPracticeClock clock) {
		this.song = song;
		this.pool = Collections.unmodifiableList(pool);
		this.clock = clock;
		this.turns = (clock != null ? clock.mostTurns() : settings.getTurns());
		// with the clock, a pool of one unit plays it every turn
		this.noConsecutiveRepeats = settings.isNoConsecutiveRepeats() && !(clock != null && pool.size() == 1);
		this.completeCycle = settings.isCompleteCycle();
		this.countInEachTurn = settings.isCountInEachTurn();
	}

	// the session as set, or the reason it cannot be played
	public static TGPracticeSession start(TGPracticeSettings settings, TGSong song) throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(settings.getMeasuresPerLine(), settings.getLinesPerPage());
		List<TGPracticeUnit> pool = pool(grouping, settings, song);
		TGPracticeClock clock = clock(settings);
		TGPracticeSession session = new TGPracticeSession(song, pool, settings, clock);
		session.check(session.weights(), session.turns);
		return session;
	}

	// the interval clock as set (null: off, the number of turns set, at least 1)
	private static TGPracticeClock clock(TGPracticeSettings settings) throws TGPracticeRefusal {
		if (settings.isIntervalClock()) {
			return TGPracticeClock.of(settings);
		}
		if (settings.getTurns() < 1) {
			throw new TGPracticeRefusal("A session needs at least 1 turn.");
		}
		return null;
	}

	private static List<TGPracticeUnit> pool(TGPracticeGrouping grouping, TGPracticeSettings settings, TGSong song) throws TGPracticeRefusal {
		return grouping.units(settings.getUnit(), settings.getPoolFrom(), settings.getPoolTo(), song, "pool");
	}

	// refuses weights that cannot keep "No consecutive repeats" (when it is on) for that many turns
	void check(int[] weights, int turnCount) throws TGPracticeRefusal {
		if (this.noConsecutiveRepeats) {
			TGPracticeRule.check(this.pool, weights, turnCount, this.completeCycle);
		}
	}

	// the interval clock that times the session; none: it plays its number of turns
	public Optional<TGPracticeClock> getClock() {
		return Optional.ofNullable(this.clock);
	}

	public List<TGPracticeUnit> getPool() {
		return this.pool;
	}

	// the number of turns set; with the interval clock, the most turns the session can hold
	public int getTurns() {
		return this.turns;
	}

	// whether no unit may come twice in a row: as set, but never for a timed pool of one unit
	public boolean isNoConsecutiveRepeats() {
		return this.noConsecutiveRepeats;
	}

	public boolean isCompleteCycle() {
		return this.completeCycle;
	}

	// whether each turn has a Count In, whether or not the player's Count In is on
	public boolean isCountInEachTurn() {
		return this.countInEachTurn;
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

	// whether the session has ended: after its last turn, or as Stop, moving the position or an edit ends it
	public boolean isEnded() {
		return this.ended;
	}

	// ends the session; one that has ended stays ended
	public void end() {
		this.ended = true;
	}

	// chooses the session's turns, one by one, at random: the same seed, the same turns
	public TGPracticeChooser chooser(long seed) {
		return new TGPracticeChooser(this, seed);
	}

	// the session's course: its turns chosen from the seed (chooser), each planned as it is chosen (schedule), each
	// turn's Count In as many clicks as the function says (the player's Count In's, whether it is on or not) with Count In
	// before each turn on, at the tempo scale
	public TGPracticeCourse course(long seed, ToIntFunction<TGMeasureHeader> clicks, TGTempoScale scale) {
		return new TGPracticeRandomCourse(this.chooser(seed), this.schedule(clicks, scale));
	}

	// plans the session's turns as they are chosen, its Count In clicks before a turn whose first measure is the header
	// as the function says with Count In before each turn on (0: none), each turn at the tempo scale
	public TGPracticeSchedule schedule(ToIntFunction<TGMeasureHeader> clicks, TGTempoScale scale) {
		return new TGPracticeSchedule(this, this.song, clicks, scale);
	}
}
