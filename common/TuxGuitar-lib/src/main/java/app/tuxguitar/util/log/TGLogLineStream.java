package app.tuxguitar.util.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A console stream that also logs each line written to it: the bytes go on to the console as they are, and each line
 * (without its line break) is logged at the stream's level. What the log prints to the console while logging a line
 * goes to the console only, so the log never feeds itself.
 */
public class TGLogLineStream extends OutputStream {

	private static final ThreadLocal<Boolean> LOGGING = new ThreadLocal<Boolean>();

	private final OutputStream console;
	private final TGLog log;
	private final TGLogLevel level;
	private final Charset charset;
	private final ByteArrayOutputStream line = new ByteArrayOutputStream();

	public TGLogLineStream(OutputStream console, TGLog log, TGLogLevel level, Charset charset) {
		this.console = console;
		this.log = log;
		this.level = level;
		this.charset = charset;
	}

	public void write(int b) throws IOException {
		this.write(new byte[] {(byte) b}, 0, 1);
	}

	public void write(byte[] bytes, int offset, int length) throws IOException {
		this.console.write(bytes, offset, length);
		this.log(this.lines(bytes, offset, length));
	}

	public void flush() throws IOException {
		this.console.flush();
	}

	// logs the line not ended yet; the console stays open
	public void close() throws IOException {
		this.console.flush();
		this.log(this.rest());
	}

	private synchronized List<String> lines(byte[] bytes, int offset, int length) {
		List<String> lines = new ArrayList<String>();
		for (int i = offset; i < offset + length; i++) {
			if (bytes[i] == '\n') {
				lines.add(this.take());
			} else {
				this.line.write(bytes[i]);
			}
		}
		return lines;
	}

	private synchronized List<String> rest() {
		List<String> lines = new ArrayList<String>();
		if (this.line.size() > 0) {
			lines.add(this.take());
		}
		return lines;
	}

	private String take() {
		String text = new String(this.line.toByteArray(), this.charset);
		this.line.reset();
		return (text.endsWith("\r") ? text.substring(0, text.length() - 1) : text);
	}

	private void log(List<String> lines) {
		if (LOGGING.get() == null) {
			LOGGING.set(Boolean.TRUE);
			try {
				for (String text : lines) {
					this.log.log(this.level, text, null);
				}
			} finally {
				LOGGING.remove();
			}
		}
	}
}
