package app.tuxguitar.audio.ui;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.dialog.settings.items.TGSoundSettings;
import app.tuxguitar.audio.TGDefaultInstrumentChoices;
import app.tuxguitar.audio.TGDefaultInstrumentChoices.Choice;
import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.audio.TGDefaultInstruments;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.chooser.UIFileChooser;
import app.tuxguitar.ui.chooser.UIFileChooserFormat;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UIReadOnlyTextBox;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.vst3.TGVst3Hosting;

/**
 * Settings > Sound, "Default instruments": for each kind of track a drop-down of General MIDI and the VST3 instruments
 * the last scan found, and for guitar and bass the profile Woodshed Guitar plays (the one TuxGuitar ships until a .nam
 * file is chosen, Shipped going back to it; only while Woodshed Guitar is the kind's choice). What can be chosen is
 * decided by TGDefaultInstrumentChoices, which is tested; OK keeps the choices in TuxGuitar's audio configuration and
 * applies them to the open song at once, when they changed (TGDefaultInstrumentSource.apply). Glue only.
 */
public class TGDefaultInstrumentsSettings implements TGSoundSettings {

	private static final String KEY = "audio.default-instruments";

	private final TGContext context;
	private TGDefaultInstrumentChoices choices;
	private final Map<TGInstrumentKind, Profile> profiles = new EnumMap<TGInstrumentKind, Profile>(TGInstrumentKind.class);
	private UIPanel panel;

	public TGDefaultInstrumentsSettings(TGContext context) {
		this.context = context;
	}

	private TGDefaultInstrumentSource source() {
		return TGDefaultInstrumentSource.install(this.context);
	}

	public void create(UIFactory factory, UIPanel panel) {
		this.panel = panel;
		this.choices = new TGDefaultInstrumentChoices(this.source().getDefaults(), TGVst3Hosting.getInstance(this.context).getPlugins());
		UILabel title = factory.createLabel(panel);
		title.setText(TuxGuitar.getProperty(KEY));
		this.layout().set(title, 1, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, true, false, 1, 4);
		int row = 2;
		for (TGInstrumentKind kind : TGInstrumentKind.values()) {
			row = this.rows(factory, row, kind);
		}
		this.showProfiles();
	}

	// the kind's drop-down, and its profile's row when it has one, from the row on; the next row
	private int rows(UIFactory factory, int row, TGInstrumentKind kind) {
		this.select(factory, row, kind);
		if (!TGDefaultInstrumentChoices.hasProfile(kind)) {
			return row + 1;
		}
		this.profiles.put(kind, new Profile(factory, row + 1, kind));
		return row + 2;
	}

	private UITableLayout layout() {
		return (UITableLayout) this.panel.getLayout();
	}

	private void label(UIFactory factory, int row, String key) {
		UILabel label = factory.createLabel(this.panel);
		label.setText(TuxGuitar.getProperty(key) + ":");
		this.layout().set(label, row, 1, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
	}

	// the kind's drop-down, its choice picked
	private void select(UIFactory factory, int row, TGInstrumentKind kind) {
		this.label(factory, row, KEY + "." + kind.name().toLowerCase(Locale.ROOT));
		UIDropDownSelect<Choice> select = factory.createDropDownSelect(this.panel);
		this.choices.choices(kind).forEach(choice -> select.addItem(new UISelectItem<Choice>(choice.getLabel(), choice)));
		select.setSelectedValue(this.choices.chosen(kind));
		select.addSelectionListener(event -> {
			this.choices.choose(kind, select.getSelectedValue());
			this.showProfiles();
		});
		this.layout().set(select, row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 3, 240f, null, null);
	}

	private void showProfiles() {
		this.profiles.forEach((kind, profile) -> profile.show());
	}

	// OK: kept and applied when changed
	public void keep() {
		this.keep(this.choices.getChosen());
	}

	public void keepDefaults() {
		this.keep(TGDefaultInstruments.BUILT_IN);
	}

	private void keep(TGDefaultInstruments defaults) {
		if (this.source().apply(defaults)) {
			TGConfigManager config = TGAudioPlugin.config(this.context);
			defaults.write(config);
			config.save();
		}
	}

	// the window the panel is in
	private static UIWindow window(UIControl control) {
		UIControl parent = control;
		while (!(parent instanceof UIWindow)) {
			parent = parent.getParent();
		}
		return (UIWindow) parent;
	}

	// a guitar or bass profile's row: what plays, Choose... a .nam file, Shipped to go back to the one TuxGuitar ships
	private final class Profile {
		private final TGInstrumentKind kind;
		private final UIReadOnlyTextBox shown;
		private final UIButton choose;
		private final UIButton shipped;

		Profile(UIFactory factory, int row, TGInstrumentKind kind) {
			this.kind = kind;
			TGDefaultInstrumentsSettings.this.label(factory, row, KEY + ".profile");
			this.shown = factory.createReadOnlyTextBox(TGDefaultInstrumentsSettings.this.panel, false, false);
			layout().set(this.shown, row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 1, 160f, null, null);
			this.choose = this.button(factory, row, 3, KEY + ".choose-profile", this::chooseFile);
			this.shipped = this.button(factory, row, 4, KEY + ".shipped", () -> this.chosen(null));
		}

		private UIButton button(UIFactory factory, int row, int column, String key, Runnable action) {
			UIButton button = factory.createButton(TGDefaultInstrumentsSettings.this.panel);
			button.setText(TuxGuitar.getProperty(key));
			button.addSelectionListener(event -> action.run());
			layout().set(button, row, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false, 1, 1, 80f, 25f, null);
			return button;
		}

		// only .nam files offered; nothing changes when none is chosen
		private void chooseFile() {
			UIFileChooser chooser = TGApplication.getInstance(TGDefaultInstrumentsSettings.this.context).getFactory().createOpenFileChooser(window(TGDefaultInstrumentsSettings.this.panel));
			chooser.setText(TuxGuitar.getProperty(KEY + ".choose-profile"));
			chooser.setSupportedFormats(Arrays.asList(new UIFileChooserFormat(TuxGuitar.getProperty(KEY + ".profile-format"), Arrays.asList("nam"))));
			chooser.choose(file -> Optional.ofNullable(file).ifPresent(chosen -> this.chosen(chosen.getAbsolutePath())));
		}

		private void chosen(String path) {
			choices.chooseProfile(this.kind, path);
			this.show();
		}

		void show() {
			boolean takes = choices.takesProfile(this.kind);
			this.shown.setText(choices.profile(this.kind));
			this.choose.setEnabled(takes);
			this.shipped.setEnabled(takes && !choices.isShipped(this.kind));
		}
	}
}
