package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeTurn;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * An interleaved practice session as the player lays it out in its sequence (MidiPractice): from where it starts, each
 * turn its Count In then its unit's measures, the next one right after it; a turn is added to the sequence once the
 * turn before has started (the first one when the session starts), up to the session's turns.
 *
 * The song has 8 measures of 4/4 (3840 ticks each, the first from tick 960); lines of 2 measures. The turns are those
 * the chooser chooses from the same seed; the Count In has a click per beat of the turn's first measure (4).
 */
public class TestMidiPractice {

	private static final long SEED = 7;
	private static final long MEASURE = 3840;

	private final TGSong song = new TGSongManager().newSong();
	private final List<String> added = new ArrayList<String>();
	private final List<MidiPracticeTurn> passes = new ArrayList<MidiPracticeTurn>();
	private TGPracticeSession session;

	public TestMidiPractice() {
		while (song.countMeasureHeaders() < 8) {
			new TGSongManager().addNewMeasureBeforeEnd(song);
		}
	}

	// a pool of units of the kind, from one to the other
	private static TGPracticeSettings pool(TGPracticeUnitKind kind, int from, int to) {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setUnit(kind);
		settings.setPool(from, to);
		return settings;
	}

	// lines 1 to 4 of 2 measures
	private static TGPracticeSettings lines() {
		TGPracticeSettings settings = pool(TGPracticeUnitKind.LINE, 1, 4);
		settings.setMeasuresPerLine(2);
		return settings;
	}

	// lines 1 to 4 for so many turns, started from tick 960, each turn added written "first-last@start setup"
	private MidiPractice practice(int turns) throws TGPracticeRefusal {
		TGPracticeSettings settings = lines();
		settings.setTurns(turns);
		return this.practice(settings, 100);
	}

	// the session as set, at the percent, started from tick 960; each turn added is kept, and written as practice() says
	private MidiPractice practice(TGPracticeSettings settings, int percent) throws TGPracticeRefusal {
		session = TGPracticeSession.start(settings, song);
		ToIntFunction<TGMeasureHeader> clicks = header -> header.getTimeSignature().getNumerator();
		MidiPractice practice = new MidiPractice(session.course(SEED, clicks, TGTempoScale.percent(percent)), song);
		practice.start(960, (turn, setup) -> {
			passes.add(turn);
			added.add(turn.getFirst().getNumber() + "-" + turn.getLast().getNumber() + "@" + turn.getStart() + (setup ? " setup" : ""));
		});
		return practice;
	}

	// lines 1 to 4, timed: turns of 1 minute, breaks of 10 seconds, sessions of so many minutes
	private static TGPracticeSettings timedLines(int sessionMinutes) {
		return timed(lines(), sessionMinutes);
	}

	// the settings, timed: turns of 1 minute, breaks of 10 seconds, sessions of so many minutes
	private static TGPracticeSettings timed(TGPracticeSettings settings, int sessionMinutes) {
		settings.setIntervalClock(true);
		settings.setTurnMinutes(1);
		settings.setBreakSeconds(10);
		settings.setSessionMinutes(sessionMinutes);
		return settings;
	}

	// lines 1 to 4 (or as many as given), timed, sessions of 2 minutes; a Count In before every pass or the first only.
	// At 120 bpm a line lasts 4 seconds (7680 ticks), a Count In 2 (3840)
	private MidiPractice timed(int lines, boolean everyPass, int percent) throws TGPracticeRefusal {
		TGPracticeSettings settings = timedLines(2);
		settings.setPool(1, lines);
		settings.setCountInEveryPass(everyPass);
		return this.practice(settings, percent);
	}

	private MidiPractice timed(boolean everyPass, int percent) throws TGPracticeRefusal {
		return this.timed(4, everyPass, percent);
	}

	// the passes laid, by index: "turn:start+rest/clicks", comma separated
	private String passes(int... indexes) {
		return IntStream.of(indexes).mapToObj(passes::get).map(pass -> pass.getTurn() + ":" + pass.getStart() + "+" + pass.getRest() + "/" + pass.getClicks())
			.collect(Collectors.joining(", "));
	}

	// Play after a pause at the tick: the passes laid from then on
	private void playAfterPause(MidiPractice practice, long paused) {
		passes.clear();
		practice.play(paused, 960, (turn, setup) -> passes.add(turn));
	}

	// the first measures of the turns the seed chooses for the session, in order
	static List<Integer> chosen(TGPracticeSession session, long seed, int turns) {
		return chosen(session, seed, turns, TGPracticeUnit::getFirst);
	}

