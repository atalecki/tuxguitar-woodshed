package app.tuxguitar.util.log;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.tuxguitar.util.TGVersion;

/**
 * TuxGuitar's log as a whole: the level of every TuxGuitar logger ("app.tuxguitar" and below), chosen by
 * {@link TGLogChoice}, and the rotating file it is written to once the application opens it. Until then (and in
 * headless modules that never open it) what the level shows goes to the console (System.err as it was when this was
 * set up), one line each.
 *
 * The file is written after what is there: tuxguitar-0.log, which moves to tuxguitar-1.log when full, and so on, up to
 * FILE_COUNT files of FILE_LIMIT bytes. Each session starts with the versions, the level and the file's path; each
 * change of level is written too, whatever the level.
 */
public final class TGLogSetup {

	public static final String ROOT_LOGGER = "app.tuxguitar";
	public static final String FOLDER = "log";
	public static final String FILE_NAME = "tuxguitar-%g.log";
	public static final int FILE_LIMIT = 5 * 1024 * 1024;
	public static final int FILE_COUNT = 5;

	private static final TGLogSetup INSTANCE = new TGLogSetup(Logger.getLogger(ROOT_LOGGER), System.getProperty(TGLogChoice.LEVEL_PROPERTY));

	private final Logger root;
	private final String commandLine;
	private final Handler console;
	private TGLogLevel settings;
	private boolean debugHelper;
	private Handler file;
	private String levelWritten;

	TGLogSetup(Logger root, String commandLine) {
		this.root = root;
		this.commandLine = commandLine;
		this.console = new ConsoleHandler();
		this.console.setFormatter(new TGLogFormatter());
		this.console.setLevel(Level.ALL);
		this.root.setUseParentHandlers(false);
		this.root.addHandler(this.console);
		this.apply();
	}

	public static TGLogSetup getInstance() {
		return INSTANCE;
	}

	public synchronized TGLogChoice getChoice() {
		return TGLogChoice.resolve(TGLogLevel.parse(this.commandLine), this.settings, this.debugHelper);
	}

	// the level the settings ask for, null for none
	public synchronized void setSettingsLevel(TGLogLevel level) {
		this.settings = level;
		this.apply();
	}

	public synchronized void setDebugHelper(boolean enabled) {
		this.debugHelper = enabled;
		this.apply();
	}

	// starts writing the log into that folder; returns the file written now
	public File openFile(File folder) throws IOException {
		return this.openFile(folder, FILE_LIMIT, FILE_COUNT);
	}

	synchronized File openFile(File folder, int limit, int count) throws IOException {
		this.closeFile();
		folder.mkdirs();
		FileHandler handler = new FileHandler(folder.getAbsolutePath().replace("%", "%%") + "/" + FILE_NAME, limit, count, true);
		handler.setEncoding("UTF-8");
		handler.setFormatter(new TGLogFormatter());
		handler.setLevel(Level.ALL);
		this.file = handler;
		this.root.removeHandler(this.console);
		this.root.addHandler(handler);

		File current = new File(folder, FILE_NAME.replace("%g", "0")).getAbsoluteFile();
		this.write(session());
		this.levelWritten = null;
		this.writeLevel(this.getChoice());
		this.writeUnknownLevel();
		this.write("log file " + current + " (" + count + " files of " + (limit / 1024) + " KB)");
		return current;
	}

	public synchronized void closeFile() {
		if (this.file != null) {
			this.root.removeHandler(this.file);
			this.file.close();
			this.file = null;
			this.root.addHandler(this.console);
		}
	}

	private void apply() {
		TGLogChoice choice = this.getChoice();
		this.root.setLevel(choice.getLevel().toJul());
		this.writeLevel(choice);
	}

	private void writeLevel(TGLogChoice choice) {
		String level = "log level " + choice;
		if (!level.equals(this.levelWritten)) {
			this.levelWritten = level;
			this.write(level);
		}
	}

	private void writeUnknownLevel() {
		if (this.commandLine != null && TGLogLevel.parse(this.commandLine) == null) {
			this.write("unknown log level \"" + this.commandLine + "\" in -D" + TGLogChoice.LEVEL_PROPERTY + ", ignored");
		}
	}

	// writes to the file whatever the level
	private void write(String message) {
		if (this.file != null) {
			this.file.publish(new TGLogRecord(TGLogLevel.INFO, message, null, this.root.getName()));
		}
	}

	private static String session() {
		return "TuxGuitar " + TGVersion.CURRENT.getVersion() + " started; Java " + System.getProperty("java.version")
				+ " (" + System.getProperty("java.vendor") + "); " + System.getProperty("os.name") + " "
				+ System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";
	}
}
