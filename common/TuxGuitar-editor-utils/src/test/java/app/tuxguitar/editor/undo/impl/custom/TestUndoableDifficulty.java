package app.tuxguitar.editor.undo.impl.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.undo.TGCannotRedoException;
import app.tuxguitar.editor.undo.TGCannotUndoException;
import app.tuxguitar.editor.undo.TGUndoableEdit;

/**
 * Flagging a measure as difficult (interleaved practice's weight), as the app runs it: TGChangeDifficultyAction under
 * TGUndoableDifficultyController. A flag from 1x to 5x is set on the measure and is one undo step; anything else is
 * refused, changes nothing and is no undo step.
 *
 * The song has four measures, read back as their difficulties.
 */
public class TestUndoableDifficulty {

	private final SongActionFixture fixture = SongActionFixture.onDocument();

	public TestUndoableDifficulty() {
		fixture.measureCount(4);
	}

	// the measure flagged under the controller; the edit the controller ends with (null: no undo step)
	private TGUndoableEdit flag(int measure, int difficulty) {
		fixture.actionContext.setAttribute(TGActionBase.ATTRIBUTE_SUCCESS, null);
		fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, fixture.song.getMeasureHeader(measure - 1));
		fixture.actionContext.setAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY, difficulty);
		return fixture.executeUndoable(new TGChangeDifficultyAction(fixture.context), new TGUndoableDifficultyController());
	}

	private String difficulties() {
		return fixture.measures(measure -> String.valueOf(measure.getHeader().getDifficulty()));
	}

	@Test
	public void aFlagIsSetOnItsMeasure() {
		flag(2, 3);
		assertEquals("1 | 3 | 1 | 1 | success", difficulties() + " | " + fixture.outcome().replace(" | ", ""));
	}

	@Test
	public void undoTakesTheFlagBackAndRedoPutsItBack() {
		flag(3, 5);
		flag(3, 2);
		fixture.undo();
		String undone = difficulties();
		fixture.undo();
		String undoneTwice = difficulties();
		fixture.redo();
		assertEquals("1 | 1 | 5 | 1 / 1 | 1 | 1 | 1 / 1 | 1 | 5 | 1", undone + " / " + undoneTwice + " / " + difficulties());
	}

	@Test
	public void aFlagOutsideOneToFiveIsRefusedAndIsNoUndoStep() {
		flag(2, 4);
		TGUndoableEdit none = flag(2, 0);
		TGUndoableEdit six = flag(2, 6);
		assertEquals("1 | 4 | 1 | 1", difficulties() + fixture.outcome());
		assertNull(none);
		assertNull(six);
	}

	@Test
	public void anEditCannotBeRedoneBeforeItIsUndone() {
		TGUndoableEdit edit = flag(2, 3);
		assertThrows(TGCannotRedoException.class, () -> edit.redo(null));
	}

	@Test
	public void anEditCannotBeUndoneTwice() {
		TGUndoableEdit edit = flag(2, 3);
		fixture.undo();
		assertThrows(TGCannotUndoException.class, () -> edit.undo(null));
	}
}
