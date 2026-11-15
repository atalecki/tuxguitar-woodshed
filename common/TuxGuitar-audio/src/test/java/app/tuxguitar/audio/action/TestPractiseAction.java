package app.tuxguitar.audio.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.audio.TGAudioDevice;
import app.tuxguitar.audio.TGAudioSession;
import app.tuxguitar.audio.TGRender;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnitKind;

/**
 * Practising offline: the session's turns, chosen from the seed, back to back, each after its Count In, at the
 * Percentage of Tempo the session starts at; stopped at a sample, up to there. Either way the session ends, and the
 * Play Mode is as it was. The song has 8 measures of 4/4 at 120 bpm (88200 samples each); the session plays measures 1
 * to 4 for 3 turns, seed 7. Read as "<measure of each turn> | <Count In clicks of each turn> | <where each turn starts>
 * | <length> | <ended>".
 */
public class TestPractiseAction {

	private static final long SEED = 7;

	private final SongActionFixture fixture = new SongActionFixture();
	private final MidiPlayer player = MidiPlayer.getInstance(fixture.context);
	private final TGPracticeSettings settings = new TGPracticeSettings();
	private TGPracticeSession session;

	public TestPractiseAction() throws TGPracticeRefusal {
		fixture.measureCount(8);
		TGAudioSession.getInstance(fixture.context).setDevice(new TGAudioDevice() {
			public int getSampleRate() {
				return 44100;
			}

			public float[] capture(int inputChannel, long from, int count) {
				return new float[count];
			}
		});
		settings.setUnit(TGPracticeUnitKind.MEASURE);
		settings.setPool(1, 4);
		settings.setTurns(3);
		session = TGPracticeSession.start(settings, fixture.song);
	}

	// the measures the seed chooses, one per turn
	private String chosen() {
		return chosen(3);
	}

	private String chosen(int turns) {
		TGPracticeChooser chooser = session.chooser(SEED);
		List<String> measures = new ArrayList<String>();
		for (int turn = 0; turn < turns; turn++) {
			measures.add("m" + chooser.next().getFirst());
		}
		return String.join(" ", measures);
	}

	private String practise(Long stopSample) {
		return practise(stopSample, TestPractiseAction::read);
	}

