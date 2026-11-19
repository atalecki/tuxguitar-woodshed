package app.tuxguitar.app.view.dialog.settings.items;

import java.util.Arrays;
import java.util.Optional;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiClickChoices;
import app.tuxguitar.player.base.MidiClickSound;
import app.tuxguitar.player.base.MidiClickSoundsFolder;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.chooser.UIFileChooser;
import app.tuxguitar.ui.chooser.UIFileChooserFormat;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UIScale;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.configuration.TGConfigManager;

/**
 * Settings > Sound, "Count In / Metronome click": Beat 1 ("Same sound, accented" or a sound), Other beats (a sound),
 * Add sound... (a WAV file copied into the user's own sounds, then chosen for the other beats; a file the click could not
 * play is refused, the reason on the line under the group) and Remove sound (the own sound of the other beats), the
 * Click volume slider with its dB, Try (beat 1 then an other beat, as chosen, not yet kept). What is offered and what a
 * choice makes is MidiClickChoices (lib, tested); OK keeps the click in the configuration. Glue only.
 */
public class TGClickSoundSettings implements TGSoundSettings {

	private static final String KEY = "metronome.sound.";

	private final TGConfigManager config;
	// the Settings window, the file chooser's parent
	private final UIWindow window;
	private UIFactory factory;
	private MidiClickChoices choices;
	private UIPanel panel;
	private UIDropDownSelect<Optional<MidiClickSound>> beatOne;
	private UIDropDownSelect<MidiClickSound> other;
	private UIButton remove;
	// from 0 (MidiClick.MIN_VOLUME) up: a scale has no values below 0
	private UIScale volume;
	private UILabel decibels;
	// why a sound was not added or removed; empty after a choice that went through
	private UILabel notice;

	public TGClickSoundSettings(TGConfigManager config, UIWindow window) {
		this.config = config;
		this.window = window;
	}

	public void create(UIFactory factory, UIPanel panel) {
		this.factory = factory;
		this.panel = panel;
		this.choices = new MidiClickChoices(MidiClick.read(this.config), MidiClickSoundsFolder.user());
		this.beatOne = this.select(factory, 1, "beat-one");
		this.beatOne.addSelectionListener(event -> this.choices.chooseBeatOne(this.beatOne.getSelectedValue()));
		this.other = this.select(factory, 2, "other");
		this.other.addSelectionListener(event -> this.otherChosen());
		this.button(factory, 2, 3, "add", this::chooseFile);
		this.remove = this.button(factory, 2, 4, "remove", this::removeSound);
		this.createVolume(factory);
		this.button(factory, 3, 4, "try", () -> TuxGuitar.getInstance().getPlayer().tryClick(this.choices.getClick()));
		this.notice = factory.createLabel(panel);
		this.layout().set(this.notice, 4, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 4);
		this.show();
	}

	private UITableLayout layout() {
		return (UITableLayout) this.panel.getLayout();
	}

	private void label(UIFactory factory, int row, String key) {
		UILabel label = factory.createLabel(this.panel);
		label.setText(TuxGuitar.getProperty(KEY + key) + ":");
		this.layout().set(label, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
	}

	private <T> UIDropDownSelect<T> select(UIFactory factory, int row, String key) {
		this.label(factory, row, key);
		UIDropDownSelect<T> select = factory.createDropDownSelect(this.panel);
		this.layout().set(select, row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 1, 180f, null, null);
		return select;
	}

	private UIButton button(UIFactory factory, int row, int column, String key, Runnable action) {
		UIButton button = factory.createButton(this.panel);
		button.setText(TuxGuitar.getProperty(KEY + key));
		button.addSelectionListener(event -> action.run());
		this.layout().set(button, row, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 80f, 25f, null);
		return button;
	}

	// the slider, MidiClick.MIN_VOLUME to MAX_VOLUME, and its dB beside it
	private void createVolume(UIFactory factory) {
		this.label(factory, 3, "volume");
		this.volume = factory.createHorizontalScale(this.panel);
		this.volume.setMinimum(0);
		this.volume.setMaximum(MidiClick.MAX_VOLUME - MidiClick.MIN_VOLUME);
		this.volume.setIncrement(1);
		this.layout().set(this.volume, 3, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false);
		this.decibels = factory.createLabel(this.panel);
		this.layout().set(this.decibels, 3, 3, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 60f, null, null);
		this.volume.addSelectionListener(event -> this.volumeChosen());
	}

	// the drop-downs filled with every sound there is now, the choices selected; Remove only for an own sound
	private void show() {
		MidiClick click = this.choices.getClick();
		this.beatOne.removeItems();
		this.choices.getBeatOneChoices().forEach(choice -> this.beatOne.addItem(new UISelectItem<Optional<MidiClickSound>>(MidiClickChoices.name(choice), choice)));
		this.beatOne.setSelectedValue(click.getBeatOne());
		this.other.removeItems();
		this.choices.getSounds().forEach(sound -> this.other.addItem(new UISelectItem<MidiClickSound>(MidiClickChoices.name(sound), sound)));
		this.other.setSelectedValue(click.getOther());
		this.remove.setEnabled(this.choices.canRemove());
		this.volume.setValue(click.getVolume() - MidiClick.MIN_VOLUME);
		this.decibels.setText(MidiClickChoices.volume(click.getVolume()));
	}

	private void otherChosen() {
		this.choices.chooseOther(this.other.getSelectedValue());
		this.remove.setEnabled(this.choices.canRemove());
	}

	private void volumeChosen() {
		this.choices.chooseVolume(this.volume.getValue() + MidiClick.MIN_VOLUME);
		this.decibels.setText(MidiClickChoices.volume(this.choices.getClick().getVolume()));
	}

	// only WAV files offered; nothing happens when none is chosen
	private void chooseFile() {
		UIFileChooser chooser = this.factory.createOpenFileChooser(this.window);
		chooser.setText(TuxGuitar.getProperty(KEY + "add"));
		chooser.setSupportedFormats(Arrays.asList(new UIFileChooserFormat(TuxGuitar.getProperty(KEY + "wav-files"), Arrays.asList("wav"))));
		chooser.choose(file -> Optional.ofNullable(file).ifPresent(chosen -> this.change(() -> this.choices.add(chosen))));
	}

	private void removeSound() {
		this.change(this.choices::remove);
	}

	// a sound added or removed
	private interface Change {
		void make() throws MidiClickSoundsFolder.Refused;
	}

	// the change made, the choices shown again; the line under them says why when it was refused, else nothing
	private void change(Change change) {
		String notice = "";
		try {
			change.make();
		} catch (MidiClickSoundsFolder.Refused refused) {
			notice = refused.getMessage();
		}
		this.notice.setText(notice);
		this.show();
		this.window.pack();
	}

	// OK: kept (TGReloadSettingsAction gives it to the player)
	public void keep() {
		this.choices.getClick().write(this.config);
	}

	public void keepDefaults() {
		MidiClick.DEFAULT.write(this.config);
	}

	// the click is chosen alike for every port (a General MIDI port plays each sound's drum note)
	public void portChosen(String portKey) {
	}
}
