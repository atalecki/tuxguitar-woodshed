package app.tuxguitar.app.view.dialog.settings.items;

import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.widget.UIPanel;

/**
 * Settings a plugin adds to Settings > Sound, under TuxGuitar's own (see TGSoundSettingsExtensions): its controls in a
 * panel of the page, kept when OK is pressed, set back to their defaults when Defaults is.
 */
public interface TGSoundSettings {

	// its controls, in the panel (whose layout is a UITableLayout), showing the settings as they are
	void create(UIFactory factory, UIPanel panel);

	// OK: what the controls show is kept (and applied)
	void keep();

	// Defaults: the settings' defaults are kept (and applied)
	void keepDefaults();

	// the MIDI Port chosen on the page (its key; null while none is): told once it is created, and each time the user
	// chooses another, before OK
	void portChosen(String portKey);
}