	private String practise(Long stopSample, java.util.function.Function<TGTimeline, String> read) {
		fixture.actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_SESSION, session);
		fixture.actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_SEED, SEED);
		fixture.actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_STOP_SAMPLE, stopSample);
		fixture.execute(new TGPractiseAction(fixture.context));
		TGRender render = fixture.actionContext.getAttribute(TGPractiseAction.ATTRIBUTE_RENDER);
		return read.apply(render.getTimeline()) + " | " + session.isEnded() + " " + fixture.outcome();
	}

	private static String read(TGTimeline timeline) {
		List<String> measures = new ArrayList<String>();
		List<String> clicks = new ArrayList<String>();
		List<String> starts = new ArrayList<String>();
		long[] positions = timeline.getCountIn().positions();
		for (int turn = 0; turn < timeline.turns(); turn++) {
			measures.add(firstMeasure(timeline, turn));
			clicks.add(Long.toString(clicks(positions, timeline.turnStart(turn), timeline.turnEnd(turn))));
			starts.add(Long.toString(timeline.turnStart(turn)));
		}
		return String.join(" ", measures) + " | " + String.join(" ", clicks) + " | " + String.join(" ", starts) + " | " + timeline.length();
	}

	@Test
	public void eachTurnPlaysItsChosenUnitAfterItsCountInAndTheSessionEnds() {
		player.getCountDown().setEnabled(true);

		assertEquals(chosen() + " | 4 4 4 | 0 176400 352800 | 529200 | true success", practise(null));
	}

	@Test
	public void withCountInOffTheTurnsFollowWithoutAGap() {
		assertEquals(chosen() + " | 0 0 0 | 0 88200 176400 | 264600 | true success", practise(null));
	}

	// stopped in the second turn's Count In: two turns started, the second up to there
	@Test
	public void stoppedTheSessionEndsWhereItStopped() {
		player.getCountDown().setEnabled(true);

		assertEquals(chosen().substring(0, 2) + " - | 4 2 | 0 176400 | 200000 | true success", practise(200000L));
	}

	// Training Mode from 50% over measures 1 to 4, already raised to 70%: the session plays at 50%, without its loop,
	// and the Play Mode is as it was
	@Test
	public void playLoopedAndTrainingModeTakeNoPart() {
		MidiPlayerMode mode = player.getMode();
		mode.setType(MidiPlayerMode.TYPE_CUSTOM);
		mode.setCustomPercentFrom(50);
		mode.setCustomPercentTo(100);
		mode.setCustomPercentIncrement(10);
		mode.setLoop(true);
		mode.setLoopSHeader(1);
		mode.setLoopEHeader(4);
		mode.setCurrentPercent(70);

		assertEquals(chosen() + " | 0 0 0 | 0 176400 352800 | 529200 | true success | 70 true", practise(null) + " | " + mode.getCurrentPercent() + " " + mode.isLoop());
	}

	// with the interval clock (turns of 1 minute, breaks of 10 seconds, sessions of 2 minutes), each turn, read as
	// "<measure> x<passes> <Count In clicks> @<start> after <break>"
	private String practiseTimed(boolean countInEveryPass) throws TGPracticeRefusal {
		settings.setIntervalClock(true);
		settings.setTurnMinutes(1);
		settings.setBreakSeconds(10);
		settings.setSessionMinutes(2);
		settings.setCountInEveryPass(countInEveryPass);
		session = TGPracticeSession.start(settings, fixture.song);
		return practise(null, TestPractiseAction::readTurns);
	}

	// the turn's first measure; "-" when it has none
	private static String firstMeasure(TGTimeline timeline, int turn) {
		int first = timeline.firstOf(turn);
		return (first < timeline.firstOf(turn + 1) ? "m" + timeline.get(first).getHeader().getNumber() : "-");
	}

	// the timeline's turns from each that opens a practice turn to the next (or the end)
	private static String readTurns(TGTimeline timeline) {
		int[] opening = IntStream.concat(IntStream.range(0, timeline.turns()).filter(timeline::opens), IntStream.of(timeline.turns())).toArray();
		List<String> turns = new ArrayList<String>();
		for (int turn = 0; turn + 1 < opening.length; turn++) {
			turns.add(readTurn(timeline, opening[turn], opening[turn + 1]));
		}
		return String.join(", ", turns) + " | " + timeline.length();
	}

	// a practice turn: the timeline's turns from the first to before the end, its passes
	private static String readTurn(TGTimeline timeline, int first, int end) {
		long from = timeline.turnStart(first);
		long clicks = clicks(timeline.getCountIn().positions(), from, timeline.turnEnd(end - 1));
		return "m" + timeline.get(timeline.firstOf(first)).getHeader().getNumber() + " x" + (end - first) + " " + clicks + " @" + from + " after " + (from - timeline.breakStart(first))
			+ " " + jumps(timeline, first, end) + " jumps";
	}

	// the timeline's turns from the first to before the end whose first measure is a jump in song time, in the pass the
	// turn's number says
	private static long jumps(TGTimeline timeline, int first, int end) {
		return IntStream.range(first, end).filter(turn -> timeline.get(timeline.firstOf(turn)).isJump() && timeline.get(timeline.firstOf(turn)).getPass() == turn).count();
	}

	// the clicks from one sample to before another
	private static long clicks(long[] positions, long from, long to) {
		return java.util.Arrays.stream(positions).filter(position -> position >= from && position < to).count();
	}

	// with Count In on, two turns of the measures the seed chooses, each its passes and Count In clicks as read, every
	// pass but the session's first a jump in song time; turn 2
	// starts after a break of 10 seconds (441000 samples) and is the last: 3087000 + 2646000 is past the session's 2
	// minutes
	private String twoTimedTurns(String eachTurn, int passes) {
		String[] chosen = chosen(2).split(" ");
		return chosen[0] + eachTurn + " @0 after 0 " + (passes - 1) + " jumps, " + chosen[1] + eachTurn + " @3087000 after 441000 " + passes + " jumps | 5733000 | true success";
	}

	// each turn loops its measure for 29 passes after one Count In (2 + 29 x 2 seconds); with a Count In before every
	// pass, for 15 passes of 4 seconds, each after its 4 clicks (another fixture: a session plays once)
	@Test
	public void withTheIntervalClockEachTurnLoopsItsUnitAndABreakComesBetweenTurns() throws TGPracticeRefusal {
		TestPractiseAction everyPass = new TestPractiseAction();
		player.getCountDown().setEnabled(true);
		everyPass.player.getCountDown().setEnabled(true);

		assertEquals(List.of(twoTimedTurns(" x29 4", 29), twoTimedTurns(" x15 60", 15)), List.of(practiseTimed(false), everyPass.practiseTimed(true)));
	}
}
