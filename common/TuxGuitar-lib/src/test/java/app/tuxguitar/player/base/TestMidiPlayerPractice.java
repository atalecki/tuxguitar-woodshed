package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeTurn;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.practice.TGPracticeUnitKind;

/**
 * An interleaved practice session played live (MidiPlayer.practise) as LivePracticeFixture plays it, the player's Count
 * In off: its turns back to back, each its own Count In on the drums (Count In before each turn, on until turned off)
 * then its measure; the player stops by itself after the last turn. Stop, moving the position and the session ending (as
 * an edit ends it) end it. The pool is measures 1 to 4 of the fixture's song.
 */
public class TestMidiPlayerPractice {

	private static final long SEED = 1;
	private static final int BLOCK = LivePracticeFixture.BLOCK;
	private static final int BEAT = LivePracticeFixture.BEAT;

	private final LivePracticeFixture live = new LivePracticeFixture();
	private final PlayerSongFixture fixture = live.song;
	private final MidiPlayer player = live.player;
	private final List<String> received = live.received;
	private final TGPracticeSettings settings = new TGPracticeSettings();
	private TGPracticeSession session;

	// practises so many turns: the start runs, the sequencer starts on the clock
	private void practise(int turns) throws MidiPlayerException, TGPracticeRefusal {
		practise(TGPracticeUnitKind.MEASURE, 4, turns);
	}

	// as practise(turns), the units of the kind in measures 1 to the one given
	private void practise(TGPracticeUnitKind kind, int to, int turns) throws MidiPlayerException, TGPracticeRefusal {
		settings.setUnit(kind);
		settings.setPool(1, to);
		settings.setTurns(turns);
		session = TGPracticeSession.start(settings, fixture.song);
		player.practise(session, SEED);
		live.run();
	}

	// practises a timed session (the interval clock: turns of 1 minute, breaks of 10 seconds, sessions of 2 minutes):
	// turn 1 is its Count In then 29 passes of its measure, 60 seconds (2646000 frames); turn 2 follows after the break,
	// the last
	private void practiseTimed() throws MidiPlayerException, TGPracticeRefusal {
		settings.setPool(1, 4);
		settings.setIntervalClock(true);
		settings.setTurnMinutes(1);
		settings.setBreakSeconds(10);
		settings.setSessionMinutes(2);
		session = TGPracticeSession.start(settings, fixture.song);
		player.practise(session, SEED);
		live.run();
	}

	// the break lasts its 10 seconds (441000 frames) from the end of turn 1's last pass (its note on the pass's first beat)
	// to turn 2's first click, and nothing starts in it
	@Test
	public void aTimedTurnsBreakIsSilentAndLastsItsSeconds() throws Exception {
		practiseTimed();
		live.blocks(6100);
		assertEquals("2557800 1 " + (64 + chosen(1).get(0)) + ", 3087000 -2 76", live.startedBetween(2557800, 3087000));
	}

	// paused 5.5 seconds into the break (the break before turn 2 shown, 5 seconds left), Play plays turn 2's Count In at once, from the
	// frame it starts on, then its measure
	@Test
	public void pausedInABreakPlayStartsTheNextTurnsCountInAtOnce() throws Exception {
		practiseTimed();
		live.blocks((2646000 + 242550) / BLOCK);
		TGPracticeTurn shown = player.getPracticeTurn().get();
		player.pause();
		received.clear();
		live.playAgain();
		live.blocks(200);
		long from = live.firstStarted();
		assertEquals("break 2 5 | " + turn(from, chosen(2).get(1)), (shown.isBreak() ? "break " : "turn ") + shown.getTurn() + " " + shown.getSecondsLeft() + " | "
			+ live.startedBetween(from, from + 4 * BEAT));
	}

	// the measures the seed chooses, in order
	private List<Integer> chosen(int turns) {
		return TestMidiPractice.chosen(session, SEED, turns);
	}

	// the Count In's clicks from the frame, then the measure's note
	private static String turn(long frame, int measure) {
		return clicks(frame, 1) + ", " + (frame + 4 * BEAT) + " 1 " + (64 + measure);
	}

