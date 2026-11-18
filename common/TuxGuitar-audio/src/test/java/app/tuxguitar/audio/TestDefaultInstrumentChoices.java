package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * What the settings offer to choose the default instruments from, and what the user chose: per kind General MIDI, the
 * instruments the scan found (no effect), and the one in use when the scan did not find it; a profile only for guitar
 * and bass while Woodshed Guitar is their choice, the shipped one until a file is chosen. Labels in English.
 */
class TestDefaultInstrumentChoices {

	private static final String SINE = "7A1C0008544753494E45000000000008";
	private static final String WOODSHED = "57534847554954415200000000000001";
	private static final List<TGVst3Plugin> SCANNED = Arrays.asList(new TGVst3Plugin("C:/VST3/Test Gain.vst3", "7A1C0001544747414E00000000000001", "Test Gain", false),
		new TGVst3Plugin("C:/VST3/Test Sine.vst3", SINE.toLowerCase(), "Test Sine", true), new TGVst3Plugin("C:/TuxGuitar/vst3/Woodshed Guitar.vst3", WOODSHED, "Woodshed Guitar", true));
	private static final TGDefaultInstrument MISSING = TGDefaultInstrument.parse("0123456789ABCDEF0123456789ABCDEF", "Missing Piano").get();

	private TGDefaultInstrumentChoices choices = new TGDefaultInstrumentChoices(TGDefaultInstruments.BUILT_IN, SCANNED);

	// the kind's choices, the one chosen in brackets
	private String offered(TGInstrumentKind kind) {
		return TuxGuitarMessages.english(() -> this.choices.choices(kind).stream()
			.map(choice -> (choice.equals(this.choices.chosen(kind)) ? "[" + choice.getLabel() + "]" : choice.getLabel())).collect(Collectors.joining(", ")));
	}

	// the kind's choice of the label (in English)
	private TGDefaultInstrumentChoices.Choice choice(TGInstrumentKind kind, String label) {
		String index = TuxGuitarMessages.english(() -> Integer.toString(this.choices.choices(kind).stream().map(TGDefaultInstrumentChoices.Choice::getLabel).collect(Collectors.toList()).indexOf(label)));
		return this.choices.choices(kind).get(Integer.parseInt(index));
	}

	@Test
	void eachKindChoosesGeneralMidiOrAnInstrumentTheScanFound() {
		assertEquals("General MIDI, Test Sine, [Woodshed Guitar] | [General MIDI], Test Sine, Woodshed Guitar", offered(TGInstrumentKind.GUITAR) + " | " + offered(TGInstrumentKind.DRUMS));
	}

	// opening the settings never loses a default set on another computer
	@Test
	void theDefaultInUseStaysAChoiceWhenTheScanDidNotFindIt() {
		this.choices = new TGDefaultInstrumentChoices(TGDefaultInstruments.NONE.with(TGInstrumentKind.OTHER, MISSING), SCANNED);
		assertEquals("General MIDI, Test Sine, Woodshed Guitar, [Missing Piano (not installed)]", offered(TGInstrumentKind.OTHER));
	}

	@Test
	void aChoiceChangesOnlyItsKind() {
		this.choices.choose(TGInstrumentKind.OTHER, choice(TGInstrumentKind.OTHER, "Test Sine"));
		this.choices.choose(TGInstrumentKind.BASS, choice(TGInstrumentKind.BASS, "General MIDI"));
		assertEquals(TGDefaultInstruments.BUILT_IN.with(TGInstrumentKind.OTHER, TGDefaultInstrument.parse(SINE, "Test Sine").get()).withGeneralMidi(TGInstrumentKind.BASS),
			this.choices.getChosen());
	}

	// a profile is Woodshed Guitar's, for guitar and bass tracks
	@Test
	void aProfileIsChosenOnlyForGuitarOrBassPlayingWoodshedGuitar() {
		boolean guitar = this.choices.takesProfile(TGInstrumentKind.GUITAR);
		this.choices.choose(TGInstrumentKind.BASS, choice(TGInstrumentKind.BASS, "Test Sine"));
		this.choices.choose(TGInstrumentKind.OTHER, choice(TGInstrumentKind.OTHER, "Woodshed Guitar"));
		assertEquals("true false false true false", guitar + " " + this.choices.takesProfile(TGInstrumentKind.BASS) + " " + this.choices.takesProfile(TGInstrumentKind.OTHER)
			+ " " + TGDefaultInstrumentChoices.hasProfile(TGInstrumentKind.BASS) + " " + TGDefaultInstrumentChoices.hasProfile(TGInstrumentKind.DRUMS));
	}

	// "profile (shipped?)" for guitar and bass
	private String profiles() {
		return TuxGuitarMessages.english(() -> this.choices.profile(TGInstrumentKind.GUITAR) + " (" + this.choices.isShipped(TGInstrumentKind.GUITAR) + ") | "
			+ this.choices.profile(TGInstrumentKind.BASS) + " (" + this.choices.isShipped(TGInstrumentKind.BASS) + ")");
	}

	// the one TuxGuitar ships for the kind until a file is chosen; back to it on choosing none
	@Test
	void theProfileIsTheShippedOneUntilAFileIsChosen() {
		String shipped = profiles();
		this.choices.chooseProfile(TGInstrumentKind.GUITAR, "C:/profiles/Lead.nam");
		String lead = profiles();
		this.choices.chooseProfile(TGInstrumentKind.GUITAR, null);
		assertEquals("RiffSmith Clean (shipped with TuxGuitar) (true) | RiffSmith Bass Clean (shipped with TuxGuitar) (true)"
			+ " / C:/profiles/Lead.nam (false) | RiffSmith Bass Clean (shipped with TuxGuitar) (true) / " + shipped, shipped + " / " + lead + " / " + profiles());
	}
}
