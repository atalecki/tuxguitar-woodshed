package app.tuxguitar.audio.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
	private final TGPracticeSession session;

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
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setUnit(TGPracticeUnitKind.MEASURE);
		settings.setPool(1, 4);
		settings.setTurns(3);
		session = TGPracticeSession.start(settings, fixture.song);
	}

	// the measures the seed chooses, one per turn
	private String chosen() {
		TGPracticeChooser chooser = session.chooser(SEED);
		List<String> measures = new ArrayList<String>();
		for (int turn = 0; turn < 3; turn++) {
			measures.add("m" + chooser.next().getFirst());
		}
		return String.join(" ", measures);
	}

	private String practise(Long stopSample) {
		fixture.actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_SESSION, session);
		fixture.actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_SEED, SEED);
		fixture.actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_STOP_SAMPLE, stopSample);
		fixture.execute(new TGPractiseAction(fixture.context));
		TGRender render = fixture.actionContext.getAttribute(TGPractiseAction.ATTRIBUTE_RENDER);
		return read(render.getTimeline()) + " | " + session.isEnded() + " " + fixture.outcome();
	}

	private static String read(TGTimeline timeline) {
		List<String> measures = new ArrayList<String>();
		List<String> clicks = new ArrayList<String>();
		List<String> starts = new ArrayList<String>();
		long[] positions = timeline.getCountIn().positions();
		for (int turn = 0; turn < timeline.turns(); turn++) {
			int first = timeline.firstOf(turn);
			measures.add(first < timeline.firstOf(turn + 1) ? "m" + timeline.get(first).getHeader().getNumber() : "-");
			long from = timeline.turnStart(turn);
			long to = timeline.turnEnd(turn);
			clicks.add(Long.toString(java.util.Arrays.stream(positions).filter(position -> position >= from && position < to).count()));
			starts.add(Long.toString(from));
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
}
