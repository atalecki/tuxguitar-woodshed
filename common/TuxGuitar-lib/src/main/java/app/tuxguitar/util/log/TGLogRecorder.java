package app.tuxguitar.util.log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Keeps in memory what is logged: each record both as "LEVEL message (error)" and as its formatted text. A headless
 * module's tests use it to see what TuxGuitar logs: {@link #attach(TGLogLevel)} records everything TuxGuitar logs at
 * that level until {@link #detach()}.
 */
public class TGLogRecorder extends Handler {

	private final List<String> recorded = new ArrayList<String>();
	private final StringBuilder text = new StringBuilder();
	private final TGLogFormatter formatter = new TGLogFormatter();
	private Logger attachedTo;
	private Level levelBefore;

	public TGLogRecorder() {
		this.setLevel(Level.ALL);
	}

	// records what TuxGuitar logs, which is set to log at that level
	public static TGLogRecorder attach(TGLogLevel level) {
		TGLogRecorder recorder = new TGLogRecorder();
		Logger root = Logger.getLogger(TGLogSetup.ROOT_LOGGER);
		TGLogSetup.getInstance();
		recorder.attachedTo = root;
		recorder.levelBefore = root.getLevel();
		root.setLevel(level.toJul());
		root.addHandler(recorder);
		return recorder;
	}

	// what TuxGuitar logs at that level while the code runs
	public static TGLogRecorder during(TGLogLevel level, Runnable code) {
		TGLogRecorder recorder = attach(level);
		try {
			code.run();
		} finally {
			recorder.detach();
		}
		return recorder;
	}

	// stops recording, and puts TuxGuitar's log level back
	public void detach() {
		this.attachedTo.removeHandler(this);
		this.attachedTo.setLevel(this.levelBefore);
	}

	public synchronized void publish(LogRecord record) {
		String thrown = (record.getThrown() != null ? " (" + record.getThrown() + ")" : "");
		this.recorded.add(TGLogLevel.named(record.getLevel()) + " " + record.getMessage() + thrown);
		this.text.append(this.formatter.format(record));
	}

	// each record, "LEVEL message", with " (error)" after it when it has one
	public synchronized List<String> recorded() {
		return new ArrayList<String>(this.recorded);
	}

	// the records as the log file shows them
	public synchronized String text() {
		return this.text.toString();
	}

	public void flush() {
		// nothing buffered
	}

	public void close() {
		// nothing to release
	}
}
