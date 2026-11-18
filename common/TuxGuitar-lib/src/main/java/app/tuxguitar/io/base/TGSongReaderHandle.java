package app.tuxguitar.io.base;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TGSongReaderHandle extends TGSongPersistenceHandle {

	private InputStream inputStream;

	// when set, this attribute means a newer file format version was detected when decoding inputStream
	// (to suggest user to upgrade)
	private boolean newerFileFormatDetected = false;

	private final List<String> warnings = new ArrayList<String>();

	public TGSongReaderHandle() {
		super();
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public void setInputStream(InputStream is) {
		this.inputStream = is;
	}

	public boolean isNewerFileFormatDetected() {
		return this.newerFileFormatDetected;
	}

	public void setNewerFileFormatDetected(boolean value) {
		this.newerFileFormatDetected = value;
	}

	// what the reader tells the user about the song it read (a part of it left out), in TuxGuitar's language, in order
	public List<String> getWarnings() {
		return Collections.unmodifiableList(this.warnings);
	}

	public void addWarning(String warning) {
		this.warnings.add(warning);
	}
}
