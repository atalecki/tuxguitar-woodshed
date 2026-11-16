package app.tuxguitar.audio.features;

import static app.tuxguitar.audio.features.PractisingSteps.firstPass;
import static app.tuxguitar.audio.features.PractisingSteps.lastPass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.Meters;
import app.tuxguitar.audio.features.HeardNotes.Note;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeUnitKind;
import io.cucumber.java.en.Then;

/**
 * Steps that measure a session practised offline with the interval clock (README, Practice: Interval clock): its turns,
 * each the passes of one unit, and the breaks between them. A pass is measured as PractisingSteps measures a turn
 * without the clock (its notes on track 1, its Count In the clicks before its first note); a turn is the passes the
 * timeline says it holds, from where its first pass starts (its first Count In click, or its first note) to where its
 * last pass's last note ends. The break before a turn is from the end of the turn before to its start.
 */
public class TimedPractisingSteps {

	// -60 dBFS
	private static final double SILENT = 0.001;

	private final AudioWorld world;
	private final PractisingSteps practising;

	public TimedPractisingSteps(AudioWorld world, PractisingSteps practising) {
		this.world = world;
		this.practising = practising;
	}

	// the passes of each turn, as indices of PractisingSteps.turns
	private List<List<Integer>> turns() {
		return practising.practiceTurns();
	}

	// the session's one turn
	private List<Integer> theTurn() {
		List<List<Integer>> turns = turns();
		assertEquals(1, turns.size(), "turns");
		return turns.get(0);
	}

	// where the pass starts: its first Count In click, or its first note without one
	long start(List<List<Note>> passes, int pass) {
		long[] clicks = practising.countIn(passes, pass);
		return (clicks.length > 0 ? clicks[0] : PractisingSteps.first(passes.get(pass)).start);
	}

	static long end(List<List<Note>> passes, int pass) {
		return PractisingSteps.last(passes.get(pass)).end();
	}

	// the turn plays one unit of the units so many times, each pass its notes once and in order, each pass starting where
	// the one before ends (its Count In first, if any)
	void assertPasses(List<Integer> turn, List<List<Integer>> units, int times, String name) {
		List<List<Note>> passes = practising.turns();
		assertEquals(times, turn.size(), name + "'s passes");
		List<Integer> notes = PractisingSteps.notes(passes.get(firstPass(turn)));
		assertTrue(units.contains(notes), name + " sounds " + notes + ", no unit of " + units);
		for (int pass : turn) {
			assertEquals(notes, PractisingSteps.notes(passes.get(pass)), name + ", pass " + pass);
		}
		assertBackToBack(passes, turn, name);
	}

	// each pass of the turn but the first starts where the one before ends
	private void assertBackToBack(List<List<Note>> passes, List<Integer> turn, String name) {
		for (int pass = firstPass(turn) + 1; pass <= lastPass(turn); pass++) {
			assertEquals(end(passes, pass - 1), start(passes, pass), 1, name + ", pass " + pass + " after the one before");
		}
	}

	// the notes of each unit of the pool
	private List<List<Integer>> units(TGPracticeUnitKind kind, int[] pool) throws TGPracticeRefusal {
		return practising.notesOfBlocks(kind, pool, 1);
	}

	@Then("the turn sounds the notes of one {unit} of {pool} {int} times back to back, each measure once and in order every time, each within 1 sample of its beat at {int}%")
	public void theTurnLoopsAUnitAt(TGPracticeUnitKind kind, int[] pool, int times, int percent) throws TGPracticeRefusal {
		theTurnLoopsABlockAt(1, kind, pool, times, percent);
	}

	@Then("the turn sounds the notes of one block of {int} {unit}s of {pool} {int} times back to back, each measure once and in order every time, each within 1 sample of its beat at {int}%")
	public void theTurnLoopsABlockAt(int units, TGPracticeUnitKind kind, int[] pool, int times, int percent) throws TGPracticeRefusal {
		List<Integer> turn = theTurn();
		assertPasses(turn, practising.notesOfBlocks(kind, pool, units), times, "the turn");
		List<List<Note>> passes = practising.turns();
		turn.forEach(pass -> PractisingSteps.assertOnTheBeat(passes.get(pass), percent, "pass " + pass));
	}

