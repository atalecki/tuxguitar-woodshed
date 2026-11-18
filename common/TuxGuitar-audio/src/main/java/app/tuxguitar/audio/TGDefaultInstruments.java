package app.tuxguitar.audio;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.util.configuration.TGConfigManager;

/**
 * The default instrument of each kind of channel (TGInstrumentKind), which a channel without an instrument of its own
 * plays: a VST3 instrument, or none (General MIDI); and, for guitar and bass, Woodshed Guitar's profile (a NAM capture's
 * path), the bass one the guitar's until it is chosen. A machine setting, kept in TuxGuitar's audio configuration
 * (KEY.<kind>.class, .name, .profile), read once where it enters: a class id that is not one, or a blank profile, is
 * none. Immutable.
 */
public final class TGDefaultInstruments {

	public static final String KEY = "audio.default-instrument.";
	public static final TGDefaultInstruments NONE = new TGDefaultInstruments(new EnumMap<TGInstrumentKind, TGDefaultInstrument>(TGInstrumentKind.class),
		new EnumMap<TGInstrumentKind, String>(TGInstrumentKind.class));

	private final Map<TGInstrumentKind, TGDefaultInstrument> instruments;
	private final Map<TGInstrumentKind, String> profiles;

	private TGDefaultInstruments(Map<TGInstrumentKind, TGDefaultInstrument> instruments, Map<TGInstrumentKind, String> profiles) {
		this.instruments = instruments;
		this.profiles = profiles;
	}

	private static String key(TGInstrumentKind kind, String part) {
		return KEY + kind.name().toLowerCase(Locale.ROOT) + "." + part;
	}

	public static TGDefaultInstruments read(TGConfigManager config) {
		TGDefaultInstruments defaults = NONE;
		for (TGInstrumentKind kind : TGInstrumentKind.values()) {
			defaults = read(config, kind, defaults);
		}
		return defaults;
	}

	private static TGDefaultInstruments read(TGConfigManager config, TGInstrumentKind kind, TGDefaultInstruments defaults) {
		TGDefaultInstruments read = TGDefaultInstrument.parse(config.getStringValue(key(kind, "class")), config.getStringValue(key(kind, "name")))
			.map(instrument -> defaults.with(kind, instrument)).orElse(defaults);
		return read.withProfile(kind, config.getStringValue(key(kind, "profile")));
	}

	// every kind: none written as an empty class id
	public void write(TGConfigManager config) {
		for (TGInstrumentKind kind : TGInstrumentKind.values()) {
			Optional<TGDefaultInstrument> instrument = this.get(kind);
			config.setValue(key(kind, "class"), instrument.map(TGDefaultInstrument::getClassId).orElse(""));
			config.setValue(key(kind, "name"), instrument.map(TGDefaultInstrument::getName).orElse(""));
			config.setValue(key(kind, "profile"), this.profiles.getOrDefault(kind, ""));
		}
	}

	// these defaults, the kind's instrument that one
	public TGDefaultInstruments with(TGInstrumentKind kind, TGDefaultInstrument instrument) {
		Map<TGInstrumentKind, TGDefaultInstrument> instruments = new EnumMap<TGInstrumentKind, TGDefaultInstrument>(this.instruments);
		instruments.put(kind, instrument);
		return new TGDefaultInstruments(instruments, this.profiles);
	}

	// these defaults, the kind's profile that one (blank: none); only guitar and bass have one
	public TGDefaultInstruments withProfile(TGInstrumentKind kind, String profile) {
		if (profile == null || profile.trim().isEmpty() || !hasProfile(kind)) {
			return this;
		}
		Map<TGInstrumentKind, String> profiles = new EnumMap<TGInstrumentKind, String>(this.profiles);
		profiles.put(kind, profile.trim());
		return new TGDefaultInstruments(this.instruments, profiles);
	}

	private static boolean hasProfile(TGInstrumentKind kind) {
		return (kind == TGInstrumentKind.GUITAR || kind == TGInstrumentKind.BASS);
	}

	// none: General MIDI
	public Optional<TGDefaultInstrument> get(TGInstrumentKind kind) {
		return Optional.ofNullable(this.instruments.get(kind));
	}

	// the profile chosen for the kind; bass, until one is chosen for it, the guitar's
	public Optional<String> getProfile(TGInstrumentKind kind) {
		String profile = this.profiles.get(kind);
		if (profile == null && kind == TGInstrumentKind.BASS) {
			profile = this.profiles.get(TGInstrumentKind.GUITAR);
		}
		return Optional.ofNullable(profile);
	}
}
