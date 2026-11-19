package app.tuxguitar.app.view.dialog.settings.items;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.config.TGConfigKeys;
import app.tuxguitar.app.view.dialog.settings.TGSettingsEditor;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiOutputPort;
import app.tuxguitar.player.base.MidiSequencer;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.toolbar.UIToolBar;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UISpinner;
import app.tuxguitar.util.TGSynchronizer;

public class SoundOption extends TGSettingsOption {

	// a channel of a General MIDI port, where the click is a drum note at a velocity
	private static final int GM_PORT_CHANNEL = 9;

	private boolean initialized;

	//**MidiSequencer module**//
	private String msCurrentKey;
	private List<MidiSequencer> msList;
	private UIDropDownSelect<MidiSequencer> msCombo;

	//**MidiPort module**//
	private String mpCurrentKey;
	private List<MidiOutputPort> mpList;
	private UIDropDownSelect<MidiOutputPort> mpCombo;

	//**Count In / Metronome click**//
	private UISpinner clickNote;
	private UISpinner clickVelocity;

	//**What plugins add**//
	private List<TGSoundSettings> extensions;

	public SoundOption(TGSettingsEditor configEditor, UIToolBar toolBar, UILayoutContainer parent){
		super(configEditor, toolBar, parent, TuxGuitar.getProperty("settings.config.sound"));
		this.initialized = false;
	}

