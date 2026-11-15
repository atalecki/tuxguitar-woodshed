package app.tuxguitar.editor.undo.impl.custom;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.undo.TGCannotRedoException;
import app.tuxguitar.editor.undo.TGCannotUndoException;
import app.tuxguitar.editor.undo.impl.TGUndoableEditBase;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.util.TGContext;

/**
 * Undoes and redoes flagging one measure as difficult: the measure found by its start, set back to the flag it had
 * or again to the new one.
 */
public class TGUndoableDifficulty extends TGUndoableEditBase {

	private int doAction;
	private long position;
	private int undoableDifficulty;
	private int redoableDifficulty;

	private TGUndoableDifficulty(TGContext context) {
		super(context);
	}

	public void redo(TGActionContext actionContext) throws TGCannotRedoException {
		if (!canRedo()) {
			throw new TGCannotRedoException();
		}
		this.changeDifficulty(actionContext, this.redoableDifficulty);
		this.doAction = UNDO_ACTION;
	}

	public void undo(TGActionContext actionContext) throws TGCannotUndoException {
		if (!canUndo()) {
			throw new TGCannotUndoException();
		}
		this.changeDifficulty(actionContext, this.undoableDifficulty);
		this.doAction = REDO_ACTION;
	}

	public boolean canRedo() {
		return (this.doAction == REDO_ACTION);
	}

	public boolean canUndo() {
		return (this.doAction == UNDO_ACTION);
	}

	public static TGUndoableDifficulty startUndo(TGContext context, TGMeasureHeader header) {
		TGUndoableDifficulty undoable = new TGUndoableDifficulty(context);
		undoable.doAction = UNDO_ACTION;
		undoable.position = header.getStart();
		undoable.undoableDifficulty = header.getDifficulty();
		return undoable;
	}

	public TGUndoableDifficulty endUndo(int difficulty) {
		this.redoableDifficulty = difficulty;
		return this;
	}

	private void changeDifficulty(TGActionContext context, int difficulty) {
		TGActionProcessor tgActionProcessor = this.createByPassUndoableAction(TGChangeDifficultyAction.NAME);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, getSong());
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, getSongManager().getMeasureHeaderAt(getSong(), this.position));
		tgActionProcessor.setAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY, difficulty);
		this.processByPassUndoableAction(tgActionProcessor, context);
	}
}
