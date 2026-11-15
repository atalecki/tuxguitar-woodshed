package app.tuxguitar.audio.action;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.audio.TGAudioSession;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.TGTurn;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.practice.TGClickUpLadder;
import app.tuxguitar.practice.TGPracticeCourse;
import app.tuxguitar.practice.TGPracticePlan;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * Plays an interleaved practice session offline, as TGPlayAction plays the song: its turns, chosen from a seed, back to
 * back, each its Count In (when Count In is on, in its first measure's time signature and tempo) then its unit's
 * measures once each in score order (TGSongTime.unit), all at the Percentage of Tempo in effect when it starts (Training
 * Mode's start percentage with Training Mode on); Play looped, the Loop Range and Training Mode take no part and stay as
 * they were. The session's schedule plans each turn as it is chosen: with the interval clock a turn loops its unit for
 * its passes and a break comes before the next (TGTurn.planned), and the schedule says which turn is the last. Given
 * interleaved click-up instead (ATTRIBUTE_CLICK_UP), it plays its steps the same way, each at its own tempo scale, and
 * the Percentage of Tempo takes no part either. It stops by itself after the last turn, or at ATTRIBUTE_STOP_SAMPLE;
 * either way the session has ended.
 */
public class TGPractiseAction extends TGPlayAction {

	public static final String NAME = "action.audio.practise";

	public static final String ATTRIBUTE_SESSION = "practice session";
	// the seed of the random choice of the turns
	public static final String ATTRIBUTE_SEED = "seed";
	// interleaved click-up as started (TGClickUpLadder), played instead of a session
	public static final String ATTRIBUTE_CLICK_UP = "click-up";

	// what this play plays, made as it starts
	private static final String ATTRIBUTE_COURSE = "practice course";

	public TGPractiseAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context) {
		TGPracticeCourse course = course(getContext(), context);
		context.setAttribute(ATTRIBUTE_COURSE, course);
		super.processAction(context);
		course.end();
	}

	// the click-up's course, if one is given; else the session's
	private static TGPracticeCourse course(TGContext context, TGActionContext actionContext) {
		TGClickUpLadder clickUp = actionContext.getAttribute(ATTRIBUTE_CLICK_UP);
		if (clickUp != null) {
			return clickUp.course();
		}
		return course(context, actionContext.getAttribute(ATTRIBUTE_SESSION), actionContext.getAttribute(ATTRIBUTE_SEED));
	}

	// the session's course, with the turns the seed chooses, with Count In and the Percentage of Tempo as they are now
	private static TGPracticeCourse course(TGContext context, TGPracticeSession session, long seed) {
		MidiPlayer player = MidiPlayer.getInstance(context);
		return session.course(seed, player.getCountDown()::clicksBefore, TGTempoScale.percent(player.getMode().getStartPercent()));
	}

	protected TGTimeline timeline(TGActionContext context) {
		TGSong song = context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG);
		return timeline(getContext(), song, context.<TGPracticeCourse>getAttribute(ATTRIBUTE_COURSE));
	}

	// the song as written: the units' measures are where it plays them (see TGSongTime.unit)
	protected MidiPlayerMode mode() {
		return new MidiPlayerMode();
	}

	// what the session plays, with the turns the seed chooses, with Count In and the Percentage of Tempo as they are now
	public static TGTimeline timeline(TGContext context, TGSong song, TGPracticeSession session, long seed) {
		return timeline(context, song, course(context, session, seed));
	}

	// what the course plays, each turn planned as it starts, up to its last, with Count In as it is now
	private static TGTimeline timeline(TGContext context, TGSong song, TGPracticeCourse course) {
		int sampleRate = TGAudioSession.getInstance(context).getDevice().getSampleRate();
		ToIntFunction<TGMeasureHeader> clicks = MidiPlayer.getInstance(context).getCountDown()::clicksBefore;
		List<TGTurn> turns = new ArrayList<TGTurn>();
		long rest = 0;
		TGPracticePlan plan;
		do {
			plan = course.next();
			turns.addAll(TGTurn.planned(song, plan, turns.size(), rest, clicks, sampleRate));
			rest = plan.getBreakSeconds() * (long) sampleRate;
		} while (!plan.isLast());
		return TGTimeline.of(song, turns, sampleRate);
	}
}