	@Then("the turn sounds the note of one beat of measure {int}, then the note of the beat after it, {int} times back to back, each within 1 sample of its beat")
	public void theTurnLoopsABeatAndItsJoint(int measure, int times) throws TGPracticeRefusal {
		theTurnLoopsAUnitAt(TGPracticeUnitKind.BEAT, new int[] {measure, measure}, times, practising.percent());
	}

	@Then("the turn sounds the notes of {unit} {int} {int} times back to back, each measure once and in order every time, each within 1 sample of its beat")
	public void theTurnLoopsTheUnit(TGPracticeUnitKind kind, int number, int times) throws TGPracticeRefusal {
		theTurnLoopsAUnitAt(kind, new int[] {number, number}, times, practising.percent());
	}

	@Then("the turn sounds the notes of one {unit} of {pool} {int} times, each after a Count In of {int} clicks")
	public void theTurnLoopsAUnitEachAfterACountIn(TGPracticeUnitKind kind, int[] pool, int times, int clicks) throws TGPracticeRefusal {
		List<Integer> turn = theTurn();
		assertPasses(turn, units(kind, pool), times, "the turn");
		assertCountIns(turn, clicks, clicks);
	}

	@Then("each turn sounds the notes of one {unit} of {pool} {int} times back to back, after a Count In of {int} clicks")
	public void eachTurnLoopsAUnitAfterACountIn(TGPracticeUnitKind kind, int[] pool, int times, int clicks) throws TGPracticeRefusal {
		List<List<Integer>> turns = turns();
		for (int turn = 0; turn < turns.size(); turn++) {
			assertPasses(turns.get(turn), units(kind, pool), times, "turn " + (turn + 1));
			assertCountIns(turns.get(turn), clicks, 0);
		}
	}

	@Then("each turn sounds the notes of {unit} {int} {int} times back to back, after a Count In of {int} clicks")
	public void eachTurnLoopsTheUnitAfterACountIn(TGPracticeUnitKind kind, int number, int times, int clicks) throws TGPracticeRefusal {
		eachTurnLoopsAUnitAfterACountIn(kind, new int[] {number, number}, times, clicks);
	}

	// the turn's first pass after a Count In of so many clicks, the others after so many
	private void assertCountIns(List<Integer> turn, int first, int others) {
		List<List<Note>> passes = practising.turns();
		assertEquals(first, practising.countIn(passes, firstPass(turn)).length, "pass " + firstPass(turn) + "'s Count In clicks");
		for (int pass : turn.subList(1, turn.size())) {
			assertEquals(others, practising.countIn(passes, pass).length, "pass " + pass + "'s Count In clicks");
		}
	}

	@Then("the output holds a Count In of {int} clicks before the first pass only")
	public void aCountInBeforeTheFirstPass(int clicks) {
		turns().forEach(turn -> assertCountIns(turn, clicks, 0));
	}

	@Then("the output holds a Count In of {int} clicks before every pass")
	public void aCountInBeforeEveryPass(int clicks) {
		turns().forEach(turn -> assertCountIns(turn, clicks, clicks));
	}

	@Then("the turn ends {int} samples after it started, within 1 sample")
	public void theTurnLasts(int samples) {
		List<Integer> turn = theTurn();
		List<List<Note>> passes = practising.turns();
		assertEquals(samples, end(passes, lastPass(turn)) - start(passes, firstPass(turn)), 1, "the turn");
	}

