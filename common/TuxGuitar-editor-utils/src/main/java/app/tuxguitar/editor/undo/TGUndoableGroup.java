package app.tuxguitar.editor.undo;

import java.util.List;

/**
 * One undo step made of other edits, undone and redone together: a composite, or the desktop's step of an action and
 * the actions it runs (TGUndoableJoined). What the step changed is what its edits change.
 */
public interface TGUndoableGroup extends TGUndoableEdit {

	// the edits of the step, in the order they were made
	List<TGUndoableEdit> getEdits();
}
