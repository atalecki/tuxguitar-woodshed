package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.composition.TGOpenClefDialogAction;
import app.tuxguitar.app.action.impl.composition.TGOpenKeySignatureDialogAction;
import app.tuxguitar.app.action.impl.composition.TGOpenSongInfoDialogAction;
import app.tuxguitar.app.action.impl.composition.TGOpenTempoDialogAction;
import app.tuxguitar.app.action.impl.composition.TGOpenTimeSignatureDialogAction;
import app.tuxguitar.app.action.impl.composition.TGOpenTripletFeelDialogAction;
import app.tuxguitar.app.action.impl.insert.TGOpenRepeatAlternativeDialogAction;
import app.tuxguitar.app.action.impl.insert.TGOpenRepeatCloseDialogAction;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.editor.action.composition.TGRepeatOpenAction;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

public class CompositionMenuItem extends TGMenuItem {

	private UIMenuSubMenuItem compositionMenuItem;
	private UIMenuActionItem timeSignature;
	private UIMenuActionItem tempo;
	private UIMenuActionItem clef;
	private UIMenuActionItem keySignature;
	private UIMenuActionItem repeatOpen;
	private UIMenuActionItem repeatClose;
	private UIMenuActionItem repeatAlternative;
	private UIMenuActionItem tripletFeel;
	private UIMenuActionItem properties;
	private DifficultyMenuItem difficulty;

	public CompositionMenuItem(UIMenu parent) {
		this.compositionMenuItem = parent.createSubMenuItem();
	}

	public void showItems(){
		this.named(this.compositionMenuItem, "composition", null, null);
		//--TIME SIGNATURE--
		this.timeSignature = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "composition.timesignature", TGOpenTimeSignatureDialogAction.NAME, TGIconManager.TIME_SIGNATURE);

		//--TEMPO--
		this.tempo = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "composition.tempo", TGOpenTempoDialogAction.NAME, TGIconManager.TEMPO);

		//--CLEF--
		this.clef = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "composition.clef", TGOpenClefDialogAction.NAME, TGIconManager.CLEF);

		//--KEY SIGNATURE--
		this.keySignature = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "composition.keysignature", TGOpenKeySignatureDialogAction.NAME, TGIconManager.KEY_SIGNATURE);

		//--TRIPLET FEEL--
		this.tripletFeel = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "composition.tripletfeel", TGOpenTripletFeelDialogAction.NAME, TGIconManager.TRIPLET_FEEL);

		//--DIFFICULTY (interleaved practice)--
		this.difficulty = new DifficultyMenuItem(this.compositionMenuItem.getMenu().createSubMenuItem());
		this.difficulty.showItems();

		//--SEPARATOR--
		this.compositionMenuItem.getMenu().createSeparator();

		//--REPEAT OPEN--
		this.repeatOpen = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "repeat.open", TGRepeatOpenAction.NAME, TGIconManager.REPEAT_OPEN);

		//--REPEAT CLOSE--
		this.repeatClose = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "repeat.close", TGOpenRepeatCloseDialogAction.NAME, TGIconManager.REPEAT_CLOSE);

		//--REPEAT ALTERNATIVE--
		this.repeatAlternative = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "repeat.alternative", TGOpenRepeatAlternativeDialogAction.NAME, TGIconManager.REPEAT_ALTERNATIVE);

		//--SEPARATOR--
		this.compositionMenuItem.getMenu().createSeparator();

		//--INFO--
		this.properties = this.actionItem(this.compositionMenuItem.getMenu().createActionItem(), "composition.properties", TGOpenSongInfoDialogAction.NAME, TGIconManager.SONG_PROPERTIES);

		this.loadIcons();
		this.loadProperties();
	}

	public void update(){
		boolean isPercussion = TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getTrack().isPercussion();
		boolean running = TuxGuitar.getInstance().getPlayer().isRunning();
		boolean pitched = (!running && !isPercussion);
		this.timeSignature.setEnabled(!running);
		this.tempo.setEnabled(!running);
		this.clef.setEnabled(pitched);
		this.keySignature.setEnabled(pitched);
		this.tripletFeel.setEnabled(!running);
		this.difficulty.update();
		this.repeatOpen.setEnabled(!running);
		this.repeatClose.setEnabled(!running);
		this.repeatAlternative.setEnabled(!running);
	}

	public void loadProperties() {
		this.loadNames(this.difficulty);
	}

	public void loadIcons() {
		this.loadImages();
	}
}