	@Then("the session's last turn ends {int} samples after the first one started, within 1 sample")
	@Then("the session's last step ends {int} samples after the first one started, within 1 sample")
	public void theSessionLasts(int samples) {
		List<List<Integer>> turns = turns();
		List<List<Note>> passes = practising.turns();
		assertEquals(samples, end(passes, lastPass(turns.get(turns.size() - 1))) - start(passes, firstPass(turns.get(0))), 1, "the session");
	}

	@Then("the player has stopped by itself within {int} samples of the turn's end")
	@Then("the player has stopped by itself within {int} samples of the session's last turn's end")
	public void stoppedByItself(int samples) {
		List<List<Integer>> turns = turns();
		long end = end(practising.turns(), lastPass(turns.get(turns.size() - 1)));
		assertEquals(end, practising.heard().left().length, samples, "the output's end");
		assertFalse(MidiPlayer.getInstance(world.fixture().context).isRunning());
	}

	// each break: from where the turn before ends to where the next starts
	private List<long[]> breaks() {
		List<List<Integer>> turns = turns();
		List<List<Note>> passes = practising.turns();
		return IntStream.range(1, turns.size()).mapToObj(turn -> new long[] {end(passes, lastPass(turns.get(turn - 1))), start(passes, firstPass(turns.get(turn)))})
			.collect(Collectors.toList());
	}

	@Then("each turn but the first starts {int} samples after the turn before ended, within 1 sample, the output silent in between")
	public void eachTurnAfterABreak(int samples) {
		Heard heard = practising.heard();
		for (long[] silence : breaks()) {
			assertEquals(samples, silence[1] - silence[0], 1, "the break " + Arrays.toString(silence));
			double peak = Math.max(Meters.peak(heard.left(), (int) silence[0], (int) silence[1]), Meters.peak(heard.right(), (int) silence[0], (int) silence[1]));
			assertTrue(peak < SILENT, "the break " + Arrays.toString(silence) + " peaks at " + peak);
		}
	}

	@Then("during the break TuxGuitar reports beat 1 of turn {int}'s first measure")
	public void duringTheBreakTheNextTurnsFirstBeat(int turn) {
		long[] silence = breaks().get(turn - 2);
		assertTrue(silence[1] > silence[0], "no break before turn " + turn + ": " + Arrays.toString(silence));
		practising.assertReportsTheFirstTick(practising.turns().get(firstPass(turns().get(turn - 1))), silence[0], "the break before turn " + turn);
	}

	@Then("track {int} fades from {float} to {float} over the last 256 samples of every pass but the session's last, within 1 sample")
	public void fadesAtTheEndOfEveryPass(int number, float from, float to) {
		practising.assertFadesAtTheEndOf(number, from, to, IntStream.range(0, practising.turns().size() - 1).toArray());
	}

	// the echo blurs the notes it rings over (Test Echo's copy of the beat before, at the same pitch), so the break is
	// read from the beats: from the end of the turn's last beat to the next Count In click
	@Then("the output holds the echo of turn {int}'s last note {int} samples after it, within 1 sample, during the break")
	public void theEchoInTheBreak(int turn, int samples) {
		Note note = PractisingSteps.last(practising.turns().get(lastPass(turns().get(turn - 1))));
		long end = note.beat.sample + note.beat.length;
		long next = Arrays.stream(practising.heard().countIn()).filter(click -> click >= end).findFirst().orElse(Long.MAX_VALUE);
		practising.assertEcho(note, samples, end, next, "the break after turn " + turn);
	}

	// whether any of the samples is in the range (from its first to before its second)
	private static boolean anyIn(long[] samples, long[] range) {
		return Arrays.stream(samples).anyMatch(sample -> sample >= range[0] && sample < range[1]);
	}

	@Then("the output holds no Metronome click during the break")
	public void noMetronomeInTheBreak() {
		long[] clicks = practising.heard().metronome();
		List<long[]> breaks = breaks();
		assertFalse(breaks.isEmpty(), "no break");
		for (long[] silence : breaks) {
			assertFalse(anyIn(clicks, silence), "a click in the break " + Arrays.toString(silence));
		}
	}
}
