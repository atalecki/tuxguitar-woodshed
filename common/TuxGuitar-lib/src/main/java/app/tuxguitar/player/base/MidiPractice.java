package app.tuxguitar.player.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticePlan;
import app.tuxguitar.practice.TGPracticeSchedule;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeTurn;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * An interleaved practice session as the player plays it live: its turns one after the other in the sequence's ticks,
 * from where it starts, each its passes as the session's schedule plans it (MidiPracticeTurn: the first after the
 * break the turn before planned, each its Count In, if any, then its unit's measures), each turn at the tempo scale
 * its plan gives it. A turn is chosen ahead and laid, all its passes, once the turn before it has started (the first one when
 * the session starts); a turn starts with its break, and the chooser is told then, so a flag set before then counts from
 * its cycle when it opens one; after the turn the schedule plans last the sequence ends.
 */
final class MidiPractice {

	private static final long SECOND = 1000000L;

	private final TGPracticeChooser chooser;
	private final TGSong song;
	// the Count In clicks before a turn whose first measure is the header (0: none)
	private final ToIntFunction<TGMeasureHeader> clicks;
	// plans each turn chosen; one for the session, so a pause does not count in its time
	private final TGPracticeSchedule schedule;
	// the passes in the sequence, from its first
	private final List<MidiPracticeTurn> turns = new ArrayList<MidiPracticeTurn>();
	// the same, as read by other threads: a copy made each time a turn is laid (the player's thread lays them)
	private volatile List<MidiPracticeTurn> laid = Collections.emptyList();
	// the plans of the turns chosen, in order
	private final List<TGPracticePlan> plans = new ArrayList<TGPracticePlan>();
	// adds a pass to the sequence; true for its first
	private BiConsumer<MidiPracticeTurn, Boolean> sequence;

	// the session's turns planned at the tempo scale
	MidiPractice(TGPracticeChooser chooser, TGSong song, ToIntFunction<TGMeasureHeader> clicks, TGTempoScale scale) {
		this.chooser = chooser;
		this.song = song;
		this.clicks = clicks;
		this.schedule = chooser.getSession().schedule(clicks, scale);
	}

	TGPracticeChooser getChooser() {
		return this.chooser;
	}

	TGPracticeSession getSession() {
		return this.chooser.getSession();
	}

	// the session's first turn, from the tick, into the sequence
	void start(long tick, BiConsumer<MidiPracticeTurn, Boolean> sequence) {
		this.sequence = sequence;
		this.add(tick);
	}

	// from the tick, into a new sequence: the session's first turn or, after a pause, the pass the tick played was in and
	// those laid after it, as they were laid
	void play(long paused, long tick, BiConsumer<MidiPracticeTurn, Boolean> sequence) {
		if (this.turns.isEmpty()) {
			this.start(tick, sequence);
		} else {
			this.resume(paused, tick, sequence);
		}
	}

	// the paused pass again after a Count In (when Count In is on), without the rest of a break it was paused in
	private void resume(long paused, long tick, BiConsumer<MidiPracticeTurn, Boolean> sequence) {
		int from = this.indexAt(paused);
		List<MidiPracticeTurn> again = new ArrayList<MidiPracticeTurn>(this.turns.subList(from, this.turns.size()));
		this.turns.clear();
		this.sequence = sequence;
		MidiPracticeTurn first = again.get(0);
		this.lay(first.again(tick, this.clicks.applyAsInt(first.getFirst())));
		for (MidiPracticeTurn turn : again.subList(1, again.size())) {
			this.lay(turn.from(this.last().getEnd()));
		}
		this.publish();
	}

	// the sequence has played up to the tick: once the last turn laid has started (the chooser told), the next is laid,
	// unless that was the last
	void follow(long tick) {
		if (tick >= this.opening().getStart()) {
			this.chooser.started();
			this.addAfter();
		}
	}

	private void addAfter() {
		if (!this.lastPlan().isLast()) {
			this.add(this.last().getEnd());
		}
	}

	// the first pass in the sequence of the last turn laid
	private MidiPracticeTurn opening() {
		int turn = this.last().getTurn();
		return this.turns.stream().filter(pass -> pass.getTurn() == turn).findFirst().get();
	}

	// the passes in the sequence, from its first: read without the player's lock
	List<MidiPracticeTurn> getLaid() {
		return this.laid;
	}

	// where the session starts in the sequence
	long getStart() {
		return this.turns.get(0).getStart();
	}

	// the session's turn the tick of the sequence is in, from 1
	int turnAt(long tick) {
		return this.turns.get(this.indexAt(tick)).getTurn();
	}

	// the sequence's pass the tick is in, from 0
	private int indexAt(long tick) {
		int index = 0;
		while (index + 1 < this.turns.size() && this.turns.get(index + 1).getStart() <= tick) {
			index++;
		}
		return index;
	}

	// the session's turn the tick of the sequence is in, as planned; with the interval clock, the seconds left to its end,
	// or in the break before it
	TGPracticeTurn turn(long tick) {
		MidiPracticeTurn pass = this.turns.get(this.indexAt(tick));
		TGPracticePlan plan = this.plans.get(pass.getTurn() - 1);
		if (!this.getSession().getClock().isPresent()) {
			return new TGPracticeTurn(pass.getTurn(), this.getSession().getTurns(), plan);
		}
		if (pass.rests(tick)) {
			return TGPracticeTurn.inBreak(pass.getTurn(), plan, seconds(pass.micros(tick, pass.getCountIn())));
		}
		long left = this.turns.stream().filter(laid -> laid.getTurn() == pass.getTurn()).mapToLong(laid -> laid.micros(tick, laid.getEnd())).sum();
		return TGPracticeTurn.timed(pass.getTurn(), plan, seconds(left));
	}

	// whole seconds, rounded up
	private static int seconds(long micros) {
		return (int) ((micros + SECOND - 1) / SECOND);
	}

	// the song's tick the tick of the sequence plays (see MidiPracticeTurn.songTick)
	long songTick(long tick) {
		return this.turns.get(this.indexAt(tick)).songTick(tick);
	}

	private MidiPracticeTurn last() {
		return this.turns.get(this.turns.size() - 1);
	}

	private TGPracticePlan lastPlan() {
		return this.plans.get(this.plans.size() - 1);
	}

	// the next turn, chosen and planned, from the tick: its passes, the first after the break the turn before planned
	private void add(long start) {
		int rest = (this.plans.isEmpty() ? 0 : this.lastPlan().getBreakSeconds());
		TGPracticePlan plan = this.schedule.plan(this.chooser.ahead());
		this.plans.add(plan);
		int turn = this.plans.size();
		TGMeasureHeader first = this.song.getMeasureHeader(plan.getFirstMeasure() - 1);
		TGMeasureHeader last = this.song.getMeasureHeader(plan.getLastMeasure() - 1);
		int clicks = this.clicks.applyAsInt(first);
		this.lay(MidiPracticeTurn.afterBreak(first, last, start, rest, plan.getScale(), clicks, turn));
		for (int pass = 1; pass < plan.getPasses(); pass++) {
			this.lay(new MidiPracticeTurn(first, last, this.last().getEnd(), 0, (plan.isCountInEveryPass() ? clicks : 0), turn, plan.getScale()));
		}
		this.publish();
	}

	private void lay(MidiPracticeTurn turn) {
		this.turns.add(turn);
		this.sequence.accept(turn, this.turns.size() == 1);
	}

	// the passes laid, for other threads to read: a turn's all at once
	private void publish() {
		this.laid = Collections.unmodifiableList(new ArrayList<MidiPracticeTurn>(this.turns));
	}
}
