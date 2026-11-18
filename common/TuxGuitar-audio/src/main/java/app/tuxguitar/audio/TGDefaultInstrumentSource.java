package app.tuxguitar.audio;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import app.tuxguitar.midi.synth.TGDefaultSource;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGSynthManager;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The default instruments the TuxGuitar Synthesizer's channels play, one per context, installed on its synth manager
 * (TGDefaultSource), so live play and offline renders ask the same one: a channel without an instrument of its own plays
 * the default instrument of its kind (TGInstrumentKind) as the defaults in use say (TGDefaultInstruments: the settings;
 * none until an app gives them), found by its class id in the last VST3 scan; else General MIDI.
 */
public final class TGDefaultInstrumentSource implements TGDefaultSource {

	// the id of a default instrument's element: "default-<kind>", apart from any id a sound chain gives
	private static final String ID = "default-";

	private final TGContext context;
	private volatile TGDefaultInstruments defaults = TGDefaultInstruments.NONE;

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

	// the defaults channels play from the next time they are sent their parameters (each Play)
	public void use(TGDefaultInstruments defaults) {
		this.defaults = defaults;
	}

	public TGDefaultInstruments getDefaults() {
		return this.defaults;
	}

	public TGProgramElement sourceFor(int bank, int program) {
		return source(TGInstrumentKind.of(bank, program), this.defaults, TGVst3Hosting.getInstance(this.context).getPlugins());
	}

	// the kind's default instrument as the scan found it, an element of its own id; null (General MIDI) when the kind has
	// none or the scan did not find it as an instrument
	static TGProgramElement source(TGInstrumentKind kind, TGDefaultInstruments defaults, List<TGVst3Plugin> scanned) {
		return defaults.get(kind).flatMap(instrument -> find(instrument, scanned))
			.map(plugin -> TGVst3Element.create(ID + kind.name().toLowerCase(Locale.ROOT), plugin)).orElse(null);
	}

	private static Optional<TGVst3Plugin> find(TGDefaultInstrument instrument, List<TGVst3Plugin> scanned) {
		return scanned.stream().filter(plugin -> plugin.isInstrument() && plugin.getClassId().equalsIgnoreCase(instrument.getClassId())).findFirst();
	}
}
