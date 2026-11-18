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
 * one chosen, else the one TuxGuitar ships (SHIPPED, in profiles next to its own plugins folder); a profile chosen whose
 * file is not there leaves it its dry voice, the user told.
 */
public final class TGDefaultInstrumentSource implements TGDefaultSource {

	// the id of a default instrument's element: "default-<kind>", apart from any id a sound chain gives
	private static final String ID = "default-";
	// the profile TuxGuitar ships, in profiles next to its own plugins folder: an A2 capture of a clean amp
	static final String SHIPPED = "RiffSmith Clean.nam";
	private static final TGLog LOG = TGLog.get(TGDefaultInstrumentSource.class);

	private final TGContext context;
	private volatile TGDefaultInstruments defaults = TGDefaultInstruments.NONE;
	private final TGFallbackNotice notice = new TGFallbackNotice();

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
		this.notice.defaultsChanged();
	}

	public TGDefaultInstruments getDefaults() {
		return this.defaults;
	}

	// a default the scan did not find (once there was a scan) plays General MIDI, the user told
	public TGProgramElement sourceFor(int bank, int program) {
		TGInstrumentKind kind = TGInstrumentKind.of(bank, program);
		TGVst3Hosting hosting = TGVst3Hosting.getInstance(this.context);
		TGProgramElement source = source(kind, this.defaults, hosting.getPlugins());
		if (source == null && hosting.hasScanned()) {
			this.defaults.get(kind).ifPresent(instrument -> this.fellBack(new TGFallback(kind, instrument, TGFallback.Why.NOT_INSTALLED),
				"the default instrument " + described(kind, instrument) + " plays General MIDI instead: it is not installed"));
		}
		if (source != null && isWoodshed(source)) {
			missingProfile(kind, this.defaults).ifPresent(profile -> this.fellBack(TGFallback.profileMissing(kind, profile),
				"the default instrument for " + tracks(kind) + ", Woodshed Guitar, plays without the profile chosen, which was not found: " + profile));
		}
		return source;
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

	// the profile Woodshed Guitar plays for the kind: the one chosen, when its file is there; with none chosen, the one
	// TuxGuitar ships, next to the folder the scan found Woodshed Guitar in, when it is there; else none (its dry voice)
	static Optional<String> profile(TGInstrumentKind kind, TGDefaultInstruments defaults, TGVst3Plugin woodshed) {
		Optional<String> chosen = defaults.getProfile(kind);
		if (chosen.isPresent()) {
			return chosen.filter(path -> new File(path).isFile());
		}
		File shipped = new File(new File(new File(woodshed.getModule()).getAbsoluteFile().getParentFile().getParentFile(), "profiles"), SHIPPED);
		return (shipped.isFile() ? Optional.of(shipped.getPath()) : Optional.<String>empty());
	}

	// the profile chosen for the kind whose file is not there
	private static Optional<String> missingProfile(TGInstrumentKind kind, TGDefaultInstruments defaults) {
		return defaults.getProfile(kind).filter(path -> !new File(path).isFile());
	}

	static Optional<TGVst3Plugin> find(String classId, List<TGVst3Plugin> scanned) {
		return scanned.stream().filter(plugin -> plugin.isInstrument() && plugin.getClassId().equalsIgnoreCase(classId)).findFirst();
	}
}
