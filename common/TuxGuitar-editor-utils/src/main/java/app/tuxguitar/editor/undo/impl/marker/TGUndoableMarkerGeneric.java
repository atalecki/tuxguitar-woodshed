package app.tuxguitar.editor.undo.impl.marker;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.editor.action.marker.TGRemoveMarkerAction;
import app.tuxguitar.editor.action.marker.TGUpdateMarkerAction;
import app.tuxguitar.editor.undo.TGCannotRedoException;
import app.tuxguitar.editor.undo.TGCannotUndoException;
import app.tuxguitar.editor.undo.impl.TGUndoableEditBase;
import app.tuxguitar.song.models.TGMarker;
import app.tuxguitar.util.TGContext;

public class TGUndoableMarkerGeneric extends TGUndoableEditBase {

	private int doAction;
	private TGMarker undoMarker;
	private TGMarker redoMarker;

	private TGUndoableMarkerGeneric(TGContext context){
		super(context);
	}

	public void redo(TGActionContext actionContext) throws TGCannotRedoException {
		if(!canRedo()){
			throw new TGCannotRedoException();
		}
		this.restore(actionContext, this.redoMarker, this.undoMarker);
		this.doAction = UNDO_ACTION;
	}

	public void undo(TGActionContext actionContext) throws TGCannotUndoException {
		if(!canUndo()){
			throw new TGCannotUndoException();
		}
		this.restore(actionContext, this.undoMarker, this.redoMarker);
		this.doAction = REDO_ACTION;
	}

	public boolean canRedo() {
		return (this.doAction == REDO_ACTION);
	}

	public boolean canUndo() {
		return (this.doAction == UNDO_ACTION);
	}

	public static TGUndoableMarkerGeneric startUndo(TGContext context, TGMarker marker){
		TGUndoableMarkerGeneric undoable = new TGUndoableMarkerGeneric(context);
		undoable.doAction = UNDO_ACTION;
		undoable.undoMarker = (marker != null ? marker.clone(getSongManager(context).getFactory()) : null);

		return undoable;
	}

	public TGUndoableMarkerGeneric endUndo(TGMarker marker){
		this.redoMarker = (marker != null ? marker.clone(getSongManager().getFactory()) : null);

		return this;
	}

	// puts the marker back on its measure; when there is none to put back, removes the marker the other side has there
	private void restore(TGActionContext context, TGMarker marker, TGMarker other) {
		if(marker != null){
			this.process(TGUpdateMarkerAction.NAME, context, marker);
		}else if(other != null){
			this.process(TGRemoveMarkerAction.NAME, context, other);
		}
	}

	private void process(String action, TGActionContext context, TGMarker marker) {
		TGActionProcessor tgActionProcessor = this.createByPassUndoableAction(action);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_SONG, getSong());
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MARKER, marker.clone(getSongManager().getFactory()));
		this.processByPassUndoableAction(tgActionProcessor, context);
	}
}
