package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.device.TGAudioDeviceException;
import app.tuxguitar.editor.action.transport.TGTransportStepAction;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.practice.TGClickUpStep;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.util.TGContext;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that practise interleaved click-up live, as the app plays it (TGTransportClickUpAction; Repeat step and
 * Previous step, TGTransportStepAction), as LivePractisingSteps practises interleaved practice, and change something
 * during a step. What the click-up plays offline (planned) says where its steps are: a step is a turn of it (1 pass a
 * step). Once a step is played again (Pause then Play, Repeat step, Previous step) the steps started before the change
 * stand as planned, the one played again starts at the change's sample, and each after it where the one before ends.
 * Read as heard: the output sounds what the player sends the plugin delay compensation later. Track 1 sounds MIDI note
 * 35 plus the measure's number on every beat (the Background), from phase 0 (Test Sine): a beat's note is read by its
 * level, or, exact, where its sine starts. A step played again is read by level only: a VST3 instrument ignores All
 * Notes Off, so the note sounding when the player started the step again rings on until it is played again (as after
 * a pause; a defect of playback, left as it is).
 */
public class ClickUpLiveSteps {

	private static final int BASE = 35;
	// the fit of a beat's note: from so many samples after it, so many samples long (see HeardNotes)
	private static final int FIT_FROM = LivePractisingSteps.FIT_FROM;
	private static final int FIT = 2048;

	private final AudioWorld world;
	private final ClickUp clickUp;
	private final LivePractisingSteps live;
	private final Practice practice;
	private final SongSteps songSteps;
	private final RecordingSteps recordingSteps;
	// once a step was played again at the change: which (from 1; 0 until a step says), else -1
	private int again = -1;
	// the steps the output holds started, in order, once a step says
	private List<Integer> played;

	public ClickUpLiveSteps(AudioWorld world, ClickUp clickUp, LivePractisingSteps live, Practice practice, SongSteps songSteps, RecordingSteps recordingSteps) {
		this.world = world;
		this.clickUp = clickUp;
		this.live = live;
		this.practice = practice;
		this.songSteps = songSteps;
		this.recordingSteps = recordingSteps;
	}

	/** A step as the output holds it: which (from 1), where it starts as the player plays it, and up to where it is heard. */
	private static final class Heard {
		final int step;
		final long start;
		final long until;

		Heard(int step, long start, long until) {
			this.step = step;
			this.start = start;
			this.until = until;
		}
	}

