package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.edit.TGCutAction;
import app.tuxguitar.app.action.impl.edit.TGOpenMeasureErrorsDialogAction;
import app.tuxguitar.app.action.impl.edit.TGCopyAction;
import app.tuxguitar.app.action.impl.edit.TGPasteAction;
import app.tuxguitar.app.action.impl.edit.TGRepeatAction;
import app.tuxguitar.app.action.impl.edit.TGSetMouseModeEditionAction;
import app.tuxguitar.app.action.impl.edit.TGSetMouseModeSelectionAction;
import app.tuxguitar.app.action.impl.edit.TGSetNaturalKeyAction;
import app.tuxguitar.app.action.impl.edit.TGSetVoice1Action;
import app.tuxguitar.app.action.impl.edit.TGSetVoice2Action;
import app.tuxguitar.app.action.impl.edit.TGToggleFreeEditionModeAction;
import app.tuxguitar.app.action.impl.selector.TGClearSelectionAction;
import app.tuxguitar.app.action.impl.selector.TGExtendSelectionFirstAction;
import app.tuxguitar.app.action.impl.selector.TGExtendSelectionLastAction;
import app.tuxguitar.app.action.impl.selector.TGExtendSelectionLeftAction;
import app.tuxguitar.app.action.impl.selector.TGExtendSelectionNextAction;
import app.tuxguitar.app.action.impl.selector.TGExtendSelectionPreviousAction;
import app.tuxguitar.app.action.impl.selector.TGExtendSelectionRightAction;
import app.tuxguitar.app.action.impl.selector.TGSelectAllAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.app.view.component.tab.edit.EditorKit;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.editor.action.edit.TGRedoAction;
import app.tuxguitar.editor.action.edit.TGUndoAction;
import app.tuxguitar.editor.clipboard.TGClipboard;
import app.tuxguitar.editor.practice.TGPracticeEdits;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

public class EditMenuItem extends TGMenuItem{

	private UIMenuSubMenuItem editMenuItem;
	private UIMenuActionItem cut;
	private UIMenuActionItem copy;
	private UIMenuActionItem paste;
	private UIMenuActionItem repeat;
	private UIMenuActionItem undo;
	private UIMenuActionItem redo;
	private UIMenuActionItem selectAll;
	private UIMenuActionItem selectNone;
	private UIMenuSubMenuItem extendSelection;
	private UIMenuActionItem extendSelectionLeft;
	private UIMenuActionItem extendSelectionRight;
	private UIMenuActionItem extendSelectionPrevious;
	private UIMenuActionItem extendSelectionNext;
	private UIMenuActionItem extendSelectionFirst;
	private UIMenuActionItem extendSelectionLast;
	private UIMenuCheckableItem modeSelection;
	private UIMenuCheckableItem modeEdition;
	private UIMenuCheckableItem notNaturalKey;
	private UIMenuCheckableItem voice1;
	private UIMenuCheckableItem voice2;
	private UIMenuCheckableItem freeEditionMode;
	private UIMenuActionItem openMeasureErrorsDialog;

	public EditMenuItem(UIMenu parent) {
		this.editMenuItem = parent.createSubMenuItem();
	}

	public void showItems() {
		this.named(this.editMenuItem, "edit.menu", null, null);
		//--CUT--
		this.cut = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "edit.cut", TGCutAction.NAME, TGIconManager.EDIT_CUT);

