package app.tuxguitar.app.action.impl.caret;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.app.transport.TGTransport;
import app.tuxguitar.app.view.component.tab.Caret;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.practice.TGPracticeCaret;
import app.tuxguitar.editor.action.measure.TGAddMeasureAction;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

public class TGGoRightAction extends TGActionBase{

	public static final String NAME = "action.caret.go-right";

	public TGGoRightAction(TGContext context) {
		super(context, NAME);
	}

	// while the song plays the player moves with the caret; never during a practice session (see TGPracticeCaret)
	protected void processAction(TGActionContext context){
		if( TGPracticeCaret.movesPlayer(getContext()) ){
			TGTransport.getInstance(getContext()).gotoNext();
		}
		else{
			this.moveRight(context);
		}
	}

	private void moveRight(TGActionContext context){
		Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
		TGCaretMoved.clearSelection(context, tablature);
		Caret caret = tablature.getCaret();
		if(!caret.moveRight()){
			this.addMeasureAndMoveRight(context, caret);
		}
		TGCaretMoved.playBeat(getContext(), caret);
	}

	// past the last measure: a measure is added after it, and the caret goes there
	private void addMeasureAndMoveRight(TGActionContext context, Caret caret){
		TGSong song = ((TGSong) context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG));
		context.setAttribute(TGAddMeasureAction.ATTRIBUTE_MEASURE_NUMBER, Integer.valueOf((song.countMeasureHeaders() + 1)));

		TGActionManager tgActionManager = TGActionManager.getInstance(getContext());
		tgActionManager.execute(TGAddMeasureAction.NAME, context);

		caret.moveRight();
	}
}
