package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.Meters;
import app.tuxguitar.audio.TGReportedPositions;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.features.HeardNotes.Note;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGPracticeGrouping;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.practice.TGTempoScale;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that practise interleaved practice offline and measure its turns (README, Practice). Track 1 plays Test Sine,
 * MIDI note BASE plus the measure's number on every beat (the Background), so the notes heard on its tap (HeardNotes)
 * say which measure sounded and where. A turn's first measure is where its first note starts; its end is where its
 * last note ends (its start and a beat); its Count In is the clicks between the end of the turn before (or the start)
 * and its first measure.
 */
public class PractisingSteps {

	// the Background's notes: MIDI note 35 plus the measure's number
	static final int BASE = 35;

	private final AudioWorld world;
	private final Practice practice;
	private final LivePractisingSteps live;

	public PractisingSteps(AudioWorld world, Practice practice, LivePractisingSteps live) {
		this.world = world;
		this.practice = practice;
		this.live = live;
	}

	@When("I practise {int} turns")
	public void practise(int turns) {
		practice.practise(turns, planned -> Long.MAX_VALUE);
	}

	// as set: with the interval clock, its time decides the turns
	@When("I practise the session")
	public void practiseTheSession() {
		practice.practise(planned -> Long.MAX_VALUE);
	}

	// where the beat of the turn's first measure (both from 1) sounds in what the session plays
	@Given("I have practised {int} turns and stopped at turn {int}'s first measure beat {int}")
	@When("I practise {int} turns and stop at turn {int}'s first measure beat {int}")
	public void practiseAndStop(int turns, int turn, int beat) {
		practice.practise(turns, planned -> {
			int first = planned.firstOf(turn - 1);
			long length = planned.get(first).getHeader().getTimeSignature().getDenominator().getTime();
			return planned.sampleAt(first, (beat - 1) * length);
		});
	}

	Heard heard() {
		return PlaySteps.heard(world);
	}

	// the notes track 1 sounds, turn by turn (with the interval clock, pass by pass); live, as the output sounds them
	List<List<Note>> turns() {
		List<List<Note>> turns = (world.live() != null ? live.heardTurns() : HeardNotes.of(heard(), heard().tap(world.track(1))));
		assertFalse(turns.isEmpty(), "no turn played");
		return turns;
	}

	// the timeline's turns of each practice turn, as indices of turns(): a practice turn opens with its first pass and
	// holds the passes up to the next one's (one, without the interval clock)
	List<List<Integer>> practiceTurns() {
		TGTimeline timeline = heard().timeline();
		int[] opening = IntStream.concat(IntStream.range(0, timeline.turns()).filter(timeline::opens), IntStream.of(timeline.turns())).toArray();
		List<List<Integer>> turns = IntStream.range(0, opening.length - 1).mapToObj(turn -> IntStream.range(opening[turn], opening[turn + 1]).boxed().collect(Collectors.toList()))
			.collect(Collectors.toList());
		assertFalse(turns.isEmpty(), "no turn played");
		return turns;
	}

	static int firstPass(List<Integer> turn) {
		return turn.get(0);
	}

	static int lastPass(List<Integer> turn) {
		return turn.get(turn.size() - 1);
	}

	static Note first(List<Note> turn) {
		return turn.get(0);
	}

	static Note last(List<Note> turn) {
		return turn.get(turn.size() - 1);
	}

	// the clicks of the turn's Count In: from the end of the turn before (or the start) to its first beat (see Note.at)
	long[] countIn(List<List<Note>> turns, int turn) {
		long from = endBefore(turns, turn);
		long to = first(turns.get(turn)).at();
		return Arrays.stream(heard().countIn()).filter(click -> click >= from && click < to).toArray();
	}

	// where the turn before ends; the start, before the first
	private static long endBefore(List<List<Note>> turns, int turn) {
		return (turn == 0 ? 0 : last(turns.get(turn - 1)).end());
	}

