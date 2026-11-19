package app.tuxguitar.audio.ui;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.view.menu.TGViewMenuExtensions;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.util.plugin.TGPlugin;
import app.tuxguitar.util.plugin.TGPluginManager;

/**
 * The Track panel as a plugin of its own (Tools > Plugins: "TuxGuitar Sound Chain panel"), so it can be dropped
 * without a rebuild: connected, it docks the panel, the picked plugin's editor in it, at the right of the main window
 * (TGTrackPanel), maps its toggle (F8 by default, changeable in Tools > Keyboard Shortcuts) and adds View > Sound
 * Chain, greyed while TuxGuitar's audio is off; disconnected, all of it goes. The Sound Chain dialog and the plugins'
 * editors do not depend on it. Glue only; connected on the UI thread.
 */
public class TGSoundChainPanelPlugin implements TGPlugin {

	public static final String MODULE_ID = "tuxguitar-sound-chain-panel";

	private TGTrackPanel panel;
	private TGViewMenuExtensions.Item item;

	public String getModuleId() {
		return MODULE_ID;
	}

	public void connect(TGContext context) {
		this.panel = new TGTrackPanel(context, new TGConfigManager(context, MODULE_ID));
		this.panel.create(audio(context));
		TGToggleSoundChainPanelAction.map(context, this.panel);
		this.item = new TGViewMenuExtensions.Item("audio.sound-chain.panel", TGToggleSoundChainPanelAction.NAME, this.panel::isVisible, () -> audio(context));
		TGViewMenuExtensions.getInstance(context).add(this.item);
		TuxGuitar.getInstance().getItemManager().createMenu();
	}

	public void disconnect(TGContext context) {
		TGViewMenuExtensions.getInstance(context).remove(this.item);
		TGToggleSoundChainPanelAction.unmap(context);
		this.panel.dispose();
		TuxGuitar.getInstance().getItemManager().createMenu();
	}

	// whether TuxGuitar's audio, whose sound chains the panel edits, is on
	private static boolean audio(TGContext context) {
		return TGPluginManager.getInstance(context).isEnabled(TGAudioPlugin.MODULE_ID);
	}
}
