package app.tuxguitar.app.system.log;

import java.io.File;
import java.io.IOException;

import app.tuxguitar.app.system.config.TGConfigKeys;
import app.tuxguitar.app.system.config.TGConfigManager;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGUserFileUtils;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.util.log.TGLogLevel;
import app.tuxguitar.util.log.TGLogSetup;

/**
 * The application's log: written to the "log" folder of TuxGuitar's user folder from the start, at the level the
 * command line, the debug helper plugin or the settings choose (see TGLogChoice).
 */
public class TGLogAdapter {

	private static final TGLog LOG = TGLog.get(TGLogAdapter.class);

	// where the log files are
	public static File folder() {
		return new File(TGUserFileUtils.PATH_USER_DIR, TGLogSetup.FOLDER);
	}

	// starts writing the log file; when it cannot be written the log stays on the console
	public static void start() {
		try {
			TGLogSetup.getInstance().openFile(folder());
		} catch (IOException e) {
			LOG.warn("the log file cannot be written in " + folder(), e);
		}
	}

	// the level of the "Log level" setting
	public static void applySettings(TGContext context) {
		TGLogSetup.getInstance().setSettingsLevel(TGLogLevel.parse(TGConfigManager.getInstance(context).getStringValue(TGConfigKeys.LOG_LEVEL)));
	}
}