	// the units of the pool, as the session groups the song (beats: those of the pool's measures)
	List<TGPracticeUnit> units(TGPracticeUnitKind kind, int[] pool) throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(practice.settings().getMeasuresPerLine(), practice.settings().getLinesPerPage());
		return grouping.units(kind, pool[0], pool[1], world.song(), "pool");
	}

	// the blocks of so many units in a row of the pool, every such run, sliding by one (README, Block)
	List<List<TGPracticeUnit>> blocks(TGPracticeUnitKind kind, int[] pool, int units) throws TGPracticeRefusal {
		List<TGPracticeUnit> all = units(kind, pool);
		return IntStream.rangeClosed(0, all.size() - units).mapToObj(first -> all.subList(first, first + units)).collect(Collectors.toList());
	}

	// the notes each block sounds (see notesOf)
	List<List<Integer>> notesOfBlocks(TGPracticeUnitKind kind, int[] pool, int units) throws TGPracticeRefusal {
		return blocks(kind, pool, units).stream().map(block -> notesOf(block.get(0), block.get(block.size() - 1))).collect(Collectors.toList());
	}

	// the notes the units from one to the other sound, one per beat: their measures', each measure once, in order; or
	// the beats'
	List<Integer> notesOf(TGPracticeUnit from, TGPracticeUnit to) {
		return (from.getKind() != TGPracticeUnitKind.BEAT ? notesOf(from.getFirst(), to.getLast()) : notesOfBeats(from, to));
	}

	// the notes the beats from one to the other sound, through the barline, then the joint's, the beat after the last (of
	// its measure, or the next measure's first; none after the song's last)
	private List<Integer> notesOfBeats(TGPracticeUnit from, TGPracticeUnit to) {
		List<Integer> notes = new ArrayList<Integer>();
		for (int measure = from.getFirst(); measure <= to.getFirst(); measure++) {
			int first = (measure == from.getFirst() ? from.getNumber() : 1);
			int last = (measure == to.getFirst() ? to.getNumber() : beats(measure));
			notes.addAll(Collections.nCopies(last - first + 1, BASE + measure));
		}
		int joint = (to.getNumber() < beats(to.getFirst()) ? to.getFirst() : to.getFirst() + 1);
		if (joint <= world.song().countMeasureHeaders()) {
			notes.add(BASE + joint);
		}
		return notes;
	}

	// the beats of the measure (from 1): its time signature's numerator
	private int beats(int measure) {
		return world.song().getMeasureHeader(measure - 1).getTimeSignature().getNumerator();
	}

	// the notes the measures (first to last, from 1) sound, each measure once, in order: one per beat
	List<Integer> notesOf(int first, int last) {
		List<Integer> notes = new ArrayList<Integer>();
		for (int measure = first; measure <= last; measure++) {
			notes.addAll(Collections.nCopies(beats(measure), BASE + measure));
		}
		return notes;
	}

	static List<Integer> notes(List<Note> turn) {
		return turn.stream().map(note -> note.note).collect(Collectors.toList());
	}

	// each played measure once and in order is the unit's notes, one per beat
	@Then("each turn sounds the notes of one {unit} of {pool}, each measure once and in order, each within 1 sample of its beat")
	@Then("each turn sounds the notes of one {unit} of {pool}, each within 1 sample of its beat")
	public void eachTurnSoundsAUnit(TGPracticeUnitKind kind, int[] pool) throws TGPracticeRefusal {
		eachTurnSoundsAUnitAt(kind, pool, percent());
	}

	// offline or live
	@Then("each turn sounds the note of one beat of {pool}, then the note of the beat after it, each within 1 sample of its beat")
	public void eachTurnSoundsABeatAndItsJoint(int[] pool) throws TGPracticeRefusal {
		eachTurnSoundsAUnitAt(TGPracticeUnitKind.BEAT, pool, percent());
	}

	// the Percentage of Tempo now
	int percent() {
		return MidiPlayer.getInstance(world.fixture().context).getMode().getCurrentPercent();
	}

	// each of the turn's notes sounds on its beat, at the Percentage of Tempo
	static void assertOnTheBeat(List<Note> turn, int percent, String name) {
		assertOnTheBeat(turn, TGTempoScale.percent(percent), name);
	}

	// each of the turn's notes sounds on its beat, at the tempo scale
	static void assertOnTheBeat(List<Note> turn, TGTempoScale scale, String name) {
		for (Note note : turn) {
			assertEquals(note.beat.sample, note.start, 1, name + ": " + turn);
			assertEquals(scale, note.beat.scale, name + "'s tempo");
		}
	}

	@Then("each turn sounds the notes of one {unit} of {pool}, each within 1 sample of its beat at {int}%")
	public void eachTurnSoundsAUnitAt(TGPracticeUnitKind kind, int[] pool, int percent) throws TGPracticeRefusal {
		eachTurnSoundsOneOf(notesOfBlocks(kind, pool, 1), percent);
	}

	// offline or live
	@Then("each turn sounds the notes of one block of {int} {unit}s of {pool}, each measure once and in order, each within 1 sample of its beat")
	public void eachTurnSoundsABlock(int units, TGPracticeUnitKind kind, int[] pool) throws TGPracticeRefusal {
		eachTurnSoundsOneOf(notesOfBlocks(kind, pool, units), percent());
	}

	// as many turns as blocks, each block's once, offline or live
	@Then("the turns sound each block of {int} beats of {pool} once, its beats, then the note of the beat after it, each within 1 sample of its beat")
	public void theTurnsSoundEachBlockOnce(int units, int[] pool) throws TGPracticeRefusal {
		List<List<Integer>> blocks = notesOfBlocks(TGPracticeUnitKind.BEAT, pool, units);
		eachTurnSoundsOneOf(blocks, percent());
		List<List<Integer>> sounded = turns().stream().map(PractisingSteps::notes).collect(Collectors.toList());
		assertEquals(sorted(blocks), sorted(sounded), "the turns sound " + sounded);
	}

	private static List<String> sorted(List<List<Integer>> notes) {
		return notes.stream().map(List::toString).sorted().collect(Collectors.toList());
	}

	// each turn's notes are one of those given, each on its beat at the Percentage of Tempo
	private void eachTurnSoundsOneOf(List<List<Integer>> units, int percent) {
		List<List<Note>> turns = turns();
		for (int turn = 0; turn < turns.size(); turn++) {
			List<Integer> notes = notes(turns.get(turn));
			assertTrue(units.contains(notes), "turn " + (turn + 1) + " sounds " + notes + ", no unit of " + units);
			assertOnTheBeat(turns.get(turn), percent, "turn " + (turn + 1));
		}
	}

	void assertCountIn(long[] clicks, int count, double apart, String turn) {
		assertEquals(count, clicks.length, turn + ": " + Arrays.toString(clicks));
		for (int i = 1; i < clicks.length; i++) {
			assertEquals(apart, clicks[i] - clicks[i - 1], 1.0, turn + ": " + Arrays.toString(clicks));
		}
		CountInSteps.assertEachSounds(heard(), clicks, Math.round(apart));
	}

	// live, as LivePractisingSteps hears it
	@Then("before each turn the output holds {int} Count In clicks, {double} samples apart within 1 sample")
	public void beforeEachTurnTheCountIn(int count, double apart) {
		if (world.live() != null) {
			live.assertCountInBeforeEachTurn(count, apart);
			return;
		}
		List<List<Note>> turns = turns();
		for (int turn = 0; turn < turns.size(); turn++) {
			assertCountIn(countIn(turns, turn), count, apart, "turn " + (turn + 1));
		}
	}

	// the turn whose first note is the unit's first measure's
	@Then("before the turn of {unit} {int} the output holds {int} Count In clicks, {double} samples apart within 1 sample")
	public void beforeTheTurnOfTheCountIn(TGPracticeUnitKind kind, int number, int count, double apart) throws TGPracticeRefusal {
		int note = BASE + units(kind, new int[] {number, number}).get(0).getFirst();
		List<List<Note>> turns = turns();
		int turn = IntStream.range(0, turns.size()).filter(each -> first(turns.get(each)).note == note).findFirst().orElse(-1);
		assertTrue(turn >= 0, "no turn of " + kind.getName() + " " + number + ": " + turns);
		assertCountIn(countIn(turns, turn), count, apart, kind.getName() + " " + number);
	}

	@Then("the last Count In click of each turn sounds {int} samples before the turn's first measure, within 1 sample")
	@Then("the last Count In click of each turn sounds {int} samples before the turn's beat, within 1 sample")
	public void theLastClickOfEachTurn(int samples) {
		List<List<Note>> turns = turns();
		for (int turn = 0; turn < turns.size(); turn++) {
			long[] clicks = countIn(turns, turn);
			assertTrue(clicks.length > 0, "turn " + (turn + 1) + " has no Count In");
			assertEquals(samples, first(turns.get(turn)).at() - clicks[clicks.length - 1], 1, "turn " + (turn + 1));
		}
	}

	// it played that many practice turns (or steps), and the output ends where the last one's last pass ends
	@Then("the player has stopped by itself at the end of turn {int}")
	@Then("the player has stopped by itself at the end of step {int}")
	public void stoppedByItself(int turn) {
		List<List<Integer>> turns = practiceTurns();
		assertEquals(turn, turns.size());
		assertEquals(last(turns().get(lastPass(turns.get(turn - 1)))).end(), heard().left().length, 1, "the output's end");
		assertFalse(MidiPlayer.getInstance(world.fixture().context).isRunning());
	}

	@Then("the first Count In click of turns {int} and {int} sounds at the end of the turn before, within 1 sample")
	public void theFirstClickAtTheEndOfTheTurnBefore(int one, int other) {
		List<List<Note>> turns = turns();
		for (int turn : new int[] {one - 1, other - 1}) {
			long[] clicks = countIn(turns, turn);
			assertTrue(clicks.length > 0, "turn " + (turn + 1) + " has no Count In");
			assertEquals(last(turns.get(turn - 1)).end(), clicks[0], 1, "turn " + (turn + 1));
		}
	}

	@Then("the output holds no Count In clicks")
	public void noCountIn() {
		assertEquals("[]", Arrays.toString(heard().countIn()));
		assertEquals(0, Math.max(Meters.peak(heard().drums(0), 0, heard().left().length), Meters.peak(heard().drums(1), 0, heard().left().length)), 0);
	}

	@Then("turns {int} and {int} start at the end of the turn before, within 1 sample")
	public void turnsStartAtTheEndOfTheTurnBefore(int one, int other) {
		List<List<Note>> turns = turns();
		for (int turn : new int[] {one - 1, other - 1}) {
			assertEquals(last(turns.get(turn - 1)).end(), first(turns.get(turn)).start, 1, "turn " + (turn + 1));
		}
	}

	@Then("every turn lasts {int} samples, within 1 sample")
	public void everyTurnLasts(int samples) {
		for (List<Note> turn : turns()) {
			assertEquals(samples, last(turn).end() - first(turn).start, 1, turn.toString());
		}
	}

	// the song tick of each note: its beat's
	private static List<Long> ticks(List<Note> turn) {
		return turn.stream().map(note -> note.beat.tick).collect(Collectors.toList());
	}

	// the output sounds the render this many samples after the clock
	private int delay() {
		return heard().latency() + world.session().getDevice().getOutputLatency();
	}

	// at every sample of the clock while the output sounds the turn's Count In, the position reported is its first tick
	@Then("during each turn's Count In TuxGuitar reports beat 1 of the turn's first measure")
	@Then("during each turn's Count In TuxGuitar reports the turn's beat")
	public void duringEachCountInTheFirstBeat() {
		List<List<Note>> turns = turns();
		for (int turn = 0; turn < turns.size(); turn++) {
			long[] clicks = countIn(turns, turn);
			assertTrue(clicks.length > 0, "turn " + (turn + 1) + " has no Count In");
			assertReportsTheFirstTick(turns.get(turn), clicks[0], "turn " + (turn + 1));
		}
	}

	// at every sample of the clock from the one the output sounds where the given sample plays to the turn's first note,
	// the position reported is the turn's first tick; without a delay, from the sample after it (a position is reported
	// once the clock has played its sample, see TGReportedPositions)
	void assertReportsTheFirstTick(List<Note> turn, long from, String name) {
		TGReportedPositions reported = heard().reportedAt(world.session().getDevice().getOutputLatency());
		long tick = ticks(turn).get(0);
		for (long sample = Math.max(from + delay(), from + 1); sample < first(turn).start + delay(); sample++) {
			assertEquals(tick, reported.tickAt(sample), name + " at " + sample);
		}
	}

	@Then("during each turn TuxGuitar reports each of its beats within 1 sample of the output sounding it")
	@Then("during each step TuxGuitar reports each of its beats within 1 sample of the output sounding it")
	@Then("during each turn TuxGuitar reports its beat and its joint's, each within 1 sample of the output sounding it")
	public void duringEachTurnEachBeat() {
		for (List<Note> turn : turns()) {
			assertReported(turn, first(turn).start + delay() - 1);
		}
	}

	// ordinary playing: one turn
	@Then("TuxGuitar reports each of the {int} beats within 1 sample of the output sounding it")
	public void reportsEachBeat(int count) {
		List<List<Note>> turns = turns();
		assertEquals(1, turns.size());
		assertEquals(count, turns.get(0).size());
		assertReported(turns.get(0), 0);
	}

	// a beat is reported on the first sample of the clock, from the given one on, whose reported position is in it (the
	// song may jump back to it, as from one pass to the next); the output sounds it where its note starts, the delay later
	private void assertReported(List<Note> turn, long from) {
		TGReportedPositions reported = heard().reportedAt(world.session().getDevice().getOutputLatency());
		List<Long> ticks = ticks(turn);
		long sample = from;
		for (int beat = 0; beat < turn.size(); beat++) {
			long length = world.song().getMeasureHeader(turn.get(beat).note - BASE - 1).getTimeSignature().getDenominator().getTime();
			sample = firstIn(reported, sample, ticks.get(beat), length);
			assertEquals(turn.get(beat).start + delay(), sample, 1, "beat " + (beat + 1) + " of " + turn);
		}
	}

	// the first sample of the clock, from the given one on, whose reported position is in the ticks from the tick
	private static long firstIn(TGReportedPositions reported, long from, long tick, long length) {
		long sample = from;
		while (Math.floorDiv(reported.tickAt(sample) - tick, length) != 0) {
			sample++;
		}
		return sample;
	}

	@Then("track {int} fades from {float} to {float} over the last 256 samples of turns {int} and {int}, within 1 sample")
	public void fadesAtTheEndOfTurns(int number, float from, float to, int one, int other) {
		assertFadesAtTheEndOf(number, from, to, new int[] {one - 1, other - 1});
	}

	@Then("track {int} fades from {float} to {float} over the last 256 samples of steps {int} to {int}, within 1 sample")
	public void fadesAtTheEndOfSteps(int number, float from, float to, int first, int last) {
		assertFadesAtTheEndOf(number, from, to, IntStream.range(first - 1, last).toArray());
	}

	// the track fades over the last 256 samples of each of the turns (from 0)
	void assertFadesAtTheEndOf(int number, float from, float to, int[] which) {
		List<List<Note>> turns = turns();
		float[] tap = heard().tap(world.track(number));
		for (int turn : which) {
			int end = last(turns.get(turn)).end();
			ListeningSteps.assertTransition(tap, end - ListeningSteps.FADE, end - 1, from, to);
		}
	}

	@Then("track {int}'s first sample of each turn is {float}")
	@Then("track {int}'s first sample of each step is {float}")
	@Then("track {int}'s first sample of each pass is {float}")
	public void firstSampleOfEachTurn(int number, float value) {
		float[] tap = heard().tap(world.track(number));
		for (List<Note> turn : turns()) {
			assertEquals(value, tap[first(turn).start], 0.000001f, turn.toString());
		}
	}

	// on track 1's tap, where it joins the output (the Count In clicks join it too): after the turn's last note ends,
	// a sine at its pitch, started that many samples after the note's beat
	@Then("the output holds the echo of turn {int}'s last note {int} samples after it, within 1 sample, during turn {int}'s Count In")
	public void theEchoOfTheLastNote(int turn, int samples, int next) {
		List<List<Note>> turns = turns();
		long[] clicks = countIn(turns, next - 1);
		assertEcho(last(turns.get(turn - 1)), samples, clicks.length > 0 ? clicks[0] : Long.MAX_VALUE, first(turns.get(next - 1)).start, "turn " + next + "'s Count In");
	}

	// on track 1's tap: a sine at the note's pitch, started that many samples after its beat, found after the note ends
	// (in 2048 samples from 16 after it), which must lie between the two samples
	void assertEcho(Note note, int samples, long after, long before, String where) {
		int from = note.beat.sample + note.beat.length + 16;
		int to = from + 2048;
		assertTrue(from >= after && to < before, "not in " + where + ": " + from + " to " + to + ", " + after + " to " + before);
		int echo = HeardNotes.sineStart(heard().tap(world.track(1)), note.note, from, to, note.beat.sample + samples);
		assertEquals(note.beat.sample + samples, echo, 1, "the echo of " + note);
	}

	@Then("the output holds {int} Metronome clicks, each within 1 sample of a beat of a turn")
	@Then("the output holds {int} Metronome clicks, each within 1 sample of a beat of a pass")
	public void metronomeOnTheBeats(int count) {
		List<Integer> beats = turns().stream().flatMap(List::stream).map(note -> note.start).collect(Collectors.toList());
		long[] clicks = heard().metronome();
		assertEquals(count, clicks.length, Arrays.toString(clicks));
		for (long click : clicks) {
			assertTrue(beats.stream().anyMatch(beat -> Math.abs(beat - click) <= 1), "click at " + click + ", beats " + beats);
		}
		CountInSteps.assertEachSounds(heard(), clicks, clicks[1] - clicks[0]);
	}

	// a turn starts with its Count In's first click, or its first note without one; with the interval clock its first
	// pass opens it. Played live, see LivePractisingSteps
	@Then("the output holds {int} turn(s) started")
	@Then("the output holds {int} steps started")
	public void turnsStarted(int count) {
		if (world.live() != null) {
			live.assertTurnsStarted(count);
		} else {
			assertTurnsStarted(count);
		}
	}

	private void assertTurnsStarted(int count) {
		TGTimeline timeline = heard().timeline();
		List<List<Note>> turns = turns();
		int[] opening = IntStream.range(0, timeline.turns()).filter(timeline::opens).toArray();
		assertEquals(count, opening.length);
		for (int turn : opening) {
			long start = timeline.turnStart(turn);
			assertTrue(startsAt(turns.get(turn), start), "turn " + (turn + 1) + " started at " + start);
		}
	}

	// the turn's first note or a Count In click is at the sample
	private boolean startsAt(List<Note> turn, long start) {
		return turn.stream().limit(1).anyMatch(note -> note.start == start) || Arrays.stream(heard().countIn()).anyMatch(click -> click == start);
	}

	@Then("interleaved practice has ended")
	public void hasEnded() {
		assertTrue(practice.session().isEnded());
	}
}