	public void createOption() {
		UIFactory uiFactory = this.getUIFactory();

		getToolItem().setText(TuxGuitar.getProperty("settings.config.sound"));
		getToolItem().setImage(TuxGuitar.getInstance().getIconManager().getOptionSound());
		getToolItem().addSelectionListener(this);

		//---Midi Sequencer---//
		showLabel(getPanel(), TuxGuitar.getProperty("midi.sequencer"), true, 1, 1);

		UITableLayout msCompositeLayout = new UITableLayout();
		UIPanel msComposite = uiFactory.createPanel(getPanel(), false);
		msComposite.setLayout(msCompositeLayout);
		this.indent(msComposite, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);

		this.msCombo = uiFactory.createDropDownSelect(msComposite);
		msCompositeLayout.set(this.msCombo, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		//---Midi Port---//
		showLabel(getPanel(), TuxGuitar.getProperty("midi.port"), true, 3, 1);

		UITableLayout mpCompositeLayout = new UITableLayout();
		UIPanel mpComposite = uiFactory.createPanel(getPanel(), false);
		mpComposite.setLayout(mpCompositeLayout);
		this.indent(mpComposite, 4, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);

		this.mpCombo = uiFactory.createDropDownSelect(mpComposite);
		this.mpCombo.addSelectionListener(event -> this.tellPort());
		mpCompositeLayout.set(this.mpCombo, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		//---Count In / Metronome click---//
		showLabel(getPanel(), TuxGuitar.getProperty("midi.click"), true, 5, 1);

		UITableLayout clickCompositeLayout = new UITableLayout();
		UIPanel clickComposite = uiFactory.createPanel(getPanel(), false);
		clickComposite.setLayout(clickCompositeLayout);
		this.indent(clickComposite, 6, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);

		// until Settings offers the sounds: the other beats' drum note and their velocity on a General MIDI port
		MidiClick click = MidiClick.read(getConfig());
		this.clickNote = this.createClickSpinner(clickComposite, 1, "midi.click.note", 0, click.getOther().getGmNote());
		this.clickVelocity = this.createClickSpinner(clickComposite, 3, "midi.click.velocity", 1, click.getVelocity(GM_PORT_CHANNEL, false));

		this.createExtensions(7);
		this.loadConfig();
	}

	// the settings plugins add (TGSoundSettingsExtensions), each in a panel of its own from the row on, told the MIDI
	// Port configured until the ports are listed
	private void createExtensions(int row) {
		this.extensions = TGSoundSettingsExtensions.getInstance(getViewContext().getContext()).create();
		for(int i = 0; i < this.extensions.size(); i++) {
			UIPanel panel = getUIFactory().createPanel(getPanel(), false);
			panel.setLayout(new UITableLayout());
			((UITableLayout) getPanel().getLayout()).set(panel, row + i, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false);
			this.extensions.get(i).create(getUIFactory(), panel);
			this.extensions.get(i).portChosen(getConfig().getStringValue(TGConfigKeys.MIDI_PORT));
		}
	}

	// the settings plugins add are told the MIDI Port selected
	private void tellPort() {
		MidiOutputPort port = this.mpCombo.getSelectedValue();
		String key = (port != null ? port.getKey() : null);
		this.extensions.forEach(extension -> extension.portChosen(key));
	}

	// a label and a spinner from the minimum to 127 (a MIDI note or velocity), in the columns from the given one
	private UISpinner createClickSpinner(UIPanel parent, int column, String key, int minimum, int value) {
		UITableLayout layout = (UITableLayout) parent.getLayout();
		UILabel label = getUIFactory().createLabel(parent);
		label.setText(TuxGuitar.getProperty(key) + ":");
		layout.set(label, 1, column, UITableLayout.ALIGN_LEFT, UITableLayout.ALIGN_CENTER, false, false);
		UISpinner spinner = getUIFactory().createSpinner(parent);
		spinner.setMinimum(minimum);
		spinner.setMaximum(127);
		spinner.setValue(value);
		layout.set(spinner, 1, column + 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false);
		return spinner;
	}

	public void loadConfig(){
		new Thread(new Runnable() {
			public void run() {
				SoundOption.this.mpList = TuxGuitar.getInstance().getPlayer().listOutputPorts();
				SoundOption.this.msList = TuxGuitar.getInstance().getPlayer().listSequencers();

				SoundOption.this.mpCurrentKey = getConfig().getStringValue(TGConfigKeys.MIDI_PORT);
				SoundOption.this.msCurrentKey = getConfig().getStringValue(TGConfigKeys.MIDI_SEQUENCER);

				MidiSequencer sequencer = TuxGuitar.getInstance().getPlayer().getSequencer();
				MidiOutputPort outputPort = TuxGuitar.getInstance().getPlayer().getOutputPort();

				final String msLoaded = (sequencer != null ? sequencer.getKey() : null ) ;
				final String mpLoaded = (outputPort != null ? outputPort.getKey() : null );

				TGSynchronizer.getInstance(getViewContext().getContext()).executeLater(new Runnable() {
					public void run() {
						if(!isDisposed()){
							SoundOption.this.showDevices(msLoaded, mpLoaded);
						}
					}
				});
			}
		}).start();
	}

	// the sequencers and the ports listed, each with the one configured selected (else the one loaded, else the first);
	// the settings plugins add told the port
	private void showDevices(String msLoaded, String mpLoaded) {
		offer(this.msCombo, this.msList, MidiSequencer::getName, MidiSequencer::getKey, this.msCurrentKey, msLoaded);
		offer(this.mpCombo, this.mpList, MidiOutputPort::getName, MidiOutputPort::getKey, this.mpCurrentKey, mpLoaded);
		this.tellPort();
		this.initialized = true;
		this.pack();
	}

	private static <T> void offer(UIDropDownSelect<T> combo, List<T> items, Function<T, String> nameOf, Function<T, String> keyOf, String configured, String loaded) {
		items.forEach(item -> combo.addItem(new UISelectItem<T>(nameOf.apply(item), item)));
		find(items, keyOf, configured).or(() -> find(items, keyOf, loaded)).or(() -> items.stream().findFirst()).ifPresent(combo::setSelectedValue);
	}

	// the item of the key; none for no key
	private static <T> Optional<T> find(List<T> items, Function<T, String> keyOf, String key) {
		return Optional.ofNullable(key).flatMap(wanted -> items.stream().filter(item -> wanted.equals(keyOf.apply(item))).findFirst());
	}

	// the click is kept whether or not the devices were listed yet: its spinners are filled at once
	public void updateConfig(){
		if(this.initialized){
			this.updateKey(TGConfigKeys.MIDI_SEQUENCER, this.msCombo, MidiSequencer::getKey);
			this.updateKey(TGConfigKeys.MIDI_PORT, this.mpCombo, MidiOutputPort::getKey);
		}
		MidiClick.ofMidi(this.clickNote.getValue(), this.clickVelocity.getValue()).write(getConfig());
		this.extensions.forEach(TGSoundSettings::keep);
	}

	// the key of what is selected, if anything is
	private <T> void updateKey(String configKey, UIDropDownSelect<T> combo, Function<T, String> keyOf){
		T selected = combo.getSelectedValue();
		if( selected != null ){
			getConfig().setValue(configKey, keyOf.apply(selected));
		}
	}

	public void updateDefaults(){
		if(this.initialized){
			getConfig().setValue(TGConfigKeys.MIDI_PORT, getDefaults().getValue(TGConfigKeys.MIDI_PORT));
			getConfig().setValue(TGConfigKeys.MIDI_SEQUENCER, getDefaults().getValue(TGConfigKeys.MIDI_SEQUENCER));
		}
		MidiClick.DEFAULT.write(getConfig());
		this.extensions.forEach(TGSoundSettings::keepDefaults);
	}
}