	// the Count In's 4 clicks from the frame before the beat (from 1) of a 4/4 measure: the drums' accented note (76) on
	// the one on beat 1, counted back from that beat, the plain one (77) on the others (before beat 2: "2 3 4 | 1")
	private static String clicks(long frame, int beat) {
		return IntStream.range(0, 4).mapToObj(click -> (frame + click * BEAT) + ((beat - 1 + click) % 4 == 0 ? " -2 76" : " -2 77")).collect(Collectors.joining(", "));
	}

	private String state() {
		return player.isRunning() + " " + session.isEnded() + " " + (TGPracticeUnderWay.getInstance(fixture.context).getCourse() != null);
	}

	// on a synthesizer without drums of its own (as on a port), a song without a percussion channel counts each turn in on
	// the player's own percussion channel, -3 (see TestMidiPlayerMetronome)
	@Test
	public void withoutDrumsOrAPercussionChannelATurnCountsInOnThePlayersPercussionChannel() throws Exception {
		LivePracticeFixture port = new LivePracticeFixture(false);
		settings.setPool(1, 4);
		settings.setTurns(1);
		session = TGPracticeSession.start(settings, port.song.song);
		port.player.practise(session, SEED);
		port.run();
		port.blocksToTheEnd();

		// the click sound's drum note on every click: no accent there
		assertEquals(turn(0, chosen(1).get(0)).replaceAll(" -2 7[67]", " -3 37"), port.started());
	}

	@Test
	public void eachTurnPlaysItsCountInThenItsMeasureAndThePlayerStopsAfterTheLast() throws Exception {
		practise(3);
		String underWay = state();
		int blocks = live.blocksToTheEnd();
		List<Integer> measures = chosen(3);
		assertEquals(turn(0, measures.get(0)) + ", " + turn(8 * BEAT, measures.get(1)) + ", " + turn(16 * BEAT, measures.get(2)), live.started());
		assertEquals("true false true | false true false | 1034", underWay + " | " + state() + " | " + blocks);
	}

	// Play looped and Training Mode take no part: the session plays at the percent Training Mode starts at (the turns'
	// measures alone, without a Count In before each turn)
	@Test
	public void aSessionPlaysAtThePercentTrainingModeStartsAtWithoutLooping() throws Exception {
		settings.setCountInEachTurn(false);
		fixture.loopMode(1, 2);
		player.getMode().setType(MidiPlayerMode.TYPE_CUSTOM);
		player.getMode().setCustomPercentFrom(50);
		player.getMode().setCurrentPercent(80);
		practise(2);
		live.blocksToTheEnd();
		List<Integer> measures = chosen(2);
		assertEquals((2 * 0) + " 1 " + (64 + measures.get(0)) + ", " + (8 * BEAT) + " 1 " + (64 + measures.get(1)), live.started());
	}

	// with Count In before each turn off, no turn counts in, the player's Count In on: the turns' measures back to back
	@Test
	public void withCountInBeforeEachTurnOffNoTurnCountsIn() throws Exception {
		settings.setCountInEachTurn(false);
		player.getCountDown().setEnabled(true);
		practise(3);
		live.blocksToTheEnd();
		List<Integer> measures = chosen(3);
		assertEquals(IntStream.range(0, 3).mapToObj(turn -> (turn * 4L * BEAT) + " 1 " + (64 + measures.get(turn))).collect(Collectors.joining(", ")), live.started());
	}

	// during a turn's Count In the player is on its measure's first tick
	@Test
	public void throughATurnsCountInThePlayerIsOnItsFirstMeasure() throws Exception {
		practise(2);
		live.blocks(100);
		assertEquals(fixture.start(chosen(1).get(0)), player.getTickPosition());
	}

	@Test
	public void stopEndsTheSession() throws Exception {
		practise(3);
		live.blocks(10);
		player.stop();
		assertEquals("false true false", state());
	}

