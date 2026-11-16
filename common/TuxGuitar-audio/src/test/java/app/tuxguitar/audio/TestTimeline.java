package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.player.base.MidiTransport;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerCountDown;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * What plays on a timeline: the Count In before its first measure, and where it stops when stopped at a sample. The
 * song has 4 measures of 4/4 at 120 bpm: 88200 samples each at 44100 Hz, a beat 22050, a tick about 23. Read as
 * "<clock> | <Count In click positions> | <measure>:<from>-<to> ... | <length>".
 */
public class TestTimeline {

	private static final int QUARTER = 960;
	private static final int BAR = 4 * QUARTER;

	private final TGSongManager songManager = new TGSongManager();
	private final TGSong song = songManager.newSong();
	private final MidiPlayerCountDown countDown = MidiPlayer.getInstance(new TGContext()).getCountDown();
	private final long origin;

	public TestTimeline() {
		for (int m = 1; m < 4; m++) {
			songManager.addNewMeasureBeforeEnd(song);
		}
		origin = song.getMeasureHeader(0).getStart();
	}

	// played from measure 2 to the end
	private TGTimeline fromMeasure2() {
		List<TGPlayedMeasure> played = TGSongTime.play(song, new MidiPlayerMode(), origin + BAR, Long.MAX_VALUE, 1);
		return new TGTimeline(song, played, 44100, TGCountIn.before(countDown, played, 44100));
	}

	private static String read(TGTimeline timeline) {
		StringBuilder read = new StringBuilder(timeline.getClock() + " | " + Arrays.toString(timeline.getCountIn().positions()) + " |");
		for (int i = 0; i < timeline.count(); i++) {
			TGPlayedMeasure measure = timeline.get(i);
			read.append(" m" + measure.getHeader().getNumber() + ":" + measure.getFrom() + "-" + measure.getTo());
		}
		return read.append(" | " + timeline.length()).toString();
	}

	@Test
	public void theCountInComesBeforeTheFirstMeasureAndMovesTheClockBack() {
		countDown.setEnabled(true);
		TGTimeline timeline = fromMeasure2();

		assertEquals("0 | [0, 22050, 44100, 66150] | m2:0-3840 m3:0-3840 m4:0-3840 | 352800", read(timeline));
		assertEquals(88200, timeline.start(0));
	}

	private static String told(TGTimeline timeline, long start) {
		MidiTransport transport = new MidiTransport();
		timeline.tell(transport, start, 512);
		StringBuilder told = new StringBuilder();
		for (int i = 0; i < transport.count(); i++) {
			MidiTransport.Span span = transport.get(i);
			told.append(String.format(java.util.Locale.ROOT, "@%d %s %.0f bpm %d/%d q%.4f s%d; ", span.getFrame(), span.isPlaying(), span.getTempo(),
				span.getNumerator(), span.getDenominator(), span.getQuarters(), span.getSamples()));
		}
		return told.toString();
	}

	// a block is told the song's state from its first frame, and again from the first frame of each measure in it: the
	// musical position counts quarter notes from the song's start, the samples too (measure 2 starts at quarter 4, sample
	// 88200); in the Count In the first measure's state runs back before it
	@Test
	public void aBlockIsToldTheSongsStateFromEachMeasureInIt() {
		assertEquals("@0 true 120 bpm 4/4 q7.9909 s176200; @200 true 120 bpm 4/4 q8.0000 s176400; ", told(fromMeasure2(), 88000));
		countDown.setEnabled(true);
		assertEquals("@0 true 120 bpm 4/4 q0.0000 s0; ", told(fromMeasure2(), 0));
	}

	// a block starting where a measure starts is that measure's from its first frame; one ending where the next starts
	// holds no part of it; playing from beat 2 of measure 2 starts at quarter 5
	@Test
	public void aBlockOnAMeasuresEdgesHoldsOnlyThatMeasure() {
		assertEquals("@0 true 120 bpm 4/4 q8.0000 s176400; ", told(fromMeasure2(), 88200));
		assertEquals("@0 true 120 bpm 4/4 q7.9768 s175888; ", told(fromMeasure2(), 87688));
		List<TGPlayedMeasure> played = TGSongTime.play(song, new MidiPlayerMode(), origin + BAR + QUARTER, Long.MAX_VALUE, 1);
		assertEquals("@0 true 120 bpm 4/4 q5.0000 s110250; ", told(new TGTimeline(song, played, 44100), 0));
	}

