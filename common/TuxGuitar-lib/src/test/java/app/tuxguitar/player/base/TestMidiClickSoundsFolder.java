package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.tuxguitar.player.base.MidiClickSoundsFolder.Reason;

/**
 * The folder of the user's own click sounds: its WAV files are the own sounds (one dropped there by hand too). Add
 * copies a file in, under a numbered name when its name is taken, and refuses (nothing copied) what the click could not
 * play: not a WAVE file, samples it does not read, a file that ends early, silence, a file it cannot read. Remove
 * deletes the copy.
 */
public class TestMidiClickSoundsFolder {

	@TempDir
	File home;

	private File folder() {
		return new File(this.home, "click-sounds");
	}

	private MidiClickSoundsFolder sounds() {
		return new MidiClickSoundsFolder(this.folder());
	}

	// a WAVE file of the format code, 16-bit mono at 44100 Hz, of the samples
	private static byte[] wave(int format, short... samples) {
		ByteBuffer wave = ByteBuffer.allocate(44 + 2 * samples.length).order(ByteOrder.LITTLE_ENDIAN);
		wave.put(ascii("RIFF")).putInt(36 + 2 * samples.length).put(ascii("WAVE"));
		wave.put(ascii("fmt ")).putInt(16).putShort((short) format).putShort((short) 1).putInt(44100).putInt(88200).putShort((short) 2).putShort((short) 16);
		wave.put(ascii("data")).putInt(2 * samples.length);
		for (short sample : samples) {
			wave.putShort(sample);
		}
		return wave.array();
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	// a file of the name and bytes in a folder of the user's, outside the click sounds folder
	private File file(String name, byte[] bytes) throws IOException {
		File source = new File(this.home, "elsewhere");
		source.mkdirs();
		return Files.write(new File(source, name).toPath(), bytes).toFile();
	}

	private File beep(String name) throws IOException {
		return this.file(name, wave(1, (short) 0, (short) 8000, (short) -8000));
	}

	private List<String> names(List<MidiClickSound> sounds) {
		List<String> names = new ArrayList<String>();
		sounds.forEach(sound -> names.add(sound.getId()));
		return names;
	}

	// what is in the folder, by name
	private String[] files() {
		String[] files = java.util.Objects.requireNonNullElse(this.folder().list(), new String[0]);
		Arrays.sort(files);
		return files;
	}

	// its WAV files by name, ignoring case; not a file of another kind, nor a folder named like one
	@Test
	public void itsWavFilesAreTheOwnSounds() throws IOException {
		this.folder().mkdirs();
		Files.write(new File(this.folder(), "b.wav").toPath(), wave(1, (short) 1));
		Files.write(new File(this.folder(), "A.WAV").toPath(), wave(1, (short) 1));
		Files.write(new File(this.folder(), "notes.txt").toPath(), ascii("x"));
		new File(this.folder(), "folder.wav").mkdirs();

		assertEquals(List.of("file:A.WAV", "file:b.wav"), this.names(this.sounds().own()));
	}

	// every sound offered: the built-in ones in their order, then the own ones
	@Test
	public void theSoundsAreTheBuiltInOnesThenTheOwnOnes() throws IOException {
		this.sounds().add(this.beep("Beep.wav"));
		List<MidiClickSound> expected = new ArrayList<MidiClickSound>(MidiClickSound.BUILT_IN);
		expected.add(MidiClickSound.own("Beep.wav"));

		assertEquals(expected, this.sounds().sounds());
	}

	@Test
	public void noFolderNoOwnSounds() {
		assertEquals("[] " + MidiClickSound.BUILT_IN, this.sounds().own() + " " + this.sounds().sounds());
	}

	// a copy, the folder made when it is not there; the file chosen stays where it is
	@Test
	public void aSoundAddedIsACopyOfTheFile() throws IOException {
		File beep = this.beep("Beep.wav");
		MidiClickSound added = this.sounds().add(beep);

		assertEquals(MidiClickSound.own("Beep.wav"), added);
		assertArrayEquals(Files.readAllBytes(beep.toPath()), Files.readAllBytes(new File(this.folder(), "Beep.wav").toPath()));
		assertEquals(true, beep.exists());
		assertEquals(this.folder().getAbsolutePath(), this.sounds().getPath());
	}

	// a sound in its first sample alone is a sound, in any channel
	@Test
	public void aSoundInItsFirstSampleAloneIsASound() throws IOException {
		MidiClickSound tick = this.sounds().add(this.file("Tick.wav", wave(1, (short) 8000, (short) 0)));

		assertEquals(MidiClickSound.own("Tick.wav"), tick);
	}

	// a name taken gets a number, the next one free
	@Test
	public void aNameTakenIsNumbered() throws IOException {
		File beep = this.beep("Beep.wav");
		String added = this.sounds().add(beep) + " " + this.sounds().add(beep) + " " + this.sounds().add(beep);

		assertEquals("file:Beep.wav file:Beep (2).wav file:Beep (3).wav", added);
	}

	// the copy's name is a WAV file's in the folder itself
	@Test
	public void theCopysNameIsAWavFilesName() {
		assertEquals("[beep.wave.wav, x.WAV, a_b_c.wav, .wav.wav]", List.of(MidiClickSoundsFolder.copyName("beep.wave"), MidiClickSoundsFolder.copyName("x.WAV"),
			MidiClickSoundsFolder.copyName("a:b\\c.wav"), MidiClickSoundsFolder.copyName(".wav")).toString());
	}

	private Reason refused(File file) {
		return assertThrows(MidiClickSoundsFolder.Refused.class, () -> this.sounds().add(file)).getReason();
	}

	// refused, nothing copied: why, in plain words
	@Test
	public void whatTheClickCouldNotPlayIsRefused() throws IOException {
		byte[] cut = Arrays.copyOf(wave(1, (short) 8000, (short) 8000), 46);
		List<Reason> reasons = List.of(this.refused(this.file("notes.wav", ascii("not a sound at all, but words"))), this.refused(this.file("adpcm.wav", wave(2, (short) 8000))),
			this.refused(this.file("cut.wav", cut)), this.refused(this.file("silent.wav", wave(1, (short) 0, (short) 0))), this.refused(new File(this.home, "gone.wav")));

		assertEquals(List.of(Reason.NOT_WAVE, Reason.NOT_SAMPLES, Reason.ENDS_EARLY, Reason.SILENT, Reason.NOT_READ), reasons);
		assertEquals(0, this.files().length);
	}

	// a file the folder cannot take (a folder of that name is in the way) is refused as not copied
	@Test
	public void aCopyThatCannotBeWrittenIsRefused() throws IOException {
		Files.write(this.folder().toPath(), ascii("a file where the folder should be"));

		assertEquals(Reason.NOT_COPIED, this.refused(this.beep("Beep.wav")));
	}

	// the reason in TuxGuitar's language: the file's name, and what the system said when it failed
	@Test
	public void aRefusalSaysWhyInTheUsersLanguage() throws IOException {
		File notes = this.file("notes.wav", ascii("not a sound at all, but words"));
		MidiClickSoundsFolder.Refused notWave = assertThrows(MidiClickSoundsFolder.Refused.class, () -> this.sounds().add(notes));
		MidiClickSoundsFolder.Refused gone = new MidiClickSoundsFolder.Refused(Reason.NOT_READ, "gone.wav", "no such file");

		assertEquals("notes.wav was not added: it is not a WAV file. | gone.wav was not added: it could not be read (no such file).",
			app.tuxguitar.practice.PracticeRefusals.english(() -> notWave.getMessage() + " | " + gone.getMessage()));
	}

	// removed: the copy deleted, no longer a sound; removed again, nothing happens
	@Test
	public void aSoundRemovedIsDeleted() throws IOException {
		MidiClickSound beep = this.sounds().add(this.beep("Beep.wav"));
		MidiClickSound boop = this.sounds().add(this.beep("Boop.wav"));
		this.sounds().remove(beep);
		this.sounds().remove(beep);

		assertEquals(List.of(boop), this.sounds().own());
		assertArrayEquals(new String[] {"Boop.wav"}, this.files());
	}

	// a copy that cannot be deleted (a folder full of files where it was) is refused as not removed
	@Test
	public void aCopyThatCannotBeDeletedIsRefused() throws IOException {
		File inTheWay = new File(this.folder(), "Beep.wav");
		inTheWay.mkdirs();
		Files.write(new File(inTheWay, "x").toPath(), ascii("x"));

		Reason reason = assertThrows(MidiClickSoundsFolder.Refused.class, () -> this.sounds().remove(MidiClickSound.own("Beep.wav"))).getReason();
		assertEquals(Reason.NOT_REMOVED, reason);
	}
}
