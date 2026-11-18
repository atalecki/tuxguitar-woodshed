package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.device.TGAudioDeviceException;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeTurn;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that practise a session timed by the interval clock live (README, Practice: Interval clock), as
 * LivePractisingSteps practises one, and change something during a pass or a break; what the session plays, offline,
 * with the same seed (planned) says where its passes and breaks are. Read as heard: the output sounds what the player
 * sends the plugin delay compensation later. The Background plays MIDI note BASE plus the measure's number on every beat.
 */
public class TimedLiveSteps {

	private static final int BASE = 35;
	private static final int SAMPLE_RATE = TestInterface.SAMPLE_RATE;
	// clear of a beat's edges when reading its note
	private static final int EDGE = 2048;
	// a note's tone sounds above SOUNDS; a Count In click leaves a little at its pitch, below it
	private static final double SOUNDS = 0.01;
	// -60 dBFS
	private static final double SILENT = 0.001;

	private final AudioWorld world;
	private final LivePractisingSteps live;
	// the turn shown just before the change
	private Optional<TGPracticeTurn> shownBefore = Optional.empty();

	public TimedLiveSteps(AudioWorld world, LivePractisingSteps live) {
		this.world = world;
		this.live = live;
	}

	// the first pass of the session's turn (from 1) in what the session plays
	private int opening(int turn) {
		return live.openings()[turn - 1];
	}

	// where the break before the turn (from 1) starts, so many seconds in
	private ToLongFunction<TGTimeline> intoTheBreak(int turn, int seconds) {
		return planned -> planned.breakStart(opening(turn)) + seconds * (long) SAMPLE_RATE;
	}

