package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import app.tuxguitar.util.TGContext;

/**
 * The session under way in a context (TGPracticeUnderWay): the course that began playing last, until it ends.
 */
public class TestPracticeUnderWay {

	private final TGContext context = new TGContext();

	private TGPracticeCourse course() throws TGPracticeRefusal {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, 4);
		return TGPracticeSession.start(settings, PracticeSongs.song(4)).course(1, header -> 0, TGTempoScale.percent(100));
	}

	@Test
	public void noSessionIsUnderWayBeforeOneBegins() {
		assertNull(TGPracticeUnderWay.getInstance(context).getCourse());
	}

	@Test
	public void theSessionBegunLastIsUnderWayUntilItEnds() throws TGPracticeRefusal {
		TGPracticeCourse first = course();
		TGPracticeCourse second = course();
		TGPracticeUnderWay.getInstance(context).begin(first);
		TGPracticeUnderWay.getInstance(context).begin(second);
		assertSame(second, TGPracticeUnderWay.getInstance(context).getCourse());
		second.end();
		assertNull(TGPracticeUnderWay.getInstance(context).getCourse());
	}
}
