package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.insert.TGOpenTextDialogAction;
import app.tuxguitar.app.action.impl.note.TGOpenBeatMoveDialogAction;
import app.tuxguitar.app.action.impl.note.TGOpenStrokeDownDialogAction;
import app.tuxguitar.app.action.impl.note.TGOpenStrokeUpDialogAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.component.tab.Caret;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.editor.action.note.TGChangePickStrokeUpAction;
import app.tuxguitar.editor.action.note.TGChangePickStrokeDownAction;
import app.tuxguitar.editor.action.note.TGInsertRestBeatAction;
import app.tuxguitar.editor.action.note.TGDeleteNoteOrRestAction;
import app.tuxguitar.editor.action.note.TGCleanBeatAction;
import app.tuxguitar.editor.action.note.TGSetVoiceAutoAction;
import app.tuxguitar.editor.action.note.TGSetVoiceUpAction;
import app.tuxguitar.editor.action.note.TGSetVoiceDownAction;
import app.tuxguitar.editor.action.note.TGIncrementNoteSemitoneAction;
import app.tuxguitar.editor.action.note.TGDecrementNoteSemitoneAction;
import app.tuxguitar.editor.action.note.TGShiftNoteUpAction;
import app.tuxguitar.editor.action.note.TGToggleNoteEnharmonicAction;
import app.tuxguitar.graphics.control.TGLayout;
import app.tuxguitar.graphics.control.TGTrackImpl;
import app.tuxguitar.editor.action.note.TGShiftNoteDownAction;
import app.tuxguitar.editor.action.note.TGMoveBeatsLeftAction;
import app.tuxguitar.editor.action.note.TGMoveBeatsRightAction;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGPickStroke;
import app.tuxguitar.song.models.TGStroke;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;
import app.tuxguitar.util.TGNoteRange;

public class BeatMenuItem extends TGMenuItem {

	private UIMenuSubMenuItem noteMenuItem;
	private UIMenuActionItem insertRestBeat;
	private UIMenuActionItem deleteNoteOrRest;
	private UIMenuActionItem cleanBeat;
	private UIMenuActionItem insertText;
	private UIMenuActionItem voiceAuto;
	private UIMenuActionItem voiceUp;
	private UIMenuActionItem voiceDown;
	private UIMenuCheckableItem strokeUp;
	private UIMenuCheckableItem strokeDown;
	private UIMenuCheckableItem pickStrokeUp;
	private UIMenuCheckableItem pickStrokeDown;
	private UIMenuActionItem shiftUp;
	private UIMenuActionItem shiftDown;
	private UIMenuCheckableItem altEnharmonic;
	private UIMenuActionItem semitoneUp;
	private UIMenuActionItem semitoneDown;
	private UIMenuActionItem moveBeatsLeft;
	private UIMenuActionItem moveBeatsRight;
	private UIMenuActionItem moveBeatsCustom;

	private DurationMenuItem durationMenuItem;
	private DynamicMenuItem dynamicMenuItem;
	private NoteEffectsMenuItem effectMenuItem;
	private ChordMenuItem chordMenuItem;

	public BeatMenuItem(UIMenu parent) {
		this.noteMenuItem = this.named(parent.createSubMenuItem(), "beat", null, null);
	}

	public void showItems(){
		this.insertRestBeat = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.insert-rest", TGInsertRestBeatAction.NAME, null);

		this.deleteNoteOrRest = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.delete-note-or-rest", TGDeleteNoteOrRestAction.NAME, null);

		this.cleanBeat = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.clean", TGCleanBeatAction.NAME, null);

		this.noteMenuItem.getMenu().createSeparator();

		this.durationMenuItem = new DurationMenuItem(this.noteMenuItem.getMenu().createSubMenuItem());
		this.durationMenuItem.showItems();

		this.dynamicMenuItem = new DynamicMenuItem(this.noteMenuItem.getMenu().createSubMenuItem());
		this.dynamicMenuItem.showItems();

		this.effectMenuItem = new NoteEffectsMenuItem(this.noteMenuItem.getMenu().createSubMenuItem());
		this.effectMenuItem.showItems();

		this.chordMenuItem = new ChordMenuItem(this.noteMenuItem.getMenu().createSubMenuItem());
		this.chordMenuItem.showItems();

		this.noteMenuItem.getMenu().createSeparator();

		this.insertText = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "text.insert", TGOpenTextDialogAction.NAME, TGIconManager.TEXT);

		this.noteMenuItem.getMenu().createSeparator();

		this.voiceAuto = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.voice-auto", TGSetVoiceAutoAction.NAME, null);

		this.voiceUp = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.voice-up", TGSetVoiceUpAction.NAME, null);

		this.voiceDown = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.voice-down", TGSetVoiceDownAction.NAME, null);

		this.noteMenuItem.getMenu().createSeparator();

		this.strokeUp = this.actionItem(this.noteMenuItem.getMenu().createCheckItem(), "beat.stroke-up", TGOpenStrokeUpDialogAction.NAME, TGIconManager.STROKE_UP);

