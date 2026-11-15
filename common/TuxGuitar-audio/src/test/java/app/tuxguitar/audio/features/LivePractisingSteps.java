package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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
	private static final int BLOCK = 512;
	// the blocks played on once the change is made
	private static final int AFTER = 400;
	// the most blocks a session that stops by itself is played on for
	private static final int LONGEST = 4000;
	// clear of a beat's edges (and of where exactly a move lands in its block) when reading its note
	private static final int EDGE = 2048;
	// a note's tone sounds above SOUNDS; none above -60 dBFS is silent
	private static final double SOUNDS = 0.01;
	private static final double SILENT = 0.001;

	private final AudioWorld world;
	private final Practice practice;
	private final SongSteps songSteps;
	// the sample of the change (the first after the blocks played before it), the turn it was made in (from 1), and
	// for each block whether the player ran after its turn of the play loop
	private long change;
	private int changedIn;
	// what the session would play, offline, with the same seed: its turns
	private TGTimeline planned;
	private final List<Boolean> running = new ArrayList<Boolean>();

	public LivePractisingSteps(AudioWorld world, Practice practice, SongSteps songSteps) {
		this.world = world;
		this.practice = practice;
		this.songSteps = songSteps;
	}

	private MidiPlayer player() {
		return MidiPlayer.getInstance(world.fixture().context);
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
		planned = TGPractiseAction.timeline(world.fixture().context, world.song(), practice.session(), Practice.SEED);
		int at = (int) ((planned.turnStart(during - 1) + planned.turnEnd(during - 1)) / 2 / BLOCK);
		change = (at + 1L) * BLOCK;
		changedIn = during;
		MidiPlayer player = player();
		player.loadSequencer(new MidiSequencerImpl(world.fixture().context));
		player.practise(practice.session(), Practice.SEED);
		world.threads().runStartHere();
		world.setLive(LivePlay.run(world.fixture().context, world.synthesizer(), world.backend(), () -> { }, block -> {
			if (block == at) {
				changing.run();
			}
			boolean goesOn = player.runPlayLoopProcess();
			running.add(player.isRunning());
			return goesOn && block < at + after;
		}));
		player.stop();
		player.closeChannels();
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

	// as the app pauses and plays again (TGTransport: no move during a session)
	@When("I practise {int} turns live, pausing during turn {int} and playing again")
	public void practiseLivePausing(int turns, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, () -> {
			player().pause();
			try {
				player().play();
			} catch (MidiPlayerException e) {
				throw new IllegalStateException(e);
			}
			world.threads().runStartHere();
		}, LONGEST);
	}

	// as the app flags it: the flag action, checked by the session's chooser (TGPracticeEdits); the turns heard, by the
	// note each sounds on its first beat, are the run the choosing steps read
	@When("I practise {int} turns live, flagging measure {int} as {int}x difficult during turn {int}")
	public void practiseLiveFlagging(int turns, int measure, int difficulty, int during) throws MidiPlayerException, TGAudioDeviceException, TGPracticeRefusal {
		practiseLive(turns, during, () -> practice.edit(() -> practice.flag(measure, difficulty)), LONGEST);
		practice.runs().add(IntStream.range(0, turns).map(turn -> heardMeasure(planned.turnStart(turn))).boxed().collect(Collectors.toList()));
	}

	// the measure whose note the output sounds on the beat that starts at the player's sample (as heard, the chain
	// delay later)
	private int heardMeasure(long sample) {
		long heard = sample + world.chainDelay();
		return noteOver(heard + EDGE, heard + beat() - EDGE) - BASE;
	}

	// a beat of the Background (4/4 at 120 bpm), in samples
	private static long beat() {
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
		int stopped = running.indexOf(Boolean.FALSE);
		assertTrue(stopped >= 0, "the player never stopped");
		long end = change + beats * beat() + world.chainDelay();
		assertEquals(end, (stopped + 1L) * BLOCK, within, "turn " + turn + " ends, as heard, at sample " + end);
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
	private double levelOf(int note, long from, long to) {
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
	public void stoppedWithin(int within, int turn) {
		int stopped = running.indexOf(Boolean.FALSE);
		assertTrue(stopped >= 0, "the player never stopped");
		assertTrue((stopped + 1L) * BLOCK - change <= within, "stopped at sample " + (stopped + 1L) * BLOCK + ", the edit at " + change);
		assertEquals(turn, changedIn);
		long heard = change + world.chainDelay();
		int before = noteOver(heard - 2 * EDGE, heard - EDGE / 2);
		assertTrue(notesOf(turn).contains(before), "before the edit the output sounded " + before + ", not one of " + notesOf(turn));
	}
}
