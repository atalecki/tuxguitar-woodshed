package app.tuxguitar.audio;

import java.io.File;
import java.util.List;
import java.util.Locale;

import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * A default instrument that did not play as set, for a kind of track: General MIDI played instead, the scan not finding
 * the instrument installed or the instrument found but not loaded; or Woodshed Guitar played without the profile chosen
 * for the kind, its file not found. Said in TuxGuitar's language ("Test Drums could not be loaded: drum tracks play
 * General MIDI.").
 */
final class TGFallback {

	enum Why {
		NOT_INSTALLED("audio.default-instrument.not-installed"), NOT_LOADED("audio.default-instrument.not-loaded"),
		PROFILE_MISSING("audio.default-instrument.profile-missing");

		// the message: "{0} ...: {1} play ..." ({0} the instrument, or the profile's file; {1} the kind's tracks)
		private final String message;

		Why(String message) {
			this.message = message;
		}
	}

	// the kind's tracks, as the message names them: "guitar tracks", "drum tracks"...
	private static final String KIND = "audio.default-instrument.kind.";

	private final TGInstrumentKind kind;
	private final Why why;
	// what did not play as the user names it (the instrument, the profile's file), and what it is (the instrument's class
	// id, the profile's path)
	private final String name;
	private final String key;

	TGFallback(TGInstrumentKind kind, TGDefaultInstrument instrument, Why why) {
		this(kind, why, instrument.getName(), instrument.getClassId());
	}

	private TGFallback(TGInstrumentKind kind, Why why, String name, String key) {
		this.kind = kind;
		this.why = why;
		this.name = name;
		this.key = key;
	}

	// the kind's profile (a path) was not found: Woodshed Guitar played without it
	static TGFallback profileMissing(TGInstrumentKind kind, String profile) {
		return new TGFallback(kind, Why.PROFILE_MISSING, new File(profile).getName(), profile);
	}

	String say() {
		String tracks = TGMessagesManager.getProperty(KIND + this.kind.name().toLowerCase(Locale.ROOT));
		return TGMessagesManager.getProperty(this.why.message, new Object[] {this.name, tracks});
	}

	// whether its cause still holds: the kind's profile is still that file and it is still not there; or the kind's
	// default is still that instrument and, not installed, the scan still does not find it
	boolean holds(TGDefaultInstruments defaults, List<TGVst3Plugin> scanned) {
		if (this.why == Why.PROFILE_MISSING) {
			return (defaults.getProfile(this.kind).filter(this.key::equals).isPresent() && !new File(this.key).isFile());
		}
		boolean same = defaults.get(this.kind).map(TGDefaultInstrument::getClassId).filter(this.key::equals).isPresent();
		return (same && (this.why == Why.NOT_LOADED || !TGDefaultInstrumentSource.find(this.key, scanned).isPresent()));
	}

	// the same kind, instrument and reason
	public boolean equals(Object other) {
		return (other instanceof TGFallback && ((TGFallback) other).key().equals(this.key()));
	}

	public int hashCode() {
		return this.key().hashCode();
	}

	private String key() {
		return this.kind + " " + this.why + " " + this.key;
	}
}
