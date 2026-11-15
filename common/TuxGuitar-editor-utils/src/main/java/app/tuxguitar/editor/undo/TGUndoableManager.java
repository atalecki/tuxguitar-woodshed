package app.tuxguitar.editor.undo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonFactory;
import app.tuxguitar.util.singleton.TGSingletonUtil;

public class TGUndoableManager {

	private static final int LIMIT = 100;

	private TGUndoableBuffer buffer;
	// told each edit once it is added, undone or redone (null: an action that changed nothing)
	private final List<Consumer<TGUndoableEdit>> listeners = new CopyOnWriteArrayList<Consumer<TGUndoableEdit>>();

	public TGUndoableManager() {
		super();
	}

	public void discardAllEdits() {
		this.reset();
	}

	private void reset() {
		this.getBuffer().setIndexOfNextAdd(0);
		this.getBuffer().getEdits().clear();
	}

	public synchronized void undo(TGActionContext actionContext) throws TGCannotUndoException {
		TGUndoableEdit edit = editToBeUndone();
		if( edit == null ) {
			throw new TGCannotUndoException();
		}
		try{
			edit.undo(actionContext);
		}catch(Throwable throwable){
			throw new TGCannotUndoException(throwable);
		}
		this.decrementIndexOfNextAdd();
		this.tell(edit);
	}

	public synchronized void redo(TGActionContext actionContext) throws TGCannotRedoException {
		TGUndoableEdit edit = editToBeRedone();
		if( edit == null ) {
			throw new TGCannotRedoException();
		}
		try{
			edit.redo(actionContext);
		}catch(Throwable throwable){
			throw new TGCannotRedoException();
		}
		this.incrementIndexOfNextAdd();
		this.tell(edit);
	}

	public synchronized boolean canUndo() {
		boolean canUndo = false;
		TGUndoableEdit edit = editToBeUndone();
		if( edit != null ) {
			canUndo = edit.canUndo();
		}
		return canUndo;
	}

	public synchronized boolean canRedo() {
		boolean canRedo = false;
		TGUndoableEdit edit = editToBeRedone();
		if (edit != null) {
			canRedo = edit.canRedo();
		}
		return canRedo;
	}

	public synchronized void addEdit(TGUndoableEdit anEdit) {
		this.checkForUnused();
		this.checkForLimit();
		this.getBuffer().getEdits().add(this.getBuffer().getIndexOfNextAdd(), anEdit);
		this.incrementIndexOfNextAdd();
		this.tell(anEdit);
	}

	// the listener is told each edit from now on once it is added, undone or redone: the song changed by it
	public void addListener(Consumer<TGUndoableEdit> listener) {
		this.listeners.add(listener);
	}

	private void tell(TGUndoableEdit edit) {
		this.listeners.forEach(listener -> listener.accept(edit));
	}

	// the edit undo would undo next; null when there is none
	public synchronized TGUndoableEdit getEditToBeUndone() {
		return this.editToBeUndone();
	}

	// the edit redo would redo next; null when there is none
	public synchronized TGUndoableEdit getEditToBeRedone() {
		return this.editToBeRedone();
	}

	private void checkForUnused() {
		TGUndoableBuffer buffer = this.getBuffer();
		while (buffer.getEdits().size() > buffer.getIndexOfNextAdd()) {
			this.remove(buffer.getEdits().get(buffer.getIndexOfNextAdd()));
		}
	}

	private void checkForLimit() {
		TGUndoableBuffer buffer = this.getBuffer();
		while (buffer.getEdits().size() >= LIMIT) {
			this.remove(buffer.getEdits().get(0));
			this.decrementIndexOfNextAdd();
		}
	}

	private void remove(TGUndoableEdit edit) {
		this.getBuffer().getEdits().remove(edit);
	}

	private TGUndoableEdit editToBeUndone() {
		return this.editAt(this.getBuffer().getIndexOfNextAdd() - 1);
	}

	private TGUndoableEdit editToBeRedone() {
		return this.editAt(this.getBuffer().getIndexOfNextAdd());
	}

	// the edit at the index, or null when there is none
	private TGUndoableEdit editAt(int index) {
		List<TGUndoableEdit> edits = this.getBuffer().getEdits();
		if (index >= 0 && index < edits.size()) {
			return edits.get(index);
		}
		return null;
	}

	private void incrementIndexOfNextAdd() {
		this.getBuffer().setIndexOfNextAdd(this.getBuffer().getIndexOfNextAdd() + 1);
	}

	private void decrementIndexOfNextAdd() {
		this.getBuffer().setIndexOfNextAdd(this.getBuffer().getIndexOfNextAdd() - 1);
	}

	private TGUndoableBuffer getBuffer() {
		if( this.buffer == null ) {
			this.setBuffer(new TGUndoableBuffer());
		}
		return this.buffer;
	}

	public void setBuffer(TGUndoableBuffer buffer) {
		this.buffer = buffer;
	}

	public static TGUndoableManager getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGUndoableManager.class.getName(), new TGSingletonFactory<TGUndoableManager>() {
			public TGUndoableManager createInstance(TGContext context) {
				return new TGUndoableManager();
			}
		});
	}
}
