package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.practice.TGClickUpLadder;
import app.tuxguitar.practice.TGClickUpSettings;
import app.tuxguitar.practice.TGPracticeCourse;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;

/**
 * Interleaved click-up played live (MidiPlayer.practise of its course) as LivePracticeFixture plays it, Count In on:
 * each step its Count In at the step's tempo, then its measures; the player stops by itself after the last. Repeat step
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

	private final LivePracticeFixture live = new LivePracticeFixture();
	private final MidiPlayer player = live.player;
	private TGPracticeCourse course;

	// practises the click-up, Count In on
	private void practise() throws MidiPlayerException, TGPracticeRefusal {
		TGClickUpSettings settings = new TGClickUpSettings();
		settings.setPassageFrom(1);
		settings.setPassageTo(2);
		settings.setStart(50);
		settings.setGoal(100);
		settings.setIncrement(50);
		player.getCountDown().setEnabled(true);
		course = TGClickUpLadder.of(settings, live.song.song).course();
		player.practise(course);
		live.run();
	}

	// the steps (from 1) one after the other from the frame, each its 4 clicks a beat apart then its measures' notes,
	// written as started() writes them
	private static String steps(long frame, int... steps) {
		List<String> started = new ArrayList<String>();
		long at = frame;
		for (int step : steps) {
			int[] played = STEPS[step - 1];
			long beat = played[0];
			for (int click = 0; click < 4; click++) {
				started.add((at + click * beat) + " -2 37");
			}
			for (int measure = played[1]; measure <= played[2]; measure++) {
				started.add((at + (4 + 4 * (measure - played[1])) * beat) + " 1 " + (64 + measure));
			}
			at += (4 + 4 * (played[2] - played[1] + 1)) * beat;
		}
		return String.join(", ", started);
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
