package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerCountDown;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * Where the player reports the song is after each block of 512 samples of a timeline: the song tick the output sounds
 * then, a delay behind the clock. The song has 4 measures of 4/4 at 120 bpm: measure 2 starts at tick 4800, a tick lasts
 * 22.96875 samples at 44100 Hz, a measure 88200 samples.
 */
public class TestReportedPositions {

	private final TGSongManager songManager = new TGSongManager();
	private final TGSong song = songManager.newSong();
	private final MidiPlayerCountDown countDown = MidiPlayer.getInstance(new TGContext()).getCountDown();
	private final MidiPlayerMode mode = new MidiPlayerMode();

	public TestReportedPositions() {
		for (int m = 1; m < 4; m++) {
			songManager.addNewMeasureBeforeEnd(song);
		}
	}

	private long start(int measure) {
		return song.getMeasureHeader(measure - 1).getStart();
	}

	// played from the tick, for the passes, at the sample rate
	private TGTimeline from(long tick, int passes, int sampleRate) {
		List<TGPlayedMeasure> played = TGSongTime.play(song, mode, tick, Long.MAX_VALUE, passes);
		return new TGTimeline(song, played, sampleRate, TGCountIn.before(countDown, played, sampleRate));
	}

	private TGTimeline fromMeasure2(int passes) {
		return from(start(2), passes, 44100);
	}

	// the positions reported after the blocks, from the first
	private static String reported(TGTimeline timeline, int delay, int first, int count) {
		return Arrays.stream(TGReportedPositions.of(timeline, delay), first, first + count).mapToObj(String::valueOf).collect(Collectors.joining(" "));
	}

	// without a delay and 1024 samples late: until the output sounds where playing starts the position is before it
	@Test
	public void afterEachBlockThePositionIsTheTickTheOutputSounds() {
		assertEquals("4822 4844 | 4777 4800 4822", reported(fromMeasure2(1), 0, 0, 2) + " | " + reported(fromMeasure2(1), 1024, 0, 3));
	}

	// playing from beat 2 of measure 2 (tick 5760)
	@Test
	public void playingFromInsideAMeasureStartsAtItsTick() {
		assertEquals("5782", reported(from(start(2) + 960, 1, 44100), 0, 0, 1));
	}

	// in the Count In (4 beats, 88200 samples) the position stays where playing starts, as live, where the Count In is a
	// timer before the sequencer runs: measure 2 (tick 4800), measure 4 (tick 12480, played alone), also when the
	// output sounds the Count In 1024 samples late; from the first measure on it moves
	@Test
	public void inTheCountInThePositionStaysWherePlayingStarts() {
		countDown.setEnabled(true);

		assertEquals("4800 4800 | 12480 | 4800 4816", reported(fromMeasure2(1), 1024, 0, 2) + " | " + reported(from(start(4), 1, 44100), 0, 171, 1) + " | "
			+ reported(fromMeasure2(1), 0, 171, 2));
	}

	// measure 2 (ticks 4800 to 8639), then measure 4, each after its Count In: in the second Count In (from sample
	// 176400 to 264600) the position is measure 4's first tick, then it moves on at its tempo (a tick every 22.96875
	// samples). Without a delay, what starts on a sample is reported once the clock has played it, from the sample after
	@Test
	public void inEachTurnsCountInThePositionStaysOnTheTurnsFirstTick() {
		countDown.setEnabled(true);
		TGTimeline timeline = TGTimeline.of(song, List.of(turn(2), turn(4)), 44100);
		TGReportedPositions positions = TGReportedPositions.at(timeline, 0);

		assertEquals("8639 8640 12480 12480 12480 12481", LongStream.of(176399, 176400, 176401, 264599, 264601, 264624).mapToObj(positions::tickAt)
			.map(String::valueOf).collect(Collectors.joining(" ")));
	}

	// measure 2, then a break of 2 beats (176400 to 220500) before measure 4's Count In (to 308700): from where the break
	// starts the position is measure 4's first tick, as in its Count In (the break is the start of its turn)
	@Test
	public void inABreakThePositionIsTheNextTurnsFirstTick() {
		countDown.setEnabled(true);
		TGTurn four = turn(4);
		TGTimeline timeline = TGTimeline.of(song, List.of(turn(2), four.afterBreak(44100)), 44100);
		TGReportedPositions positions = TGReportedPositions.at(timeline, 0);

		assertEquals("8640 12480 12480 12480 12481", LongStream.of(176400, 176401, 220501, 308700, 308724).mapToObj(positions::tickAt).map(String::valueOf)
			.collect(Collectors.joining(" ")));
	}

	// a delay: the output sounds the clock that many samples later
	@Test
	public void aPositionIsReadTheDelayBehindTheClock() {
		countDown.setEnabled(true);
		TGTimeline timeline = TGTimeline.of(song, List.of(turn(2), turn(4)), 44100);

		assertEquals(TGReportedPositions.at(timeline, 0).tickAt(264600), TGReportedPositions.at(timeline, 1024).tickAt(264600 + 1024));
	}

	// the measure alone, after the Count In
	private TGTurn turn(int measure) {
		List<TGPlayedMeasure> played = TGSongTime.play(song, mode, start(measure), start(measure) + song.getMeasureHeader(measure - 1).getLength(), 1);
		return new TGTurn(TGCountIn.before(countDown, played, 44100), played);
	}

	// measure 3 at 60 bpm: measure 2 is reported at its own tempo; measure 3 from its start (sample 88200, in block 172) at
	// 60 bpm, a tick every 45.9375 samples
	@Test
	public void eachMeasureIsReportedAtItsTempo() {
		song.getMeasureHeader(2).getTempo().setQuarterValue(60);

		assertEquals("4822 | 8648", reported(fromMeasure2(1), 0, 0, 1) + " | " + reported(fromMeasure2(1), 0, 172, 1));
	}

	private void loopMeasures2To3() {
		mode.setLoop(true);
		mode.setLoopSHeader(2);
		mode.setLoopEHeader(3);
	}

	// the loop jumps back to measure 2 at sample 176400, in block 344: the output sounds the jump 1024 samples later, at
	// the end of block 346
	@Test
	public void aJumpBackIsReportedWhenTheOutputSoundsIt() {
		loopMeasures2To3();

		assertEquals("12445 12468 4810", reported(fromMeasure2(2), 1024, 344, 3));
	}

	// at 32768 Hz a measure is 65536 samples: the jump back is on the first frame of block 256, so after block 255 the end
	// of measure 3 is reported (as live, where the next block plays the jump), after block 256 the jump
	@Test
	public void aJumpOnTheFirstFrameOfABlockIsReportedAfterThatBlock() {
		loopMeasures2To3();

		assertEquals("12480 4830", reported(from(start(2), 2, 32768), 0, 255, 2));
	}

	// the last block runs past the end of the timeline (3 measures: 516.8 blocks), or ends with it (104 samples more)
	@Test
	public void theBlocksCoverTheWholeTimeline() {
		assertEquals("517 517", TGReportedPositions.of(fromMeasure2(1), 0).length + " " + TGReportedPositions.of(fromMeasure2(1).runningOn(104), 0).length);
	}

	// with nothing played (stopped in the Count In) the position is 0
	@Test
	public void withNothingPlayedThePositionIsZero() {
		countDown.setEnabled(true);

		assertEquals("0", reported(fromMeasure2(1).until(30000), 0, 0, 1));
	}
}