	// stopped in the Count In, no measure plays: the song is stopped
	@Test
	public void aTimelineWithoutMeasuresTellsTheSongIsStopped() {
		countDown.setEnabled(true);
		assertEquals("@0 false 120 bpm 4/4 q0.0000 s0; ", told(fromMeasure2().until(30000), 0));
	}

	// a timeline is an offline clock: playing or stopped, it tells the processors it waits for each block
	@Test
	public void aTimelineTellsItWaitsForEachBlock() {
		MidiTransport playing = new MidiTransport();
		fromMeasure2().tell(playing, 0, 512);
		countDown.setEnabled(true);
		MidiTransport stopped = new MidiTransport();
		fromMeasure2().until(30000).tell(stopped, 0, 512);
		assertEquals("true true", playing.isOffline() + " " + stopped.isOffline());
	}

	// running on after the last measure (for the tails of an export) the timeline is longer, its measures where they
	// were, and the last measure's state is told past its end
	@Test
	public void aTimelineRunsOnAfterItsLastMeasure() {
		TGTimeline timeline = fromMeasure2().runningOn(441000);
		assertEquals("88200 | [] | m2:0-3840 m3:0-3840 m4:0-3840 | 705600", read(timeline));
		assertEquals(264600, timeline.end(2));
		assertEquals("@0 true 120 bpm 4/4 q16.0000 s352800; ", told(timeline, 264600));
	}

	// measure 2 alone, then measure 4 alone, each after its own Count In
	private TGTimeline measures2And4() {
		return TGTimeline.of(song, List.of(turn(2), turn(4)), 44100);
	}

	private TGTurn turn(int measure) {
		long start = song.getMeasureHeader(measure - 1).getStart();
		List<TGPlayedMeasure> played = TGSongTime.play(song, new MidiPlayerMode(), start, start + BAR, 1);
		return new TGTurn(TGCountIn.before(countDown, played, 44100), played);
	}

	// where each turn starts, and its first measure
	private static String turns(TGTimeline timeline) {
		StringBuilder turns = new StringBuilder();
		for (int turn = 0; turn < timeline.turns(); turn++) {
			turns.append(" " + timeline.turnStart(turn) + ":" + timeline.firstOf(turn) + "-" + timeline.turnEnd(turn));
		}
		return turns.toString();
	}

	// each turn starts where the one before ends, its Count In first: the clock is where the first turn starts
	@Test
	public void turnsFollowOneAnotherEachAfterItsOwnCountIn() {
		countDown.setEnabled(true);
		TGTimeline timeline = measures2And4();

		assertEquals("0 | [0, 22050, 44100, 66150, 176400, 198450, 220500, 242550] | m2:0-3840 m4:0-3840 | 352800 | 0:0-176400 176400:1-352800 | 264600 352800",
			read(timeline) + " |" + turns(timeline) + " | " + timeline.start(1) + " " + timeline.end(1));
	}

	// a turn ends where another turn's measures follow: not inside a turn, not at the last measure
	@Test
	public void aMeasureEndsItsTurnWhenAnotherTurnFollows() {
		TGTimeline twoTurns = measures2And4();
		TGTimeline oneTurn = fromMeasure2();

		assertEquals("true false | false false false", twoTurns.endsTurn(0) + " " + twoTurns.endsTurn(1) + " | " + oneTurn.endsTurn(0) + " " + oneTurn.endsTurn(1) + " "
			+ oneTurn.endsTurn(2));
	}

	// nothing to play (from past the song's end): no turn, the clock at 0
	@Test
	public void withoutMeasuresTheClockIsZero() {
		assertEquals("0 | [] | | 0", read(new TGTimeline(song, new java.util.ArrayList<TGPlayedMeasure>(), 44100)));
	}

	// with Count In off, the next turn's first measure starts where the turn before ends
	@Test
	public void withoutCountInTheNextTurnStartsWhereTheOneBeforeEnds() {
		assertEquals("88200 | [] | m2:0-3840 m4:0-3840 | 176400 | 0:0-88200 88200:1-176400", read(measures2And4()) + " |" + turns(measures2And4()));
	}

