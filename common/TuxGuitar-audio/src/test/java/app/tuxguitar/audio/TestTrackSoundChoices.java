package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.impl.GervillProcessorFactory;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * What the Track panel offers in its drop-downs, and which is picked: on VST3, the default instrument of the track's
 * kind, then each VST3 instrument the scan found (and the track's own instrument when the scan did not find it); on
 * MIDI, General MIDI's 128 programs (a percussion channel's numbered, as the channel settings name them). A name too
 * long for the panel ends in "…".
 */
class TestTrackSoundChoices {

	private static final TGVst3Plugin SINE = new TGVst3Plugin("sine.vst3", "7A1C0008544753494E45000000000008", "Test Sine", true);
	private static final TGVst3Plugin TONE = new TGVst3Plugin("tone.vst3", "7A1C0008544754F4E450000000000009", "Test Tone", true);
	private static final TGVst3Plugin GONE = new TGVst3Plugin("gone.vst3", "0123456789ABCDEF0123456789ABCDEF", "Gone", true);
	private static final List<TGVst3Plugin> SCANNED = Arrays.asList(SINE, TONE);

	private static String names(List<TGVst3Plugin> choices) {
		return choices.stream().map(choice -> (choice != null ? choice.getName() : "default")).collect(Collectors.joining(", "));
	}

	// the default first (no plugin), then the scan's; the track's own instrument the scan did not find, last, so it
	// shows as the one picked
	@Test
	void theDefaultFirstThenTheScansInstrumentsThenTheTracksOwnNotFound() {
		List<TGVst3Plugin> gone = TGTrackSoundChoices.instruments(TGVst3Element.create("source", GONE), SCANNED);
		assertEquals("default, Test Sine, Test Tone | default, Test Sine, Test Tone | default, Test Sine, Test Tone, Gone (instrument true)",
			names(TGTrackSoundChoices.instruments(TGSoundChain.source(GervillProcessorFactory.TYPE), SCANNED)) + " | "
			+ names(TGTrackSoundChoices.instruments(TGVst3Element.create("source", TONE), SCANNED)) + " | "
			+ names(gone) + " (instrument " + gone.get(3).isInstrument() + ")");
	}

	// the default for a default source; the instrument of the source's class id; none on MIDI or for another source
	@Test
	void theOnePickedIsTheSourcesNoneOnMidi() {
		assertEquals("0 2 3 -1 -1", picked(TGSoundChain.source(GervillProcessorFactory.TYPE)) + " " + picked(TGVst3Element.create("source", TONE)) + " "
			+ picked(TGVst3Element.create("source", GONE)) + " " + picked(TGSoundChain.midi()) + " " + picked(TGSoundChain.source("drone")));
	}

	private static int picked(TGProgramElement source) {
		return TGTrackSoundChoices.picked(source, TGTrackSoundChoices.instruments(source, SCANNED));
	}

	// in TuxGuitar's language; a percussion channel's numbered, as without the language's names
	@Test
	void generalMidisProgramsInTheLanguagePercussionNumbered() {
		String[] guitars = TuxGuitarMessages.english(() -> String.join("|", TGTrackSoundChoices.programs(false, "Program"))).split("\\|");
		String[] kits = TuxGuitarMessages.english(() -> String.join("|", TGTrackSoundChoices.programs(true, "Program"))).split("\\|");
		assertEquals("128 Acoustic Grand Piano Gunshot | 128 Program #0 Program #127 | 128 Program #30",
			guitars.length + " " + guitars[0] + " " + guitars[127] + " | " + kits.length + " " + kits[0] + " " + kits[127] + " | "
			+ TGTrackSoundChoices.programs(false, "Program").size() + " " + TGTrackSoundChoices.programs(false, "Program").get(30));
	}

	// cut to the most characters given, its last "…"
	@Test
	void aNameTooLongEndsInAnEllipsis() {
		assertEquals("Distortion Elec… | Test Sine | Test Sine", TGTrackSoundChoices.shorten("Distortion Electric Guitar", 16) + " | "
			+ TGTrackSoundChoices.shorten("Test Sine", 16) + " | " + TGTrackSoundChoices.shorten("Test Sine", 9));
	}
}
