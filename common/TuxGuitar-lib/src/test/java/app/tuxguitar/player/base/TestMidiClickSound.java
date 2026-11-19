package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * A sound Count In and the Metronome can click with: one of the built-in sounds (the code-made Click, then the
 * recordings) or a WAV file of the user's own in the click sounds folder; each with the General MIDI drum note other
 * MIDI Ports play for it. Kept as a text (its id), parsed once where it comes in: what is no sound is null.
 */
public class TestMidiClickSound {

	private static String describe(MidiClickSound sound) {
		return sound.getId() + " " + sound.getGmNote();
	}

	@Test
	public void theBuiltInSoundsAreClickThenTheRecordingsEachWithItsDrumNote() {
		assertEquals("click 37, wood-block 76, cowbell 56, snare 38, side-stick 37, claves 75, hi-hat 42, clap 39",
			MidiClickSound.BUILT_IN.stream().map(TestMidiClickSound::describe).collect(Collectors.joining(", ")));
	}

	@Test
	public void onlyTheCodeMadeClickHasNoRecording() {
		assertEquals("click", MidiClickSound.BUILT_IN.stream().filter(sound -> !sound.isRecording()).map(MidiClickSound::getId).collect(Collectors.joining()));
	}

	@Test
	public void aBuiltInSoundIsParsedFromItsIdAsTheSameSound() {
		for (MidiClickSound sound : MidiClickSound.BUILT_IN) {
			assertSame(sound, MidiClickSound.parse(sound.getId()));
		}
	}

	// the user's own: a file name in the click sounds folder, played as Side Stick on other ports
	@Test
	public void anOwnSoundIsAWavFileNamePlayedAsSideStickElsewhere() {
		MidiClickSound own = MidiClickSound.own("My Click.WAV");

		assertEquals("file:My Click.WAV 37 My Click.WAV true", describe(own) + " " + own.getFileName() + " " + own.isRecording());
		assertEquals(own, MidiClickSound.parse(own.getId()));
		assertEquals(own.hashCode(), MidiClickSound.parse("file:My Click.WAV").hashCode());
		assertNotEquals(own, MidiClickSound.own("Other.wav"));
		assertNull(MidiClickSound.CLICK.getFileName());
	}

	// what names no sound (an edited configuration, a sound since removed from TuxGuitar): null, the caller falls back
	@Test
	public void whatIsNoSoundIsNull() {
		for (String text : new String[] {null, "", "gong", "file:", "file:click.mp3", "file:../up.wav", "file:sub/a.wav", "file:sub\\a.wav", "file:c:a.wav", "file:.wav", "same-accented"}) {
			assertNull(MidiClickSound.parse(text), text);
		}
	}

	// an own sound's file name, where it comes in (a file the user adds): only a plain WAV file name is one
	@Test
	public void onlyAPlainWavFileNameIsAnOwnSound() {
		assertEquals("true false false false", MidiClickSound.isOwnFileName("a.wav") + " " + MidiClickSound.isOwnFileName("a/b.wav")
			+ " " + MidiClickSound.isOwnFileName("a.txt") + " " + MidiClickSound.isOwnFileName(null));
	}

	@Test
	public void theOwnSoundOfAnIllegalFileNameIsRefused() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> MidiClickSound.own("../a.wav"));
	}

	// the built-in sound General MIDI plays with that drum note, the first when two share it (Click before Side Stick);
	// none for another note
	@Test
	public void theBuiltInSoundOfADrumNoteIsTheFirstWithIt() {
		assertEquals("click wood-block hi-hat null", MidiClickSound.ofGmNote(37).getId() + " " + MidiClickSound.ofGmNote(76).getId()
			+ " " + MidiClickSound.ofGmNote(42).getId() + " " + MidiClickSound.ofGmNote(60));
	}
}
