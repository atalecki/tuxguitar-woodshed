package app.tuxguitar.editor.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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
 * The song has four measures, read back as their difficulties; the session (6 turns of measures over a pool of the
 * given size, its chooser begun, one turn chosen) is under way unless a test says otherwise.
 */
public class TestPracticeEdits {

	private static final String TOO_HEAVY = "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row.";

	private final SongActionFixture fixture = SongActionFixture.onDocument();
	private TGPracticeSession session;

	public TestPracticeEdits() {
		fixture.measureCount(4);
		TGPracticeEdits.install(fixture.context);
	}

	// a session over measures 1 to the last, under way, its first turn chosen
	private void underWay(int last) throws TGPracticeRefusal {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, last);
		settings.setTurns(6);
		session = TGPracticeSession.start(settings, fixture.song);
		TGPracticeChooser chooser = session.chooser(1);
		chooser.next();
		TGPracticeUnderWay.getInstance(fixture.context).begin(chooser);
	}

	private void flag(int measure, int difficulty) {
		fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, fixture.song.getMeasureHeader(measure - 1));
		fixture.actionContext.setAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY, difficulty);
		fixture.executeUndoable(new TGChangeDifficultyAction(fixture.context), new TGUndoableDifficultyController());
	}

	// the message of the refusal
	private String refused(Runnable edit) {
		return assertThrows(TGActionException.class, edit::run).getMessage();
	}

	// the difficulties, then whether the session has ended
	private String state() {
		return fixture.measures(measure -> String.valueOf(measure.getHeader().getDifficulty())) + " | " + session.isEnded();
	}

	@Test
	public void aFlagTheNextCyclesCanPlayGoesOn() throws TGPracticeRefusal {
		underWay(4);
		flag(2, 3);
		assertEquals("1 | 3 | 1 | 1 | false", state());
	}

	@Test
	public void aFlagTheNextCyclesCouldNotPlayIsRefusedAndChangesNothing() throws TGPracticeRefusal {
		underWay(2);
		String refusal = refused(() -> flag(2, 3));
		fixture.undo();
		assertEquals(TOO_HEAVY + " | 1 | 1 | 1 | 1 | false", refusal + " | " + state());
	}

	@Test
	public void undoingAFlagFollowsTheSameRule() throws TGPracticeRefusal {
		flag(2, 3);
		flag(2, 1);
		underWay(2);
		String refusal = refused(fixture::undo);
		String afterRefusal = state();
		flag(3, 2);
		fixture.undo();
		assertEquals(TOO_HEAVY + " | 1 | 1 | 1 | 1 | false | 1 | 1 | 1 | 1 | false", refusal + " | " + afterRefusal + " | " + state());
	}

	@Test
	public void redoingAFlagFollowsTheSameRule() throws TGPracticeRefusal {
		flag(2, 3);
		fixture.undo();
		underWay(2);
		String refusal = refused(fixture::redo);
		String afterRefusal = state();
		session.end();
		underWay(4);
		fixture.redo();
		assertEquals(TOO_HEAVY + " | 1 | 1 | 1 | 1 | false | 1 | 3 | 1 | 1 | false", refusal + " | " + afterRefusal + " | " + state());
	}

	// a 0x or 6x flag is the flag action's to refuse
	@Test
	public void aFlagOutsideOneToFiveIsNotChecked() throws TGPracticeRefusal {
		underWay(2);
		flag(2, 6);
		assertEquals("1 | 1 | 1 | 1 | false", state());
	}

	@Test
	public void anyOtherEditEndsTheSession() throws TGPracticeRefusal {
		underWay(4);
		tripletFeel();
		assertEquals("1 | 1 | 1 | 1 | true", state());
	}

	@Test
	public void undoingAnyOtherEditEndsTheSession() throws TGPracticeRefusal {
		tripletFeel();
		underWay(4);
		fixture.undo();
		assertEquals("1 | 1 | 1 | 1 | true", state());
	}

	@Test
	public void withoutASessionUnderWayAFlagIsNotChecked() throws TGPracticeRefusal {
		underWay(2);
		session.end();
		flag(2, 3);
		assertEquals("1 | 3 | 1 | 1 | true", state());
	}

	private void tripletFeel() {
		TGMeasureHeader second = fixture.song.getMeasureHeader(1);
		fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, second);
		fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, fixture.song.getTrack(0).getMeasure(1));
		fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, fixture.song.getTrack(0));
		fixture.actionContext.setAttribute(TGChangeTripletFeelAction.ATTRIBUTE_TRIPLET_FEEL, TGMeasureHeader.TRIPLET_FEEL_EIGHTH);
		fixture.actionContext.setAttribute(TGChangeTripletFeelAction.ATTRIBUTE_APPLY_TO_END, false);
		fixture.actionContext.setAttribute(TGActionBase.ATTRIBUTE_SUCCESS, null);
		fixture.executeUndoable(new TGChangeTripletFeelAction(fixture.context), new TGUndoableTripletFeelController());
	}
}
