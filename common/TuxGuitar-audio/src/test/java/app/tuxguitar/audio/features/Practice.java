package app.tuxguitar.audio.features;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.undo.impl.custom.TGUndoableDifficultyController;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * Interleaved practice in a scenario: the settings as the steps set them, the session started last (or why starting
 * was refused), and the turns it chose, one list of unit numbers per random seed. Cucumber makes one per scenario and
 * gives it to every step class that takes it.
 */
public class Practice {

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
}