		//--COPY--
		this.copy = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "edit.copy", TGCopyAction.NAME, TGIconManager.EDIT_COPY);

		//--PASTE--
		this.paste = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "edit.paste", TGPasteAction.NAME, TGIconManager.EDIT_PASTE);

		//--REPEAT--
		this.repeat = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "edit.repeat", TGRepeatAction.NAME, TGIconManager.EDIT_REPEAT);

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--UNDO--
		this.undo = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "edit.undo", TGUndoAction.NAME, TGIconManager.UNDO);

		//--REDO--
		this.redo = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "edit.redo", TGRedoAction.NAME, TGIconManager.REDO);

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--Selection--
		this.selectAll = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "action.selection.select-all", TGSelectAllAction.NAME, null);

		this.selectNone = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "action.selection.clear", TGClearSelectionAction.NAME, null);

		this.extendSelection = this.named(this.editMenuItem.getMenu().createSubMenuItem(), "edit.selection.extend", null, null);
		this.extendSelectionLeft = this.actionItem(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-left", TGExtendSelectionLeftAction.NAME, null);
		this.extendSelectionRight = this.actionItem(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-right", TGExtendSelectionRightAction.NAME, null);
		this.extendSelectionPrevious = this.actionItem(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-previous", TGExtendSelectionPreviousAction.NAME, null);
		this.extendSelectionNext = this.actionItem(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-next", TGExtendSelectionNextAction.NAME, null);
		this.extendSelectionFirst = this.actionItem(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-first", TGExtendSelectionFirstAction.NAME, null);
		this.extendSelectionLast = this.actionItem(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-last", TGExtendSelectionLastAction.NAME, null);

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--TABLATURE EDIT MODE--
		this.modeSelection = this.actionItem(this.editMenuItem.getMenu().createRadioItem(), "edit.mouse-mode-selection", TGSetMouseModeSelectionAction.NAME, TGIconManager.EDIT_MODE_SELECTION);

		//--SCORE EDIT MODE--
		this.modeEdition = this.actionItem(this.editMenuItem.getMenu().createRadioItem(), "edit.mouse-mode-edition", TGSetMouseModeEditionAction.NAME, TGIconManager.EDIT_MODE_EDITION);

		//--NATURAL NOTES--
		this.notNaturalKey = this.actionItem(this.editMenuItem.getMenu().createCheckItem(), "edit.not-natural-key", TGSetNaturalKeyAction.NAME, TGIconManager.EDIT_MODE_EDITION_NO_NATURAL);

		// -- FREE EDITION MODE --
		this.freeEditionMode = this.actionItem(this.editMenuItem.getMenu().createCheckItem(), "edit.free-edition-mode", TGToggleFreeEditionModeAction.NAME, TGIconManager.EDIT_MODE_FREE);
		this.openMeasureErrorsDialog = this.actionItem(this.editMenuItem.getMenu().createActionItem(), "edit.measure-errors-dialog", TGOpenMeasureErrorsDialogAction.NAME, TGIconManager.EDIT_MEASURE_STATUS_CHECK);

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--VOICE 1--
		this.voice1 = this.actionItem(this.editMenuItem.getMenu().createRadioItem(), "edit.voice-1", TGSetVoice1Action.NAME, TGIconManager.EDIT_VOICE_1);

		//--VOICE 2--
		this.voice2 = this.actionItem(this.editMenuItem.getMenu().createRadioItem(), "edit.voice-2", TGSetVoice2Action.NAME, TGIconManager.EDIT_VOICE_2);

		this.loadIcons();
		this.loadProperties();
	}

	public void update(){
		EditorKit kit = TuxGuitar.getInstance().getTablatureEditor().getTablature().getEditorKit();
		Tablature tablature = TablatureEditor.getInstance(findContext()).getTablature();
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		boolean selecting = stopped(running, tablature.getSelector().isActive());
		this.cut.setEnabled(selecting);
		this.copy.setEnabled(!running);
		this.paste.setEnabled(stopped(running, TGClipboard.getInstance(findContext()).hasContents()));
		this.repeat.setEnabled(selecting);
		this.undo.setEnabled(this.allows(TuxGuitar.getInstance().getUndoableManager().canUndo(), TGUndoAction.NAME));
		this.redo.setEnabled(this.allows(TuxGuitar.getInstance().getUndoableManager().canRedo(), TGRedoAction.NAME));
		this.selectAll.setEnabled(stopped(running, !TuxGuitar.getInstance().getSongManager().getTrackManager().hasMeasureDurationError(tablature.getCaret().getTrack())));
		this.selectNone.setEnabled(selecting);
		this.extendSelection.setEnabled(selecting);
		this.extendSelectionLeft.setEnabled(selecting);
		this.extendSelectionRight.setEnabled(selecting);
		this.extendSelectionPrevious.setEnabled(selecting);
		this.extendSelectionNext.setEnabled(selecting);
		this.extendSelectionFirst.setEnabled(selecting);
		this.extendSelectionLast.setEnabled(selecting);
		this.modeSelection.setChecked(kit.getMouseMode() == EditorKit.MOUSE_MODE_SELECTION);
		this.modeSelection.setEnabled(!running);
		this.modeEdition.setChecked(kit.getMouseMode() == EditorKit.MOUSE_MODE_EDITION);
		this.modeEdition.setEnabled(!running);
		this.notNaturalKey.setChecked(!kit.isNatural());
		this.notNaturalKey.setEnabled(stopped(running, kit.getMouseMode() == EditorKit.MOUSE_MODE_EDITION));
		this.voice1.setChecked(kit.getTablature().getCaret().getVoice() == 0);
		this.voice2.setChecked(kit.getTablature().getCaret().getVoice() == 1);
		this.freeEditionMode.setChecked(tablature.getSongManager().isFreeEditionMode(tablature.getCaret().getMeasure()));
		this.freeEditionMode.setEnabled(!running);
		this.openMeasureErrorsDialog.setEnabled(!running);
	}

	// enabled as the item says, while the player is not running
	private static boolean stopped(boolean running, boolean enabled) {
		return (!running && enabled);
	}

	// undo or redo, when there is one to take, unless the player runs, or a practice session lets only flags through
	// (see TGPracticeEdits.disables)
	private boolean allows(boolean can, String actionId) {
		return (can && !TGPracticeEdits.disables(findContext(), actionId));
	}

	public void loadProperties() {
		this.loadNames();
	}

	public void loadIcons() {
		this.loadImages();
	}
}
