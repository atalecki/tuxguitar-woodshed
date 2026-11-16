package app.tuxguitar.app.action.impl.measure;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.app.transport.TGTransport;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.practice.TGPracticeCaret;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.util.TGContext;

public class TGGoPreviousMeasureAction extends TGActionBase{

	public static final String NAME = "action.measure.go-previous";

	public TGGoPreviousMeasureAction(TGContext context) {
		super(context, NAME);
	}

	// while the song plays the player moves a measure; never during a practice session, where only the caret moves (see
	// TGPracticeCaret)
	protected void processAction(TGActionContext context){
		if( TGPracticeCaret.movesPlayer(getContext()) ){
			TGTransport.getInstance(getContext()).gotoPrevious();
		}
		else{
			TGMeasure measure = (TGMeasure) context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE);
			TGMeasureCaret.moveTo(getContext(), context, getSongManager(context).getTrackManager().getPrevMeasure(measure));
		}
	}
}
