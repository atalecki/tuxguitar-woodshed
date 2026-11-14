package app.tuxguitar.util.log;

import java.util.logging.Logger;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A logger of a test's own, outside TuxGuitar's, whose records a TGLogRecorder keeps. Register it with
 * {@code @RegisterExtension}: each test gets a new logger, and the recorder is taken off it after the test.
 */
final class RecordedLogger implements BeforeEachCallback, AfterEachCallback {

	final TGLogRecorder recorder = new TGLogRecorder();
	Logger logger;

	public void beforeEach(ExtensionContext context) {
		this.logger = Logger.getLogger("test.recorded." + System.nanoTime());
		this.logger.setUseParentHandlers(false);
		this.logger.addHandler(this.recorder);
	}

	public void afterEach(ExtensionContext context) {
		this.logger.removeHandler(this.recorder);
	}
}
