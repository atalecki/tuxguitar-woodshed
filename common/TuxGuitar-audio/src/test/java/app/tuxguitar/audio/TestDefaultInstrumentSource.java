package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The source a channel without an instrument of its own plays: its kind's default instrument, found in the last scan by
 * its class id wherever its module is, as a VST3 instrument element of an id of its own ("default-<kind>"); none
 * (General MIDI) when its kind has none, or the scan did not find it as an instrument.
 */
class TestDefaultInstrumentSource {

	private static final String SINE = "7A1C0008544753494E45000000000008";
	private static final String GAIN = "7A1C0001544747414E00000000000001";
	private static final String WOODSHED = "57534847554954415200000000000001";

	private static final List<TGVst3Plugin> SCANNED = Arrays.asList(new TGVst3Plugin("C:/VST3/Test Sine.vst3", SINE, "Test Sine", true),
		new TGVst3Plugin("C:/VST3/Test Gain.vst3", GAIN, "Test Gain", false));

	private static TGDefaultInstrument instrument(String classId, String name) {
		return TGDefaultInstrument.parse(classId, name).get();
	}

	// "id type {parameters}", or "none"
	private static String describe(TGProgramElement source) {
		return (source != null ? source.getId() + " " + source.getType() + " " + new TreeMap<String, String>(source.getParameters()) : "none");
	}