	// stopped in the second turn's Count In: that Count In up to there, no measure of it; stopped where the second turn
	// starts, nothing of it
	@Test
	public void stoppedInATurnsCountInItKeepsTheTurnsBefore() {
		countDown.setEnabled(true);
		List<String> stopped = new java.util.ArrayList<String>();
		for (long sample : new long[] {200000, 176400}) {
			stopped.add(read(measures2And4().until(sample)) + " |" + turns(measures2And4().until(sample)));
		}

		assertEquals(List.of("0 | [0, 22050, 44100, 66150, 176400, 198450] | m2:0-3840 | 200000 | 0:0-176400 176400:1-200000",
			"0 | [0, 22050, 44100, 66150] | m2:0-3840 | 176400 | 0:0-176400"), stopped);
	}

	// in a turn's Count In its first measure's state runs back before it, as in the first turn's (quarter 12 is where
	// measure 4 starts); a block that ends in it is told the measure before running on, then that from the turn's start
	@Test
	public void aTurnsCountInIsToldItsFirstMeasureRunningBack() {
		countDown.setEnabled(true);

		assertEquals("@0 true 120 bpm 4/4 q8.0000 s176400; | @0 true 120 bpm 4/4 q7.9819 s176000; @400 true 120 bpm 4/4 q8.0000 s176400; ",
			told(measures2And4(), 176400) + "| " + told(measures2And4(), 176000));
	}

	// played from beat 2 of measure 2: its beat 3 sounds a beat after playing starts
	@Test
	public void aTicksSampleCountsFromWherePlayingStarts() {
		List<TGPlayedMeasure> played = TGSongTime.play(song, new MidiPlayerMode(), origin + BAR + QUARTER, Long.MAX_VALUE, 1);

		assertEquals(22050, new TGTimeline(song, played, 44100).sampleAt(0, 2 * QUARTER));
	}

	@Test
	public void withCountInOffTheFirstMeasureStartsPlaying() {
		assertEquals("88200 | [] | m2:0-3840 m3:0-3840 m4:0-3840 | 264600", read(fromMeasure2()));
	}

	// stopped where a measure starts; 1000 samples into measure 3 (up to its first tick sounding there or later: tick 44,
	// at sample 1011); on the sample of its beat 2 (tick 960, not after it); in the Count In, on the third click (which
	// does not sound); after it all
	@Test
	public void stoppedAtASampleItPlaysWhatComesBeforeIt() {
		String atAMeasure = read(fromMeasure2().until(88200));
		String insideAMeasure = read(fromMeasure2().until(89200));
		String onATick = read(fromMeasure2().until(110250));
		countDown.setEnabled(true);
		String inTheCountIn = read(fromMeasure2().until(44100));
		String afterItAll = read(fromMeasure2().until(Long.MAX_VALUE));

		assertEquals(List.of("88200 | [] | m2:0-3840 | 88200", "88200 | [] | m2:0-3840 m3:0-44 | 89211", "88200 | [] | m2:0-3840 m3:0-960 | 110250",
			"0 | [0, 22050] | | 44100", read(fromMeasure2())), List.of(atAMeasure, insideAMeasure, onATick, inTheCountIn, afterItAll));
	}

	// measure 2 after its Count In; after a break of 2 beats (44100 samples), measure 4 after its Count In; then measure 4
	// again, a pass of that turn, without one
	private TGTimeline withABreak() {
		TGTurn four = turn(4);
		return TGTimeline.of(song, List.of(turn(2), TGTurn.afterBreak(44100, four.getCountIn(), four.getMeasures()), TGTurn.pass(TGCountIn.NONE, four.getMeasures())),
			44100);
	}

	// where each turn's break starts, and whether it opens a turn of interleaved practice
	private static String breaks(TGTimeline timeline) {
		return java.util.stream.IntStream.range(0, timeline.turns()).mapToObj(turn -> " " + timeline.breakStart(turn) + (timeline.opens(turn) ? " opens" : " passes"))
			.collect(java.util.stream.Collectors.joining());
	}

	// a break is silence before its turn: the turn starts after it, its Count In first, and the turn before ends where it
	// starts; a pass continues the turn before it, which it does not open
	@Test
	public void aBreakComesBeforeItsTurnAndAPassContinuesTheTurnBefore() {
		countDown.setEnabled(true);
		TGTimeline timeline = withABreak();

		assertEquals("0 | [0, 22050, 44100, 66150, 220500, 242550, 264600, 286650] | m2:0-3840 m4:0-3840 m4:0-3840 | 485100 | 0:0-176400 220500:1-396900 396900:2-485100 | "
			+ "0 opens 176400 opens 396900 passes", read(timeline) + " |" + turns(timeline) + " |" + breaks(timeline));
	}