		this.strokeDown = this.actionItem(this.noteMenuItem.getMenu().createCheckItem(), "beat.stroke-down", TGOpenStrokeDownDialogAction.NAME, TGIconManager.STROKE_DOWN);

		this.pickStrokeDown = this.actionItem(this.noteMenuItem.getMenu().createCheckItem(), "beat.pick-stroke-down", TGChangePickStrokeDownAction.NAME, TGIconManager.PICK_STROKE_DOWN);

		this.pickStrokeUp = this.actionItem(this.noteMenuItem.getMenu().createCheckItem(), "beat.pick-stroke-up", TGChangePickStrokeUpAction.NAME, TGIconManager.PICK_STROKE_UP);

		this.noteMenuItem.getMenu().createSeparator();

		this.altEnharmonic = this.actionItem(this.noteMenuItem.getMenu().createCheckItem(), "note.alternative-enharmonic", TGToggleNoteEnharmonicAction.NAME, TGIconManager.ALTERNATIVE_ENHARMONIC);

		this.noteMenuItem.getMenu().createSeparator();

		this.semitoneUp = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "note.semitone-up", TGIncrementNoteSemitoneAction.NAME, null);

		this.semitoneDown = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "note.semitone-down", TGDecrementNoteSemitoneAction.NAME, null);

		this.noteMenuItem.getMenu().createSeparator();

		this.shiftUp = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "note.shift-up", TGShiftNoteUpAction.NAME, null);

		this.shiftDown = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "note.shift-down", TGShiftNoteDownAction.NAME, null);

		this.noteMenuItem.getMenu().createSeparator();

		this.moveBeatsLeft = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.move-left", TGMoveBeatsLeftAction.NAME, null);

		this.moveBeatsRight = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.move-right", TGMoveBeatsRightAction.NAME, null);

		this.moveBeatsCustom = this.actionItem(this.noteMenuItem.getMenu().createActionItem(), "beat.move-custom", TGOpenBeatMoveDialogAction.NAME, null);

		this.loadIcons();
		this.loadProperties();
	}

	public void update(){
		Tablature tablature = TuxGuitar.getInstance().getTablatureEditor().getTablature();
		Caret caret = tablature.getCaret();
		TGBeat beat = caret.getSelectedBeat();
		TGNote note = caret.getSelectedNote();
		boolean restBeat = caret.isRestBeatSelected();
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		TGTrackImpl track = caret.getTrack();
		TGNoteRange noteRange = tablature.getCurrentNoteRange();
		boolean atLeastOneNoteSelected = (note != null) || (noteRange!=null && !noteRange.isEmpty());
		int style = tablature.getViewLayout().getStyle();

		this.insertRestBeat.setEnabled(!running);
		this.deleteNoteOrRest.setEnabled(!running);
		this.cleanBeat.setEnabled(!running);
		this.voiceAuto.setEnabled(!running && !restBeat);
		this.voiceUp.setEnabled(!running && !restBeat);
		this.voiceDown.setEnabled(!running && !restBeat);
		this.strokeUp.setEnabled(!running && !restBeat && !track.isPercussion());
		this.strokeUp.setChecked( beat != null && beat.getStroke().getDirection() == TGStroke.STROKE_UP );
		this.strokeDown.setEnabled(!running && !restBeat && !track.isPercussion());
		this.strokeDown.setChecked( beat != null && beat.getStroke().getDirection() == TGStroke.STROKE_DOWN );
		this.pickStrokeUp.setEnabled(!running && !restBeat);
		this.pickStrokeUp.setChecked( beat != null && beat.getPickStroke().getDirection() == TGPickStroke.PICK_STROKE_UP );
		this.pickStrokeDown.setEnabled(!running && !restBeat);
		this.pickStrokeDown.setChecked( beat != null && beat.getPickStroke().getDirection() == TGPickStroke.PICK_STROKE_DOWN );
		this.altEnharmonic.setEnabled(!running && ((style & TGLayout.DISPLAY_SCORE) != 0) && !restBeat);
		this.altEnharmonic.setChecked((note!=null) && (note.isAltEnharmonic()));
		this.semitoneUp.setEnabled(!running && atLeastOneNoteSelected);
		this.semitoneDown.setEnabled(!running && atLeastOneNoteSelected);
		this.shiftUp.setEnabled(!running && atLeastOneNoteSelected);
		this.shiftDown.setEnabled(!running && atLeastOneNoteSelected);
		this.insertText.setEnabled(!running);
		this.moveBeatsLeft.setEnabled(!running);
		this.moveBeatsRight.setEnabled(!running);
		this.moveBeatsCustom.setEnabled(!running);
		this.durationMenuItem.update();
		this.effectMenuItem.update();
		if (track.isPercussion()) {
			this.chordMenuItem.setEnabled(false);
		} else {
			this.chordMenuItem.setEnabled(true);
			this.chordMenuItem.update();
		}
		this.dynamicMenuItem.update();
	}

	public void loadProperties(){
		this.loadNames(this.durationMenuItem, this.chordMenuItem, this.effectMenuItem, this.dynamicMenuItem);
	}

	public void loadIcons(){
		this.loadImages();
	}
}
