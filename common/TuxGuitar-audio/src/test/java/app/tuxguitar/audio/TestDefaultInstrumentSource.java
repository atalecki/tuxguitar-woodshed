package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

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
}
