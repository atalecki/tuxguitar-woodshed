package app.tuxguitar.midi.synth.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import app.tuxguitar.player.base.MidiClickSound;

/**
 * The built-in recordings TuxGuitar ships (click-sounds, see its README): these files exactly, as made from VCSL
 * (CC0 1.0) by tools/make-click-sounds.sh; each already evened (16-bit mono at 44100 Hz, its peak 0.5, at most 2
 * seconds).
 */
public class TestClickSoundFiles {

	private static final Map<String, String> SHIPPED = new TreeMap<String, String>(Map.of(
		"wood-block", "ad109957c89e3b27f8c6a76ca693c7f3d7e28e04ffebf895bdb16120b9c69947",
		"cowbell", "d1ad588e749d1109a9388b46c5fd0826582150491650a724b77b8818641b8225",
		"snare", "6035cc9c2b8f060d3e191fe158748822e814261dc55c61a12ec582668712a3ed",
		"side-stick", "35e0450203ccdd832b0c70a16a0481e0c4b820eb44b74332f800fcbfc22137b3",
		"claves", "3a6fc1a4760b9b08c4ba9ca7d97990bfbe46902cc2f7d6a81c348c785c897f1b",
		"hi-hat", "a8807f285c297c1ac0d767e9762d9a982898de6b4597fd2b32274fc546bcf93c",
		"clap", "033c6410f3508238ef1c32c14b6f4fb1b348ef79d5c61c2abd1239e7c7fd6b3c"));

	private static String sha256(byte[] bytes) throws Exception {
		StringBuilder hex = new StringBuilder();
		for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

	private static byte[] shipped(MidiClickSound sound) throws IOException {
		try (InputStream in = TGClickSounds.open(sound, null)) {
			return in.readAllBytes();
		}
	}

	@Test
	public void everyRecordingShippedIsTheFilePinned() throws Exception {
		Map<String, String> found = new TreeMap<String, String>();
		for (MidiClickSound sound : MidiClickSound.BUILT_IN) {
			if (sound.isRecording()) {
				found.put(sound.getId(), sha256(shipped(sound)));
			}
		}

		assertEquals(SHIPPED, found);
	}

	// the README says where each came from and what was shipped
	@Test
	public void theReadmeNamesEachShippedFileAndItsLicence() throws Exception {
		String readme;
		try (InputStream in = TGClickSounds.class.getResourceAsStream("click-sounds/README.md")) {
			readme = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertTrue(readme.contains("CC0 1.0 Universal") && readme.contains("c1ea7bcc3c7309650ab0da9d15c9cd1fbc4a4c7e"), "licence and commit");
		for (Map.Entry<String, String> file : SHIPPED.entrySet()) {
			assertTrue(readme.contains("| " + file.getKey() + ".wav |") && readme.contains(file.getValue()), file.getKey());
		}
	}

	// evened again, each is what was shipped: 44100 Hz, its peak 0.5, at most 2 seconds
	@Test
	public void eachIsAlreadyEvened() throws Exception {
		for (MidiClickSound sound : MidiClickSound.BUILT_IN) {
			if (sound.isRecording()) {
				TGClickRecording recording = TGClickSounds.recording(sound, null);
				float peak = app.tuxguitar.midi.synth.Signals.peak(recording.samples(), 0, recording.samples().length);
				assertEquals("44100 0.5 true", recording.sampleRate() + " " + peak + " " + (recording.samples().length <= 2 * 44100), sound.getId());
			}
		}
	}
}
