package app.tuxguitar.io.tg;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;

import app.tuxguitar.io.base.TGFileFormatException;
import app.tuxguitar.song.models.TGTake;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.util.TGVersion;

/**
 * The entries of a TuxGuitar file, read in one pass: version.txt read as soon as it comes (first, in the files TuxGuitar
 * writes) and refused there when the reader cannot read that version, content.xml kept as it is, each take's entry
 * decoded by the take codec as it streams by, once version.txt said format 3.0 (none without a codec), and whether any
 * other entry was there. A take the codec cannot read is left out: its entry's bytes are kept as they were (an unreadable
 * take, which saving writes back), and why the codec could not read it, for the warning that says so.
 */
final class TGFileEntries {

	// "A take in this song could not be read and was left out: {0}", {0} the codec's reason
	private static final String LEFT_OUT = "audio.take.warning.left-out";
	// "{0} takes in this song could not be read and were left out: {1}", {0} how many, {1} each reason once
	private static final String LEFT_OUT_SEVERAL = "audio.take.warning.left-out-several";

	// the version of a file, or why the reader cannot read it
	interface Versions {
		TGVersion readable(InputStream version) throws TGFileFormatException;
	}

	private TGVersion version;
	private byte[] content;
	private final Map<String, TGTake> takes = new HashMap<String, TGTake>();
	// why the codec could not read each take left out, in the file's order
	private final List<String> leftOut = new ArrayList<String>();
	private boolean unknown;

	static TGFileEntries read(InputStream inputStream, TGTakeCodec codec, Versions versions) throws IOException, TGFileFormatException {
		TGFileEntries entries = new TGFileEntries();
		try (ZipArchiveInputStream zip = new ZipArchiveInputStream(new BufferedInputStream(inputStream))) {
			for (ArchiveEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				entries.add(entry.getName(), zip, codec, versions);
			}
		}
		return entries;
	}

	private void add(String name, InputStream entry, TGTakeCodec codec, Versions versions) throws IOException, TGFileFormatException {
		if (TGStream.VERSION_FILE_NAME.equals(name)) {
			this.version = versions.readable(entry);
		} else if (TGStream.CONTENT_FILE_NAME.equals(name)) {
			this.content = IOUtils.toByteArray(entry);
		} else if (this.isTake(name, codec)) {
			this.takes.put(name, this.take(IOUtils.toByteArray(entry), codec));
		} else {
			this.unknown = true;
		}
	}

	// the take of the entry, as the codec reads it; an unreadable take keeping the entry when the codec cannot
	private TGTake take(byte[] entry, TGTakeCodec codec) {
		try {
			return codec.read(new ByteArrayInputStream(entry));
		} catch (IOException e) {
			this.leftOut.add(e.getMessage());
			return TGTake.unreadable(entry);
		}
	}

	// the warning that takes were left out, in TuxGuitar's language: none when every take was read
	List<String> warnings() {
		if (this.leftOut.isEmpty()) {
			return Collections.emptyList();
		}
		String reasons = String.join("; ", new LinkedHashSet<String>(this.leftOut));
		return Collections.singletonList(this.leftOut.size() == 1 ? TGMessagesManager.getProperty(LEFT_OUT, new Object[] {reasons})
			: TGMessagesManager.getProperty(LEFT_OUT_SEVERAL, new Object[] {this.leftOut.size(), reasons}));
	}

	private boolean isTake(String name, TGTakeCodec codec) {
		return this.hasAudio() && name.startsWith(TGAudioPart.TAKES) && name.endsWith("." + codec.getExtension());
	}

	// the version read says format 3.0, whose takes are entries (it is readable only with a take codec)
	boolean hasAudio() {
		return (this.version != null && this.version.getMajor() >= TGStream.AUDIO_FILE_FORMAT_TGVERSION.getMajor());
	}

	// none when the file has no version.txt
	TGVersion getVersion() {
		return this.version;
	}

	// none when the file has no content.xml
	InputStream getContent() {
		return (this.content != null ? new ByteArrayInputStream(this.content) : null);
	}

	// the takes read, unnumbered, by entry name
	Map<String, TGTake> getTakes() {
		return this.takes;
	}

	// an entry the file's version does not have (a take's in format 2.0)
	boolean hasUnknown() {
		return this.unknown;
	}
}
