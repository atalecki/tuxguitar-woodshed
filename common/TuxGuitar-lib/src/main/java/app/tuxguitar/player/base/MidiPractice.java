package app.tuxguitar.player.base;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * An interleaved practice session as the player plays it live: its turns one after the other in the sequence's ticks,
 * from where it starts, each its Count In then its unit's measures (MidiPracticeTurn). A turn is chosen ahead and added
 * to the sequence once the turn before it has started (the first one when the session starts), and the chooser is told
 * when it starts, so a flag set before then counts from its cycle when it opens one; after the last turn the sequence
 * ends.
 */
final class MidiPractice {

	private final TGPracticeChooser chooser;
	private final TGSong song;
	// the Count In clicks before a turn whose first measure is the header (0: none)
	private final ToIntFunction<TGMeasureHeader> clicks;
	// the turns in the sequence, from its first
	private final List<MidiPracticeTurn> turns = new ArrayList<MidiPracticeTurn>();
	// the session's turns before the sequence's first: those played before a pause
	private int before;
	// adds a turn to the sequence; true for its first turn
	private BiConsumer<MidiPracticeTurn, Boolean> sequence;

	MidiPractice(TGPracticeChooser chooser, TGSong song, ToIntFunction<TGMeasureHeader> clicks) {
		this.chooser = chooser;
		this.song = song;
		this.clicks = clicks;
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

	// from the tick, into a new sequence: the session's first turn or, after a pause, the turn the tick played was in
	// and those chosen after it, each with its Count In, as they were chosen
	void play(long paused, long tick, BiConsumer<MidiPracticeTurn, Boolean> sequence) {
		if (this.turns.isEmpty()) {
			this.start(tick, sequence);
		} else {
			this.resume(paused, tick, sequence);
		}
	}

	private void resume(long paused, long tick, BiConsumer<MidiPracticeTurn, Boolean> sequence) {
		int from = this.indexAt(paused);
		List<MidiPracticeTurn> again = new ArrayList<MidiPracticeTurn>(this.turns.subList(from, this.turns.size()));
		this.before += from;
		this.turns.clear();
		this.sequence = sequence;
		long start = tick;
		for (MidiPracticeTurn turn : again) {
			this.lay(turn.from(start));
			start = this.last().getEnd();
		}
	}

	// the sequence has played up to the tick: once the last turn added has started (the chooser told), the next is added
	void follow(long tick) {
		MidiPracticeTurn last = this.last();
		if (tick >= last.getStart()) {
			this.chooser.started();
			this.addAfter(last);
		}
	}

	private void addAfter(MidiPracticeTurn last) {
		if (this.before + this.turns.size() < this.getSession().getTurns()) {
			this.add(last.getEnd());
		}
	}

	// where the session starts in the sequence
	long getStart() {
		return this.turns.get(0).getStart();
	}

	// the session's turn the tick of the sequence is in, from 1
	int turnAt(long tick) {
		return this.before + this.indexAt(tick) + 1;
	}

	// the sequence's turn the tick is in, from 0
	private int indexAt(long tick) {
		int index = 0;
		while (index + 1 < this.turns.size() && this.turns.get(index + 1).getStart() <= tick) {
			index++;
		}
		return index;
	}

	// the song's tick the tick of the sequence plays (see MidiPracticeTurn.songTick)
	long songTick(long tick) {
		return this.turns.get(this.indexAt(tick)).songTick(tick);
	}

	private MidiPracticeTurn last() {
		return this.turns.get(this.turns.size() - 1);
	}

	private void add(long start) {
		TGPracticeUnit unit = this.chooser.ahead();
		TGMeasureHeader first = this.song.getMeasureHeader(unit.getFirst() - 1);
		this.lay(new MidiPracticeTurn(first, this.song.getMeasureHeader(unit.getLast() - 1), start, this.clicks.applyAsInt(first)));
	}

	private void lay(MidiPracticeTurn turn) {
		this.turns.add(turn);
		this.sequence.accept(turn, this.turns.size() == 1);
	}
}