	// what the function says of each unit the seed chooses for the session, in order
	private static <T> List<T> chosen(TGPracticeSession session, long seed, int turns, Function<TGPracticeUnit, T> of) {
		TGPracticeChooser chooser = session.chooser(seed);
		return IntStream.range(0, turns).mapToObj(turn -> of.apply(chooser.next())).collect(Collectors.toList());
	}

	private List<Integer> chosen(int turns) {
		return chosen(session, SEED, turns);
	}

	private static String turn(int first, long start) {
		return first + "-" + (first + 1) + "@" + start;
	}

	@Test
	public void eachTurnFollowsTheOneBeforeOnceItHasStarted() throws TGPracticeRefusal {
		MidiPractice practice = practice(3);
		String atTheStart = String.join(", ", added);
		long second = 960 + MEASURE + 2 * MEASURE;
		practice.follow(second - 1);
		practice.follow(second);
		List<Integer> first = chosen(3);
		assertEquals(turn(first.get(0), 960) + " setup | " + turn(first.get(0), 960) + " setup, " + turn(first.get(1), second) + ", "
			+ turn(first.get(2), second + 3 * MEASURE), atTheStart + " | " + String.join(", ", added));
	}

	@Test
	public void noTurnIsAddedAfterTheLast() throws TGPracticeRefusal {
		MidiPractice practice = practice(2);
		practice.follow(960);
		practice.follow(960 + 6 * MEASURE);
		practice.follow(960 + 100 * MEASURE);
		assertEquals(2, added.size(), added.toString());
	}

	// during a turn's Count In the song plays its first measure's first tick; then its measures' ticks. Before the session
	// starts (the sequencer before its first block) it is in the first turn
	@Test
	public void theSongTickAndTheTurnOfATickOfTheSequence() throws TGPracticeRefusal {
		MidiPractice practice = practice(3);
		practice.follow(960 + 3 * MEASURE);
		List<Integer> first = chosen(2);
		long secondStart = 960 + 3 * MEASURE;
		TGMeasureHeader secondFirst = song.getMeasureHeader(first.get(1) - 1);
		long firstStart = song.getMeasureHeader(first.get(0) - 1).getStart();
		assertEquals(secondFirst.getStart() + " " + (secondFirst.getStart() + 10) + " " + firstStart + " " + firstStart + " 1 1 2 2",
			practice.songTick(secondStart + MEASURE - 1) + " " + practice.songTick(secondStart + MEASURE + 10) + " " + practice.songTick(960) + " "
				+ practice.songTick(0) + " " + practice.turnAt(0) + " " + practice.turnAt(secondStart - 1) + " " + practice.turnAt(secondStart) + " "
				+ practice.turnAt(secondStart + 3 * MEASURE));
	}

	// each turn starts in turn: the next one is chosen and added as it does
	private void startEachTurn(MidiPractice practice, int turns) {
		for (int turn = 1; turn <= turns; turn++) {
			String last = added.get(added.size() - 1);
			practice.follow(Long.parseLong(last.replaceAll(".*@(\\d+).*", "$1")));
		}
	}

	// the first measures of the turns added, from..to (from 1), sorted
	private List<Integer> firsts(int from, int to) {
		return added.subList(from - 1, to).stream().map(turn -> Integer.valueOf(turn.replaceAll("-.*", ""))).sorted().collect(Collectors.toList());
	}

	// the first turn of cycle 2 is chosen once turn 4, the last of cycle 1, starts: line 2 (measures 3 and 4) flagged 3x
	// after that, before turn 5 starts, counts from cycle 2
	@Test
	public void aFlagDuringACyclesLastTurnCountsFromTheNextCycle() throws TGPracticeRefusal {
		MidiPractice practice = practice(10);
		startEachTurn(practice, 4);
		practice.getCourse().checkFlag(3, 3);
		song.getMeasureHeader(2).setDifficulty(3);
		startEachTurn(practice, 6);
		assertEquals("[1, 3, 3, 3, 5, 7] 10", firsts(5, 10) + " " + added.size());
	}

