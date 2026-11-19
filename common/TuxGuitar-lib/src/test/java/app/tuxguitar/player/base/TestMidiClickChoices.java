package app.tuxguitar.player.base;

import static app.tuxguitar.practice.PracticeRefusals.english;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The choices of Settings > Sound's "Count In / Metronome click" and the Transport menu's Metronome Sound, before they
 * are kept: beat 1's sound ("Same sound, accented" first, then every sound), the other beats' (every sound: the
 * built-in ones, then the user's own), the click volume; the click they make is the one Try plays and OK keeps. An own
 * sound is added (and chosen for the other beats) or removed (what played it plays the Click). Each sound is named in
 * the user's language; an own one by its file's name.
 */
public class TestMidiClickChoices {

	@TempDir
	File home;

	private MidiClickSoundsFolder folder() {
		return new MidiClickSoundsFolder(new File(this.home, "click-sounds"));
	}

	private MidiClickChoices choices(MidiClick click) {
		return new MidiClickChoices(click, this.folder());
	}

	// a 16-bit mono WAVE file of the name, with a sound in it, outside the folder
	private File beep(String name) throws IOException {
		ByteBuffer wave = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);
		wave.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(40).put("WAVE".getBytes(StandardCharsets.US_ASCII));
		wave.put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short) 1).putShort((short) 1).putInt(44100).putInt(88200).putShort((short) 2).putShort((short) 16);
		wave.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(4).putShort((short) 8000).putShort((short) -8000);
		return Files.write(new File(this.home, name).toPath(), wave.array()).toFile();
	}

	@Test
	public void theOtherBeatsOfferEverySoundBeatOneSameAccentedFirst() throws IOException {
		this.folder().add(this.beep("Beep.wav"));
		MidiClickChoices choices = this.choices(MidiClick.DEFAULT);
		List<MidiClickSound> sounds = new ArrayList<MidiClickSound>(MidiClickSound.BUILT_IN);
		sounds.add(MidiClickSound.own("Beep.wav"));
		List<Optional<MidiClickSound>> beatOne = new ArrayList<Optional<MidiClickSound>>();
		beatOne.add(Optional.empty());
		sounds.forEach(sound -> beatOne.add(Optional.of(sound)));

		assertEquals(sounds, choices.getSounds());
		assertEquals(beatOne, choices.getBeatOneChoices());
	}

	// from the click kept; each choice changes the click Try plays and OK keeps
	@Test
	public void eachChoiceChangesTheClick() {
		MidiClickChoices choices = this.choices(MidiClick.DEFAULT);
		MidiClick kept = choices.getClick();
		choices.chooseOther(MidiClickSound.COWBELL);
		choices.chooseBeatOne(Optional.of(MidiClickSound.CLAP));
		choices.chooseVolume(6);

		assertEquals(MidiClick.DEFAULT, kept);
		assertEquals(new MidiClick(MidiClickSound.CLAP, MidiClickSound.COWBELL, 6), choices.getClick());
		choices.chooseBeatOne(Optional.empty());
		assertEquals(MidiClick.sameAccented(MidiClickSound.COWBELL, 6), choices.getClick());
	}

	// added: offered from now on, and chosen for the other beats
	@Test
	public void aSoundAddedIsChosenForTheOtherBeats() throws IOException {
		MidiClickChoices choices = this.choices(new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, 2));
		MidiClickSound added = choices.add(this.beep("Beep.wav"));

		assertEquals(MidiClickSound.own("Beep.wav"), added);
		assertEquals(new MidiClick(MidiClickSound.CLAP, added, 2), choices.getClick());
		assertEquals(added, choices.getSounds().get(choices.getSounds().size() - 1));
	}

	// only an own sound chosen for the other beats can be removed
	@Test
	public void onlyAnOwnSoundForTheOtherBeatsCanBeRemoved() throws IOException {
		MidiClickChoices choices = this.choices(MidiClick.DEFAULT);
		boolean builtIn = choices.canRemove();
		choices.add(this.beep("Beep.wav"));

		assertEquals("false true", builtIn + " " + choices.canRemove());
	}

	// removed: no longer offered, and whatever played it plays the Click
	@Test
	public void aSoundRemovedIsGoneAndWhatPlayedItPlaysTheClick() throws IOException {
		MidiClickSound beep = this.folder().add(this.beep("Beep.wav"));
		MidiClickChoices choices = this.choices(new MidiClick(beep, beep, -4));
		choices.remove();

		assertEquals(new MidiClick(MidiClickSound.CLICK, MidiClickSound.CLICK, -4), choices.getClick());
		assertEquals(MidiClickSound.BUILT_IN, choices.getSounds());
	}

	// a built-in sound by its translated name, beat 1 played accented as "Same sound, accented", an own one its file name
	// without .wav
	@Test
	public void eachSoundIsNamedInTheUsersLanguage() {
		String names = english(() -> MidiClickChoices.name(Optional.empty()) + " | " + MidiClickChoices.name(Optional.of(MidiClickSound.WOOD_BLOCK)) + " | "
			+ MidiClickChoices.name(MidiClickSound.CLAP) + " | " + MidiClickChoices.name(MidiClickSound.own("My Beep.WAV")));

		assertEquals("Same sound, accented | Wood Block | Hand Clap | My Beep", names);
	}

	@Test
	public void everyBuiltInSoundHasAName() {
		List<String> names = new ArrayList<String>();
		english(() -> {
			MidiClickSound.BUILT_IN.forEach(sound -> names.add(MidiClickChoices.name(sound)));
			return "";
		});

		assertEquals("[Click, Wood Block, Cowbell, Snare Drum, Side Stick, Claves, Hi-Hat, Hand Clap]", names.toString());
	}

	// the click volume, signed: louder than 0 dB with a plus
	@Test
	public void theVolumeIsShownInDecibelsSigned() {
		assertEquals("+12 dB | +1 dB | 0 dB | -3 dB | -30 dB", english(() -> MidiClickChoices.volume(12) + " | " + MidiClickChoices.volume(1) + " | "
			+ MidiClickChoices.volume(0) + " | " + MidiClickChoices.volume(-3) + " | " + MidiClickChoices.volume(-30)));
	}
}