	// as an edit ends it: the player stops at its next turn, and the blocks after send nothing
	@Test
	public void aSessionThatEndsStopsThePlayer() throws Exception {
		practise(3);
		live.blocks(10);
		session.end();
		live.turn();
		received.clear();
		live.blocks(400);
		assertEquals("false true false | ", state() + " | " + live.started());
	}

	// moved to measure 3 during the second turn: from the next block on the song plays measure 3, then measure 4, to its end
	@Test
	public void movingThePositionEndsTheSessionAndTheSongPlaysOnFromThere() throws Exception {
		practise(3);
		live.blocks(200);
		player.setTickPosition(fixture.start(3));
		String moved = state();
		received.clear();
		live.blocks(400);
		assertEquals("true true false | 102400 1 67, 190600 1 68", moved + " | " + live.started());
	}

	// Play looped turned on during the session counts once it has ended: after measure 4 the song plays measure 3 again
	@Test
	public void afterASessionTheSongLoopsAsTheModeSaysNow() throws Exception {
		practise(3);
		live.blocks(200);
		fixture.loopMode(3, 4);
		player.setTickPosition(fixture.start(3));
		received.clear();
		live.blocks(500);
		assertEquals("102400 1 67, 190600 1 68, 278800 1 67", live.started());
	}

	// paused during turn 1's measure (block 200), the session is kept; Play plays turn 1 again from its Count In, then
	// turns 2 and 3, the same turns as without the pause, from the frame Play starts on
	@Test
	public void pauseKeepsTheSessionAndPlayPlaysThePausedTurnAgainFromItsCountIn() throws Exception {
		practise(3);
		live.blocks(200);
		player.pause();
		String paused = state();
		received.clear();
		live.playAgain();
		live.blocksToTheEnd();
		List<Integer> measures = chosen(3);
		long from = live.firstStarted();
		assertEquals("false false true | " + turn(from, measures.get(0)) + ", " + turn(from + 8 * BEAT, measures.get(1)) + ", "
			+ turn(from + 16 * BEAT, measures.get(2)) + " | false true false", paused + " | " + live.started() + " | " + state());
	}

	// paused in turn 2's Count In (block 400), Play plays turn 2 again from its Count In: the turns before it are not
	// played again, and the session still has 3 turns
	@Test
	public void playAfterPausePlaysFromThePausedTurnOn() throws Exception {
		practise(3);
		live.blocks(400);
		player.pause();
		received.clear();
		live.playAgain();
		live.blocksToTheEnd();
		List<Integer> measures = chosen(3);
		long from = live.firstStarted();
		assertEquals(turn(from, measures.get(1)) + ", " + turn(from + 8 * BEAT, measures.get(2)), live.started());
	}

	// a session that ended while paused (as an edit ends it), in turn 2's Count In: Play plays the song from where the
	// player paused, turn 2's first measure, to the song's end
	@Test
	public void aSessionEndedWhilePausedPlaysTheSongFromWhereItPaused() throws Exception {
		practise(3);
		live.blocks(400);
		player.pause();
		session.end();
		received.clear();
		live.playAgain();
		live.blocksToTheEnd();
		String keys = IntStream.rangeClosed(chosen(2).get(1), 4).mapToObj(measure -> "1 " + (64 + measure)).collect(Collectors.joining(", "));
		assertEquals(keys, live.started().replaceAll("\\d+ (1 \\d+)", "$1"));
	}

	// a sequencer that cannot take the turns added while it plays (the fake one, as Java Sound's) refuses a session,
	// saying why, before anything plays
	@Test
	public void aSequencerThatCannotTakeTurnsAddedWhilePlayingRefusesASession() throws Exception {
		player.loadSequencer(new FakeSequencer(new ArrayList<String>()));
		session = TGPracticeSession.start(settings, fixture.song);
		String refusal;
		try {
			player.practise(session, SEED);
			refusal = "started";
		} catch (TGPracticeRefusal refused) {
			refusal = refused.getMessage();
		}
		assertEquals("Interleaved practice needs a sequencer that takes turns added while it plays, and \"fake\" does not:"
			+ " choose the TuxGuitar Sequencer in Settings > Sound. | false false false", refusal + " | " + state());
	}

