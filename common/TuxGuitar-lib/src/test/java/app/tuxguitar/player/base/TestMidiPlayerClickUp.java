package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.practice.TGClickUpLadder;
import app.tuxguitar.practice.TGClickUpSettings;
import app.tuxguitar.practice.TGPracticeCourse;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;

/**
 * Interleaved click-up played live (MidiPlayer.practise of its course) as LivePracticeFixture plays it, the player's
 * Count In off: each step its own Count In at the step's tempo (Count In before each step, on until turned off), then
 * its measures; the player stops by itself after the last. Repeat step
 * plays the step under way again at once from its Count In, Previous step the one before it (during the first, the
 * first again), then the steps after it; paused, they choose the step Play plays from its Count In.
 *
 * The passage is measures 1 to 2, from 50% to 100% by 50%: 1@50%, 1@100%, 1-2@50%, 2@100%, 1-2@100%. At 50% a beat lasts
 * 2 BEATs. Step 3's first measure starts at frame 705600; step 1's at 176400.
 */
public class TestMidiPlayerClickUp {

	private static final int BLOCK = LivePracticeFixture.BLOCK;
	private static final int BEAT = LivePracticeFixture.BEAT;
	// each step's beat, first and last measure
	private static final int[][] STEPS = {{2 * BEAT, 1, 1}, {BEAT, 1, 1}, {2 * BEAT, 1, 2}, {BEAT, 2, 2}, {BEAT, 1, 2}};
	// the blocks to the middle of step 3's first measure, and of step 1's
	private static final int IN_STEP_3 = (705600 + 2 * BEAT) / BLOCK;
	private static final int IN_STEP_1 = (176400 + 2 * BEAT) / BLOCK;
	// the blocks to the middle of step 2's first measure (it starts 4 beats after step 1's ends, at frame 352800)
	private static final int IN_STEP_2 = (441000 + 2 * BEAT) / BLOCK;

	private final LivePracticeFixture live = new LivePracticeFixture();
	private final MidiPlayer player = live.player;
	private final TGClickUpSettings settings = new TGClickUpSettings();
	private TGPracticeCourse course;

	public TestMidiPlayerClickUp() {
		settings.setPassageFrom(1);
		settings.setPassageTo(2);
		settings.setStart(50);
		settings.setGoal(100);
		settings.setIncrement(50);
	}

	// practises the click-up, the player's Count In as it is (off until turned on)
	private void practise() throws MidiPlayerException, TGPracticeRefusal {
		player.practise(course());
		live.run();
	}

	// a play of the click-up, its Count In as many clicks as the player's Count In has, on or off
	private TGPracticeCourse course() throws TGPracticeRefusal {
		course = TGClickUpLadder.of(settings, live.song.song).course(player.getCountDown()::clicks);
		return course;
	}

	// the steps (from 1) one after the other from the frame, each its 4 clicks a beat apart then its measures' notes,
	// written as started() writes them
	private static String steps(long frame, int... steps) {
		return countedIn(4, frame, steps);
	}

	// as steps(), each step counted in by so many clicks
	private static String countedIn(int clicks, long frame, int... steps) {
		List<String> started = new ArrayList<String>();
		long at = frame;
		for (int step : steps) {
			at = step(started, clicks, at, STEPS[step - 1]);
		}
		return String.join(", ", started);
	}

	// the step played (its beat, first and last measure) from the frame, counted in by so many clicks, added to those
	// started: where the step after it starts
	private static long step(List<String> started, int clicks, long at, int[] played) {
		long beat = played[0];
		LongStream.range(0, clicks).forEach(click -> started.add((at + click * beat) + " -2 37"));
		IntStream.rangeClosed(played[1], played[2]).forEach(measure -> started.add((at + (clicks + 4 * (measure - played[1])) * beat) + " 1 " + (64 + measure)));
		return at + (clicks + 4 * (played[2] - played[1] + 1)) * beat;
	}

	// whether the player runs, whether the course has ended, whether there are steps to go back over
	private String state() {
		return player.isRunning() + " " + course.isEnded() + " " + player.hasSteps();
	}

	@Test
	public void eachStepPlaysItsCountInThenItsMeasuresAtItsTempoAndThePlayerStopsAfterTheLast() throws Exception {
		practise();
		String underWay = state();
		live.blocksToTheEnd();
		assertEquals(steps(0, 1, 2, 3, 4, 5) + " | true false true | false true false", live.started() + " | " + underWay + " | " + state());
	}

	// with Count In before each step off, no step counts in, the player's Count In on
	@Test
	public void withCountInBeforeEachStepOffNoStepCountsIn() throws Exception {
		settings.setCountInEachStep(false);
		player.getCountDown().setEnabled(true);
		practise();
		live.blocksToTheEnd();
		assertEquals(countedIn(0, 0, 1, 2, 3, 4, 5), live.started());
	}

	// the player's Count In ticks set the length of every step's Count In, the player's Count In off
	@Test
	public void theCountInTicksSetTheLengthOfEachStepsCountIn() throws Exception {
		player.getCountDown().setTickCount(2);
		practise();
		live.blocksToTheEnd();
		assertEquals(countedIn(2, 0, 1, 2, 3, 4, 5), live.started());
	}

