package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGVoice;
import app.tuxguitar.thread.TGThreadManager;

/**
 * An interleaved practice session played live (MidiPlayer.practise) by TuxGuitar's sequencer on a synthesizer's sample
 * clock with drums of its own, blocks of 512 frames rendered one at a time, the play loop taking a turn after each: its
 * turns back to back, each a Count In on the drums then its measure; the player stops by itself after the last turn.
 * Stop, moving the position and the session ending (as an edit ends it) end it.
 *
 * The song has 4 measures of 4/4 at 120 bpm (a quarter note lasts 22050 frames at 44100 Hz), a quarter note on the
 * first beat of each, key 64 + its number on channel 1; the pool is measures 1 to 4. A note or click is written
 * "frame channel key"; the drums are channel -2, the click key 37.
 */
public class TestMidiPlayerPractice {

	private static final int BLOCK = 512;
	private static final int MOST_BLOCKS = 3000;
	private static final long SEED = 1;
	private static final int BEAT = 22050;

	private final PlayerSongFixture fixture = new PlayerSongFixture();
	private final MidiPlayer player = fixture.player;
	private final SteppedThreads threads = new SteppedThreads();
	private final List<String> received = new ArrayList<String>();
	private final ClockedSynthesizer synthesizer = new ClockedSynthesizer(received, true);
	private TGPracticeSession session;

	public TestMidiPlayerPractice() {
		TGThreadManager.getInstance(fixture.context).setThreadHandler(threads);
		player.loadSequencer(new MidiSequencerImpl(fixture.context));
		player.getSynthesizerProxy().setMidiSynthesizer(synthesizer);
		for (int measure = 1; measure <= 4; measure++) {
			quarter(measure);
		}
	}

	private void quarter(int measure) {
		TGVoice voice = fixture.beat(measure).getVoice(0);
		voice.setEmpty(false);
		voice.getDuration().setValue(TGDuration.QUARTER);
		TGNote note = fixture.songManager.getFactory().newNote();
		note.setString(1);
		note.setValue(measure);
		voice.addNote(note);
	}

	// practises so many turns: the start runs, the sequencer starts on the clock
	private void practise(int turns) throws MidiPlayerException, TGPracticeRefusal {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, 4);
		settings.setTurns(turns);
		session = TGPracticeSession.start(settings, fixture.song);
		player.practise(session, SEED);
		threads.runStarted();
		threads.runLoop();
	}

	// the measures the seed chooses, in order
	private List<Integer> chosen(int turns) {
		return TestMidiPractice.chosen(session, SEED, turns);
	}

	// the clock renders a block, the play loop takes a turn: whether the play loop goes on
	private boolean turn() {
		try {
			synthesizer.block(BLOCK);
			return player.runPlayLoopProcess();
		} catch (MidiPlayerException e) {
			throw new IllegalStateException(e);
		}
	}

	// blocks and turns until the play loop ends: how many blocks
	private int blocksToTheEnd() {
		return IntStream.rangeClosed(1, MOST_BLOCKS).filter(block -> !this.turn()).findFirst().orElse(MOST_BLOCKS);
	}

	private void blocks(int count) {
		IntStream.range(0, count).forEach(block -> this.turn());
	}

	// the notes and clicks started, "frame channel key", comma separated
	private String started() {
		return received.stream().filter(line -> line.contains(" on ")).map(line -> line.replaceAll(" on (-?\\d+) (\\d+) .*", " $1 $2"))
			.collect(Collectors.joining(", "));
	}

	// the Count In's clicks from the frame, then the measure's note
	private static String turn(long frame, int measure) {
		return IntStream.range(0, 4).mapToObj(click -> (frame + click * BEAT) + " -2 37").collect(Collectors.joining(", ")) + ", "
			+ (frame + 4 * BEAT) + " 1 " + (64 + measure);
	}

	private String state() {
		return player.isRunning() + " " + session.isEnded() + " " + (TGPracticeUnderWay.getInstance(fixture.context).getChooser() != null);
	}

	@Test
	public void eachTurnPlaysItsCountInThenItsMeasureAndThePlayerStopsAfterTheLast() throws Exception {
		player.getCountDown().setEnabled(true);
		practise(3);
		String underWay = state();
		int blocks = blocksToTheEnd();
		List<Integer> measures = chosen(3);
		assertEquals(turn(0, measures.get(0)) + ", " + turn(8 * BEAT, measures.get(1)) + ", " + turn(16 * BEAT, measures.get(2)), started());
		assertEquals("true false true | false true false | 1034", underWay + " | " + state() + " | " + blocks);
	}

	// Play looped and Training Mode take no part: the session plays at the percent Training Mode starts at
	@Test
	public void aSessionPlaysAtThePercentTrainingModeStartsAtWithoutLooping() throws Exception {
		fixture.loopMode(1, 2);
		player.getMode().setType(MidiPlayerMode.TYPE_CUSTOM);
		player.getMode().setCustomPercentFrom(50);
		player.getMode().setCurrentPercent(80);
		practise(2);
		blocksToTheEnd();
		List<Integer> measures = chosen(2);
		assertEquals((2 * 0) + " 1 " + (64 + measures.get(0)) + ", " + (8 * BEAT) + " 1 " + (64 + measures.get(1)), started());
	}

	// during a turn's Count In the player is on its measure's first tick
	@Test
	public void throughATurnsCountInThePlayerIsOnItsFirstMeasure() throws Exception {
		player.getCountDown().setEnabled(true);
		practise(2);
		blocks(100);
		assertEquals(fixture.start(chosen(1).get(0)), player.getTickPosition());
	}

	@Test
	public void stopEndsTheSession() throws Exception {
		practise(3);
		blocks(10);
		player.stop();
		assertEquals("false true false", state());
	}

	// as an edit ends it: the player stops at its next turn, and the blocks after send nothing
	@Test
	public void aSessionThatEndsStopsThePlayer() throws Exception {
		practise(3);
		blocks(10);
		session.end();
		turn();
		received.clear();
		blocks(400);
		assertEquals("false true false | ", state() + " | " + started());
	}

	// moved to measure 3 during the second turn: from the next block on the song plays measure 3, then measure 4, to its end
	@Test
	public void movingThePositionEndsTheSessionAndTheSongPlaysOnFromThere() throws Exception {
		practise(3);
		blocks(200);
		player.setTickPosition(fixture.start(3));
		String moved = state();
		received.clear();
		blocks(400);
		assertEquals("true true false | 102400 1 67, 190600 1 68", moved + " | " + started());
	}

	// Play looped turned on during the session counts once it has ended: after measure 4 the song plays measure 3 again
	@Test
	public void afterASessionTheSongLoopsAsTheModeSaysNow() throws Exception {
		practise(3);
		blocks(200);
		fixture.loopMode(3, 4);
		player.setTickPosition(fixture.start(3));
		received.clear();
		blocks(500);
		assertEquals("102400 1 67, 190600 1 68, 278800 1 67", started());
	}
}
