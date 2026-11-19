package app.tuxguitar.audio;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.util.configuration.TGConfigManager;

/**
 * The default instrument of each kind of channel (TGInstrumentKind), which a channel without an instrument of its own
 * plays: a VST3 instrument, or none (General MIDI); and, for guitar and bass, Woodshed Guitar's profile (a NAM capture's
 * path), each kind its own (none: the one TuxGuitar ships for the kind). A machine setting, kept in TuxGuitar's audio
 * configuration (KEY.<kind>.class, .name, .profile), read once where it enters: a class id that is not one, or a blank
 * profile, is none; a kind never set plays its built-in default: Woodshed Guitar, TuxGuitar's own instrument, for guitar and bass,
 * General MIDI for drums and other tracks (General MIDI chosen is kept as an empty class id). Immutable.
 */
public final class TGDefaultInstruments {

	public static final String KEY = "audio.default-instrument.";
	// Woodshed Guitar, the VST3 instrument TuxGuitar ships, by its class id
	public static final TGDefaultInstrument WOODSHED_GUITAR = new TGDefaultInstrument("57534847554954415200000000000001", "Woodshed Guitar");
	public static final TGDefaultInstruments NONE = new TGDefaultInstruments(new EnumMap<TGInstrumentKind, TGDefaultInstrument>(TGInstrumentKind.class),
		new EnumMap<TGInstrumentKind, String>(TGInstrumentKind.class));
	// what a computer whose settings never set them plays (Settings' Defaults)
	public static final TGDefaultInstruments BUILT_IN = NONE.with(TGInstrumentKind.GUITAR, WOODSHED_GUITAR).with(TGInstrumentKind.BASS, WOODSHED_GUITAR);

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
		String classId = config.getStringValue(key(kind, "class"));
		Optional<TGDefaultInstrument> instrument = (classId != null ? TGDefaultInstrument.parse(classId, config.getStringValue(key(kind, "name"))) : builtIn(kind));
		TGDefaultInstruments read = instrument.map(chosen -> defaults.with(kind, chosen)).orElse(defaults);
		return read.withProfile(kind, config.getStringValue(key(kind, "profile")));
	}

	// the kind's default when none was ever set: Woodshed Guitar for guitar and bass
	private static Optional<TGDefaultInstrument> builtIn(TGInstrumentKind kind) {
		return (hasProfile(kind) ? Optional.of(WOODSHED_GUITAR) : Optional.<TGDefaultInstrument>empty());
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

	// these defaults, the kind playing General MIDI
	public TGDefaultInstruments withGeneralMidi(TGInstrumentKind kind) {
		Map<TGInstrumentKind, TGDefaultInstrument> instruments = new EnumMap<TGInstrumentKind, TGDefaultInstrument>(this.instruments);
		instruments.remove(kind);
		return new TGDefaultInstruments(instruments, this.profiles);
	}

	// these defaults, the kind's profile that one (blank: none, the one TuxGuitar ships); only guitar and bass have one
	public TGDefaultInstruments withProfile(TGInstrumentKind kind, String profile) {
		if (!hasProfile(kind)) {
			return this;
		}
		Map<TGInstrumentKind, String> profiles = new EnumMap<TGInstrumentKind, String>(this.profiles);
		profiles.remove(kind);
		Optional.ofNullable(profile).map(String::trim).filter(path -> !path.isEmpty()).ifPresent(path -> profiles.put(kind, path));
		return new TGDefaultInstruments(this.instruments, profiles);
	}

	public static boolean hasProfile(TGInstrumentKind kind) {
		return (kind == TGInstrumentKind.GUITAR || kind == TGInstrumentKind.BASS);
	}

	// none: General MIDI
	public Optional<TGDefaultInstrument> get(TGInstrumentKind kind) {
		return Optional.ofNullable(this.instruments.get(kind));
	}

	// the profile chosen for the kind; none until one is chosen for it
	public Optional<String> getProfile(TGInstrumentKind kind) {
		return Optional.ofNullable(this.profiles.get(kind));
	}

	// the same instruments (by class id) and profiles
	public boolean equals(Object other) {
		return (other instanceof TGDefaultInstruments && ((TGDefaultInstruments) other).instruments.equals(this.instruments)
			&& ((TGDefaultInstruments) other).profiles.equals(this.profiles));
	}

	public int hashCode() {
		return Objects.hash(this.instruments, this.profiles);
	}
}
