package app.tuxguitar.io.wave;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * A WAVE file read as samples from -1 to 1, a channel each, at its sample rate: integers of 8 (unsigned), 16, 24 or 32
 * bits, or 32-bit floats, in the plain format chunk or the extensible one; the chunks before the samples skipped. Any
 * other file is refused, saying why in plain words.
 */
public class TestWaveReader {

	private static final short PCM = 1;
	private static final short FLOAT = 3;
	private static final short EXTENSIBLE = (short) 0xFFFE;
	// where the extensible format chunk keeps its sub-format's code
	private static final int SUB_FORMAT = 24;

	// a WAVE file of the format, its chunks before the samples, then the samples' bytes
	private static byte[] wave(byte[] format, byte[] before, byte[] samples) {
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		for (byte[] part : new byte[][] {ascii("RIFF"), le(4).putInt(4 + 8 + format.length + before.length + 8 + samples.length).array(), ascii("WAVE"),
				chunk("fmt ", format), before, chunk("data", samples)}) {
			file.write(part, 0, part.length);
		}
		return file.toByteArray();
	}

	private static byte[] chunk(String id, byte[] body) {
		ByteBuffer chunk = le(8 + body.length);
		return chunk.put(ascii(id)).putInt(body.length).put(body).array();
	}

	// the plain format chunk
	private static byte[] format(short code, int channels, int rate, int bits) {
		return formatChunk(16, code, channels, rate, bits).array();
	}

	// the extensible one: its sub-format's code first in the GUID
	private static byte[] extensible(short code, int channels, int rate, int bits) {
		ByteBuffer format = formatChunk(40, EXTENSIBLE, channels, rate, bits);
		format.putShort((short) 22).putShort((short) bits).putInt(channels == 1 ? 4 : 3).putShort(code);
		return format.array();
	}

	private static ByteBuffer formatChunk(int size, short code, int channels, int rate, int bits) {
		ByteBuffer format = le(size);
		format.putShort(code).putShort((short) channels).putInt(rate).putInt(rate * channels * bits / 8);
		return format.putShort((short) (channels * bits / 8)).putShort((short) bits);
	}

	private static ByteBuffer le(int size) {
		return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}

