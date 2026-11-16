package app.tuxguitar.editor.action.composition;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.util.TGContext;

/**
 * Flags the header's measure as difficult (interleaved practice's weight): 1x (not flagged) to 5x. Any other value is
 * refused: the measure is left as it was and the action does not report success.
 */
public class TGChangeDifficultyAction extends TGActionBase {

	public static final String NAME = "action.composition.change-difficulty";

	public static final String ATTRIBUTE_DIFFICULTY = "difficulty";

	public TGChangeDifficultyAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context) {
		int difficulty = ((Integer) context.getAttribute(ATTRIBUTE_DIFFICULTY)).intValue();
		TGMeasureHeader header = ((TGMeasureHeader) context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER));
		if (TGMeasureHeader.isDifficulty(difficulty)) {
			header.setDifficulty(difficulty);
			context.setAttribute(ATTRIBUTE_SUCCESS, Boolean.TRUE);
		}
	}
}
