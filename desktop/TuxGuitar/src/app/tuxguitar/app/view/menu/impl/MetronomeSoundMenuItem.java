package app.tuxguitar.app.view.menu.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.TGActionProcessorListener;
import app.tuxguitar.app.action.impl.settings.TGOpenSettingsEditorAction;
import app.tuxguitar.app.action.impl.transport.TGSetMetronomeSoundAction;
import app.tuxguitar.app.view.dialog.settings.TGSettingsEditor;
import app.tuxguitar.app.view.dialog.settings.items.SoundOption;
import app.tuxguitar.app.view.menu.TGMenuItem;
import app.tuxguitar.player.base.MidiClickChoices;
import app.tuxguitar.player.base.MidiClickSound;
import app.tuxguitar.player.base.MidiClickSoundsFolder;
import app.tuxguitar.ui.UIComponent;
import app.tuxguitar.ui.event.UISelectionEvent;
import app.tuxguitar.ui.menu.UIMenu;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuCheckableItem;
import app.tuxguitar.ui.menu.UIMenuSubMenuItem;

/**
 * Transport > Metronome Sound: a radio item for every sound (the built-in ones, then the user's own, as their folder
 * has them: MidiClickSoundsFolder), picking the other beats' sound at once (TGSetMetronomeSoundAction), the one the
 * click plays checked; last, "Sounds and volume..." opens Settings on the Sound page. Its items are made again when
 * the sounds change (one added or removed) and when the language does. Glue only.
 */
public class MetronomeSoundMenuItem extends TGMenuItem {

	private final UIMenuSubMenuItem menuItem;
	// the items made, to dispose of when made again
	private final List<UIComponent> parts = new ArrayList<UIComponent>();
	private final Map<MidiClickSound, UIMenuCheckableItem> soundItems = new LinkedHashMap<MidiClickSound, UIMenuCheckableItem>();
	private List<MidiClickSound> shown = List.of();

	public MetronomeSoundMenuItem(UIMenuSubMenuItem menuItem) {
		this.menuItem = menuItem;
	}

	public void showItems() {
		this.show(MidiClickSoundsFolder.user().sounds());
	}

	// a radio item for each sound, then "Sounds and volume..."
	private void show(List<MidiClickSound> sounds) {
		this.parts.forEach(UIComponent::dispose);
		this.parts.clear();
		this.soundItems.clear();
		UIMenu menu = this.menuItem.getMenu();
		sounds.forEach(sound -> this.soundItems.put(sound, this.soundItem(menu, sound)));
		this.parts.addAll(this.soundItems.values());
		this.parts.add(menu.createSeparator());
		UIMenuActionItem settings = menu.createActionItem();
		settings.setText(TuxGuitar.getProperty("transport.metronome-sound.settings"));
		settings.addSelectionListener(this.createActionProcessor(TGOpenSettingsEditorAction.NAME, TGSettingsEditor.ATTRIBUTE_PAGE, SoundOption.class));
		this.parts.add(settings);
		this.shown = sounds;
	}

	private UIMenuCheckableItem soundItem(UIMenu menu, MidiClickSound sound) {
		UIMenuCheckableItem item = menu.createRadioItem();
		item.setText(MidiClickChoices.name(sound));
		TGActionProcessorListener processor = this.createActionProcessor(TGSetMetronomeSoundAction.NAME, TGSetMetronomeSoundAction.ATTRIBUTE_SOUND, sound);
		item.addSelectionListener(event -> picked(item, processor, event));
		return item;
	}

	// a radio item is told when it is unchecked too: only the one checked picks its sound
	private static void picked(UIMenuCheckableItem item, TGActionProcessorListener processor, UISelectionEvent event) {
		if (item.isChecked()) {
			processor.onSelect(event);
		}
	}

	// the sounds there are now (made again when they changed), the other beats' sound checked
	public void update() {
		List<MidiClickSound> sounds = MidiClickSoundsFolder.user().sounds();
		if (!sounds.equals(this.shown)) {
			this.show(sounds);
		}
		MidiClickSound other = TuxGuitar.getInstance().getPlayer().getClick().getOther();
		this.soundItems.forEach((sound, item) -> item.setChecked(sound.equals(other)));
	}

	public void loadProperties() {
		this.setMenuItemTextAndAccelerator(this.menuItem, "transport.metronome-sound", null);
		this.show(this.shown);
	}
}
