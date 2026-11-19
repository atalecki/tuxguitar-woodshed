package app.tuxguitar.app.view.dialog.settings.items;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The settings plugins add to Settings > Sound, one per context: each plugin adds how to make its settings (a new one
 * each time the page is shown) when it connects, and removes it when it disconnects.
 */
public final class TGSoundSettingsExtensions {

	private final List<Supplier<TGSoundSettings>> extensions = new CopyOnWriteArrayList<Supplier<TGSoundSettings>>();

	private TGSoundSettingsExtensions() {
	}

	public static TGSoundSettingsExtensions getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGSoundSettingsExtensions.class.getName(), key -> new TGSoundSettingsExtensions());
	}

	public void add(Supplier<TGSoundSettings> extension) {
		this.extensions.add(extension);
	}

	public void remove(Supplier<TGSoundSettings> extension) {
		this.extensions.remove(extension);
	}

	// the settings of each plugin, made anew, in the order the plugins added them
	List<TGSoundSettings> create() {
		return this.extensions.stream().map(Supplier::get).collect(Collectors.toList());
	}
}
