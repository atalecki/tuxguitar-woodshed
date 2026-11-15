package app.tuxguitar.app.action.impl.caret;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.app.transport.TGTransport;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.practice.TGPracticeCaret;
import app.tuxguitar.util.TGContext;

public class TGGoLeftAction extends TGActionBase{

	public static final String NAME = "action.caret.go-left";

	public TGGoLeftAction(TGContext context) {
		super(context, NAME);
	}

	// while the song plays the player moves with the caret; never during a practice session (see TGPracticeCaret)
	protected void processAction(TGActionContext context){
		if( TGPracticeCaret.movesPlayer(getContext()) ){
			TGTransport.getInstance(getContext()).gotoPrevious();
		}
		else{
			this.moveLeft(context);
		}
	}

	private void moveLeft(TGActionContext context){
		Tablature tablature = TablatureEditor.getInstance(getContext()).getTablature();
		tablature.getCaret().moveLeft();
		TGCaretMoved.clearSelection(context, tablature);
		TGCaretMoved.playBeat(getContext(), tablature.getCaret());
	}
}
