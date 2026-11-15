package app.tuxguitar.audio;

import static app.tuxguitar.audio.TestLiveTimeline.at;
import static app.tuxguitar.audio.TestLiveTimeline.read;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.player.base.MidiPracticeTurn;

/**
 * What an interleaved practice session plays live, followed a stretch at a time from what the transport is told (in the
 * sequence's quarter notes, where the turns lie one after the other): a song of 8 measures of 4/4 at 120 bpm (a beat
 * lasts 22050 samples), turn A measure 3 from the sequence's tick 960, turn B measure 6 after it, each after a Count In
 * of 4 clicks (176400 samples a turn; B from the sequence's quarter note 8).
 */
public class TestLiveSessionTimeline {

	private final SongActionFixture fixture = SongActionFixture.onDocument();
	// the turns the sequence holds
	private List<MidiPracticeTurn> laid = new ArrayList<MidiPracticeTurn>();
	private final TGLiveTimeline followed = new TGLiveTimeline(this.fixture.context, 2, true, () -> this.laid);
	private final MidiPracticeTurn turnA;
	private final MidiPracticeTurn turnB;

	public TestLiveSessionTimeline() {
		this.fixture.measureCount(8);
		this.followed.setSampleRate(44100);
		this.turnA = new MidiPracticeTurn(this.fixture.song.getMeasureHeader(2), this.fixture.song.getMeasureHeader(2), 960, 4);
		this.turnB = new MidiPracticeTurn(this.fixture.song.getMeasureHeader(5), this.fixture.song.getMeasureHeader(5), this.turnA.getEnd(), 4);
	}

	// the transport's quarter note after so many samples
	private static double after(long samples) {
		return samples * 2.0 / 44100;
	}

	// whether the stretch from the quarter note on starts a timeline, how long that is, and the sample the stretch starts
	// on
	private String follow(double quarters, int frames) {
		return read(this.followed, this.followed.follow(at(quarters), frames));
	}

	// the first stretch starts a timeline of the turns laid (turn A), from the session's start
	@Test
	public void aSessionsTimelineHoldsTheTurnsLaidFromItsStart() {
		this.laid = Arrays.asList(this.turnA);

		assertEquals("true 176400 0", this.follow(0, 512));
	}

	// turn B laid while turn A plays: a timeline holding both goes on from the same sample, and goes on with no change
	@Test
	public void aTurnLaidMeanwhileJoinsTheTimelineFromTheSameSample() {
		this.laid = Arrays.asList(this.turnA);
		this.follow(0, 512);
		this.laid = Arrays.asList(this.turnA, this.turnB);
		String joined = this.follow(after(512), 512);

		assertEquals("true 352800 512 | false 352800 1024", joined + " | " + this.follow(after(1024), 512));
	}

	// where turn A ends the sequence plays on into turn B's Count In, and the timeline with it: no new one
	@Test
	public void theNextTurnsCountInIsOnTheTimeline() {
		this.laid = Arrays.asList(this.turnA, this.turnB);
		this.follow(0, 176400);

		assertEquals("false 352800 176400", this.follow(8.0, 512));
	}

	// started half way through turn A's Count In (2 beats), or 1.5 beats into turn B's measure, the timeline plays from
	// there
	@Test
	public void startedInACountInOrAMeasureTheTimelinePlaysFromThere() {
		this.laid = Arrays.asList(this.turnA, this.turnB);
		String countIn = this.follow(2.0, 512);
		this.followed.stop();

		assertEquals("true 352800 44100 | true 352800 297675", countIn + " | " + this.follow(13.5, 512));
	}

	// the session over (a move ended it: no turns laid), the song plays on where the transport is (measure 6): a timeline
	// of the song from there
	@Test
	public void theSessionOverTheSongPlaysOnFromWhereTheTransportIs() {
		this.laid = Arrays.asList(this.turnA, this.turnB);
		this.follow(0, 512);
		this.laid = new ArrayList<MidiPracticeTurn>();

		assertEquals("true 264600 0", this.follow(20.0, 512));
	}
}
