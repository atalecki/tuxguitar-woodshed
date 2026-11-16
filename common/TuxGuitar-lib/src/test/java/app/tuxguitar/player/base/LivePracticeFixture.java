package app.tuxguitar.player.base;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGVoice;
import app.tuxguitar.thread.TGThreadManager;

/**
 * Practice played live (MidiPlayer.practise) by TuxGuitar's sequencer on a synthesizer's sample clock with drums of its
 * own, blocks of 512 frames rendered one at a time, the play loop taking a turn after each. The song has 4 measures of
 * 4/4 at 120 bpm (a quarter note lasts BEAT frames at 44100 Hz), a quarter note on the first beat of each, key 64 + its
 * number on channel 1. A note or click started is written "frame channel key"; the drums are channel -2, the click key
 * 37.
 */
final class LivePracticeFixture {

	static final int BLOCK = 512;
	static final int BEAT = 22050;
	private static final int MOST_BLOCKS = 6000;

	final PlayerSongFixture song = new PlayerSongFixture();
	final MidiPlayer player = song.player;
	final List<String> received = new ArrayList<String>();
	private final SteppedThreads threads = new SteppedThreads();
	private final ClockedSynthesizer synthesizer = new ClockedSynthesizer(received, true);

	LivePracticeFixture() {
		TGThreadManager.getInstance(song.context).setThreadHandler(threads);
		player.loadSequencer(new MidiSequencerImpl(song.context));
		player.getSynthesizerProxy().setMidiSynthesizer(synthesizer);
		for (int measure = 1; measure <= 4; measure++) {
			quarter(measure, 1, measure);
		}
	}

	// a quarter note on the beat (from 1) of the measure, string 1 at the fret (key 64 + fret)
	void quarter(int measure, int beat, int fret) {
		TGVoice voice = song.beat(measure).getVoice(0);
		voice.getBeat().setStart(song.start(measure) + (beat - 1) * TGDuration.QUARTER_TIME);
		voice.setEmpty(false);
		voice.getDuration().setValue(TGDuration.QUARTER);
		TGNote note = song.songManager.getFactory().newNote();
		note.setString(1);
		note.setValue(fret);
		voice.addNote(note);
	}

	// once the player has been told to play: the start runs, the sequencer starts on the clock
	void run() {
		threads.runStarted();
		threads.runLoop();
	}

	// Play (after Pause): the sequencer starts again on the clock
	void playAgain() throws MidiPlayerException {
		player.play();
		run();
	}

	// the clock renders a block, the play loop takes a turn: whether the play loop goes on
	boolean turn() {
		try {
			synthesizer.block(BLOCK);
			return player.runPlayLoopProcess();
		} catch (MidiPlayerException e) {
			throw new IllegalStateException(e);
		}
	}

	void blocks(int count) {
		IntStream.range(0, count).forEach(block -> this.turn());
	}

	// blocks and turns until the play loop ends: how many blocks
	int blocksToTheEnd() {
		return IntStream.rangeClosed(1, MOST_BLOCKS).filter(block -> !this.turn()).findFirst().orElse(MOST_BLOCKS);
	}

	// the notes and clicks started, "frame channel key", comma separated
	String started() {
		return received.stream().filter(line -> line.contains(" on ")).map(line -> line.replaceAll(" on (-?\\d+) (\\d+) .*", " $1 $2"))
			.collect(Collectors.joining(", "));
	}

	// the frame the first note or click started on
	long firstStarted() {
		return Long.parseLong(started().replaceAll(" .*", ""));
	}

	// the notes and clicks started from the frame to the one given, as started() writes them
	String startedBetween(long from, long to) {
		return Stream.of(started().split(", ")).filter(line -> Long.parseLong(line.replaceAll(" .*", "")) >= from && Long.parseLong(line.replaceAll(" .*", "")) <= to)
			.collect(Collectors.joining(", "));
	}
}
