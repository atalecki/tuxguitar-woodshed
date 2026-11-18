package app.tuxguitar.audio;

import java.util.List;
import java.util.Locale;

import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * A default instrument that did not play, so a track of its kind played General MIDI instead: which kind, which
 * instrument, and why (the scan did not find it installed, or it was found but could not be loaded). Said in
 * TuxGuitar's language ("Test Drums could not be loaded: drum tracks play General MIDI.").
 */
final class TGFallback {

	enum Why {
		NOT_INSTALLED("audio.default-instrument.not-installed"), NOT_LOADED("audio.default-instrument.not-loaded");

		// the message: "{0} ... : {1} play General MIDI." ({0} the instrument, {1} the kind's tracks)
		private final String message;

		Why(String message) {
			this.message = message;
		}
	}

	// the kind's tracks, as the message names them: "guitar tracks", "drum tracks"...
	private static final String KIND = "audio.default-instrument.kind.";

	private final TGInstrumentKind kind;
	private final TGDefaultInstrument instrument;
	private final Why why;

	TGFallback(TGInstrumentKind kind, TGDefaultInstrument instrument, Why why) {
		this.kind = kind;
		this.instrument = instrument;
		this.why = why;
	}

	String say() {
		String tracks = TGMessagesManager.getProperty(KIND + this.kind.name().toLowerCase(Locale.ROOT));
		return TGMessagesManager.getProperty(this.why.message, new Object[] {this.instrument.getName(), tracks});
	}

	// whether its cause still holds: the kind's default is still that instrument and, not installed, the scan still does
	// not find it
	boolean holds(TGDefaultInstruments defaults, List<TGVst3Plugin> scanned) {
		boolean same = defaults.get(this.kind).map(TGDefaultInstrument::getClassId).filter(this.instrument.getClassId()::equals).isPresent();
		return (same && (this.why == Why.NOT_LOADED || !TGDefaultInstrumentSource.find(this.instrument, scanned).isPresent()));
	}

	// as the log says it, in English: "for drum tracks, Test Drums (<class id>)"
	public String toString() {
		return "for " + this.kind.name().toLowerCase(Locale.ROOT) + " tracks, " + this.instrument.getName() + " (" + this.instrument.getClassId() + ")";
	}

	// the same kind, instrument and reason
	public boolean equals(Object other) {
		return (other instanceof TGFallback && ((TGFallback) other).key().equals(this.key()));
	}

	public int hashCode() {
		return this.key().hashCode();
	}

	private String key() {
		return this.kind + " " + this.why + " " + this.instrument.getClassId();
	}
}
