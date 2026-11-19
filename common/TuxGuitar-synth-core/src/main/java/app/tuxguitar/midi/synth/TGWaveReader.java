package app.tuxguitar.midi.synth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Reads a WAVE file as samples from -1 to 1 (floats as they are), a channel each, at its sample rate: integers of 8
 * (unsigned), 16, 24 or 32 bits, or 32-bit floats, in the plain format chunk or WAVE_FORMAT_EXTENSIBLE (its sub-format
 * says which). First its format (readFormat), then its samples (readSamples), which stream through a buffer of BLOCK
 * frames into the channels' own arrays; the chunks before each are skipped. Any other file is refused (Refused), saying
 * why in plain words.
 */
public final class TGWaveReader {

	private static final int BLOCK = 4096;
	private static final int PCM = 1;
	private static final int FLOAT = 3;
	private static final int EXTENSIBLE = 0xFFFE;
	// the size of the plain format chunk
	private static final int FORMAT_SIZE = 16;
	// where the extensible format chunk keeps its sub-format's code, the first bytes of its GUID
	private static final int SUB_FORMAT = 24;

	// why a file is refused
	public enum Refusal {
		NOT_WAVE("not a WAVE file"),
		NOT_SAMPLES("not integer samples of 8, 16, 24 or 32 bits, nor 32-bit float samples"),
		ENDS_EARLY("the file ends early");

		private final String reason;

		Refusal(String reason) {
			this.reason = reason;
		}
	}

	public static final class Refused extends IOException {

		private static final long serialVersionUID = 1L;

		private final Refusal refusal;

		Refused(Refusal refusal) {
			super(refusal.reason);
			this.refusal = refusal;
		}

		public Refusal getRefusal() {
			return this.refusal;
		}
	}

	// a sample read from a block, from -1 to 1
	private interface Decoder {
		float decode(ByteBuffer block);
	}

	// the samples of a file: how many channels, at what rate, of how many bits, floats or integers
	public static final class Format {

		private final int channels;
		private final int sampleRate;
		private final int bits;
		private final boolean floats;
		private final Decoder decoder;

		private Format(int channels, int sampleRate, int bits, boolean floats, Decoder decoder) {
			this.channels = channels;
			this.sampleRate = sampleRate;
			this.bits = bits;
			this.floats = floats;
			this.decoder = decoder;
		}

		public int getChannels() {
			return this.channels;
		}

		public int getSampleRate() {
			return this.sampleRate;
		}

		public int getBits() {
			return this.bits;
		}

		public boolean isFloat() {
			return this.floats;
		}

		private int frameBytes() {
			return this.channels * this.bits / 8;
		}
	}

	private TGWaveReader() {
	}

	// the format of the file whose start is next in the stream: its RIFF header, then its format chunk
	public static Format readFormat(InputStream in) throws IOException {
		ByteBuffer riff = bytes(in, 12);
		if (!"RIFF".equals(ascii(riff, 0)) || !"WAVE".equals(ascii(riff, 8))) {
			throw new Refused(Refusal.NOT_WAVE);
		}
		ByteBuffer chunk = bytes(in, chunkSize(in, "fmt "));
		int channels = (chunk.capacity() >= FORMAT_SIZE ? chunk.getShort(2) : 0);
		if (channels < 1) {
			throw new Refused(Refusal.NOT_WAVE);
		}
		int code = code(chunk);
		int bits = chunk.getShort(14);
		return new Format(channels, chunk.getInt(4), bits, code == FLOAT, decoder(code, bits));
	}

	// the format's code: an extensible format's is its sub-format's
	private static int code(ByteBuffer chunk) {
		int code = chunk.getShort(0) & 0xFFFF;
		return (code == EXTENSIBLE && chunk.capacity() >= SUB_FORMAT + 2 ? chunk.getShort(SUB_FORMAT) : code);
	}

	private static Decoder decoder(int code, int bits) throws Refused {
		Decoder decoder = (code == PCM ? integers(bits) : (code == FLOAT && bits == 32 ? ByteBuffer::getFloat : null));
		if (decoder == null) {
			throw new Refused(Refusal.NOT_SAMPLES);
		}
		return decoder;
	}

	// 8 bits unsigned (128 is silence), the others signed
	private static Decoder integers(int bits) {
		switch (bits) {
			case 8: return block -> ((block.get() & 0xFF) - 128) / 128f;
			case 16: return block -> block.getShort() / 32768f;
			case 24: return block -> ((block.get() & 0xFF) | ((block.get() & 0xFF) << 8) | (block.get() << 16)) / 8388608f;
			case 32: return block -> block.getInt() / 2147483648f;
			default: return null;
		}
	}

	// the samples of the file in the format, whose next chunk with them is next in the stream, a channel each
	public static float[][] readSamples(InputStream in, Format format) throws IOException {
		float[][] samples = new float[format.channels][chunkSize(in, "data") / format.frameBytes()];
		int frames = samples[0].length;
		ByteBuffer block = ByteBuffer.allocate(BLOCK * format.frameBytes()).order(ByteOrder.LITTLE_ENDIAN);
		for (int frame = 0; frame < frames; frame += BLOCK) {
			int to = Math.min(frame + BLOCK, frames);
			fill(in, block, (to - frame) * format.frameBytes());
			for (int i = frame; i < to; i++) {
				decode(block, format.decoder, samples, i);
			}
		}
		return samples;
	}

	private static void decode(ByteBuffer block, Decoder decoder, float[][] samples, int frame) {
		for (float[] channel : samples) {
			channel[frame] = decoder.decode(block);
		}
	}

	private static void fill(InputStream in, ByteBuffer block, int count) throws IOException {
		block.clear();
		if (in.readNBytes(block.array(), 0, count) < count) {
			throw new Refused(Refusal.ENDS_EARLY);
		}
	}

	// the size of the next chunk of the id, whose body is next in the stream; the chunks before it skipped (each is
	// padded to an even size)
	private static int chunkSize(InputStream in, String id) throws IOException {
		ByteBuffer header = bytes(in, 8);
		while (!id.equals(ascii(header, 0))) {
			int size = header.getInt(4);
			bytes(in, size + (size & 1));
			header = bytes(in, 8);
		}
		return header.getInt(4);
	}

	private static ByteBuffer bytes(InputStream in, int count) throws IOException {
		ByteBuffer bytes = ByteBuffer.allocate(count).order(ByteOrder.LITTLE_ENDIAN);
		fill(in, bytes, count);
		return bytes;
	}

	private static String ascii(ByteBuffer buffer, int at) {
		return new String(buffer.array(), at, 4, StandardCharsets.US_ASCII);
	}
}