	// the tempo scale heard, as the app shows the tempo: before the session the Percentage of Tempo (80%), during step 1
	// its 50%, during step 2 its 100%, after the session the Percentage of Tempo again
	@Test
	public void theScaleHeardIsTheStepsDuringASessionElseThePercentageOfTempo() throws Exception {
		player.getMode().setSimplePercent(80);
		player.getMode().reset();
		String before = String.valueOf(player.getHeardScale());
		practise();
		live.blocks(IN_STEP_1);
		String step1 = String.valueOf(player.getHeardScale());
		live.blocks(IN_STEP_2 - IN_STEP_1);
		String step2 = String.valueOf(player.getHeardScale());
		live.blocksToTheEnd();
		assertEquals("4/5 1/2 1/1 4/5", before + " " + step1 + " " + step2 + " " + player.getHeardScale());
	}

	/** Repeat step or Previous step, as the app chooses it. */
	private interface Choice {
		void choose(MidiPlayer player) throws MidiPlayerException;
	}

	// the notes and clicks started after the choice, made after so many blocks
	private String chosen(int blocks, Choice choice) throws Exception {
		practise();
		live.blocks(blocks);
		choice.choose(player);
		live.received.clear();
		live.blocksToTheEnd();
		return live.started();
	}

	// from the first frame after the choice
	@Test
	public void repeatStepPlaysTheStepUnderWayAgainAtOnceFromItsCountInThenTheStepsAfterIt() throws Exception {
		assertEquals(steps(IN_STEP_3 * (long) BLOCK, 3, 4, 5), chosen(IN_STEP_3, MidiPlayer::repeatStep));
	}

	@Test
	public void previousStepPlaysTheStepBeforeTheOneUnderWayAtOnceThenTheStepsAfterIt() throws Exception {
		assertEquals(steps(IN_STEP_3 * (long) BLOCK, 2, 3, 4, 5), chosen(IN_STEP_3, MidiPlayer::previousStep));
	}

	@Test
	public void previousStepDuringTheFirstStepPlaysTheFirstAgain() throws Exception {
		assertEquals(steps(IN_STEP_1 * (long) BLOCK, 1, 2, 3, 4, 5), chosen(IN_STEP_1, MidiPlayer::previousStep));
	}

	// what the app shows: whether there are steps to go back over, and the step under way
	private String shown() {
		String step = player.getPracticeTurn().map(shown -> shown.getTurn() + " of " + shown.getTurns() + ": measure " + shown.getFrom().getNumber() + " at "
			+ shown.getStep().map(played -> played.getTempoMode().reading(played.getRung())).orElse("no step")).orElse("");
		return player.hasSteps() + " " + step;
	}

	// paused in step 3, Previous step shows step 2 at once; Play plays it from its Count In, then the steps after it
	@Test
	public void pausedPreviousStepChoosesTheStepPlayPlaysFromItsCountIn() throws Exception {
		practise();
		live.blocks(IN_STEP_3);
		player.pause();
		String paused = shown();
		player.previousStep();
		String chosen = shown();
		live.received.clear();
		live.playAgain();
		live.blocksToTheEnd();
		long from = live.firstStarted();
		assertEquals("true 3 of 5: measure 1 at 50% | true 2 of 5: measure 1 at 100% | " + steps(from, 2, 3, 4, 5),
			paused + " | " + chosen + " | " + live.started());
	}

	// paused in step 3, Repeat step keeps it: Play plays it again from its Count In, as it would without
	@Test
	public void pausedRepeatStepKeepsThePausedStep() throws Exception {
		practise();
		live.blocks(IN_STEP_3);
		player.pause();
		player.repeatStep();
		String chosen = shown();
		live.received.clear();
		live.playAgain();
		live.blocksToTheEnd();
		assertEquals("true 3 of 5: measure 1 at 50% | " + steps(live.firstStarted(), 3, 4, 5), chosen + " | " + live.started());
	}

	// Stop ends the session: nothing to go back over, and Repeat step plays nothing
	@Test
	public void afterStopThereAreNoStepsAndRepeatStepPlaysNothing() throws Exception {
		practise();
		live.blocks(IN_STEP_3);
		player.stop();
		live.received.clear();
		player.repeatStep();
		live.blocks(10);
		assertEquals("false true false | ", state() + " | " + live.started());
	}

	// a sequencer that cannot take the turns added while it plays (the fake one, as Java Sound's) refuses a click-up,
	// naming it, before anything plays
	@Test
	public void aSequencerThatCannotTakeTurnsAddedWhilePlayingRefusesAClickUpNamingIt() throws Exception {
		player.loadSequencer(new FakeSequencer(new ArrayList<String>()));
		String refusal;
		try {
			player.practise(course());
			refusal = "started";
		} catch (TGPracticeRefusal refused) {
			refusal = refused.getMessage();
		}
		assertEquals("Interleaved click-up needs a sequencer that takes turns added while it plays, and \"fake\" does not:"
			+ " choose the TuxGuitar Sequencer in Settings > Sound. | false false false", refusal + " | " + state());
	}

	// interleaved practice's turns are no steps: Repeat step and Previous step leave its turns as they were laid
	@Test
	public void aRandomSessionHasNoStepsAndRepeatStepLeavesItsTurns() throws Exception {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, 4);
		settings.setTurns(3);
		player.practise(TGPracticeSession.start(settings, live.song.song), 1);
		live.run();
		live.blocks(200);
		String laid = laid();
		player.repeatStep();
		player.previousStep();
		assertEquals("false " + laid, player.hasSteps() + " " + laid());
	}

	private String laid() {
		return player.getPracticeTurns().stream().map(turn -> turn.getTurn() + "@" + turn.getStart()).collect(Collectors.joining(" "));
	}
}
