package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import app.tuxguitar.util.TGContext;

/**
 * The session under way in a context (TGPracticeUnderWay): the one whose chooser began playing it last, until it ends.
 */
public class TestPracticeUnderWay {

	private final TGContext context = new TGContext();

	private TGPracticeChooser chooser() throws TGPracticeRefusal {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, 4);
		return TGPracticeSession.start(settings, PracticeSongs.song(4)).chooser(1);
	}

	@Test
	public void noSessionIsUnderWayBeforeOneBegins() {
		assertNull(TGPracticeUnderWay.getInstance(context).getChooser());
	}

	@Test
	public void theSessionBegunLastIsUnderWayUntilItEnds() throws TGPracticeRefusal {
		TGPracticeChooser first = chooser();
		TGPracticeChooser second = chooser();
		TGPracticeUnderWay.getInstance(context).begin(first);
		TGPracticeUnderWay.getInstance(context).begin(second);
		assertSame(second, TGPracticeUnderWay.getInstance(context).getChooser());
		second.getSession().end();
		assertNull(TGPracticeUnderWay.getInstance(context).getChooser());
	}
}
