package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.StringJoiner;

import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.util.properties.TGPropertiesManager;

/**
 * The default instrument of each kind of channel, kept in TuxGuitar's audio configuration: a VST3 instrument by its class
 * id and name, or General MIDI; and Woodshed Guitar's profile for guitar and bass, the bass one the guitar's until
 * chosen. Read once where it enters, each value checked there: a class id that is not 32 hexadecimal digits, or a
 * blank profile, is none.
 */
class TestDefaultInstruments {

	private static final String SINE = "7A1C0008544753494E45000000000008";
	private static final String CLICK = "7A1C0009544743434C49434B00000009";
	private static final String WOODSHED = "57534847554954415200000000000001";

	private final TGContext context = new TGContext();

	TestDefaultInstruments() {
		TGPropertiesManager.getInstance(this.context).setPropertiesFactory(MapProperties::new);
	}

	private TGConfigManager config() {
		return new TGConfigManager(this.context, "tuxguitar-audio");
	}

	// "KIND=name (class) profile" for each kind; "-" for General MIDI or no profile
	private static String describe(TGDefaultInstruments defaults) {
		StringJoiner kinds = new StringJoiner(", ");
		for (TGInstrumentKind kind : TGInstrumentKind.values()) {
			String instrument = defaults.get(kind).map(chosen -> chosen.getName() + " (" + chosen.getClassId() + ")").orElse("-");
			kinds.add(kind + "=" + instrument + " " + defaults.getProfile(kind).orElse("-"));
		}
		return kinds.toString();
	}

	// the kind's profile, "-" for none
	private static String describe(TGDefaultInstruments defaults, TGInstrumentKind kind) {
		return defaults.getProfile(kind).orElse("-");
	}

	// Woodshed Guitar is the guitar and bass default until another (or General MIDI) is chosen; drums and other tracks
	// play General MIDI until one is chosen
	@Test
	void nothingSetIsWoodshedGuitarForGuitarAndBassAndGeneralMidiForTheOthers() {
		assertEquals("GUITAR=Woodshed Guitar (" + WOODSHED + ") -, BASS=Woodshed Guitar (" + WOODSHED + ") -, DRUMS=- -, OTHER=- -",
			describe(TGDefaultInstruments.read(config())));
	}

	@Test
	void generalMidiChosenForGuitarAndBassIsReadBackAsGeneralMidi() {
		TGConfigManager config = config();
		TGDefaultInstruments.NONE.write(config);
		assertEquals("GUITAR=- -, BASS=- -, DRUMS=- -, OTHER=- -", describe(TGDefaultInstruments.read(config)));
	}

	@Test
	void theDefaultsAreReadBackAsWritten() {
		TGConfigManager config = config();
		TGDefaultInstruments.NONE.with(TGInstrumentKind.DRUMS, new TGDefaultInstrument(CLICK, "Test Click"))
			.with(TGInstrumentKind.OTHER, new TGDefaultInstrument(SINE, "Test Sine"))
			.withProfile(TGInstrumentKind.GUITAR, "C:/profiles/Clean.nam").withProfile(TGInstrumentKind.BASS, "C:/profiles/Bass.nam").write(config);
		assertEquals("GUITAR=- C:/profiles/Clean.nam, BASS=- C:/profiles/Bass.nam, DRUMS=Test Click (" + CLICK + ") -, OTHER=Test Sine (" + SINE + ") -",
			describe(TGDefaultInstruments.read(config)));
	}

	@Test
	void generalMidiWrittenOverAnInstrumentIsReadBackAsGeneralMidi() {
		TGConfigManager config = config();
		TGDefaultInstruments drums = TGDefaultInstruments.NONE.with(TGInstrumentKind.DRUMS, new TGDefaultInstrument(CLICK, "Test Click"));
		drums.with(TGInstrumentKind.OTHER, new TGDefaultInstrument(SINE, "Test Sine")).write(config);
		drums.write(config);
		assertEquals("GUITAR=- -, BASS=- -, DRUMS=Test Click (" + CLICK + ") -, OTHER=- -", describe(TGDefaultInstruments.read(config)));
	}

