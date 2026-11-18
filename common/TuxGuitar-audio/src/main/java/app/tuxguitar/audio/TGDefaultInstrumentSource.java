package app.tuxguitar.audio;

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
 * TGFallbackNotice) and the log says why, in English.
 */
public final class TGDefaultInstrumentSource implements TGDefaultSource {

	// the id of a default instrument's element: "default-<kind>", apart from any id a sound chain gives
	private static final String ID = "default-";
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
			this.defaults.get(kind).ifPresent(instrument -> this.fellBack(new TGFallback(kind, instrument, TGFallback.Why.NOT_INSTALLED), "it is not installed"));
		}
		return source;
	}

	// a default that could not be loaded plays General MIDI, the user told (the log says why)
	public void notOpened(TGProgramElement source, TGMidiProcessor processor) {
		TGInstrumentKind kind = TGInstrumentKind.valueOf(source.getId().substring(ID.length()).toUpperCase(Locale.ROOT));
		String why = (processor instanceof TGVst3PluginProcessor ? ((TGVst3PluginProcessor) processor).getLoggedProblem() : "no VST3 instrument is run");
		this.defaults.get(kind).ifPresent(instrument -> this.fellBack(new TGFallback(kind, instrument, TGFallback.Why.NOT_LOADED), "it could not be loaded: " + why));
	}

	// told the user once per song (TGFallbackNotice), and logged then
	private void fellBack(TGFallback fallback, String why) {
		if (this.notice.fellBack(fallback, TGDocumentManager.getInstance(this.context).getSong(), now())) {
			LOG.warn("the default instrument " + fallback + " plays General MIDI instead: " + why);
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
		return defaults.get(kind).flatMap(instrument -> find(instrument, scanned))
			.map(plugin -> TGVst3Element.create(ID + kind.name().toLowerCase(Locale.ROOT), plugin)).orElse(null);
	}

	static Optional<TGVst3Plugin> find(TGDefaultInstrument instrument, List<TGVst3Plugin> scanned) {
		return scanned.stream().filter(plugin -> plugin.isInstrument() && plugin.getClassId().equalsIgnoreCase(instrument.getClassId())).findFirst();
	}
}
