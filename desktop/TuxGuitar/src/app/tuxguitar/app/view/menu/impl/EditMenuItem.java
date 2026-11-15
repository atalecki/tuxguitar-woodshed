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
		this.cut = this.named(this.editMenuItem.getMenu().createActionItem(), "edit.cut", TGCutAction.NAME, TGIconManager.EDIT_CUT);
		this.cut.addSelectionListener(this.createActionProcessor(TGCutAction.NAME));

		//--COPY--
		this.copy = this.named(this.editMenuItem.getMenu().createActionItem(), "edit.copy", TGCopyAction.NAME, TGIconManager.EDIT_COPY);
		this.copy.addSelectionListener(this.createActionProcessor(TGCopyAction.NAME));

		//--PASTE--
		this.paste = this.named(this.editMenuItem.getMenu().createActionItem(), "edit.paste", TGPasteAction.NAME, TGIconManager.EDIT_PASTE);
		this.paste.addSelectionListener(this.createActionProcessor(TGPasteAction.NAME));

		//--REPEAT--
		this.repeat = this.named(this.editMenuItem.getMenu().createActionItem(), "edit.repeat", TGRepeatAction.NAME, TGIconManager.EDIT_REPEAT);
		this.repeat.addSelectionListener(this.createActionProcessor(TGRepeatAction.NAME));

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--UNDO--
		this.undo = this.named(this.editMenuItem.getMenu().createActionItem(), "edit.undo", TGUndoAction.NAME, TGIconManager.UNDO);
		this.undo.addSelectionListener(this.createActionProcessor(TGUndoAction.NAME));

		//--REDO--
		this.redo = this.named(this.editMenuItem.getMenu().createActionItem(), "edit.redo", TGRedoAction.NAME, TGIconManager.REDO);
		this.redo.addSelectionListener(this.createActionProcessor(TGRedoAction.NAME));

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--Selection--
		this.selectAll = this.named(this.editMenuItem.getMenu().createActionItem(), "action.selection.select-all", TGSelectAllAction.NAME, null);
		this.selectAll.addSelectionListener(this.createActionProcessor(TGSelectAllAction.NAME));

		this.selectNone = this.named(this.editMenuItem.getMenu().createActionItem(), "action.selection.clear", TGClearSelectionAction.NAME, null);
		this.selectNone.addSelectionListener(this.createActionProcessor(TGClearSelectionAction.NAME));

		this.extendSelection = this.named(this.editMenuItem.getMenu().createSubMenuItem(), "edit.selection.extend", null, null);
		this.extendSelectionLeft = this.named(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-left", TGExtendSelectionLeftAction.NAME, null);
		this.extendSelectionLeft.addSelectionListener(this.createActionProcessor(TGExtendSelectionLeftAction.NAME));
		this.extendSelectionRight = this.named(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-right", TGExtendSelectionRightAction.NAME, null);
		this.extendSelectionRight.addSelectionListener(this.createActionProcessor(TGExtendSelectionRightAction.NAME));
		this.extendSelectionPrevious = this.named(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-previous", TGExtendSelectionPreviousAction.NAME, null);
		this.extendSelectionPrevious.addSelectionListener(this.createActionProcessor(TGExtendSelectionPreviousAction.NAME));
		this.extendSelectionNext = this.named(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-next", TGExtendSelectionNextAction.NAME, null);
		this.extendSelectionNext.addSelectionListener(this.createActionProcessor(TGExtendSelectionNextAction.NAME));
		this.extendSelectionFirst = this.named(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-first", TGExtendSelectionFirstAction.NAME, null);
		this.extendSelectionFirst.addSelectionListener(this.createActionProcessor(TGExtendSelectionFirstAction.NAME));
		this.extendSelectionLast = this.named(this.extendSelection.getMenu().createActionItem(), "action.selection.extend-last", TGExtendSelectionLastAction.NAME, null);
		this.extendSelectionLast.addSelectionListener(this.createActionProcessor(TGExtendSelectionLastAction.NAME));

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--TABLATURE EDIT MODE--
		this.modeSelection = this.named(this.editMenuItem.getMenu().createRadioItem(), "edit.mouse-mode-selection", TGSetMouseModeSelectionAction.NAME, TGIconManager.EDIT_MODE_SELECTION);
		this.modeSelection.addSelectionListener(this.createActionProcessor(TGSetMouseModeSelectionAction.NAME));

		//--SCORE EDIT MODE--
		this.modeEdition = this.named(this.editMenuItem.getMenu().createRadioItem(), "edit.mouse-mode-edition", TGSetMouseModeEditionAction.NAME, TGIconManager.EDIT_MODE_EDITION);
		this.modeEdition.addSelectionListener(this.createActionProcessor(TGSetMouseModeEditionAction.NAME));

		//--NATURAL NOTES--
		this.notNaturalKey = this.named(this.editMenuItem.getMenu().createCheckItem(), "edit.not-natural-key", TGSetNaturalKeyAction.NAME, TGIconManager.EDIT_MODE_EDITION_NO_NATURAL);
		this.notNaturalKey.addSelectionListener(this.createActionProcessor(TGSetNaturalKeyAction.NAME));

		// -- FREE EDITION MODE --
		this.freeEditionMode = this.named(this.editMenuItem.getMenu().createCheckItem(), "edit.free-edition-mode", TGToggleFreeEditionModeAction.NAME, TGIconManager.EDIT_MODE_FREE);
		this.freeEditionMode.addSelectionListener(this.createActionProcessor(TGToggleFreeEditionModeAction.NAME));
		this.openMeasureErrorsDialog = this.named(this.editMenuItem.getMenu().createActionItem(), "edit.measure-errors-dialog", TGOpenMeasureErrorsDialogAction.NAME, TGIconManager.EDIT_MEASURE_STATUS_CHECK);
		this.openMeasureErrorsDialog.addSelectionListener(this.createActionProcessor(TGOpenMeasureErrorsDialogAction.NAME));

		//--SEPARATOR--
		this.editMenuItem.getMenu().createSeparator();

		//--VOICE 1--
		this.voice1 = this.named(this.editMenuItem.getMenu().createRadioItem(), "edit.voice-1", TGSetVoice1Action.NAME, TGIconManager.EDIT_VOICE_1);
		this.voice1.addSelectionListener(this.createActionProcessor(TGSetVoice1Action.NAME));

		//--VOICE 2--
		this.voice2 = this.named(this.editMenuItem.getMenu().createRadioItem(), "edit.voice-2", TGSetVoice2Action.NAME, TGIconManager.EDIT_VOICE_2);
		this.voice2.addSelectionListener(this.createActionProcessor(TGSetVoice2Action.NAME));

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