	// what the app shows: whether a session is held (playing or paused), whether there is anything to stop, the turn
	// (its number, the session's turns, its unit's kind and number; nothing without a session)
	private String shown() {
		String turn = player.getPracticeTurn().map(shown -> shown.getTurn() + " of " + shown.getTurns() + ": " + shown.getFrom().getKind() + " " + shown.getFrom().getNumber())
			.orElse("");
		return player.isPractising() + " " + player.isRunningOrPractising() + " " + turn;
	}

	// in turn 2's Count In (block 400) the turn shown is turn 2, also once paused; Stop ends it
	@Test
	public void theSessionAndItsTurnAreShownWhilePlayingAndPaused() throws Exception {
		String before = shown();
		practise(3);
		live.blocks(400);
		String playing = shown();
		player.pause();
		String paused = shown();
		player.stop();
		String turn = "2 of 3: MEASURE " + chosen(2).get(1);
		assertEquals("false false  | true true " + turn + " | true true " + turn + " | false false ", before + " | " + playing + " | " + paused + " | " + shown());
	}

	// the turns the sequence holds, "measure@tick" (what the takes play live): the first one when the session starts,
	// the next once it has started (after a block), the third once the second has (in turn 2's Count In, block 400);
	// none once Stop ends the session
	@Test
	public void theTurnsTheSequenceHoldsAreReadable() throws Exception {
		practise(3);
		String first = laid();
		live.blocks(1);
		String next = laid();
		live.blocks(399);
		String third = laid();
		player.stop();
		List<Integer> measures = chosen(3);
		String turns = measures.get(0) + "@960 " + measures.get(1) + "@8640 " + measures.get(2) + "@16320";
		assertEquals(turns.substring(0, turns.indexOf(' ')) + " | " + turns.substring(0, turns.lastIndexOf(' ')) + " | " + turns + " | ",
			first + " | " + next + " | " + third + " | " + laid());
	}

	private String laid() {
		return player.getPracticeTurns().stream().map(turn -> turn.getFirst().getNumber() + "@" + turn.getStart()).collect(Collectors.joining(" "));
	}

	// during a session the player reports the song's tick as written (see throughATurnsCountIn...): it maps to itself,
	// though the song repeats measures 2 and 3, where the tick of measure 4 would play measure 2 again
	@Test
	public void duringASessionThePlayersTickIsTheSongsTick() throws Exception {
		TestMidiSongTicks.repeat(fixture);
		practise(3);
		live.blocks(10);
		long tick = fixture.start(4) + 5;
		assertEquals(tick, MidiSongTicks.getStart(player, fixture.song, tick));
	}

	// the keys of measure 1's beats (from 1) once beatsOfMeasure1 has added theirs, then measure 2's first
	private static final int[] BEAT_KEYS = {0, 65, 70, 71, 72, 66};

	// a session of measure 1's beats, its beats 2 to 4 given quarters (keys 70 to 72): each turn its 4 clicks,
	// then its beat's note and its joint's (the next beat's; beat 4's is measure 2's first), the next turn 6 beats on
	@Test
	public void aBeatsTurnPlaysItsCountInThenItsBeatAndItsJoint() throws Exception {
		IntStream.rangeClosed(2, 4).forEach(beat -> live.quarter(1, beat, 4 + beat));
		practise(TGPracticeUnitKind.BEAT, 1, 3);
		live.blocksToTheEnd();
		List<Integer> beats = TestMidiPractice.chosen(session, SEED, 3, TGPracticeUnit::getNumber);
		assertEquals(IntStream.range(0, 3).mapToObj(turn -> beat(turn * 6L * BEAT, beats.get(turn))).collect(Collectors.joining(", ")), live.started());
	}

	// the Count In's clicks from the frame, then the beat's note and its joint's
	private static String beat(long frame, int beat) {
		return clicks(frame, beat) + ", " + (frame + 4 * BEAT) + " 1 " + BEAT_KEYS[beat] + ", " + (frame + 5 * BEAT) + " 1 " + BEAT_KEYS[beat + 1];
	}
}