	// starts the click-up as set live as the app does, the change made where the function finds in what it plays
	// offline, then plays on for at most so many blocks, or until the player stops by itself
	private void practiseLive(ToLongFunction<TGTimeline> when, Runnable changing, int after) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		live.practiseLive(clickUp.planned(), player -> clickUp.startLive(), when, changing, after);
	}

	// as practiseLive, the change made half way through the step
	private void practiseLive(int during, Runnable changing, int after) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		live.practiseLive(clickUp.planned(), player -> clickUp.startLive(), during, changing, after);
	}

	@When("I practise interleaved click-up live")
	public void practiseLive() throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(planned -> 0, () -> { }, LivePractisingSteps.LONGEST);
	}

	// as the app pauses and plays again (see LivePractisingSteps)
	@When("I practise interleaved click-up live, pausing during step {int} and playing again")
	public void practisePausing(int step) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		again = step;
		practiseLive(step, live::pauseAndPlay, LivePractisingSteps.LONGEST);
	}

	@ParameterType("Repeat step|Previous step")
	public Function<TGContext, TGTransportStepAction> command(String name) {
		return ("Repeat step".equals(name) ? TGTransportStepAction::repeat : TGTransportStepAction::previous);
	}

	// as the app's command runs it, half way through the step's first measure
	@When("I practise interleaved click-up live, choosing {command} during step {int}'s first measure")
	public void practiseChoosing(Function<TGContext, TGTransportStepAction> command, int step) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		again = 0;
		practiseLive(planned -> (planned.start(planned.firstOf(step - 1)) + planned.end(planned.firstOf(step - 1))) / 2,
			() -> world.execute(command.apply(world.fixture().context)), LivePractisingSteps.LONGEST);
	}

	// where the beat of the step's first measure (both from 1) sounds in what the click-up plays
	@When("I practise interleaved click-up live and stop at step {int}'s first measure beat {int}")
	public void practiseStopping(int step, int beat) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(planned -> {
			int first = planned.firstOf(step - 1);
			return planned.sampleAt(first, (beat - 1) * planned.get(first).getHeader().getTimeSignature().getDenominator().getTime());
		}, () -> live.player().stop(), LivePractisingSteps.AFTER);
	}

	// as the app moves it (TGTransport: the song's tick, here with no repeats)
	@When("I practise interleaved click-up live, moving the position to {position} during step {int}")
	public void practiseMoving(SongPosition to, int step) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(step, () -> live.player().setTickPosition(to.tick(world.song())), LivePractisingSteps.AFTER);
	}

	@When("I practise interleaved click-up live, removing measure {int} during step {int}")
	public void practiseRemoving(int measure, int step) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(step, () -> songSteps.removeMeasure(measure), LivePractisingSteps.AFTER);
	}

	// an edit the session goes on through (see KeptEditSteps), as the app makes it
	@When("I practise interleaved click-up live, {keptEdit} during step {int}")
	public void practiseKeeping(Runnable edit, int step) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(step, edit, LivePractisingSteps.LONGEST);
	}

	@When("I practise interleaved click-up live, choosing Record during step {int}")
	public void practiseRecording(int step) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(step, recordingSteps::chooseRecord, LivePractisingSteps.LONGEST);
	}

	// as the app flags it: the flag action, which the course under way takes (TGPracticeEdits)
	@When("I practise interleaved click-up live, flagging measure {int} as {int}x difficult during step {int}")
	public void practiseFlagging(int measure, int difficulty, int step) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(step, () -> practice.edit(() -> practice.flag(measure, difficulty)), LivePractisingSteps.LONGEST);
	}

	@Then("interleaved click-up has ended")
	public void hasEnded() {
		assertTrue(clickUp.playing().isEnded());
	}

	// "50%, 75% and 100%"
	@ParameterType("\\d+%(?:, \\d+%)*(?: and \\d+%)?")
	public List<TGTempoScale> percents(String text) {
		return Arrays.stream(text.split(", | and ")).map(percent -> TGTempoScale.percent(Integer.parseInt(percent.replace("%", "")))).collect(Collectors.toList());
	}

	// the click-up's steps are those: their measures, at their tempos; and the output holds them, as planned
	@Then("the output holds {int} steps, measures {string} at {percents}")
	public void theOutputHoldsTheSteps(int count, String measures, List<TGTempoScale> tempos) {
		List<TGClickUpStep> steps = clickUp.ladder().getSteps();
		String expected = Arrays.stream(measures.split(", ")).map(ClickUpPractisingSteps::span).map(span -> span[0] + "-" + span[1]).collect(Collectors.joining(", "));
		assertEquals(count + " " + expected + " " + tempos, steps.size() + " " + steps.stream().map(step -> step.getFirstMeasure() + "-" + step.getLastMeasure())
			.collect(Collectors.joining(", ")) + " " + steps.stream().map(TGClickUpStep::getScale).collect(Collectors.toList()));
		heard().forEach(step -> assertSounds(step, false));
	}

	// live, each step's beats as heard, each note started within 1 sample of its beat (see ClickUpPractisingSteps)
	void assertEachBeatOnTime() {
		heard().forEach(step -> assertSounds(step, true));
	}

	@Then("after Play, step {int}'s measures {int} and {int} sound again at {int}% after a Count In of {int} beats {int} samples apart")
	public void afterPlay(int step, int first, int last, int percent, int beats, int apart) {
		TGClickUpStep planned = clickUp.ladder().getSteps().get(step - 1);
		assertEquals(step + ": " + first + "-" + last + "@" + TGTempoScale.percent(percent),
			again + ": " + planned.getFirstMeasure() + "-" + planned.getLastMeasure() + "@" + planned.getScale());
		Heard played = playedAgain();
		assertEquals(step, played.step);
		assertCountIn(played, beats, apart);
		assertSounds(played, false);
	}

	@Then("then steps {int} and {int} sound, each after its Count In")
	public void thenTheStepsSound(int one, int other) {
		List<Heard> heard = heard();
		List<Heard> after = heard.subList(startedBefore() + 1, heard.size());
		assertEquals(one + " " + other, after.stream().map(step -> String.valueOf(step.step)).collect(Collectors.joining(" ")));
		after.forEach(step -> assertSounds(step, false));
	}

	@Then("the player stopped by itself within {int} samples of step {int}'s end")
	public void stoppedByItself(int within, int step) {
		Heard last = heard().get(heard().size() - 1);
		assertEquals(step, last.step);
		assertEquals(last.until + world.chainDelay(), live.stopped(), within, "step " + step + " ends, as heard");
	}

	// "1, 2, 3, 3, 4, 5": the steps started before the change as planned, then the one played again and those after it
	@ParameterType("\\d+(?:, \\d+)*")
	public List<Integer> steps(String text) {
		return Arrays.stream(text.split(", ")).map(Integer::valueOf).collect(Collectors.toList());
	}

	@Then("the steps the output holds started are {steps}")
	public void theStepsStarted(List<Integer> steps) {
		int before = startedBefore();
		assertEquals(IntStream.rangeClosed(1, before).boxed().collect(Collectors.toList()), steps.subList(0, before), "the steps started before the change");
		played = steps;
		again = steps.get(before);
		heard().forEach(step -> assertSounds(step, false));
	}

	@Then("step {int}'s second time starts with its Count In within {int} samples of choosing it")
	public void playedAgainStarts(int step, int within) {
		Heard played = playedAgain();
		assertEquals(step + " true", played.step + " " + played().subList(0, startedBefore()).contains(step), "step " + step + " played before");
		long first = countIn(played)[0];
		assertEquals(live.change() + world.chainDelay(), first, within, "step " + step + "'s first Count In click, as heard");
	}

	// the steps of what the click-up plays offline whose start comes before the change
	private int startedBefore() {
		TGTimeline planned = live.planned();
		return (int) IntStream.range(0, planned.turns()).filter(turn -> planned.turnStart(turn) < live.change()).count();
	}

	// the steps the output holds started: as a step said, else those planned, or, once one was played again, those
	// started before the change, then the one played again and those after it
	private List<Integer> played() {
		if (played != null) {
			return played;
		}
		int steps = live.planned().turns();
		IntStream first = (again > 0 ? IntStream.concat(IntStream.rangeClosed(1, startedBefore()), IntStream.rangeClosed(again, steps)) : IntStream.rangeClosed(1, steps));
		return first.boxed().collect(Collectors.toList());
	}

	// the steps the output holds, where each starts: as planned, or, once one was played again, from the change on
	// one after the other
	private List<Heard> heard() {
		TGTimeline planned = live.planned();
		List<Integer> steps = played();
		int before = (again >= 0 ? startedBefore() : steps.size());
		List<Heard> heard = new ArrayList<Heard>();
		long start = 0;
		for (int index = 0; index < steps.size(); index++) {
			int turn = steps.get(index) - 1;
			start = (index < before ? planned.turnStart(turn) : (index == before ? live.change() : start));
			long end = start + planned.turnEnd(turn) - planned.turnStart(turn);
			heard.add(new Heard(turn + 1, start, (index + 1 == before && again >= 0 ? live.change() : end)));
			start = end;
		}
		return heard;
	}

	// the step played again: the first started at the change
	private Heard playedAgain() {
		return heard().get(startedBefore());
	}

	// each beat of the step's measures heard (up to where it is heard) sounds its measure's note; exact: its note starts
	// within 1 sample of the beat
	private void assertSounds(Heard step, boolean exact) {
		TGTimeline planned = live.planned();
		int turn = step.step - 1;
		long shift = step.start - planned.turnStart(turn);
		for (int index = planned.firstOf(turn); index < planned.firstOf(turn + 1); index++) {
			TGMeasureHeader header = planned.get(index).getHeader();
			int beats = header.getTimeSignature().getNumerator();
			long length = (planned.end(index) - planned.start(index)) / beats;
			for (int beat = 0; beat < beats && planned.start(index) + shift + (beat + 1) * length <= step.until; beat++) {
				long at = planned.start(index) + shift + beat * length;
				String name = "step " + step.step + " at " + at + ", measure " + header.getNumber() + " beat " + (beat + 1);
				assertEquals(header.getNumber(), live.heardMeasure(at), name);
				assertOnTime(BASE + header.getNumber(), at, exact, name);
			}
		}
	}

	// exact: the note heard starts within 1 sample of the player's sample, as heard (the chain delay later)
	private void assertOnTime(int note, long sample, boolean exact, String name) {
		if (exact) {
			int heard = (int) (sample + world.chainDelay());
			assertEquals(heard, HeardNotes.sineStart(world.live()[0], note, heard + FIT_FROM, heard + FIT_FROM + FIT, heard), 1, name);
		}
	}

	// the Count In clicks heard from the step's start to its first measure (see LivePractisingSteps.countInHeard)
	private long[] countIn(Heard step) {
		TGTimeline planned = live.planned();
		int turn = step.step - 1;
		return live.countInHeard(step.start - FIT_FROM, step.start + planned.start(planned.firstOf(turn)) - planned.turnStart(turn));
	}

	// so many Count In clicks heard before the step's first measure, so many samples apart, within 1 sample
	private void assertCountIn(Heard step, int beats, int apart) {
		long[] clicks = countIn(step);
		long[] gaps = IntStream.range(1, clicks.length).mapToLong(click -> clicks[click] - clicks[click - 1]).toArray();
		assertEquals(beats, clicks.length, "step " + step.step + "'s Count In, as heard: " + Arrays.toString(clicks));
		LongStream.of(gaps).forEach(gap -> assertEquals(apart, gap, 1, "step " + step.step + "'s Count In, as heard: " + Arrays.toString(clicks)));
	}
}
