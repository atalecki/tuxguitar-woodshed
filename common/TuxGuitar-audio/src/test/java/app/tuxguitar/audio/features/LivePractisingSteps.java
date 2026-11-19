package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.LivePlay;
import app.tuxguitar.audio.Meters;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.action.TGPractiseAction;
import app.tuxguitar.audio.device.TGAudioDeviceException;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeTurn;
import app.tuxguitar.song.models.TGMeasureHeader;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that practise interleaved practice live, as the app plays it (MidiPlayer.practise) on the TuxGuitar
 * Synthesizer's sample clock through the live audio device (see LivePlay), and change something half way through a
 * turn, between two blocks, as the app does while it plays. Track 1 plays Test Sine, MIDI note BASE plus the measure's
 * number on every beat (the Background); the output sounds what the player sends the plugin delay compensation later.
 */
public class LivePractisingSteps {

	private static final int BASE = 35;
	// the live device's blocks
	static final int BLOCK = 512;
	// the blocks played on once the change is made
	static final int AFTER = 400;
	// the most blocks a session that stops by itself is played on for (a timed session of 5 minutes and more)
	static final int LONGEST = 30000;
	// clear of a beat's edges (and of where exactly a move lands in its block) when reading its note
	private static final int EDGE = 2048;
	// the fit of a beat's note from so many samples after it (see HeardNotes); a Count In click's jump from one sample to
	// the next, as the output sounds it: a note's tone at -10 dBFS moves less than a tenth of it
	static final int FIT_FROM = 64;
	private static final double JUMP = 0.02;
	// a note's tone sounds above SOUNDS; none above -60 dBFS is silent
	private static final double SOUNDS = 0.01;
	private static final double SILENT = 0.001;

	private final AudioWorld world;
	private final Practice practice;
	private final SongSteps songSteps;
	private final RecordingSteps recordingSteps;
	// the sample of the change (the first after the blocks played before it), the turn it was made in (from 1), and
	// for each block whether the player ran after its turn of the play loop, and the turn it showed (none: empty)
	private long change;
	private int changedIn;
	// what the session would play, offline, with the same seed: its turns (with the interval clock, its passes)
	private TGTimeline planned;
	private final List<Boolean> running = new ArrayList<Boolean>();
	private final List<Optional<TGPracticeTurn>> shown = new ArrayList<Optional<TGPracticeTurn>>();

	public LivePractisingSteps(AudioWorld world, Practice practice, SongSteps songSteps, RecordingSteps recordingSteps) {
		this.world = world;
		this.practice = practice;
		this.songSteps = songSteps;
		this.recordingSteps = recordingSteps;
	}

	MidiPlayer player() {
		return MidiPlayer.getInstance(world.fixture().context);
	}

	Practice practice() {
		return practice;
	}

	// what the session would play, offline, with the same seed
	TGTimeline planned() {
		return planned;
	}

	// the sample of the change, as the player plays it
	long change() {
		return change;
	}

	// the turn the player showed after each block (none: empty)
	List<Optional<TGPracticeTurn>> shown() {
		return shown;
	}

	// the sample the player stopped at: the end of the first block after which it no longer ran
	long stopped() {
		int stopped = running.indexOf(Boolean.FALSE);
		assertTrue(stopped >= 0, "the player never stopped");
		return (stopped + 1L) * BLOCK;
	}