	// the bass profile is its own: none (the one TuxGuitar ships for bass) until chosen, whatever the guitar's
	@Test
	void theBassProfileIsItsOwn() {
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.withProfile(TGInstrumentKind.GUITAR, "Clean.nam");
		assertEquals("Clean.nam - Bass.nam", describe(defaults, TGInstrumentKind.GUITAR) + " " + describe(defaults, TGInstrumentKind.BASS) + " "
			+ describe(defaults.withProfile(TGInstrumentKind.BASS, "Bass.nam"), TGInstrumentKind.BASS));
	}

	// Settings' Defaults: what the settings never set play
	@Test
	void theBuiltInDefaultsAreWhatNothingSetReads() {
		assertEquals(TGDefaultInstruments.read(config()), TGDefaultInstruments.BUILT_IN);
	}

	// as the settings choose them: General MIDI for a kind, back to the shipped profile with none
	@Test
	void aKindChoosesGeneralMidiAndAProfileGoesBackToNone() {
		TGDefaultInstruments chosen = TGDefaultInstruments.BUILT_IN.withProfile(TGInstrumentKind.GUITAR, "Lead.nam").withGeneralMidi(TGInstrumentKind.BASS);
		assertEquals("GUITAR=Woodshed Guitar (" + WOODSHED + ") Lead.nam, BASS=- -, DRUMS=- -, OTHER=- - | GUITAR=Woodshed Guitar (" + WOODSHED + ") -, BASS=- -, DRUMS=- -, OTHER=- -",
			describe(chosen) + " | " + describe(chosen.withProfile(TGInstrumentKind.GUITAR, " ")));
	}

	// the same instruments by class id (whatever their names) and the same profiles
	@Test
	void defaultsAreEqualWithTheSameInstrumentsAndProfiles() {
		TGDefaultInstruments sine = TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, new TGDefaultInstrument(SINE, "Test Sine"));
		TGDefaultInstruments renamed = TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, new TGDefaultInstrument(SINE, "Sine renamed"));
		assertEquals("true true false false false", sine.equals(renamed) + " " + (sine.hashCode() == renamed.hashCode()) + " " + sine.equals(TGDefaultInstruments.NONE) + " "
			+ sine.equals(sine.withProfile(TGInstrumentKind.GUITAR, "Lead.nam")) + " " + sine.equals("not defaults"));
	}

	// drums and other kinds play no profile
	@Test
	void onlyGuitarAndBassHaveAProfile() {
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.withProfile(TGInstrumentKind.GUITAR, "Clean.nam");
		assertEquals("Clean.nam - -", describe(defaults, TGInstrumentKind.GUITAR) + " " + describe(defaults, TGInstrumentKind.DRUMS) + " " + describe(defaults, TGInstrumentKind.OTHER));
	}

	@Test
	void aGarbledClassIdOrABlankProfileIsNone() {
		TGConfigManager config = config();
		config.setValue("audio.default-instrument.other.class", "not a class id");
		config.setValue("audio.default-instrument.other.name", "Test Sine");
		config.setValue("audio.default-instrument.drums.class", SINE + "0");
		config.setValue("audio.default-instrument.guitar.class", "");
		config.setValue("audio.default-instrument.guitar.profile", "  ");
		config.setValue("audio.default-instrument.bass.class", CLICK);
		config.setValue("audio.default-instrument.bass.name", "Test Click");
		config.setValue("audio.default-instrument.bass.profile", "Bass.nam");
		assertEquals("GUITAR=- -, BASS=Test Click (" + CLICK + ") Bass.nam, DRUMS=- -, OTHER=- -", describe(TGDefaultInstruments.read(config)));
	}

	// a class id in lower case is the same class; a missing name is named by the class id
	@Test
	void aClassIdInLowerCaseIsTakenAndAMissingNameIsTheClassId() {
		TGConfigManager config = config();
		TGDefaultInstruments.NONE.write(config);
		config.setValue("audio.default-instrument.other.class", SINE.toLowerCase());
		config.setValue("audio.default-instrument.other.name", "");
		assertEquals("GUITAR=- -, BASS=- -, DRUMS=- -, OTHER=" + SINE + " (" + SINE + ") -", describe(TGDefaultInstruments.read(config)));
	}
}