	@Test
	void theKindsDefaultIsTheInstrumentTheScanFoundByItsClassId() {
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, instrument(SINE, "Sine as it was named"));
		assertEquals("default-other vst3-instrument {vst3.class=" + SINE + ", vst3.module=C:/VST3/Test Sine.vst3, vst3.name=Test Sine}",
			describe(TGDefaultInstrumentSource.source(TGInstrumentKind.OTHER, defaults, SCANNED)));
	}

	// the scan names the class id in any case
	@Test
	void theClassIdIsFoundInAnyCase() {
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.with(TGInstrumentKind.DRUMS, instrument(SINE, "Test Sine"));
		List<TGVst3Plugin> lower = Arrays.asList(new TGVst3Plugin("C:/VST3/Test Sine.vst3", SINE.toLowerCase(), "Test Sine", true));
		assertEquals("default-drums", TGDefaultInstrumentSource.source(TGInstrumentKind.DRUMS, defaults, lower).getId());
	}

	@Test
	void aKindWithoutADefaultPlaysGeneralMidi() {
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, instrument(SINE, "Test Sine"));
		assertEquals("none default-other", describe(TGDefaultInstrumentSource.source(TGInstrumentKind.GUITAR, defaults, SCANNED)) + " "
			+ TGDefaultInstrumentSource.source(TGInstrumentKind.OTHER, defaults, SCANNED).getId());
	}

	// not installed on this computer, or an effect of that class id
	@Test
	void aDefaultTheScanDidNotFindAsAnInstrumentPlaysGeneralMidi() {
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, instrument("0123456789ABCDEF0123456789ABCDEF", "Missing Piano"))
			.with(TGInstrumentKind.GUITAR, instrument(GAIN, "Test Gain")).with(TGInstrumentKind.BASS, instrument(SINE, "Test Sine"));
		assertEquals("none none default-bass", describe(TGDefaultInstrumentSource.source(TGInstrumentKind.OTHER, defaults, SCANNED)) + " "
			+ describe(TGDefaultInstrumentSource.source(TGInstrumentKind.GUITAR, defaults, SCANNED)) + " "
			+ TGDefaultInstrumentSource.source(TGInstrumentKind.BASS, defaults, SCANNED).getId());
	}

	// before the first scan no plugin is known, installed or not: General MIDI plays, and nothing is said
	@Test
	void beforeTheFirstScanADefaultNotFoundIsNotTold() {
		TGContext context = new TGContext();
		TGDefaultInstrumentSource source = TGDefaultInstrumentSource.install(context);
		source.use(TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, instrument(SINE, "Test Sine")));
		assertEquals("none null", describe(source.sourceFor(0, 25)) + " " + source.getNotice());
	}

	// Settings' OK: defaults other than those in use are used (and applied at once: with no MIDI Port open, nothing to
	// play again); the same ones change nothing (playing goes on)
	@Test
	void defaultsAreAppliedOnlyWhenTheyChanged() {
		TGDefaultInstrumentSource source = TGDefaultInstrumentSource.install(new TGContext());
		TGDefaultInstruments sine = TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, instrument(SINE, "Test Sine"));
		boolean changed = source.apply(sine);
		boolean same = source.apply(TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, instrument(SINE, "Test Sine")));
		assertEquals("true false true", changed + " " + same + " " + (source.getDefaults() == sine));
	}

	// the Sound Chain dialog's source row: before the first scan, General MIDI plays a default not found yet
	@Test
	void beforeTheFirstScanGeneralMidiPlaysUntilTheScanIsDone() {
		TGDefaultInstrumentSource source = TGDefaultInstrumentSource.install(new TGContext());
		source.use(TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, instrument(SINE, "Test Sine")));
		assertEquals("General MIDI, Steel String Acoustic Guitar (the VST3 plugins are not scanned yet)",
			TuxGuitarMessages.english(() -> source.play(0, 25).say("Steel String Acoustic Guitar")));
	}

	// Woodshed Guitar where TuxGuitar installs it: its own plugins folder (vst3), the profiles it ships next to it
	private static TGVst3Plugin woodshed(File install) {
		return new TGVst3Plugin(new File(new File(install, "vst3"), "Woodshed Guitar.vst3").getPath(), WOODSHED, "Woodshed Guitar", true);
	}

	private static File file(File folder, String name) throws IOException {
		File file = new File(folder, name);
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), new byte[] {'{', '}'});
		return file;
	}

	// the profile of the kind's default Woodshed Guitar, "-" for none
	private static String profile(TGInstrumentKind kind, TGDefaultInstruments defaults, TGVst3Plugin plugin) {
		TGProgramElement source = TGDefaultInstrumentSource.source(kind, defaults, Arrays.asList(plugin));
		return source.getParameters().getOrDefault("vst3.profile", "-");
	}

	// the one chosen for the kind; bass, until chosen, the one TuxGuitar ships for bass, not the guitar's
	@Test
	void woodshedGuitarPlaysTheProfileChosenForItsKind(@TempDir File folder) throws IOException {
		File clean = file(folder, "Clean.nam");
		File bass = file(folder, "Bass.nam");
		File shippedBass = file(new File(folder, "profiles"), "RiffSmith Bass Clean.nam");
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, TGDefaultInstruments.WOODSHED_GUITAR)
			.with(TGInstrumentKind.BASS, TGDefaultInstruments.WOODSHED_GUITAR).withProfile(TGInstrumentKind.GUITAR, clean.getPath());
		String guitar = profile(TGInstrumentKind.GUITAR, defaults, woodshed(folder));
		String notChosen = profile(TGInstrumentKind.BASS, defaults, woodshed(folder));
		assertEquals(clean + " " + shippedBass.getAbsoluteFile() + " " + bass, guitar + " " + notChosen + " "
			+ profile(TGInstrumentKind.BASS, defaults.withProfile(TGInstrumentKind.BASS, bass.getPath()), woodshed(folder)));
	}

	// the profiles TuxGuitar ships: one for guitar tracks, one for bass tracks
	@Test
	void tuxGuitarShipsAProfileForGuitarAndOneForBass() {
		assertEquals("RiffSmith Clean.nam RiffSmith Bass Clean.nam", TGDefaultInstrumentSource.shipped(TGInstrumentKind.GUITAR) + " "
			+ TGDefaultInstrumentSource.shipped(TGInstrumentKind.BASS));
	}

	// none chosen, or the one chosen whose file is gone: the one TuxGuitar ships, when it is there (else none: its dry
	// voice)
	@Test
	void withNoProfileChosenOrItsFileGoneWoodshedGuitarPlaysTheOneTuxGuitarShips(@TempDir File folder) throws IOException {
		TGDefaultInstruments defaults = TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, TGDefaultInstruments.WOODSHED_GUITAR);
		TGDefaultInstruments gone = defaults.withProfile(TGInstrumentKind.GUITAR, new File(folder, "Gone.nam").getPath());
		String without = profile(TGInstrumentKind.GUITAR, gone, woodshed(folder));
		File shipped = file(new File(folder, "profiles"), "RiffSmith Clean.nam");
		assertEquals("- " + shipped.getAbsoluteFile() + " " + shipped.getAbsoluteFile(), without + " " + profile(TGInstrumentKind.GUITAR, defaults, woodshed(folder)) + " "
			+ profile(TGInstrumentKind.GUITAR, gone, woodshed(folder)));
	}

	// a profile is Woodshed Guitar's: another instrument chosen as the guitars' default is not given one
	@Test
	void anotherInstrumentIsGivenNoProfile(@TempDir File folder) throws IOException {
		File clean = file(folder, "Clean.nam");
		TGDefaultInstruments sine = TGDefaultInstruments.NONE.with(TGInstrumentKind.GUITAR, instrument(SINE, "Test Sine")).withProfile(TGInstrumentKind.GUITAR, clean.getPath());
		TGDefaultInstruments woodshed = sine.with(TGInstrumentKind.GUITAR, TGDefaultInstruments.WOODSHED_GUITAR);
		assertEquals("- " + clean, profile(TGInstrumentKind.GUITAR, sine, SCANNED.get(0)) + " " + profile(TGInstrumentKind.GUITAR, woodshed, woodshed(folder)));
	}
}
