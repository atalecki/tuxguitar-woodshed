package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.measure.TGGoFirstMeasureAction;
import app.tuxguitar.app.action.impl.measure.TGGoLastMeasureAction;
import app.tuxguitar.app.action.impl.measure.TGGoNextMeasureAction;
import app.tuxguitar.app.action.impl.measure.TGGoPreviousMeasureAction;
import app.tuxguitar.app.action.impl.measure.TGOpenMeasureAddDialogAction;
import app.tuxguitar.app.action.impl.measure.TGOpenMeasureCleanDialogAction;
import app.tuxguitar.app.action.impl.measure.TGOpenMeasureCopyDialogAction;
import app.tuxguitar.app.action.impl.measure.TGOpenMeasurePasteDialogAction;
import app.tuxguitar.app.action.impl.measure.TGOpenMeasureRemoveDialogAction;
import app.tuxguitar.app.action.impl.measure.TGToggleLineBreakAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.editor.action.measure.TGRemoveUnusedVoiceAction;
import app.tuxguitar.editor.clipboard.TGClipboard;
import app.tuxguitar.graphics.control.TGMeasureImpl;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

public class MeasureMenuItem extends TGMenuItem {

	private UIMenuSubMenuItem measureMenuItem;
	private UIMenuCheckableItem lineBreak;
	private UIMenuActionItem first;
	private UIMenuActionItem last;
	private UIMenuActionItem next;
	private UIMenuActionItem previous;
	private UIMenuActionItem addMeasure;
	private UIMenuActionItem cleanMeasure;
	private UIMenuActionItem removeMeasure;
	private UIMenuActionItem copyMeasure;
	private UIMenuActionItem pasteMeasure;
	private UIMenuActionItem removeVoice;

	public MeasureMenuItem(UIMenu parent) {
		this.measureMenuItem = this.named(parent.createSubMenuItem(), "measure", null, null);
	}

	public void showItems(){
		//--LINEBREAK--
		this.lineBreak = this.actionItem(this.measureMenuItem.getMenu().createCheckItem(), "measure.linebreak", TGToggleLineBreakAction.NAME, null);

		//--SEPARATOR--
		this.measureMenuItem.getMenu().createSeparator();

		//--FIRST--
		this.first = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.first", TGGoFirstMeasureAction.NAME, TGIconManager.MEASURE_FIRST);

		//--PREVIOUS--
		this.previous = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.previous", TGGoPreviousMeasureAction.NAME, TGIconManager.MEASURE_PREVIOUS);

		//--NEXT--
		this.next = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.next", TGGoNextMeasureAction.NAME, TGIconManager.MEASURE_NEXT);

		//--LAST--
		this.last = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.last", TGGoLastMeasureAction.NAME, TGIconManager.MEASURE_LAST);

		//--SEPARATOR--
		this.measureMenuItem.getMenu().createSeparator();

		//--ADD--
		this.addMeasure = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.add", TGOpenMeasureAddDialogAction.NAME, TGIconManager.MEASURE_ADD);

		//--CLEAN--
		this.cleanMeasure = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.clean", TGOpenMeasureCleanDialogAction.NAME, TGIconManager.MEASURE_CLEAN);

		//--REMOVE--
		this.removeMeasure = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.remove", TGOpenMeasureRemoveDialogAction.NAME, TGIconManager.MEASURE_REMOVE);

		//--SEPARATOR--
		this.measureMenuItem.getMenu().createSeparator();

		//--COPY--
		this.copyMeasure = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.copy", TGOpenMeasureCopyDialogAction.NAME, TGIconManager.MEASURE_COPY);

		//--PASTE--
		this.pasteMeasure = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.paste", TGOpenMeasurePasteDialogAction.NAME, TGIconManager.MEASURE_PASTE);

		//--SEPARATOR--
		this.measureMenuItem.getMenu().createSeparator();

		// -- REMOVE UNUSED VOICE --
		this.removeVoice = this.actionItem(this.measureMenuItem.getMenu().createActionItem(), "measure.voice.remove-unused", TGRemoveUnusedVoiceAction.NAME, null);

		this.loadIcons();
		this.loadProperties();
	}

	public void update(){
		TGMeasureImpl measure = TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getMeasure();
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		boolean isFirst = (measure.getNumber() == 1);
		boolean isLast = (measure.getNumber() == measure.getTrack().countMeasures());
		this.lineBreak.setEnabled(!running);
		this.lineBreak.setChecked(measure.isLineBreak());
		this.first.setEnabled(!isFirst);
		this.previous.setEnabled(!isFirst);
		this.next.setEnabled(!isLast);
		this.last.setEnabled(!isLast);
		this.addMeasure.setEnabled(!running);
		this.cleanMeasure.setEnabled(!running);
		this.removeMeasure.setEnabled(!running);
		this.copyMeasure.setEnabled(!running);
		this.pasteMeasure.setEnabled(!running && TGClipboard.getInstance(findContext()).getSegment() != null);
		this.removeVoice.setEnabled(!running);
	}

	public void loadProperties(){
		this.loadNames();
	}

	public void loadIcons(){
		this.loadImages();
	}
}
