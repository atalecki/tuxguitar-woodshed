package app.tuxguitar.debug;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.util.log.TGLogLevel;
import app.tuxguitar.util.log.TGLogLineStream;
import app.tuxguitar.util.log.TGLogSetup;
import app.tuxguitar.util.singleton.TGSingletonFactory;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * While the debug helper is on, TuxGuitar's log is at DEBUG at least, and what is printed to the console goes on to it
 * and into the log too, line by line: System.out at INFO ("console.out"), System.err at WARN ("console.err"), in time
 * order with everything else logged.
 */
public class TGSystemOutManager {

	private PrintStream defaultSystemOut;
	private PrintStream defaultSystemErr;

	public TGSystemOutManager() {
		this.defaultSystemOut = System.out;
		this.defaultSystemErr = System.err;
	}

	public void install() throws IOException {
		TGLogSetup.getInstance().setDebugHelper(true);
		System.setOut(capture(this.defaultSystemOut, "console.out", TGLogLevel.INFO));
		System.setErr(capture(this.defaultSystemErr, "console.err", TGLogLevel.WARN));
	}

	public void uninstall() throws IOException {
		System.setOut(this.defaultSystemOut);
		System.setErr(this.defaultSystemErr);
		TGLogSetup.getInstance().setDebugHelper(false);
	}

	private static PrintStream capture(PrintStream console, String log, TGLogLevel level) throws IOException {
		Charset charset = Charset.defaultCharset();
		return new PrintStream(new TGLogLineStream(console, TGLog.named(log), level, charset), true, charset.name());
	}

	public static TGSystemOutManager getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGSystemOutManager.class.getName(), new TGSingletonFactory<TGSystemOutManager>() {
			public TGSystemOutManager createInstance(TGContext context) {
				return new TGSystemOutManager();
			}
		});
	}
}
