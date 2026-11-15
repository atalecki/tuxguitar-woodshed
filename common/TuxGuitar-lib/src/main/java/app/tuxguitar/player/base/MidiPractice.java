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
 * from where it starts, each its Count In then its unit's measures (MidiPracticeTurn). A turn is chosen and added to the
 * sequence once the turn before it has started (the first one when the session starts), so a flag set meanwhile counts
 * from the next cycle the chooser starts; after the last turn the sequence ends.
 */
final class MidiPractice {

	private final TGPracticeChooser chooser;
	private final TGSong song;
	// the Count In clicks before a turn whose first measure is the header (0: none)
	private final ToIntFunction<TGMeasureHeader> clicks;
	private final List<MidiPracticeTurn> turns = new ArrayList<MidiPracticeTurn>();
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

	// the sequence has played up to the tick: the next turn is added once the last one added has started
	void follow(long tick) {
		MidiPracticeTurn last = this.last();
		if (this.turns.size() < this.getSession().getTurns() && tick >= last.getStart()) {
			this.add(last.getEnd());
		}
	}

	// where the session starts in the sequence
	long getStart() {
		return this.turns.get(0).getStart();
	}

	// the turn the tick of the sequence is in, from 1
	int turnAt(long tick) {
		int turn = 1;
		while (turn < this.turns.size() && this.turns.get(turn).getStart() <= tick) {
			turn++;
		}
		return turn;
	}

	// the song's tick the tick of the sequence plays (see MidiPracticeTurn.songTick)
	long songTick(long tick) {
		return this.turns.get(this.turnAt(tick) - 1).songTick(tick);
	}

	private MidiPracticeTurn last() {
		return this.turns.get(this.turns.size() - 1);
	}

	private void add(long start) {
		TGPracticeUnit unit = this.chooser.next();
		TGMeasureHeader first = this.song.getMeasureHeader(unit.getFirst() - 1);
		MidiPracticeTurn turn = new MidiPracticeTurn(first, this.song.getMeasureHeader(unit.getLast() - 1), start, this.clicks.applyAsInt(first));
		this.turns.add(turn);
		this.sequence.accept(turn, this.turns.size() == 1);
	}
}
