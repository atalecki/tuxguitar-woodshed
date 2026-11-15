package app.tuxguitar.app.action.listener.cache.controller;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.app.transport.TGTransport;
import app.tuxguitar.editor.practice.TGPracticeCaret;
import app.tuxguitar.util.TGContext;

public class TGUpdateTransportPositionController extends TGUpdateItemsController {

	public TGUpdateTransportPositionController() {
		super();
	}

	// the caret moved: while the song plays, the player goes there too; never during a practice session, which moving
	// the player would end (see TGPracticeCaret)
	@Override
	public void update(TGContext context, TGActionContext actionContext) {
		if( TGPracticeCaret.movesPlayer(context) ){
			TGTransport.getInstance(context).gotoCaretPosition();
		}

		// Call super update.
		super.update(context, actionContext);
	}
}
