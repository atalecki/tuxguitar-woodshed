package app.tuxguitar.editor.undo.impl.custom;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.undo.TGUndoableActionController;
import app.tuxguitar.editor.undo.TGUndoableEdit;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.util.TGContext;

/**
 * The undo step of flagging a measure as difficult; a refused flag (TGChangeDifficultyAction did not report success)
 * is none.
 */
public class TGUndoableDifficultyController implements TGUndoableActionController {

	public TGUndoableEdit startUndoable(TGContext context, TGActionContext actionContext) {
		return TGUndoableDifficulty.startUndo(context, (TGMeasureHeader) actionContext.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER));
	}

	public TGUndoableEdit endUndoable(TGContext context, TGActionContext actionContext, TGUndoableEdit undoableEdit) {
		if (!Boolean.TRUE.equals(actionContext.getAttribute(TGActionBase.ATTRIBUTE_SUCCESS))) {
			return null;
		}
		int difficulty = ((Integer) actionContext.getAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY)).intValue();
		return ((TGUndoableDifficulty) undoableEdit).endUndo(difficulty);
	}
}
