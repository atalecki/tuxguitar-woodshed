package app.tuxguitar.audio;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.midi.synth.TGDefaultSource;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.midi.synth.TGMidiProcessor;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGSynthManager;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.util.singleton.TGSingletonUtil;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Plugin;
import app.tuxguitar.vst3.TGVst3PluginProcessor;

/**
 * The default instruments the TuxGuitar Synthesizer's channels play, one per context, installed on its synth manager
 * (TGDefaultSource), so live play and offline renders ask the same one: a channel without an instrument of its own plays
 * the default instrument of its kind (TGInstrumentKind) as the defaults in use say (TGDefaultInstruments: the settings;
 * none until an app gives them), found by its class id in the last VST3 scan; else General MIDI. A default the scan did
 * not find, or one that could not be loaded, plays General MIDI too, and the user is told so quietly (getNotice,
 * TGFallbackNotice) and the log says why, in English. Woodshed Guitar plays the kind's profile (guitar and bass): the
 * one chosen, else the one TuxGuitar ships (shipped, in profiles next to its own plugins folder), which also plays in
 * place of a profile chosen whose file is not there, the user told.
 */
public final class TGDefaultInstrumentSource implements TGDefaultSource {

	// the id of a default instrument's element: "default-<kind>", apart from any id a sound chain gives
	private static final String ID = "default-";
	// the profiles TuxGuitar ships, in profiles next to its own plugins folder: A2 captures of a clean amp (guitar tracks)
	// and of a clean bass rig (bass tracks)
	private static final String SHIPPED = "RiffSmith Clean.nam";
	private static final String SHIPPED_BASS = "RiffSmith Bass Clean.nam";
	private static final TGLog LOG = TGLog.get(TGDefaultInstrumentSource.class);

	private final TGContext context;
	private volatile TGDefaultInstruments defaults = TGDefaultInstruments.NONE;
	private final TGFallbackNotice notice = new TGFallbackNotice();
	// the plugins of the scan the defaults were last tried with (a scan's list stays the same object until the next scan)
	private List<TGVst3Plugin> scan;

	private TGDefaultInstrumentSource(TGContext context) {
		this.context = context;
	}