	@When("I practise the session live")
	public void practiseTheSessionLive() throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		live.practiseLive(planned -> 0, () -> { }, LivePractisingSteps.LONGEST);
	}

	// as the app flags it (see LivePractisingSteps), half way through the break; the turns heard are the run the choosing
	// steps read
	@When("I practise the session live, flagging measure {int} as {int}x difficult during the break before turn {int}")
	public void practiseFlaggingInTheBreak(int measure, int difficulty, int turn) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		Practice practice = live.practice();
		live.practiseLive(planned -> (planned.breakStart(opening(turn)) + planned.turnStart(opening(turn))) / 2, () -> {
			shownBefore = live.player().getPracticeTurn();
			practice.edit(() -> practice.flag(measure, difficulty));
		}, LivePractisingSteps.LONGEST);
		live.addHeardRun();
	}

	// half way through the pass (from 1) of the session's first turn
	@When("I practise the session live, pausing during pass {int} and playing again")
	public void practisePausingInAPass(int pass) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		live.practiseLive(planned -> (planned.turnStart(pass - 1) + planned.turnEnd(pass - 1)) / 2, live::pauseAndPlay, LivePractisingSteps.LONGEST);
	}

	// the session's break: the one before turn 2
	@When("I practise the session live, pausing {int} seconds into the break and playing again")
	public void practisePausingInTheBreak(int seconds) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		live.practiseLive(intoTheBreak(2, seconds), live::pauseAndPlay, LivePractisingSteps.LONGEST);
	}

	@When("I practise the session live and stop {int} seconds into the break")
	public void practiseStoppingInTheBreak(int seconds) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		live.practiseLive(intoTheBreak(2, seconds), () -> live.player().stop(), LivePractisingSteps.AFTER);
	}

	// as the app moves it (TGTransport: the song's tick, here with no repeats)
	@When("I practise the session live, moving the position to {position} {int} seconds into the break")
	public void practiseMovingInTheBreak(SongPosition to, int seconds) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		live.practiseLive(intoTheBreak(2, seconds), () -> live.player().setTickPosition(to.tick(world.song())), LivePractisingSteps.AFTER);
	}

	// the turn (from 1) heard: the measure whose note its first measure sounds on its first beat
	private int heardMeasure(int turn) {
		TGTimeline planned = live.planned();
		return live.heardMeasure(planned.start(planned.firstOf(opening(turn))));
	}

	// the turn shown as the scenarios word it: the unit the turn sounds named so, else by its number
	private String worded(TGPracticeTurn shown) {
		return turnOrBreak(shown) + ", the " + shown.getFrom().getKind().getName() + " " + unit(shown) + ", " + seconds(shown.getSecondsLeft()) + " left";
	}

	private static String turnOrBreak(TGPracticeTurn shown) {
		return (shown.isBreak() ? "the break before turn " : "turn ") + shown.getTurn();
	}

	// "turn N sounds" when turn N is heard sounding the unit, else its number
	private String unit(TGPracticeTurn shown) {
		return (shown.getFrom().getFirst() == heardMeasure(shown.getTurn()) ? "turn " + shown.getTurn() + " sounds" : shown.getFrom().getNumber() + "");
	}

	private static String seconds(int seconds) {
		return seconds + (seconds == 1 ? " second" : " seconds");
	}

	// as heard, the first turn starts where its first Count In click sounds; a report is read from the first block that
	// ends that many seconds or more after it
	@Then("TuxGuitar reports, so many seconds after the first turn started:")
	public void reportsAfter(List<Map<String, String>> rows) {
		long started = live.planned().turnStart(0) + world.chainDelay();
		List<Optional<TGPracticeTurn>> shown = live.shown();
		String expected = rows.stream().map(row -> row.get("seconds") + ": " + row.get("report")).collect(Collectors.joining("\n"));
		String reported = rows.stream().map(row -> {
			double seconds = Double.parseDouble(row.get("seconds"));
			int block = (int) Math.ceil((started + seconds * SAMPLE_RATE) / LivePractisingSteps.BLOCK) - 1;
			return row.get("seconds") + ": " + shown.get(block).map(this::worded).orElse("nothing");
		}).collect(Collectors.joining("\n"));
		assertEquals(expected, reported);
	}

	@Then("turn {int} sounds the measure TuxGuitar reported during that break")
	public void soundsTheReportedMeasure(int turn) {
		TGPracticeTurn shown = shownBefore.orElseThrow(() -> new AssertionError("no turn shown during the break"));
		assertEquals("the break before turn " + turn + ": measure " + heardMeasure(turn), turnOrBreak(shown) + ": measure " + shown.getFrom().getFirst());
	}

	// the turn's measure, as heard: from Play, a Count In (its beats not read: a VST3 instrument ignores All Notes Off, so
	// the note held at the pause rings on), then the measure's note on every beat, the times given
	@Then("after Play, the turn's measure sounds again after a Count In of {int} beats, then {int} more times back to back")
	public void afterPlayTheTurnsMeasure(int beats, int more) {
		int note = BASE + heardMeasure(1);
		long start = live.change() + world.chainDelay() + beats * LivePractisingSteps.beat();
		int measure = world.song().getMeasureHeader(note - BASE - 1).getTimeSignature().getNumerator();
		IntStream.range(0, (more + 1) * measure).forEach(beat -> assertBeat(note, start, beat, true));
	}

	// whether the note sounds on the beat (from 0) from the sample, read clear of the beat's edges (a click leaves a
	// little at its pitch, below SOUNDS)
	private void assertBeat(int note, long from, int beat, boolean sounds) {
		long at = from + beat * LivePractisingSteps.beat() + EDGE;
		double level = live.levelOf(note, at, at + LivePractisingSteps.beat() - 2 * EDGE);
		assertEquals(sounds, level > SOUNDS, "beat " + (beat + 1) + " from sample " + from + ": note " + note + " at " + level);
	}

	@Then("the player stopped by itself within {int} samples of the turn's end, {int} seconds after Play")
	public void stoppedAfterTheTurn(int within, int seconds) {
		live.assertStoppedAfter(within, seconds * (long) SAMPLE_RATE, "the turn");
	}

	@Then("the player stopped by itself within {int} samples of turn {int}'s end, {int} seconds after Play")
	public void stoppedAfterTurn(int within, int turn, int seconds) {
		live.assertStoppedAfter(within, seconds * (long) SAMPLE_RATE, "turn " + turn);
	}

	@Then("the player stopped by itself at the end of the block turn {int}'s end is heard in, {int} seconds after Play")
	public void stoppedAsTurnEndIsHeard(int turn, int seconds) {
		live.assertStoppedAsHeard(seconds * (long) SAMPLE_RATE, "turn " + turn);
	}

	// as heard: silence up to Play, then the first click within the samples; turn 2's note not before its Count In's
	// beats are over (a click leaves a little at its pitch), then on every beat of its first measure
	@Then("after Play, turn {int}'s Count In of {int} beats starts within {int} samples")
	public void afterPlayTheCountInStarts(int turn, int beats, int within) {
		long play = live.change() + world.chainDelay();
		long sounds = firstSoundingFrom(play - LivePractisingSteps.BLOCK);
		assertEquals(play + within / 2.0, sounds, within / 2.0, "the output sounds from sample " + sounds + ", Play at " + play);
		int note = BASE + heardMeasure(turn);
		IntStream.range(0, 2 * beats).forEach(beat -> assertBeat(note, play, beat, beat >= beats));
	}

	// the first sample of the output's left side from the one given that is not silent (its length: none)
	private int firstSoundingFrom(long from) {
		float[] left = world.live()[0];
		return IntStream.range((int) from, left.length).filter(sample -> Math.abs(left[sample]) > SILENT).findFirst().orElse(left.length);
	}
}
