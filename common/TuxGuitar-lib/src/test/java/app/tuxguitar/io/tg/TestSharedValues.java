package app.tuxguitar.io.tg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.io.base.TGFileFormatException;
import app.tuxguitar.io.base.TGSongReaderHandle;
import app.tuxguitar.io.base.TGSongWriterHandle;
import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGChannelParameter;
import app.tuxguitar.song.models.TGSong;

/**
 * A channel parameter's shared value (TGChannelParameter.SHARED, a plugin's capture say) is kept once in a TuxGuitar
 * file however many parameters hold it; any other value is kept where it is, as before. The song has three channels.
 */
public class TestSharedValues {

	// long texts of one letter, each found in content.xml by itself
	private static final String CAPTURE = String.join("", Collections.nCopies(5000, "c"));
	private static final String OTHER = String.join("", Collections.nCopies(5000, "o"));

	private final TGFactory factory = new TGFactory();
	private final TGSongManager songManager = new TGSongManager(this.factory);
	private final TGSong song = this.songManager.newSong();

	public TestSharedValues() {
		this.songManager.addTrack(this.song);
		this.songManager.addTrack(this.song);
	}

	private TGChannel channel(TGSong song, int index) {
		Iterator<TGChannel> channels = song.getChannels();
		for (int i = 0; i < index; i++) {
			channels.next();
		}
		return channels.next();
	}

	private void set(int channel, String key, String value) {
		TGChannelParameter parameter = this.factory.newChannelParameter();
		parameter.setKey(key);
		parameter.setValue(value);
		channel(this.song, channel).addParameter(parameter);
	}

	// each parameter of the channel as key=value
	private static List<String> parameters(TGChannel channel) {
		List<String> parameters = new ArrayList<String>();
		channel.getParameters().forEachRemaining(parameter -> parameters.add(parameter.getKey() + "=" + parameter.getValue()));
		return parameters;
	}

	private byte[] write(TGSong song) throws TGFileFormatException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		TGSongWriterHandle handle = new TGSongWriterHandle();
		handle.setFactory(this.factory);
		handle.setSong(song);
		handle.setOutputStream(bytes);
		new TGSongWriterImpl().write(handle);
		return bytes.toByteArray();
	}

	private TGSong read(byte[] file) throws TGFileFormatException {
		TGSongReaderHandle handle = new TGSongReaderHandle();
		handle.setFactory(this.factory);
		handle.setInputStream(new ByteArrayInputStream(file));
		new TGSongReaderImpl().read(handle);
		return handle.getSong();
	}

	private static String content(byte[] file) throws IOException {
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(file))) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (entry.getName().equals("content.xml")) {
					return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		}
		return null;
	}

	// the file with its content.xml as given
	private static byte[] withContent(byte[] file, String content) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(file)); ZipOutputStream out = new ZipOutputStream(bytes)) {
			for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
				byte[] data = (entry.getName().equals("content.xml") ? content.getBytes(StandardCharsets.UTF_8) : in.readAllBytes());
				out.putNextEntry(new ZipEntry(entry.getName()));
				out.write(data);
				out.closeEntry();
			}
		}
		return bytes.toByteArray();
	}

	private static int count(String text, String found) {
		Matcher matcher = Pattern.compile(Pattern.quote(found)).matcher(text);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	@Test
	public void aSharedValueThatSeveralParametersHoldIsKeptOnce() throws Exception {
		set(0, "a", TGChannelParameter.SHARED + CAPTURE);
		set(1, "a", TGChannelParameter.SHARED + CAPTURE);
		set(2, "b", TGChannelParameter.SHARED + OTHER);
		String content = content(write(this.song));
		assertEquals("1 1", count(content, CAPTURE) + " " + count(content, OTHER));
	}

	@Test
	public void eachParameterGetsItsSharedValueBack() throws Exception {
		set(0, "a", TGChannelParameter.SHARED + CAPTURE);
		set(1, "a", TGChannelParameter.SHARED + CAPTURE);
		set(1, "b", TGChannelParameter.SHARED + OTHER);
		set(2, "c", "plain");
		TGSong reopened = read(write(this.song));
		assertEquals(Arrays.asList(parameters(channel(this.song, 0)), parameters(channel(this.song, 1)), parameters(channel(this.song, 2))),
			Arrays.asList(parameters(channel(reopened, 0)), parameters(channel(reopened, 1)), parameters(channel(reopened, 2))));
	}

	@Test
	public void aValueNotSharedIsKeptWhereItIsEachTime() throws Exception {
		set(0, "a", CAPTURE);
		set(1, "a", CAPTURE);
		assertEquals(2, count(content(write(this.song)), CAPTURE));
	}

	// an older version reads every parameter's value attribute: a shared one keeps one, which that version takes as it is
	@Test
	public void aSharedParameterKeepsAValueAttributeInTheFile() throws Exception {
		set(0, "a", TGChannelParameter.SHARED + CAPTURE);
		assertEquals(1, count(content(write(this.song)), "<TGChannelParameter key=\"a\" value=\""));
	}

	// a file whose shared value went (an older version saved it again) opens, without the parameters that held it
	@Test
	public void aParameterWhoseSharedValueIsGoneIsLeftOut() throws Exception {
		set(0, "a", TGChannelParameter.SHARED + CAPTURE);
		set(0, "b", "plain");
		byte[] file = write(this.song);
		String content = content(file);
		String gone = content.substring(0, content.indexOf("<TGSharedValues>")) + content.substring(content.indexOf("</TGSharedValues>") + "</TGSharedValues>".length());
		assertEquals(Arrays.asList("b=plain"), parameters(channel(read(withContent(file, gone)), 0)));
	}
}