	// once per context: the synthesizers of the context play its defaults from now on
	public static TGDefaultInstrumentSource install(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGDefaultInstrumentSource.class.getName(), TGDefaultInstrumentSource::installed);
	}

	private static TGDefaultInstrumentSource installed(TGContext context) {
		TGDefaultInstrumentSource source = new TGDefaultInstrumentSource(context);
		TGSynthManager.getInstance(context).addExtension(TGDefaultSource.class, source);
		return source;
	}

	// the defaults channels play from the next time they are sent their parameters (each Play); a default that does not
	// play is told again
	public void use(TGDefaultInstruments defaults) {
		this.defaults = defaults;
		this.notice.tryAgain();
	}

	// the defaults heard at once on the open song, as after a change of a sound chain: with the TuxGuitar Synthesizer as
	// the MIDI Port, playing stops (as when the MIDI Port changes) and the song's channels are sent their parameters
	// again, so they play the defaults from now on; another MIDI Port plays General MIDI: nothing changes
	public void applyNow() {
		MidiPlayer player = MidiPlayer.getInstance(this.context);
		if (player.isOutputPortOpen(TGAudioSession.SYNTHESIZER_PORT_KEY)) {
			stop(player);
			try {
				player.updateChannels();
			} catch (MidiPlayerException e) {
				LOG.warn("the default instruments could not be applied to the song's channels: " + e.getMessage(), e);
			}
		}
	}

	// as when the MIDI Port changes: a paused session stays paused
	private static void stop(MidiPlayer player) {
		if (player.isRunning()) {
			player.stop();
		}
	}

	public TGDefaultInstruments getDefaults() {
		return this.defaults;
	}

	// a default the scan did not find (once there was a scan) plays General MIDI, the user told; so does one that could
	// not be loaded, without being tried again, until the defaults change, the VST3 folders are scanned again or another
	// song is played
	public TGProgramElement sourceFor(int bank, int program) {
		TGInstrumentKind kind = TGInstrumentKind.of(bank, program);
		TGVst3Hosting hosting = TGVst3Hosting.getInstance(this.context);
		TGDefaultInstruments defaults = this.defaults;
		TGProgramElement source = source(kind, defaults, this.scanned(hosting.getPlugins()));
		if (source == null) {
			this.notFound(kind, defaults, hosting.hasScanned());
		} else if (this.notLoaded(kind, defaults)) {
			return null;
		} else if (isWoodshed(source)) {
			this.checkProfile(kind, defaults);
		}
		return source;
	}

	// the plugins of a scan: once it is another scan than the defaults were last tried with, they are tried again
	private synchronized List<TGVst3Plugin> scanned(List<TGVst3Plugin> plugins) {
		if (plugins != this.scan) {
			this.scan = plugins;
			this.notice.tryAgain();
		}
		return plugins;
	}

	private void notFound(TGInstrumentKind kind, TGDefaultInstruments defaults, boolean scanned) {
		if (scanned) {
			defaults.get(kind).ifPresent(instrument -> this.fellBack(new TGFallback(kind, instrument, TGFallback.Why.NOT_INSTALLED),
				"the default instrument " + described(kind, instrument) + " plays General MIDI instead: it is not installed"));
		}
	}

	// whether the kind's default could not be loaded playing this song since it was last tried
	private boolean notLoaded(TGInstrumentKind kind, TGDefaultInstruments defaults) {
		Object song = TGDocumentManager.getInstance(this.context).getSong();
		return defaults.get(kind).filter(instrument -> this.notice.hasFallenBack(new TGFallback(kind, instrument, TGFallback.Why.NOT_LOADED), song)).isPresent();
	}

	private void checkProfile(TGInstrumentKind kind, TGDefaultInstruments defaults) {
		missingProfile(kind, defaults).ifPresent(profile -> this.fellBack(TGFallback.profileMissing(kind, profile, shipped(kind)),
			"the default instrument for " + tracks(kind) + ", Woodshed Guitar, plays the profile TuxGuitar ships, " + shipped(kind)
				+ ", instead of the one chosen, which was not found: " + profile));
	}

	// a default that could not be loaded plays General MIDI, the user told (the log says why)
	public void notOpened(TGProgramElement source, TGMidiProcessor processor) {
		TGInstrumentKind kind = TGInstrumentKind.valueOf(source.getId().substring(ID.length()).toUpperCase(Locale.ROOT));
		String why = (processor instanceof TGVst3PluginProcessor ? ((TGVst3PluginProcessor) processor).getLoggedProblem() : "no VST3 instrument is run");
		this.defaults.get(kind).ifPresent(instrument -> this.fellBack(new TGFallback(kind, instrument, TGFallback.Why.NOT_LOADED),
			"the default instrument " + described(kind, instrument) + " plays General MIDI instead: it could not be loaded: " + why));
	}

	// "for drums tracks, Test Drums (<class id>)", as the log says it
	private static String described(TGInstrumentKind kind, TGDefaultInstrument instrument) {
		return "for " + tracks(kind) + ", " + instrument.getName() + " (" + instrument.getClassId() + ")";
	}

	private static String tracks(TGInstrumentKind kind) {
		return kind.name().toLowerCase(Locale.ROOT) + " tracks";
	}

	// told the user once per song (TGFallbackNotice), and logged then
	private void fellBack(TGFallback fallback, String logged) {
		if (this.notice.fellBack(fallback, TGDocumentManager.getInstance(this.context).getSong(), now())) {
			LOG.warn(logged);
		}
	}

	private static long now() {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
	}

	// that a default instrument did not play, as the status line says it now; null for none
	public String getNotice() {
		return this.notice.getText(now(), this.defaults, TGVst3Hosting.getInstance(this.context).getPlugins());
	}

	// the kind's default instrument as the scan found it, an element of its own id; null (General MIDI) when the kind has
	// none or the scan did not find it as an instrument
	static TGProgramElement source(TGInstrumentKind kind, TGDefaultInstruments defaults, List<TGVst3Plugin> scanned) {
		return defaults.get(kind).flatMap(instrument -> find(instrument.getClassId(), scanned)).map(plugin -> element(kind, plugin, defaults)).orElse(null);
	}

	// the plugin as the kind's default; Woodshed Guitar with the kind's profile, when it has one
	private static TGProgramElement element(TGInstrumentKind kind, TGVst3Plugin plugin, TGDefaultInstruments defaults) {
		TGProgramElement element = TGVst3Element.create(ID + kind.name().toLowerCase(Locale.ROOT), plugin);
		if (isWoodshed(plugin.getClassId())) {
			profile(kind, defaults, plugin).ifPresent(profile -> TGVst3Element.setProfile(element, profile));
		}
		return element;
	}

	private static boolean isWoodshed(String classId) {
		return TGDefaultInstruments.WOODSHED_GUITAR.getClassId().equalsIgnoreCase(classId);
	}

	private static boolean isWoodshed(TGProgramElement source) {
		return isWoodshed(TGVst3Element.getClassId(source));
	}

	// the profile Woodshed Guitar plays for the kind: the one chosen, when its file is there; else (none chosen, or its
	// file gone) the one TuxGuitar ships for the kind, next to the folder the scan found Woodshed Guitar in, when it is
	// there; else none (its dry voice)
	static Optional<String> profile(TGInstrumentKind kind, TGDefaultInstruments defaults, TGVst3Plugin woodshed) {
		return defaults.getProfile(kind).filter(path -> new File(path).isFile()).or(() -> shipped(kind, woodshed));
	}

	private static Optional<String> shipped(TGInstrumentKind kind, TGVst3Plugin woodshed) {
		File shipped = new File(new File(new File(woodshed.getModule()).getAbsoluteFile().getParentFile().getParentFile(), "profiles"), shipped(kind));
		return (shipped.isFile() ? Optional.of(shipped.getPath()) : Optional.<String>empty());
	}

	// the file name of the profile TuxGuitar ships for the kind: bass tracks have their own
	static String shipped(TGInstrumentKind kind) {
		return (kind == TGInstrumentKind.BASS ? SHIPPED_BASS : SHIPPED);
	}

	// the profile chosen for the kind whose file is not there
	private static Optional<String> missingProfile(TGInstrumentKind kind, TGDefaultInstruments defaults) {
		return defaults.getProfile(kind).filter(path -> !new File(path).isFile());
	}

	static Optional<TGVst3Plugin> find(String classId, List<TGVst3Plugin> scanned) {
		return scanned.stream().filter(plugin -> plugin.isInstrument() && plugin.getClassId().equalsIgnoreCase(classId)).findFirst();
	}
}
