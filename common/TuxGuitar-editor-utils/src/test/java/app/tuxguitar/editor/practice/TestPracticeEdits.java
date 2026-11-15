package app.tuxguitar.editor.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import app.tuxguitar.action.TGActionException;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.action.composition.TGChangeTripletFeelAction;
import app.tuxguitar.editor.undo.impl.custom.TGUndoableDifficultyController;
import app.tuxguitar.editor.undo.impl.custom.TGUndoableTripletFeelController;
import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * Edits during an interleaved practice session (TGPracticeEdits), as the app runs them: a flag, and the undo or redo of
 * one, goes on when the next cycles can play its weights, else it is refused saying why and changes nothing; any other
 * edit, or its undo, ends the session. Without a session under way nothing is checked.
 *
 * Each case: a song of four measures, the edits made before, then a session of 6 turns of measures over measures 1 to
 * the last given, its chooser under way with one turn chosen, then the edits made during it. It reads "the refusal's
 * message (go on without one) | each measure's flag | whether the session has ended".
 */
public class TestPracticeEdits {

	private static final String TOO_HEAVY = "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row.";

	private static final Consumer<Edits> NOTHING = edits -> { };

	// a case's song, its edits and its session
	private static final class Edits {
		private final SongActionFixture fixture = SongActionFixture.onDocument();
		private TGPracticeSession session;

		Edits() {
			fixture.measureCount(4);
			TGPracticeEdits.install(fixture.context);
		}

		void underWay(int last) throws TGPracticeRefusal {
			TGPracticeSettings settings = new TGPracticeSettings();
			settings.setPool(1, last);
			settings.setTurns(6);
			session = TGPracticeSession.start(settings, fixture.song);
			TGPracticeChooser chooser = session.chooser(1);
			chooser.next();
			TGPracticeUnderWay.getInstance(fixture.context).begin(chooser);
		}

		void flag(int measure, int difficulty) {
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, fixture.song.getMeasureHeader(measure - 1));
			fixture.actionContext.setAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY, difficulty);
			fixture.executeUndoable(new TGChangeDifficultyAction(fixture.context), new TGUndoableDifficultyController());
		}

		void undo() {
			fixture.undo();
		}

		void redo() {
			fixture.redo();
		}

		void flaggedAndUndone() {
			flag(2, 3);
			undo();
		}

		// a refused flag is no undo step: undo after it undoes nothing
		void refusedThenUndone() {
			try {
				flag(2, 3);
			} catch (TGActionException refused) {
				undo();
			}
		}

		void tripletFeel() {
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, fixture.song.getMeasureHeader(1));
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, fixture.song.getTrack(0).getMeasure(1));
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, fixture.song.getTrack(0));
			fixture.actionContext.setAttribute(TGChangeTripletFeelAction.ATTRIBUTE_TRIPLET_FEEL, TGMeasureHeader.TRIPLET_FEEL_EIGHTH);
			fixture.actionContext.setAttribute(TGChangeTripletFeelAction.ATTRIBUTE_APPLY_TO_END, false);
			fixture.actionContext.setAttribute(TGActionBase.ATTRIBUTE_SUCCESS, null);
			fixture.executeUndoable(new TGChangeTripletFeelAction(fixture.context), new TGUndoableTripletFeelController());
		}

		void endedThenFlagged() {
			session.end();
			flag(2, 3);
		}

		String during(Consumer<Edits> before, int last, Consumer<Edits> edit) throws TGPracticeRefusal {
			before.accept(this);
			underWay(last);
			String outcome = "go on";
			try {
				edit.accept(this);
			} catch (TGActionException refused) {
				outcome = refused.getMessage();
			}
			return outcome + " | " + fixture.measures(measure -> String.valueOf(measure.getHeader().getDifficulty())) + " | " + session.isEnded();
		}
	}

	private static DynamicTest reads(String name, String expected, Consumer<Edits> before, int last, Consumer<Edits> edit) {
		return DynamicTest.dynamicTest(name, () -> assertEquals(expected, new Edits().during(before, last, edit)));
	}

	@TestFactory
	public List<DynamicTest> editsDuringASession() {
		return Arrays.asList(
			reads("a flag the next cycles can play goes on", "go on | 1 | 3 | 1 | 1 | false", NOTHING, 4, edits -> edits.flag(2, 3)),
			reads("a flag the next cycles could not play is refused", TOO_HEAVY + " | 1 | 1 | 1 | 1 | false", NOTHING, 2, edits -> edits.flag(2, 3)),
			reads("a refused flag is no undo step", "go on | 1 | 1 | 1 | 1 | false", NOTHING, 2, Edits::refusedThenUndone),
			reads("undoing a flag the next cycles could not play is refused", TOO_HEAVY + " | 1 | 1 | 1 | 1 | false", edits -> {
				edits.flag(2, 3);
				edits.flag(2, 1);
			}, 2, Edits::undo),
			reads("undoing a flag the next cycles can play goes on", "go on | 1 | 1 | 1 | 1 | false", edits -> edits.flag(3, 2), 2, Edits::undo),
			reads("redoing a flag the next cycles could not play is refused", TOO_HEAVY + " | 1 | 1 | 1 | 1 | false", Edits::flaggedAndUndone, 2, Edits::redo),
			reads("redoing a flag the next cycles can play goes on", "go on | 1 | 3 | 1 | 1 | false", Edits::flaggedAndUndone, 4, Edits::redo),
			// the flag action's to refuse
			reads("a flag outside 1x to 5x is not checked", "go on | 1 | 1 | 1 | 1 | false", NOTHING, 2, edits -> edits.flag(2, 6)),
			reads("any other edit ends the session", "go on | 1 | 1 | 1 | 1 | true", NOTHING, 4, Edits::tripletFeel),
			reads("undoing any other edit ends the session", "go on | 1 | 1 | 1 | 1 | true", Edits::tripletFeel, 4, Edits::undo),
			reads("without a session under way a flag is not checked", "go on | 1 | 3 | 1 | 1 | true", NOTHING, 2, Edits::endedThenFlagged));
	}
}
