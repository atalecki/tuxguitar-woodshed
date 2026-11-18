package app.tuxguitar.audio.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.TGActionAdapterManager;
import app.tuxguitar.app.view.dialog.settings.items.TGSoundSettings;
import app.tuxguitar.app.view.dialog.settings.items.TGSoundSettingsExtensions;
import app.tuxguitar.app.view.main.TGStatusLine;
import app.tuxguitar.audio.TGAudioSettings;
import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.audio.TGDefaultInstruments;
import app.tuxguitar.audio.TGLiveAudio;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGStatusHint;
import app.tuxguitar.event.TGEventListener;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGSynchronizer;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.util.plugin.TGPlugin;

/**
 * TuxGuitar's audio on the desktop: at startup (before the MIDI Port opens) it installs the headless audio features
 * (TGLiveAudio.install: takes, note entry, saving, the recording veto, which also refuses what the desktop disables
 * while playing), maps the audio actions, loads the native audio backend from the native folder (VST3 hosting too) and
 * opens the devices of the settings, so the TuxGuitar Synthesizer plays through them, and gives it the default
 * instruments of the settings (TGDefaultInstruments), chosen in Settings > Sound (TGDefaultInstrumentsSettings). Then,
 * off the UI thread, it scans the VST3 folders and applies the default instruments at once (TGLiveAudio.startPlugins),
 * every 100 ms asks the plugins kept open that nothing else calls (so their editors' gestures come back), every 50 ms
 * has the UI thread drain the gestures of plugins' editors into the song, and every 250 ms reads the quiet hint
 * (TGStatusHint: why Record was just refused, else a plugin Play stopped waiting for), which the main window's status
 * line shows. Glue only:
 * every decision is in common/TuxGuitar-audio, which is tested.
 */
public class TGAudioPlugin implements TGPlugin {

	public static final String MODULE_ID = "tuxguitar-audio";

	private static final TGLog LOG = TGLog.get(TGAudioPlugin.class);

	private static final long DRAIN_MILLISECONDS = 50;
	// how often the plugins kept open that nothing else calls (their element runs in no render) are asked
	private static final long POLL_MILLISECONDS = 100;
	// how often the status line's hint is read
	private static final long HINT_MILLISECONDS = 250;

	private ScheduledExecutorService plugins;
	// tells the plugins TuxGuitar's language each time it changes (Woodshed Guitar follows it)
	private TGEventListener language;
	// the default instruments in Settings > Sound
	private Supplier<TGSoundSettings> settings;

	public String getModuleId() {
		return MODULE_ID;
	}

	public void connect(TGContext context) {
		TGLiveAudio audio = TGLiveAudio.install(context, TGActionAdapterManager.getInstance(context).getDisableOnPlayInterceptor()::containsActionId);
		TGAudioActions.map(context);
		audio.load(TGLiveAudio.nativeFolder(System.getProperties()));
		open(context, TGAudioSettings.read(config(context)));
		TGDefaultInstrumentSource.install(context).use(TGDefaultInstruments.read(config(context)));
		this.settings = () -> new TGDefaultInstrumentsSettings(context);
		TGSoundSettingsExtensions.getInstance(context).add(this.settings);
		this.plugins = scheduler("TuxGuitar VST3 plugins");
		TGLivePlugins.getInstance(context).setFrames(new TGConfigEditorFrames(config(context)));
		this.language = event -> this.plugins.execute(TGLivePlugins.getInstance(context)::languageChanged);
		TuxGuitar.getInstance().getLanguageManager().addLoader(this.language);
		this.plugins.execute(() -> LOG.info(audio.startPlugins()));
		this.plugins.scheduleWithFixedDelay(() -> drainLater(context), DRAIN_MILLISECONDS, DRAIN_MILLISECONDS, TimeUnit.MILLISECONDS);
		this.plugins.scheduleWithFixedDelay(TGLivePlugins.getInstance(context)::poll, POLL_MILLISECONDS, POLL_MILLISECONDS, TimeUnit.MILLISECONDS);
		this.plugins.scheduleWithFixedDelay(() -> showHintLater(context), HINT_MILLISECONDS, HINT_MILLISECONDS, TimeUnit.MILLISECONDS);
	}

	public void disconnect(TGContext context) {
		TuxGuitar.getInstance().getLanguageManager().removeLoader(this.language);
		this.plugins.shutdownNow();
		TGSoundSettingsExtensions.getInstance(context).remove(this.settings);
		TGLiveAudio.getInstance(context).close();
	}

	static TGConfigManager config(TGContext context) {
		return new TGConfigManager(context, MODULE_ID);
	}

	// the devices of the settings; the log says why there is none
	static void open(TGContext context, TGAudioSettings settings) {
		TGLiveAudio audio = TGLiveAudio.getInstance(context);
		audio.open(settings);
		LOG.info(audio.describe());
	}

	// one daemon thread running tasks in turn
	static ScheduledExecutorService scheduler(String name) {
		return Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, name);
			thread.setDaemon(true);
			return thread;
		});
	}

	// read here (it asks the plugins' hosts), shown on the UI thread
	private static void showHintLater(TGContext context) {
		String hint = TGStatusHint.getInstance(context).getText();
		TGSynchronizer.getInstance(context).executeLater(() -> TGStatusLine.getInstance(context).show(hint));
	}

	// the gestures ended in plugins' editors change the song on the UI thread
	private static void drainLater(TGContext context) {
		TGLivePlugins live = TGLivePlugins.getInstance(context);
		if (live.hasEnded()) {
			TGSynchronizer.getInstance(context).executeLater(live::drain);
		}
	}
}