	// practises so many turns live (random seed 1), the change made half way through the turn (from 1), then plays on
	// for AFTER blocks; the player stopped then, the synthesizer's channels close
	private void practiseLive(int turns, int during, Runnable changing) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, changing, AFTER);
	}

	// as practiseLive, playing on for at most so many blocks, or until the player stops by itself
	private void practiseLive(int turns, int during, Runnable changing, int after) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practice.settings().setTurns(turns);
		practice.start();
		practiseLive(sessionPlanned(), this::practise, during, changing, after);
	}

	/** Practice started live, as the app starts it. */
	interface Starting {
		void start(MidiPlayer player) throws MidiPlayerException, TGPracticeRefusal;
	}

	// what the session started would play, offline, with the same seed
	private TGTimeline sessionPlanned() {
		return TGPractiseAction.timeline(world.fixture().context, world.song(), practice.session(), Practice.SEED);
	}

	// the session started, live, random seed 1
	private void practise(MidiPlayer player) throws MidiPlayerException, TGPracticeRefusal {
		player.practise(practice.session(), Practice.SEED);
	}

	// practises the session as set live (random seed 1), the change made in the block of the sample the function finds
	// in what the session plays (planned), then plays on for at most so many blocks, or until the player stops by itself;
	// the player stopped then, the synthesizer's channels close
	void practiseLive(ToLongFunction<TGTimeline> when, Runnable changing, int after) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practice.start();
		practiseLive(sessionPlanned(), this::practise, when, changing, after);
	}

	// as practiseLive, what is planned started as the starting does, the change made half way through the turn (from 1)
	// of what is planned
	void practiseLive(TGTimeline planned, Starting starting, int during, Runnable changing, int after) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		changedIn = during;
		practiseLive(planned, starting, timeline -> (timeline.turnStart(during - 1) + timeline.turnEnd(during - 1)) / 2, changing, after);
	}

	// practises live what is planned (what it would play, offline), started as the starting does, the change made in the
	// block of the sample the function finds in what is planned, then plays on for at most so many blocks, or until the
	// player stops by itself; the player stopped then, the synthesizer's channels close
	void practiseLive(TGTimeline planned, Starting starting, ToLongFunction<TGTimeline> when, Runnable changing, int after)
		throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		this.planned = planned;
		int at = (int) (when.applyAsLong(planned) / BLOCK);
		change = (at + 1L) * BLOCK;
		MidiPlayer player = player();
		player.loadSequencer(new MidiSequencerImpl(world.fixture().context));
		starting.start(player);
		world.threads().runStartHere();
		world.setLive(LivePlay.run(world.fixture().context, world.synthesizer(), world.backend(), () -> { }, block -> playBlock(player, block, at, changing) && block < at + after));
		player.stop();
		player.closeChannels();
	}

	// after the device played the block (the change made first in the change's block), the play loop takes its turn:
	// whether it goes on
	private boolean playBlock(MidiPlayer player, int block, int at, Runnable changing) {
		changeAt(block, at, changing);
		boolean goesOn = player.runPlayLoopProcess();
		running.add(player.isRunning());
		shown.add(player.getPracticeTurn());
		return goesOn;
	}

	private static void changeAt(int block, int at, Runnable changing) {
		if (block == at) {
			changing.run();
		}
	}

	// with no change: played on until the player stops by itself after the last turn
	@When("I practise {int} turns live")
	public void practiseLive(int turns) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, 1, () -> { }, LONGEST);
	}

	// as the app moves it (TGTransport: the song's tick, here with no repeats)
	@When("I practise {int} turns live, moving the position to {position} during turn {int}")
	public void practiseLiveMoving(int turns, SongPosition to, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, () -> player().setTickPosition(to.tick(world.song())));
	}

	@When("I practise {int} turns live, removing measure {int} during turn {int}")
	public void practiseLiveRemoving(int turns, int measure, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, () -> songSteps.removeMeasure(measure));
	}

	// an edit the session goes on through (see KeptEditSteps), as the app makes it
	@When("I practise {int} turns live, {keptEdit} during turn {int}")
	public void practiseLiveKeeping(int turns, Runnable edit, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, edit, LONGEST);
	}

	@When("I practise {int} turns live, choosing Record during turn {int}")
	public void practiseLiveRecording(int turns, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, recordingSteps::chooseRecord, LONGEST);
	}

	// as the app pauses and plays again (TGTransport: no move during a session)
	@When("I practise {int} turns live, pausing during turn {int} and playing again")
	public void practiseLivePausing(int turns, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, this::pauseAndPlay, LONGEST);
	}

	// as the app pauses and plays again (TGTransport: no move during a session)
	void pauseAndPlay() {
		player().pause();
		try {
			player().play();
		} catch (MidiPlayerException e) {
			throw new IllegalStateException(e);
		}
		world.threads().runStartHere();
	}

	// as the app flags it: the flag action, checked by the session's chooser (TGPracticeEdits); the turns heard, by the
	// note each sounds on its first beat, are the run the choosing steps read
	@When("I practise {int} turns live, flagging measure {int} as {int}x difficult during turn {int}")
	public void practiseLiveFlagging(int turns, int measure, int difficulty, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, () -> practice.edit(() -> practice.flag(measure, difficulty)), LONGEST);
		addHeardRun();
	}

	// the turns heard, by the note each sounds on its first measure's first beat: the run the choosing steps read
	void addHeardRun() {
		int[] openings = openings();
		practice.runs().add(IntStream.of(openings).map(turn -> heardMeasure(planned.start(planned.firstOf(turn)))).boxed().collect(Collectors.toList()));
	}

	// the turns of what the session plays that open its turns (with the interval clock, each turn's first pass)
	int[] openings() {
		return IntStream.range(0, planned.turns()).filter(planned::opens).toArray();
	}

	// the notes track 1 sounds on the beats of each turn of what the session plays (planned), as the output sounds them,
	// the chain delay later
	List<List<HeardNotes.Note>> heardTurns() {
		List<List<Heard.PlayedBeat>> beats = Heard.turnBeats(planned).stream().map(turn -> turn.stream().map(beat -> beat.later(world.chainDelay())).collect(Collectors.toList()))
			.collect(Collectors.toList());
		return HeardNotes.of(beats, world.live()[0]);
	}

	// the Count In clicks the output sounds from the player's sample to the other, as heard (the chain delay later): where
	// the output jumps by more than JUMP from one sample to the next (a note's tone moves less), the first jump of each run
	long[] countInHeard(long from, long to) {
		float[] left = world.live()[0];
		float[] jumps = new float[left.length];
		IntStream.range(1, left.length).forEach(sample -> jumps[sample] = left[sample] - left[sample - 1]);
		return Meters.onsets(jumps, (int) (from + world.chainDelay()), (int) (to + world.chainDelay()), JUMP).stream().mapToLong(Integer::longValue).toArray();
	}

	// before each turn of what the session plays (planned), from its start to its first measure, so many Count In clicks
	// heard, so many samples apart; read from the sample after its start, where the turn before's last note is cut (a
	// jump too), the first click's jump a few samples later (its drum's attack)
	void assertCountInBeforeEachTurn(int count, double apart) {
		for (int turn = 0; turn < planned.turns(); turn++) {
			long[] clicks = countInHeard(planned.turnStart(turn) + 1, planned.start(planned.firstOf(turn)));
			assertEquals(count, clicks.length, "turn " + (turn + 1) + "'s Count In, as heard: " + Arrays.toString(clicks));
			for (int click = 1; click < clicks.length; click++) {
				assertEquals(apart, clicks[click] - clicks[click - 1], 1, "turn " + (turn + 1) + "'s Count In, as heard: " + Arrays.toString(clicks));
			}
		}
	}

	// the player stops by itself at the first block after the turn of what the session plays ends, as heard
	@Then("the player stopped by itself within {int} samples of turn {int}'s end")
	public void stoppedByItself(int within, int turn) {
		assertEquals(planned.turns(), turn, "turns");
		assertEquals(planned.turnEnd(turn - 1) + world.chainDelay(), stopped(), within, "turn " + turn + " ends, as heard");
	}

	// the measure whose note the output sounds on the beat that starts at the player's sample (as heard, the chain
	// delay later)
	int heardMeasure(long sample) {
		long heard = sample + world.chainDelay();
		return noteOver(heard + EDGE, heard + beat() - EDGE) - BASE;
	}

	// a beat of the Background (4/4 at 120 bpm), in samples
	static long beat() {
		return TestInterface.SAMPLE_RATE / 2;
	}

	// after the change (Play, as heard), part by part, a part a Count In or a measure (as many beats): the turn's Count
	// In, its measure, the next turn's Count In, its measure. How each part sounds the turn's note and the next turn's:
	// SOUNDS, QUIET (below SOUNDS: the other note sounds, and a little of it is read at this pitch), SILENT, or not read
	// (a VST3 instrument ignores All Notes Off, so the note held at the pause rings on until it is played again)
	private static final double QUIET = -SOUNDS;
	private static final double UNREAD = 0;
	private static final double[][] PARTS = {{UNREAD, UNREAD}, {SOUNDS, QUIET}, {SILENT, SILENT}, {QUIET, SOUNDS}};

	// whether the level is as the part expects
	private static boolean as(double expected, double level) {
		return (expected == SOUNDS ? level > SOUNDS : expected == UNREAD || level < Math.abs(expected));
	}

	@Then("after Play, turn {int}'s measure sounds again after a Count In of {int} beats, then turn {int}'s after another")
	public void afterPlay(int turn, int beats, int next) {
		assertEquals(turn, changedIn);
		int[] notes = {notesOf(turn).get(0), notesOf(next).get(0)};
		long start = change + world.chainDelay();
		for (int at = 0; at < 4 * beats; at++) {
			long from = start + at * beat() + EDGE;
			long to = start + (at + 1) * beat() - EDGE;
			for (int note = 0; note < 2; note++) {
				double level = levelOf(notes[note], from, to);
				assertTrue(as(PARTS[at / beats][note], level), "beat " + (at + 1) + " after Play: note " + notes[note] + " at " + level);
			}
		}
	}

	// the player stops by itself (no change does) at the first block after the session's last turn ends, as heard
	@Then("the player stopped by itself within {int} samples of turn {int}'s end, {int} beats after Play")
	public void stoppedAfter(int within, int turn, int beats) {
		assertStoppedAfter(within, beats * beat(), "turn " + turn);
	}

	// the player stopped at the first block after the turn ends so many samples after Play, as heard
	void assertStoppedAfter(int within, long samples, String turn) {
		long end = change + samples + world.chainDelay();
		assertEquals(end, stopped(), within, turn + " ends, as heard, at sample " + end);
	}

	// the player stopped at the end of the block the output sounds the turn's end in, so many samples after Play: not
	// before the end is heard, and not a block later
	void assertStoppedAsHeard(long samples, String turn) {
		long end = change + samples + world.chainDelay();
		long stopped = stopped();
		assertTrue(stopped >= end && stopped < end + BLOCK, turn + " ends, as heard, at sample " + end + "; the player stopped at " + stopped);
	}

	// where the turn (from 1) ends, as heard: where the next one starts
	private int heardEnd(int turn) {
		return (int) (planned.turnEnd(turn - 1) + world.chainDelay());
	}

	@Then("the output's right side fades from {float} to {float} over the last 256 samples of turns {int} and {int}, within 1 sample")
	public void rightFadesAtTheEndOfTurns(float from, float to, int one, int other) {
		assertRightFadesAtTheEndOf(from, to, new int[] {one, other});
	}

	@Then("the output's right side fades from {float} to {float} over the last 256 samples of steps {int} to {int}, within 1 sample")
	public void rightFadesAtTheEndOfSteps(float from, float to, int first, int last) {
		assertRightFadesAtTheEndOf(from, to, IntStream.rangeClosed(first, last).toArray());
	}

	// as heard, the right side fades over the last 256 samples of each of the turns (from 1)
	private void assertRightFadesAtTheEndOf(float from, float to, int[] turns) {
		for (int turn : turns) {
			ListeningSteps.assertTransition(world.live()[1], heardEnd(turn) - ListeningSteps.FADE, heardEnd(turn) - 1, from, to);
		}
	}

	// as heard, where the turn's first measure starts: by how much the level rises from the sample before (the Count In
	// click before it still rings a little on that side)
	@Then("the output's right side steps up by {float} on the first sample of each turn")
	@Then("the output's right side steps up by {float} on the first sample of each step")
	public void rightStepsUpOnTheFirstSampleOfEachTurn(float step) {
		float[] right = world.live()[1];
		for (int turn = 0; turn < planned.turns(); turn++) {
			int first = (int) (planned.start(planned.firstOf(turn)) + world.chainDelay());
			assertEquals(step, right[first] - right[first - 1], step / 512, "turn " + (turn + 1) + " at sample " + first);
		}
	}

	// the notes of the turn's measures (from 1)
	private List<Integer> notesOf(int turn) {
		return IntStream.range(planned.firstOf(turn - 1), planned.firstOf(turn)).map(index -> BASE + planned.get(index).getHeader().getNumber()).boxed()
			.collect(Collectors.toList());
	}

	// the MIDI note the output sounds over the samples, NONE without one
	private int noteOver(long from, long to) {
		float[] left = world.live()[0];
		return HeardNotes.nearest(Meters.frequency(left, (int) from, (int) to, TestInterface.SAMPLE_RATE));
	}

	// the level of the note's tone over the samples (Meters.level)
	double levelOf(int note, long from, long to) {
		return Meters.level(world.live()[0], (int) from, (int) to, HeardNotes.pitch(note), TestInterface.SAMPLE_RATE);
	}

	// the player moves at the block after the change: from there each beat of the position's measure sounds its note and
	// not the next measure's, then each beat of the next measure its own and not the one before (each beat read clear of
	// its edges). By level, not by the nearest note: a VST3 instrument ignores All Notes Off, so the note held when the
	// player moved rings on (a defect of ordinary playback, left as it is in this slice)
	@Then("the player plays on from {position}")
	public void playsOnFrom(SongPosition from) {
		TGMeasureHeader header = world.song().getMeasureHeader(from.measure() - 1);
		int beats = header.getTimeSignature().getNumerator();
		long beat = Math.round(TestInterface.SAMPLE_RATE * 60.0 / header.getTempo().getQuarterValue());
		long start = change + world.chainDelay();
		for (int at = 0; at < 2 * beats; at++) {
			int note = BASE + from.measure() + (at < beats ? 0 : 1);
			int other = BASE + from.measure() + (at < beats ? 1 : 0);
			long beatFrom = start + at * beat + EDGE;
			long beatTo = start + (at + 1) * beat - EDGE;
			assertTrue(levelOf(note, beatFrom, beatTo) > SOUNDS && levelOf(other, beatFrom, beatTo) < SILENT,
				"beat " + (at + 1) + " from the move: note " + note + " at " + levelOf(note, beatFrom, beatTo) + ", note " + other + " at " + levelOf(other, beatFrom, beatTo));
		}
	}

	// the player stops at the first block after the edit; the output had sounded the turn's unit up to it
	@Then("the player stopped within {int} samples of the edit, during turn {int}")
	@Then("the player stopped within {int} samples of the edit, during step {int}")
	public void stoppedWithin(int within, int turn) {
		int stopped = running.indexOf(Boolean.FALSE);
		assertTrue(stopped >= 0, "the player never stopped");
		assertTrue((stopped + 1L) * BLOCK - change <= within, "stopped at sample " + (stopped + 1L) * BLOCK + ", the edit at " + change);
		assertEquals(turn, changedIn);
		long heard = change + world.chainDelay();
		int before = noteOver(heard - 2 * EDGE, heard - EDGE / 2);
		assertTrue(notesOf(turn).contains(before), "before the edit the output sounded " + before + ", not one of " + notesOf(turn));
	}

	// the session's turns whose first pass started before the player stopped, each heard sounding a measure of the song
	// on its first measure's first beat
	void assertTurnsStarted(int count) {
		long stopped = stopped();
		int[] started = IntStream.of(openings()).filter(turn -> planned.turnStart(turn) < stopped).toArray();
		assertEquals(count, started.length, "turns started before the player stopped at " + stopped);
		IntStream.of(started).forEach(this::assertSoundsAMeasure);
	}

	// the turn of what the session plays sounds a measure of the song on its first measure's first beat
	private void assertSoundsAMeasure(int turn) {
		int measure = heardMeasure(planned.start(planned.firstOf(turn)));
		assertTrue(measure >= 1 && measure <= world.song().countMeasureHeaders(), "the turn at " + turn + " sounds " + measure);
	}
}
