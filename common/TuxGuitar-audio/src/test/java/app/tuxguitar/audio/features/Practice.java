package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

import app.tuxguitar.action.TGActionException;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.action.TGPractiseAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.undo.impl.custom.TGUndoableDifficultyController;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * Interleaved practice in a scenario: the settings as the steps set them, the session started last (or why starting
 * was refused), and the turns it chose, one list of unit numbers per random seed; practising it plays it offline.
 * Cucumber makes one per scenario and gives it to every step class that takes it.
 */
public class Practice {

	// the random seed the scenarios practise with (README, Random seed)
	static final long SEED = 1;

	private final AudioWorld world;
	private final TGPracticeSettings settings = new TGPracticeSettings();
	private TGPracticeSession session;
	private final List<List<Integer>> runs = new ArrayList<List<Integer>>();

	public Practice(AudioWorld world) {
		this.world = world;
	}

	TGPracticeSettings settings() {
		return this.settings;
	}

	// starting interleaved practice as set: the session, or the reason it is refused, as the world's refusal (no
	// playback yet)
	void start() {
		this.session = null;
		this.world.setRefusal(null);
		try {
			this.session = TGPracticeSession.start(this.settings, this.world.song());
		} catch (TGPracticeRefusal refused) {
			this.world.setRefusal(refused.getMessage());
		}
	}

	// starts the session of so many turns as set, then practises it offline
	void practise(int turns, ToLongFunction<TGTimeline> stop) {
		this.settings.setTurns(turns);
		this.practise(stop);
	}

	// starts the session as set, then practises it offline (TGPractiseAction, random seed 1), stopped at the sample the
	// stop finds on what the session plays (Long.MAX_VALUE: not stopped)
	void practise(ToLongFunction<TGTimeline> stop) {
		this.start();
		TGTimeline planned = TGPractiseAction.timeline(this.world.fixture().context, this.world.song(), this.session(), SEED);
		this.world.fixture().actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_SESSION, this.session());
		this.world.fixture().actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_SEED, SEED);
		this.world.fixture().actionContext.setAttribute(TGPractiseAction.ATTRIBUTE_STOP_SAMPLE, stop.applyAsLong(planned));
		this.world.execute(new TGPractiseAction(this.world.fixture().context));
		assertTrue(this.world.succeeded());
	}

	// the session started last, which must have been accepted
	TGPracticeSession session() {
		if (this.session == null) {
			throw new IllegalStateException("interleaved practice was refused: " + this.world.refusal());
		}
		return this.session;
	}

	// the unit numbers chosen, one list per seed, in the order of the seeds
	List<List<Integer>> runs() {
		return this.runs;
	}

	TGMeasureHeader header(int measure) {
		return this.world.song().getMeasureHeader(measure - 1);
	}

	// as the app flags the measure: the flag action, undoable
	void flag(int measure, int difficulty) {
		this.world.fixture().actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, this.header(measure));
		this.world.fixture().actionContext.setAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY, difficulty);
		this.world.executeUndoable(new TGChangeDifficultyAction(this.world.fixture().context), new TGUndoableDifficultyController());
	}

	boolean flagged() {
		return this.world.succeeded();
	}

	// an edit as the app makes it, which may be refused saying why (a TGActionException): the world's refusal
	void edit(Runnable edit) {
		this.world.setRefusal(null);
		try {
			edit.run();
		} catch (TGActionException refused) {
			this.world.setRefusal(refused.getMessage());
		}
	}
}