	private static byte[] bytes(int... values) {
		byte[] bytes = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			bytes[i] = (byte) values[i];
		}
		return bytes;
	}

	private static float[][] read(byte[] file) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(file);
		return TGWaveReader.readSamples(in, TGWaveReader.readFormat(in));
	}

	// little endian, the channels interleaved: left, right, left, right
	@Test
	public void sixteenBitStereoIsReadAChannelEachAtItsRate() throws IOException {
		byte[] file = wave(format(PCM, 2, 44100, 16), new byte[0], bytes(0x00, 0x40, 0x00, 0xC0, 0xFF, 0x7F, 0x00, 0x80));
		TGWaveReader.Format format = TGWaveReader.readFormat(new ByteArrayInputStream(file));

		assertEquals("2 44100 16 false", format.getChannels() + " " + format.getSampleRate() + " " + format.getBits() + " " + format.isFloat());
		assertArrayEquals(new float[][] {{0.5f, 32767 / 32768f}, {-0.5f, -1f}}, read(file));
	}

	// unsigned: 128 is silence
	@Test
	public void eightBitSamplesAreUnsigned() throws IOException {
		assertArrayEquals(new float[][] {{0f, -1f, 0.5f, 127 / 128f}}, read(wave(format(PCM, 1, 8000, 8), new byte[0], bytes(128, 0, 192, 255))));
	}

	@Test
	public void twentyFourAndThirtyTwoBitSamples() throws IOException {
		assertArrayEquals(new float[][] {{0.5f, -0.25f}}, read(wave(format(PCM, 1, 48000, 24), new byte[0], bytes(0, 0, 0x40, 0, 0, 0xE0))));
		assertArrayEquals(new float[][] {{-1f, 0.5f}}, read(wave(format(PCM, 1, 48000, 32), new byte[0], bytes(0, 0, 0, 0x80, 0, 0, 0, 0x40))));
	}

	@Test
	public void floatSamplesAreReadAsTheyAre() throws IOException {
		byte[] samples = le(8).putFloat(1.5f).putFloat(-0.125f).array();
		TGWaveReader.Format format = TGWaveReader.readFormat(new ByteArrayInputStream(wave(format(FLOAT, 1, 96000, 32), new byte[0], samples)));

		assertEquals("32 true", format.getBits() + " " + format.isFloat());
		assertArrayEquals(new float[][] {{1.5f, -0.125f}}, read(wave(format(FLOAT, 1, 96000, 32), new byte[0], samples)));
	}

	// WAVE_FORMAT_EXTENSIBLE: its sub-format says integers or floats
	@Test
	public void theExtensibleFormatIsReadByItsSubFormat() throws IOException {
		assertArrayEquals(new float[][] {{0.5f}, {-0.25f}}, read(wave(extensible(PCM, 2, 44100, 24), new byte[0], bytes(0, 0, 0x40, 0, 0, 0xE0))));
		assertArrayEquals(new float[][] {{0.75f}}, read(wave(extensible(FLOAT, 1, 44100, 32), new byte[0], le(4).putFloat(0.75f).array())));
	}

	// more frames than one block of the reader holds (4096): each where it was
	@Test
	public void aLongFileIsReadWhole() throws IOException {
		ByteBuffer samples = le(2 * 2 * 5000);
		for (int frame = 0; frame < 5000; frame++) {
			samples.putShort((short) frame).putShort((short) -frame);
		}
		float[][] read = read(wave(format(PCM, 2, 44100, 16), new byte[0], samples.array()));

		assertEquals("5000 4096 -4999", read[0].length + " " + Math.round(read[0][4096] * 32768) + " " + Math.round(read[1][4999] * 32768));
	}

	// a plain format chunk with bytes after its 16 (the size of the extra bytes): read as plain
	@Test
	public void aPlainFormatChunkWithExtraBytesIsPlain() throws IOException {
		byte[] format = java.util.Arrays.copyOf(format(PCM, 1, 44100, 16), 40);
		format[SUB_FORMAT] = FLOAT;

		assertArrayEquals(new float[][] {{0.5f}}, read(wave(format, new byte[0], bytes(0x00, 0x40))));
	}

	// an extensible format chunk long enough to say its sub-format, and one too short to
	@Test
	public void anExtensibleFormatChunkTooShortForItsSubFormatIsRefused() throws IOException {
		byte[] extensible = extensible(PCM, 1, 44100, 16);

		assertArrayEquals(new float[][] {{0.5f}}, read(wave(java.util.Arrays.copyOf(extensible, SUB_FORMAT + 2), new byte[0], bytes(0x00, 0x40))));
		assertEquals("NOT_SAMPLES", refusal(wave(java.util.Arrays.copyOf(extensible, SUB_FORMAT), new byte[0], bytes(0x00, 0x40))).split(":")[0]);
	}

	// a list of tags, then an odd-sized chunk, padded
	@Test
	public void theChunksBeforeTheSamplesAreSkipped() throws IOException {
		byte[] before = bytes('L', 'I', 'S', 'T', 3, 0, 0, 0, 'a', 'b', 'c', 0, 'j', 'u', 'n', 'k', 2, 0, 0, 0, 1, 2);

		assertArrayEquals(new float[][] {{0.5f}}, read(wave(format(PCM, 1, 44100, 16), before, bytes(0x00, 0x40))));
	}

	private static String refusal(byte[] file) {
		TGWaveReader.Refused refused = assertThrows(TGWaveReader.Refused.class, () -> read(file));
		return refused.getRefusal() + ": " + refused.getMessage();
	}

	// not a RIFF file, not a WAVE one, of no channels, its format chunk too short
	@Test
	public void aFileThatIsNoWaveFileIsRefused() {
		byte[] file = wave(format(PCM, 1, 44100, 16), new byte[0], bytes(0, 0));
		byte[] riff = file.clone();
		riff[3] = 'X';
		byte[] wave = file.clone();
		wave[11] = 'X';

		assertEquals("NOT_WAVE: not a WAVE file | NOT_WAVE: not a WAVE file | NOT_WAVE: not a WAVE file | NOT_WAVE: not a WAVE file", refusal(riff)
			+ " | " + refusal(wave) + " | " + refusal(wave(format(PCM, 0, 44100, 16), new byte[0], new byte[0]))
			+ " | " + refusal(wave(java.util.Arrays.copyOf(format(PCM, 1, 44100, 16), 14), new byte[0], new byte[0])));
	}

	// compressed (ADPCM), 64-bit floats, 12-bit integers: none of the samples it reads
	@Test
	public void samplesItDoesNotReadAreRefused() {
		String refused = "NOT_SAMPLES: not integer samples of 8, 16, 24 or 32 bits, nor 32-bit float samples";

		assertEquals(refused + " | " + refused + " | " + refused + " | " + refused, refusal(wave(format((short) 2, 1, 44100, 4), new byte[0], new byte[0]))
			+ " | " + refusal(wave(format(FLOAT, 1, 44100, 64), new byte[0], new byte[0])) + " | " + refusal(wave(format(PCM, 1, 44100, 12), new byte[0], new byte[0]))
			+ " | " + refusal(wave(extensible((short) 2, 1, 44100, 16), new byte[0], new byte[0])));
	}

	// in the header, or in the samples
	@Test
	public void aFileThatEndsEarlyIsRefused() {
		byte[] file = wave(format(PCM, 1, 44100, 16), new byte[0], bytes(0, 0x40, 0, 0x40));

		assertEquals("ENDS_EARLY: the file ends early | ENDS_EARLY: the file ends early", refusal(java.util.Arrays.copyOf(file, 20))
			+ " | " + refusal(java.util.Arrays.copyOf(file, file.length - 1)));
	}
}
