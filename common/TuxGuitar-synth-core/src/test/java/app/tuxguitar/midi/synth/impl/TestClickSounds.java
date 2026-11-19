package app.tuxguitar.midi.synth.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import app.tuxguitar.player.base.MidiClickSound;

/**
 * Where a click sound's recording is read from: a built-in one from those TuxGuitar ships, the user's own from the
 * folder of own click sounds by its file name. The code-made Click has none; one that cannot be read is logged, and the
 * Click plays in its place (no recording).
 */
public class TestClickSounds {

	@TempDir
	File folder;

	// what was logged
	private final List<String> logged = new ArrayList<String>();
	private final Handler handler = new Handler() {
		public void publish(LogRecord record) {
			logged.add(record.getLevel() + " " + record.getMessage());
		}

		public void flush() {
		}

		public void close() {
		}
	};

	@BeforeEach
	public void listen() {
		Logger.getLogger(TGClickSounds.class.getName()).addHandler(this.handler);
	}

	@AfterEach
	public void stopListening() {
		Logger.getLogger(TGClickSounds.class.getName()).removeHandler(this.handler);
	}

	// a 16-bit mono WAVE file of two samples, a quarter of full scale up then down
	private void ownWave(String name) throws IOException {
		byte[] wave = {'R', 'I', 'F', 'F', 40, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ', 16, 0, 0, 0, 1, 0, 1, 0, (byte) 0x80, (byte) 0xBB, 0, 0,
			0, 0x77, 1, 0, 2, 0, 16, 0, 'd', 'a', 't', 'a', 4, 0, 0, 0, 0, 0x20, 0, (byte) 0xE0};
		Files.write(new File(this.folder, name).toPath(), wave);
	}

	@Test
	public void aBuiltInRecordingIsOneTuxGuitarShips() {
		assertEquals(10006, TGClickSounds.recording(MidiClickSound.CLAP, null).samples().length);
	}

	@Test
	public void theCodeMadeClickHasNoRecording() {
		assertNull(TGClickSounds.recording(MidiClickSound.CLICK, this.folder.getPath()));
		assertEquals("[]", this.logged.toString());
	}

	@Test
	public void anOwnSoundIsReadFromTheFolderByItsName() throws IOException {
		ownWave("Mine.wav");
		TGClickRecording recording = TGClickSounds.recording(MidiClickSound.own("Mine.wav"), this.folder.getPath());

		assertEquals("48000 [0.5, -0.5]", recording.sampleRate() + " " + java.util.Arrays.toString(recording.samples()));
	}

	// gone, or not a WAVE file, or no folder told: logged, no recording (the Click plays)
	@Test
	public void anOwnSoundThatCannotBeReadIsLoggedAndHasNoRecording() throws IOException {
		Files.write(new File(this.folder, "Text.wav").toPath(), "this is not a wave file at all".getBytes());
		ownWave("Mine.wav");

		assertNull(TGClickSounds.recording(MidiClickSound.own("Gone.wav"), this.folder.getPath()));
		assertNull(TGClickSounds.recording(MidiClickSound.own("Text.wav"), this.folder.getPath()));
		assertNull(TGClickSounds.recording(MidiClickSound.own("Mine.wav"), null));
		assertEquals(3, this.logged.size());
		assertEquals("WARNING The click sound file:Text.wav could not be read, the Click plays in its place: not a WAVE file | "
			+ "WARNING The click sound file:Mine.wav could not be read, the Click plays in its place: no folder of own click sounds",
			this.logged.get(1) + " | " + this.logged.get(2));
	}
}
