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
		"wood-block", "2f827c865a3e57082c03651d5cd5e91aa223125e08ef8ca3f8e3571348d2dda5",
		"cowbell", "31ffeb1a49f59c7fdbbc60552187bcfc7951f51b90b977be196e254ba1bf5206",
		"snare", "d1aa619fdf5aabcdeb27702056d2705721b63d348f80f62d483a764f2e96727c",
		"side-stick", "b529eb7b1a24ffb06689417491a18775ca84fc0d8f0aae4ba7065d6c1522da41",
		"claves", "529d8d6465b0608ff4154014838f5e09e8fe160c88a02153730d82a71b363df2",
		"hi-hat", "503362c73f37d0a339a13d12d0b1f3c6b85fdab32091b90c18a6132b160468f6",
		"clap", "84ea7879b02d28e5e8e3f6a9785f68a7923e534ab288f6204db3db99d4a35658"));

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

	// each sounds on the beat: as shipped, its first sample at a hundredth of its peak (-40 dB) is within 1 ms
	@Test
	public void eachStartsOnTheBeat() throws Exception {
		Map<String, Integer> onsets = new TreeMap<String, Integer>();
		for (MidiClickSound sound : MidiClickSound.BUILT_IN) {
			if (sound.isRecording()) {
				try (InputStream in = TGClickSounds.open(sound, null)) {
					float[] samples = app.tuxguitar.midi.synth.TGWaveReader.readSamples(in, app.tuxguitar.midi.synth.TGWaveReader.readFormat(in))[0];
					float peak = app.tuxguitar.midi.synth.Signals.peak(samples, 0, samples.length);
					onsets.put(sound.getId(), (int) java.util.stream.IntStream.range(0, samples.length).filter(i -> Math.abs(samples[i]) >= peak / 100).findFirst().getAsInt());
				}
			}
		}

		assertTrue(onsets.values().stream().allMatch(frame -> frame <= 44), "frames at 44100 Hz: " + onsets);
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
