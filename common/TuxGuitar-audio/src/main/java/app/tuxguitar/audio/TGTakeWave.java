package app.tuxguitar.audio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import app.tuxguitar.io.tg.TGTakeCodec;
import app.tuxguitar.io.wave.TGWaveReader;
import app.tuxguitar.song.models.TGTake;
import app.tuxguitar.util.TGMessagesManager;

/**
 * A take in a TuxGuitar file (format 3.0): an uncompressed WAVE file of 32-bit float samples (IEEE float, little
 * endian, the channels interleaved) at the sample rate the take was recorded at; it is resampled only to play or export.
 * The samples stream through a buffer of BLOCK frames both ways: writing does not copy a take, reading (TGWaveReader)
 * fills the take's own arrays. Reading skips the chunks it does not need, and refuses what is not a 32-bit float wave
 * file, saying why in TuxGuitar's language (the messages NOT_WAVE, NOT_FLOAT, ENDS_EARLY).
 */
public final class TGTakeWave implements TGTakeCodec {

	private static final int BLOCK = 4096;
	private static final int BYTES = 4;
	private static final short IEEE_FLOAT = 3;
	// why a file is refused: "Not a wave file", "... of 32-bit float samples", "The wave file ends early"
	private static final String NOT_WAVE = "audio.take.refusal.not-wave";
	private static final String NOT_FLOAT = "audio.take.refusal.not-float";
	private static final String ENDS_EARLY = "audio.take.refusal.ends-early";
	// what a refusal of the wave reader says: samples that are not floats are not 32-bit floats
	private static final Map<TGWaveReader.Refusal, String> REFUSALS = Map.of(TGWaveReader.Refusal.NOT_WAVE, NOT_WAVE,
		TGWaveReader.Refusal.NOT_SAMPLES, NOT_FLOAT, TGWaveReader.Refusal.ENDS_EARLY, ENDS_EARLY);

	public String getExtension() {
		return "wav";
	}

	public void write(TGTake take, OutputStream out) throws IOException {
		int channels = take.countChannels();
		int data = take.countSamples() * channels * BYTES;
		ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
		header.put(ascii("RIFF")).putInt(36 + data).put(ascii("WAVE"));
		header.put(ascii("fmt ")).putInt(16).putShort(IEEE_FLOAT).putShort((short) channels).putInt(take.getSampleRate());
		header.putInt(take.getSampleRate() * channels * BYTES).putShort((short) (channels * BYTES)).putShort((short) (BYTES * 8));
		header.put(ascii("data")).putInt(data);
		out.write(header.array());
		ByteBuffer block = ByteBuffer.allocate(BLOCK * channels * BYTES).order(ByteOrder.LITTLE_ENDIAN);
		for (int frame = 0; frame < take.countSamples(); frame += BLOCK) {
			block.clear();
			put(take, frame, Math.min(frame + BLOCK, take.countSamples()), block);
			out.write(block.array(), 0, block.position());
		}
	}

	// the frames from one to another, interleaved
	private static void put(TGTake take, int from, int to, ByteBuffer block) {
		for (int frame = from; frame < to; frame++) {
			for (int channel = 0; channel < take.countChannels(); channel++) {
				block.putFloat(take.getSample(channel, frame));
			}
		}
	}

	public TGTake read(InputStream in) throws IOException {
		try {
			TGWaveReader.Format format = TGWaveReader.readFormat(in);
			if (!format.isFloat()) {
				throw new IOException(TGMessagesManager.getProperty(NOT_FLOAT));
			}
			return TGTake.adopt(0, TGWaveReader.readSamples(in, format), format.getSampleRate());
		} catch (TGWaveReader.Refused refused) {
			throw new IOException(TGMessagesManager.getProperty(REFUSALS.get(refused.getRefusal())), refused);
		}
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}
}
