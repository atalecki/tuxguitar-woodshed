package app.tuxguitar.practice;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The interleaved practice session under way in a context: the one whose chooser began playing it last, until it ends
 * (TGPracticeSession.end: after its last turn, or as Stop, moving the position or an edit ends it). An edit made
 * meanwhile asks it: the chooser checks a flag, and any other edit ends the session.
 */
public final class TGPracticeUnderWay {

	// set on the thread that starts playing, read on the one that edits
	private volatile TGPracticeChooser chooser;

	private TGPracticeUnderWay() {
	}

	public static TGPracticeUnderWay getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGPracticeUnderWay.class.getName(), unused -> new TGPracticeUnderWay());
	}

	// the chooser's session is under way from now on
	public void begin(TGPracticeChooser chooser) {
		this.chooser = chooser;
	}

	// the chooser of the session under way; null when none is
	public TGPracticeChooser getChooser() {
		TGPracticeChooser playing = this.chooser;
		return (playing != null && !playing.getSession().isEnded() ? playing : null);
	}
}
