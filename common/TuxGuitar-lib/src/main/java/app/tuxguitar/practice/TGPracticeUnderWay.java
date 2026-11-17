package app.tuxguitar.practice;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The practice session under way in a context: the course that began playing last, until it ends (TGPracticeCourse.end:
 * after its last turn, or as Stop, moving the position or an edit ends it). An edit made meanwhile asks it (see
 * editor-utils' TGPracticeEdits): the course checks a flag, mute, solo and markers keep it, and any other edit ends it.
 */
public final class TGPracticeUnderWay {

	// set on the thread that starts playing, read on the one that edits
	private volatile TGPracticeCourse course;

	private TGPracticeUnderWay() {
	}

	public static TGPracticeUnderWay getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGPracticeUnderWay.class.getName(), unused -> new TGPracticeUnderWay());
	}

	// the course is under way from now on
	public void begin(TGPracticeCourse course) {
		this.course = course;
	}

	// the course under way; null when none is
	public TGPracticeCourse getCourse() {
		TGPracticeCourse playing = this.course;
		return (playing != null && !playing.isEnded() ? playing : null);
	}
}