	// with the interval clock a turn lays all its passes, its Count In before the first only: 2 + 15 x 4 = 62 seconds;
	// once it has started, the next turn follows after a break of 10 seconds (19200 ticks at 120 bpm) with its Count In,
	// and it is the last: the session's 2 minutes are up at its end
	@Test
	public void aTimedTurnLaysItsPassesAndTheNextTurnFollowsAfterItsBreak() throws TGPracticeRefusal {
		MidiPractice practice = timed(false, 100);
		String first = passes(0, 1, 14) + " " + passes.size() + " " + passes.get(14).getEnd();
		practice.follow(959);
		int before = passes.size();
		practice.follow(960);
		int started = passes.size();
		practice.follow(120000);
		practice.follow(400000);
		assertEquals("1:960+0/4, 1:12480+0/0, 1:112320+0/0 15 120000 | 15 30 | 2:120000+19200/4, 2:150720+0/0 30 258240",
			first + " | " + before + " " + started + " | " + passes(15, 16) + " " + passes.size() + " " + passes.get(29).getEnd());
	}

	// a Count In before every pass: 10 passes of 6 seconds
	@Test
	public void aTimedTurnWithACountInBeforeEveryPass() throws TGPracticeRefusal {
		timed(true, 100);
		assertEquals("1:960+0/4, 1:12480+0/4, 1:104640+0/4 10", passes(0, 1, 9) + " " + passes.size());
	}

	// at 50% a second lasts 960 ticks: the break 9600; 7 passes of 8 seconds after a Count In of 4
	@Test
	public void theBreakLastsItsSecondsAtThePercentage() throws TGPracticeRefusal {
		MidiPractice practice = timed(false, 50);
		practice.follow(960);
		assertEquals("2:58560+9600/4 14", passes(7) + " " + passes.size());
	}

	// the break is at the tempo of the next turn's first measure: line 2 at 60 bpm, line 1 at 120; no line comes twice in
	// a row, so turn 2 plays the other line
	@Test
	public void theBreakLastsItsSecondsAtTheNextTurnsTempo() throws TGPracticeRefusal {
		song.getMeasureHeader(2).getTempo().setQuarterValue(60);
		song.getMeasureHeader(3).getTempo().setQuarterValue(60);
		MidiPractice practice = timed(2, false, 100);
		practice.follow(960);
		MidiPracticeTurn second = passes.stream().filter(pass -> pass.getTurn() == 2).findFirst().get();
		assertEquals((second.getFirst().getNumber() == 3 ? 9600 : 19200), second.getRest());
	}

	// what the app shows: the turn and the seconds left to its end (rounded up), all its passes; in the break before the
	// next turn, that turn and the seconds left in the break
	@Test
	public void aTimedTurnAndTheBreakBeforeTheNextAreReportedWithTheTimeLeft() throws TGPracticeRefusal {
		MidiPractice practice = timed(false, 100);
		practice.follow(960);
		int one = passes.get(0).getFirst().getNumber();
		int two = passes.get(15).getFirst().getNumber();
		assertEquals("turn 1 " + one + " 61, turn 1 " + one + " 1, break 2 " + two + " 10, break 2 " + two + " 1, turn 2 " + two + " 62, turn 2 " + two + " 1",
			IntStream.of(960 + 1920, 119999, 120000, 139200 - 960, 139200, 258239).mapToObj(tick -> shown(practice.turn(tick))).collect(Collectors.joining(", ")));
	}

	// "turn 1 5 61": a turn of one unit, by its first measure ("5-8 to 9-12" for a turn of several)
	private static String shown(TGPracticeTurn turn) {
		String units = (turn.getTo() == turn.getFrom() ? "" + turn.getFrom().getFirst() : turn.getFrom() + " to " + turn.getTo());
		return (turn.isBreak() ? "break " : "turn ") + turn.getTurn() + " " + units + " " + turn.getSecondsLeft() + of(turn);
	}

	// " of" the session's turns, unless it is timed
	private static String of(TGPracticeTurn turn) {
		return (turn.isTimed() ? "" : " of " + turn.getTurns());
	}

	// without the clock: the turn of the session's turns, no time
	@Test
	public void aCountedTurnIsReportedOfTheSessionsTurns() throws TGPracticeRefusal {
		MidiPractice practice = practice(3);
		assertEquals("turn 1 " + chosen(1).get(0) + " 0 of 3", shown(practice.turn(1000)));
	}

	// all through the break the song plays the next turn's first measure's first tick
	@Test
	public void throughTheBreakTheSongTickIsTheNextTurnsFirst() throws TGPracticeRefusal {
		MidiPractice practice = timed(false, 100);
		practice.follow(960);
		long first = passes.get(15).getFirst().getStart();
		assertEquals(first + " " + first + " " + (first + 10), practice.songTick(120005) + " " + practice.songTick(139200 + 3839) + " " + practice.songTick(139200 + 3850));
	}