	// in a break the next turn's first measure is told running back, as in its Count In (quarter 12 is where measure 4
	// starts, 6 quarters after the break starts); a block that ends in it is told the measure before running on first
	@Test
	public void aBreakIsToldItsTurnsFirstMeasureRunningBack() {
		countDown.setEnabled(true);

		assertEquals("@0 true 120 bpm 4/4 q6.0000 s132300; | @0 true 120 bpm 4/4 q7.9773 s175900; @500 true 120 bpm 4/4 q6.0000 s132300; ",
			told(withABreak(), 176400) + "| " + told(withABreak(), 175900));
	}

	// a break before the first turn plays before its first measure: the clock is that much earlier
	@Test
	public void aBreakBeforeTheFirstTurnMovesTheClockBack() {
		TGTurn two = turn(2);
		TGTimeline timeline = TGTimeline.of(song, List.of(TGTurn.afterBreak(1000, two.getCountIn(), two.getMeasures())), 44100);

		assertEquals("87200 | [] | m2:0-3840 | 89200 | 1000", read(timeline) + " | " + timeline.start(0));
	}

	// measures 2 and 3, then after a break measure 4 played twice (its second pass without a Count In), then measure 2:
	// four turns of five measures, where a turn's first measure is told from its break's start
	private TGTimeline fourTurns() {
		TGTurn twoAndThree = new TGTurn(TGCountIn.before(countDown, played(2, 3), 44100), played(2, 3));
		TGTurn four = turn(4);
		return TGTimeline.of(song, List.of(twoAndThree, TGTurn.afterBreak(44100, four.getCountIn(), four.getMeasures()), TGTurn.pass(TGCountIn.NONE, four.getMeasures()),
			turn(2)), 44100);
	}

	// the measures first to last as the song plays them
	private List<TGPlayedMeasure> played(int first, int last) {
		return TGSongTime.play(song, new MidiPlayerMode(), song.getMeasureHeader(first - 1).getStart(), song.getMeasureHeader(last - 1).getStart() + BAR, 1);
	}

	// which measures end their turn, and the measure each block is told from first: blocks starting a sample before, on
	// and a sample after where each measure is told from (a turn's first: its break's start), and in its middle
	@Test
	public void eachMeasureIsFoundInItsTurnAndEachBlockToldFromTheMeasureItStartsIn() {
		countDown.setEnabled(true);
		TGTimeline timeline = fourTurns();
		StringBuilder found = new StringBuilder();
		for (int index = 0; index < timeline.count(); index++) {
			found.append(timeline.endsTurn(index) ? "E" : "-");
		}
		for (int turn = 0; turn < timeline.turns(); turn++) {
			for (int index = timeline.firstOf(turn); index < timeline.firstOf(turn + 1); index++) {
				long from = (index == timeline.firstOf(turn) ? timeline.breakStart(turn) : timeline.start(index));
				for (long sample : new long[] {from - 1, from, from + 1, (timeline.start(index) + timeline.end(index)) / 2}) {
					found.append(" " + told(timeline, Math.max(0, sample)).split(" ")[5]);
				}
			}
		}

		assertEquals("-EEE- q0.0000 q0.0000 q0.0000 q6.0000 q8.0000 q8.0000 q8.0000 q10.0000 q12.0000 q6.0000 q6.0000 q14.0000 q16.0000 q12.0000 q12.0000 q14.0000"
			+ " q16.0000 q0.0000 q0.0000 q6.0000", found.toString());
	}

	// stopped in a break: the turns before it, not the break; stopped in a pass: the turns before it with their breaks,
	// and the pass up to there, still not opening a turn
	@Test
	public void stoppedInABreakOrAPassItKeepsWhatCameBefore() {
		countDown.setEnabled(true);

		assertEquals(List.of("0:0-176400 | 0 opens", "0:0-176400 220500:1-396900 396900:2-400001 | 0 opens 176400 opens 396900 passes"),
			List.of(turns(withABreak().until(200000)).trim() + " |" + breaks(withABreak().until(200000)),
				turns(withABreak().until(400000)).trim() + " |" + breaks(withABreak().until(400000))));
	}
}
