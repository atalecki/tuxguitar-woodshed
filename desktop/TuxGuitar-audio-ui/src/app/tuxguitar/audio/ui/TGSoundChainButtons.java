package app.tuxguitar.audio.ui;

import java.util.EnumMap;
import java.util.Map;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.TGSoundChainRows.Control;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The buttons of a sound chain editor (the Sound Chain dialog, the Sound Chain panel), one per control, each enabled or
 * greyed as the rows decide it (TGSoundChainRows.isEnabled) for the row picked; the enable/disable toggle says what it
 * would do. Glue only.
 */
final class TGSoundChainButtons {

	private final Map<Control, UIButton> buttons = new EnumMap<Control, UIButton>(Control.class);

	// the control's button
	UIButton put(Control control, UIButton button) {
		this.buttons.put(control, button);
		return button;
	}

	void apply(TGSoundChainRows rows, int row, TGVst3Plugin effect, TGVst3Plugin instrument) {
		this.buttons.forEach((control, button) -> button.setEnabled(rows.isEnabled(control, row, effect, instrument)));
		this.buttons.get(Control.TOGGLE).setText(TuxGuitar.getProperty(rows.toggleKey(row)));
	}
}