	// paused in pass 3 of turn 1: from the tick, pass 3 again after a Count In, then the turn's passes left, then turn 2
	// after its break
	@Test
	public void pausedInAPassPlayPlaysItAgainFromACountInThenThePassesLeft() throws TGPracticeRefusal {
		MidiPractice practice = timed(false, 100);
		practice.follow(960);
		playAfterPause(practice, 20160 + 100);
		practice.follow(960);
		assertEquals("1:960+0/4, 1:12480+0/0, 2:104640+19200/4 28 1", passes(0, 1, 13) + " " + passes.size() + " " + practice.turnAt(104000));
	}

	// paused in the break before turn 2: Play plays turn 2's Count In at once, and the session ends after it
	@Test
	public void pausedInABreakPlayPlaysTheNextTurnsCountInAtOnce() throws TGPracticeRefusal {
		MidiPractice practice = timed(false, 100);
		practice.follow(960);
		playAfterPause(practice, 120000 + 100);
		practice.follow(960);
		practice.follow(1000000);
		assertEquals("2:960+0/4, 2:12480+0/0 15 2", passes(0, 1) + " " + passes.size() + " " + practice.turnAt(960));
	}

	// the turn (from 1) starts: its first pass, from its break
	private void startTurn(MidiPractice practice, int turn) {
		practice.follow(passes.stream().filter(pass -> pass.getTurn() == turn).findFirst().get().getStart());
	}

	// timed, 10 minutes of turns of 1 minute and breaks of 10 seconds: 9 turns. Turn 5 opens cycle 2 and starts with its
	// break: line 2 (measures 3 and 4) flagged 3x during that break counts from cycle 3, turns 5 to 8 play each line once
	@Test
	public void aFlagDuringABreakCountsFromTheCycleAfterTheOneItsTurnOpens() throws TGPracticeRefusal {
		MidiPractice practice = this.practice(timedLines(10), 100);
		IntStream.rangeClosed(1, 5).forEach(turn -> startTurn(practice, turn));
		practice.getCourse().checkFlag(3, 3);
		song.getMeasureHeader(2).setDifficulty(3);
		IntStream.rangeClosed(6, 9).forEach(turn -> startTurn(practice, turn));
		List<Integer> firsts = IntStream.rangeClosed(5, 8).mapToObj(turn -> passes.stream().filter(pass -> pass.getTurn() == turn).findFirst().get().getFirst().getNumber())
			.sorted().collect(Collectors.toList());
		assertEquals("[1, 3, 5, 7]", firsts.toString());
	}

	// the beats of measure 2 (ticks 4800 to 8640)
	private static TGPracticeSettings beats() {
		return pool(TGPracticeUnitKind.BEAT, 2, 2);
	}

	// a beat's turn, after its Count In (4 clicks, to tick 4800), plays its beat then the joint (to 6720), where the next
	// turn's Count In starts; through its Count In the song plays the beat's first tick
	@Test
	public void aBeatsTurnPlaysItsBeatAndItsJointAfterItsCountIn() throws TGPracticeRefusal {
		TGPracticeSettings settings = beats();
		settings.setTurns(2);
		MidiPractice practice = practice(settings, 100);
		practice.follow(960);
		List<Long> beats = chosen(session, SEED, 2, unit -> MEASURE + 960 + (unit.getNumber() - 1) * 960L);
		long beat = beats.get(0);
		assertEquals(beat + " " + (beat + 10) + " " + (beat + 1919) + " | 1:960+0/4, 2:6720+0/4 12480 " + (beats.get(1) + 5),
			practice.songTick(960) + " " + practice.songTick(4810) + " " + practice.songTick(6719) + " | " + passes(0, 1) + " " + passes.get(1).getEnd() + " "
				+ practice.songTick(10565));
	}

	// timed (turns of 1 minute): a pass is the beat and its joint, 1 second, so turn 1 is its Count In (2 seconds) then 58
	// passes; halfway through its Count In 59 seconds are left
	@Test
	public void aTimedBeatsTurnPassesItsBeatAndItsJoint() throws TGPracticeRefusal {
		MidiPractice practice = practice(timed(beats(), 2), 100);
		assertEquals("58 turn 1 2 59 1:6720+0/0", passes.size() + " " + shown(practice.turn(960 + 1920)) + " " + passes(1));
	}
}
