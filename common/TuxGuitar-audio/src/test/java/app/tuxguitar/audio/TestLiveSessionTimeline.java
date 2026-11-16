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
import app.tuxguitar.practice.TGPracticeRefusal;

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

	// turn B laid while turn A plays: a timeline holding both goes on from the same sample (though the transport is a
	// little off, 110 samples, still on the timeline), and goes on with no change
	@Test
	public void aTurnLaidMeanwhileJoinsTheTimelineFromTheSameSample() {
		this.laid = Arrays.asList(this.turnA);
		this.follow(0, 512);
		this.laid = Arrays.asList(this.turnA, this.turnB);
		String joined = this.follow(after(512 + 110), 512);

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

	// a turn of two measures (3 and 4) started 1.5 beats into its first, or into its second: the timeline plays from there
	@Test
	public void startedInEitherMeasureOfATurnTheTimelinePlaysFromThere() {
		this.laid = Arrays.asList(new MidiPracticeTurn(this.fixture.song.getMeasureHeader(2), this.fixture.song.getMeasureHeader(3), 960, 4));
		String first = this.follow(5.5, 512);
		this.followed.stop();

		assertEquals("true 264600 121275 | true 264600 209475", first + " | " + this.follow(9.5, 512));
	}

	// a turn of measure 3's beat 2, then its joint (beat 3), after a Count In of 4 clicks: 6 beats (132300 samples);
	// started half way through its Count In, or half way through its joint, the timeline plays from there
	@Test
	public void aBeatsTurnPlaysTheBeatThenItsJointFromWhereTheTransportIs() throws TGPracticeRefusal {
		this.laid = Arrays.asList(new MidiPracticeTurn(TestSongTime.beat(this.fixture.song, 3, 2), 960, 4));
		String countIn = this.follow(2.0, 512);
		this.followed.stop();

		assertEquals("true 132300 44100 | true 132300 121275", countIn + " | " + this.follow(5.5, 512));
	}

	// the next turn's measure comes earlier in the song (measure 6, then 3): started 1.5 beats into the first turn's
	// measure, the timeline plays from there, nothing of the next turn before it
	@Test
	public void theNextTurnsMeasuresDoNotCountBeforeItStarts() {
		MidiPracticeTurn six = new MidiPracticeTurn(this.fixture.song.getMeasureHeader(5), this.fixture.song.getMeasureHeader(5), 960, 4);
		this.laid = Arrays.asList(six, new MidiPracticeTurn(this.fixture.song.getMeasureHeader(2), this.fixture.song.getMeasureHeader(2), six.getEnd(), 4));

		assertEquals("true 352800 121275", this.follow(5.5, 512));
	}

	// turn B after a break of a second (1920 ticks at 120 bpm, 44100 samples), from the sequence's quarter note 8 to 10:
	// the timeline holds it before B's Count In; started half way through it, it plays from there
	@Test
	public void aBreakBeforeATurnIsOnTheTimeline() {
		this.laid = Arrays.asList(this.turnA, afterBreak(this.turnB, 1920, 2));
		String start = this.follow(0, 512);
		this.followed.stop();

		assertEquals("true 396900 0 | true 396900 198450", start + " | " + this.follow(9.0, 512));
	}

	// the same pass from its tick, after a break of so many ticks, of the session's turn
	private static MidiPracticeTurn afterBreak(MidiPracticeTurn pass, long rest, int turn) {
		return new MidiPracticeTurn(pass.getFirst(), pass.getLast(), pass.getStart(), rest, pass.getClicks(), turn, pass.getScale());
	}

	// turn 1 plays A twice (its second pass without a Count In), then turn 2 B after a break: a pass of the same turn
	// does not open a practice turn and has no Count In; the first after the break opens one, 44100 samples after it
	@Test
	public void aTurnsPassesAfterTheFirstContinueIt() {
		MidiPracticeTurn again = new MidiPracticeTurn(this.turnA.getFirst(), this.turnA.getLast(), this.turnA.getEnd(), 0, 0, 1, this.turnA.getScale());
		this.laid = Arrays.asList(this.turnA, again, new MidiPracticeTurn(this.turnB.getFirst(), this.turnB.getLast(), again.getEnd(), 1920, 4, 2, this.turnB.getScale()));
		this.follow(0, 512);
		TGTimeline timeline = this.followed.getTimeline();

		assertEquals("true false true 0 44100", timeline.opens(0) + " " + timeline.opens(1) + " " + timeline.opens(2) + " "
			+ (timeline.start(timeline.firstOf(1)) - timeline.turnStart(1)) + " " + (timeline.turnStart(2) - timeline.breakStart(2)));
	}

	// a session laid while the song plays (from measure 3) starts the session's timeline, where the transport is in it
	// (512 samples into turn B's Count In), though the transport goes on as the song's did
	@Test
	public void aSessionLaidWhileTheSongPlaysStartsItsOwnTimeline() {
		this.follow(8.0, 512);
		this.laid = Arrays.asList(this.turnA, this.turnB);

		assertEquals("true 352800 176912", this.follow(8.0 + after(512), 512));
	}

	// turns other than the timeline's (the session over, or another one) start a new timeline, even where the transport
	// goes on as the timeline did: the song's, from there (measure 1, 22 ticks in)
	@Test
	public void otherTurnsStartANewTimelineWhereTheTransportGoesOn() {
		this.laid = Arrays.asList(this.turnA, this.turnB);
		this.follow(0, 512);
		this.laid = new ArrayList<MidiPracticeTurn>();

		assertEquals("true 705095 0", this.follow(after(512), 512));
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
