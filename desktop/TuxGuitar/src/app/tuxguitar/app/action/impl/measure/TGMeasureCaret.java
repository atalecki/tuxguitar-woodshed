package app.tuxguitar.app.action.impl.measure;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.app.action.impl.caret.TGCaretMoved;
import app.tuxguitar.app.action.impl.caret.TGMoveToAction;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.util.TGContext;

/**
 * Where Measure First, Previous, Next and Last take the caret when they do not move the player: to the first beat of
 * the measure (TGMoveToAction), the selection gone unless the action keeps it; with no such measure (before the first,
 * after the last) the caret stays.
 */
final class TGMeasureCaret {

	private TGMeasureCaret() {
	}

	static void moveTo(TGContext context, TGActionContext actionContext, TGMeasure measure) {
		TGCaretMoved.clearSelection(actionContext, TablatureEditor.getInstance(context).getTablature());
		if( measure != null ){
			actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, measure);
			actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_BEAT, measure.getBeat(0));
			TGActionManager.getInstance(context).execute(TGMoveToAction.NAME, actionContext);
		}
	}
}
