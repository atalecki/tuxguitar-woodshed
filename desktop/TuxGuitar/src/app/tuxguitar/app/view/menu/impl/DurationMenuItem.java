package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.component.tab.Caret;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.editor.action.duration.TGChangeDottedDurationAction;
import app.tuxguitar.editor.action.duration.TGChangeDoubleDottedDurationAction;
import app.tuxguitar.editor.action.duration.TGSetEighthDurationAction;
import app.tuxguitar.editor.action.duration.TGSetHalfDurationAction;
import app.tuxguitar.editor.action.duration.TGSetQuarterDurationAction;
import app.tuxguitar.editor.action.duration.TGSetSixteenthDurationAction;
import app.tuxguitar.editor.action.duration.TGSetSixtyFourthDurationAction;
import app.tuxguitar.editor.action.duration.TGSetThirtySecondDurationAction;
import app.tuxguitar.editor.action.duration.TGSetWholeDurationAction;
import app.tuxguitar.editor.action.note.TGChangeTiedNoteAction;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

public class DurationMenuItem  extends TGMenuItem {

	private UIMenuSubMenuItem durationMenuItem;
	private UIMenuCheckableItem whole;
	private UIMenuCheckableItem half;
	private UIMenuCheckableItem quarter;
	private UIMenuCheckableItem eighth;
	private UIMenuCheckableItem sixteenth;
	private UIMenuCheckableItem thirtySecond;
	private UIMenuCheckableItem sixtyFourth;
	private UIMenuCheckableItem dotted;
	private UIMenuCheckableItem doubleDotted;
	private UIMenuCheckableItem tiedNote;

	private DivisionMenuItem divisionMenuItem;

	public DurationMenuItem(UIMenuSubMenuItem durationMenuItem) {
		this.durationMenuItem = this.named(durationMenuItem, "duration", null, TGIconManager.DURATION);
	}

	public DurationMenuItem(UIMenu parent) {
		this(parent.createSubMenuItem());
	}

	public void showItems(){
		//--WHOLE--
		this.whole = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.whole", TGSetWholeDurationAction.NAME, null);

		//--HALF--
		this.half = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.half", TGSetHalfDurationAction.NAME, null);

		//--QUARTER--
		this.quarter = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.quarter", TGSetQuarterDurationAction.NAME, null);

		//--EIGHTH--
		this.eighth = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.eighth", TGSetEighthDurationAction.NAME, null);

		//--SIXTEENTH--
		this.sixteenth = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.sixteenth", TGSetSixteenthDurationAction.NAME, null);

		//--THIRTY SECOND--
		this.thirtySecond = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.thirtysecond", TGSetThirtySecondDurationAction.NAME, null);

		//--SIXTY FOURTH--
		this.sixtyFourth = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.sixtyfourth", TGSetSixtyFourthDurationAction.NAME, null);

		//--SEPARATOR--
		this.durationMenuItem.getMenu().createSeparator();

		//--DOTTED--
		this.dotted = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.dotted", TGChangeDottedDurationAction.NAME, TGIconManager.DOTTED);

		//--DOUBLE-DOTTED--
		this.doubleDotted = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.doubledotted", TGChangeDoubleDottedDurationAction.NAME, TGIconManager.DOUBLE_DOTTED);

		//--SEPARATOR--
		this.durationMenuItem.getMenu().createSeparator();

		//--TIED NOTE--
		this.tiedNote = this.actionItem(this.durationMenuItem.getMenu().createCheckItem(), "duration.tiednote", TGChangeTiedNoteAction.NAME, TGIconManager.TIED_NOTE);

		//--SEPARATOR--
		this.durationMenuItem.getMenu().createSeparator();

		//--DIVISION--
		this.divisionMenuItem = new DivisionMenuItem(this.durationMenuItem.getMenu().createSubMenuItem());
		this.divisionMenuItem.showItems();

		this.loadIcons();
		this.loadProperties();
	}

	public void update(){
		Tablature tablature = TuxGuitar.getInstance().getTablatureEditor().getTablature();
		Caret caret = tablature.getCaret();
		TGDuration duration = caret.getDuration();
		TGNote note = caret.getSelectedNote();
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		this.whole.setChecked(duration.getValue() == TGDuration.WHOLE);
		this.whole.setEnabled(!running);
		this.half.setChecked(duration.getValue() == TGDuration.HALF);
		this.half.setEnabled(!running);
		this.quarter.setChecked(duration.getValue() == TGDuration.QUARTER);
		this.quarter.setEnabled(!running);
		this.eighth.setChecked(duration.getValue() == TGDuration.EIGHTH);
		this.eighth.setEnabled(!running);
		this.sixteenth.setChecked(duration.getValue() == TGDuration.SIXTEENTH);
		this.sixteenth.setEnabled(!running);
		this.thirtySecond.setChecked(duration.getValue() == TGDuration.THIRTY_SECOND);
		this.thirtySecond.setEnabled(!running);
		this.sixtyFourth.setChecked(duration.getValue() == TGDuration.SIXTY_FOURTH);
		this.sixtyFourth.setEnabled(!running);
		this.dotted.setChecked(duration.isDotted());
		this.dotted.setEnabled(!running);
		this.doubleDotted.setChecked(duration.isDoubleDotted());
		this.doubleDotted.setEnabled(!running);
		this.tiedNote.setChecked(note != null && note.isTiedNote());
		this.tiedNote.setEnabled(!running);

		this.divisionMenuItem.update();
	}

	public void loadProperties(){
		this.loadNames(this.divisionMenuItem);
	}

	public void loadIcons() {
		this.loadImages();
		this.whole.setImage(TuxGuitar.getInstance().getIconManager().getDuration(TGDuration.WHOLE));
		this.half.setImage(TuxGuitar.getInstance().getIconManager().getDuration(TGDuration.HALF));
		this.quarter.setImage(TuxGuitar.getInstance().getIconManager().getDuration(TGDuration.QUARTER));
		this.eighth.setImage(TuxGuitar.getInstance().getIconManager().getDuration(TGDuration.EIGHTH));
		this.sixteenth.setImage(TuxGuitar.getInstance().getIconManager().getDuration(TGDuration.SIXTEENTH));
		this.thirtySecond.setImage(TuxGuitar.getInstance().getIconManager().getDuration(TGDuration.THIRTY_SECOND));
		this.sixtyFourth.setImage(TuxGuitar.getInstance().getIconManager().getDuration(TGDuration.SIXTY_FOURTH));
		this.divisionMenuItem.loadIcons();
	}
}
