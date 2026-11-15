package app.tuxguitar.app.view.menu.impl;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.TGActionProcessorListener;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.practice.TGPracticeEdits;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

/**
 * Composition > Difficulty: flags the measure at the caret 1x (not flagged) to 5x difficult, for interleaved practice;
 * the item of its flag is checked. Enabled whenever a flag may be set: not while the song plays, but during a practice
 * session (see TGPracticeEdits.disables).
 */
public class DifficultyMenuItem extends TGMenuItem {

	private final UIMenuSubMenuItem difficultyMenuItem;
	private final UIMenuCheckableItem[] items = new UIMenuCheckableItem[5];

	public DifficultyMenuItem(UIMenuSubMenuItem difficultyMenuItem) {
		this.difficultyMenuItem = difficultyMenuItem;
	}

	public void showItems() {
		for (int i = 0; i < this.items.length; i++) {
			this.items[i] = this.difficultyMenuItem.getMenu().createRadioItem();
			this.act(this.items[i], this.createFlagAction(i + 1));
		}
	}

	private TGActionProcessorListener createFlagAction(int difficulty) {
		TGActionProcessorListener processor = this.createActionProcessor(TGChangeDifficultyAction.NAME);
		processor.setAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY, difficulty);
		return processor;
	}

	public void update() {
		int flag = TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getMeasure().getHeader().getDifficulty();
		boolean enabled = !TGPracticeEdits.disables(this.findContext(), TGChangeDifficultyAction.NAME);
		for (int i = 0; i < this.items.length; i++) {
			this.items[i].setChecked(flag == i + 1);
			this.items[i].setEnabled(enabled);
		}
	}

	public void loadProperties() {
		setMenuItemTextAndAccelerator(this.difficultyMenuItem, "composition.difficulty", null);
		for (int i = 0; i < this.items.length; i++) {
			setMenuItemTextAndAccelerator(this.items[i], "composition.difficulty." + (i + 1), null);
		}
	}
}
