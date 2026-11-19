package app.tuxguitar.midi.synth.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import app.tuxguitar.player.base.MidiClickSound;

/**
 * The tool that makes the built-in click sounds TuxGuitar ships (run by tools/make-click-sounds.sh, see the README of
 * click-sounds): each recording downloaded as <id>.wav in one folder, evened as every click sound is
 * (TGClickRecording), written to the other as 16-bit mono WAVE at its own rate. Prints each sound's id, length, and the
 * SHA-256 of the file read and of the one written.
 */
public final class MakeClickSounds {

	private MakeClickSounds() {
	}

	// the folder of the downloaded recordings, the folder of the click sounds
	public static void main(String[] args) throws IOException {
		for (MidiClickSound sound : MidiClickSound.BUILT_IN) {
			if (sound.isRecording()) {
				make(new File(args[0], sound.getId() + ".wav"), new File(args[1], sound.getId() + ".wav"));
			}
		}
	}

	private static void make(File source, File shipped) throws IOException {
		TGClickRecording recording;
		try (InputStream in = new FileInputStream(source)) {
			recording = TGClickRecording.read(in);
		}
		try (OutputStream out = new FileOutputStream(shipped)) {
			out.write(wave(recording.samples(), recording.sampleRate()));
		}
		System.out.println(shipped.getName() + " " + recording.samples().length + " frames at " + recording.sampleRate() + " Hz, source " + sha256(source)
			+ ", shipped " + sha256(shipped) + " (" + shipped.length() + " bytes)");
	}

	// 16-bit mono WAVE: each sample times 32768, rounded, up to 32767
	private static byte[] wave(float[] samples, int rate) {
		ByteBuffer wave = ByteBuffer.allocate(44 + 2 * samples.length).order(ByteOrder.LITTLE_ENDIAN);
		wave.put(ascii("RIFF")).putInt(36 + 2 * samples.length).put(ascii("WAVE"));
		wave.put(ascii("fmt ")).putInt(16).putShort((short) 1).putShort((short) 1).putInt(rate).putInt(2 * rate).putShort((short) 2).putShort((short) 16);
		wave.put(ascii("data")).putInt(2 * samples.length);
		for (float sample : samples) {
			wave.putShort((short) Math.min(Short.MAX_VALUE, Math.round(sample * 32768)));
		}
		return wave.array();
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	static String sha256(File file) throws IOException {
		try {
			StringBuilder hex = new StringBuilder();
			for (byte b : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()))) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException notInTheJdk) {
			throw new IOException(notInTheJdk);
		}
	}
}
