package app.tuxguitar.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGVst3Plugin;
import app.tuxguitar.vst3.TGVst3SkippedModule;

/**
 * What a scan of the VST3 folders found, as an app shows it to the user, a line each: how many VST3 effects and VST3
 * instruments; when the folders hold NAM captures (CAPTURE files: models of an amp or pedal, which are not plugins),
 * which plugin plays them, and whether one is installed (TGCapturePlayer); each module it skipped, and why (long:
 * after the hint); last, the folders scanned. Each line is said when it is read, in the language TuxGuitar shows then
 * (the messages below); the reasons the host gave stay as it gave them.
 */
public final class TGPluginScan {

	// what the name of a NAM capture's file ends with
	public static final String CAPTURE = ".nam";
	// the lines: no scan yet; a scan failed ({0} why); the effects and instruments found ({0}, {1}); a module skipped ({0}
	// its name, {1} why); the folders scanned ({0})
	private static final String NOT_YET = "audio.scan.not-yet";
	private static final String FAILED = "audio.scan.failed";
	private static final String FOUND = "audio.scan.found";
	private static final String SKIPPED = "audio.scan.skipped";
	private static final String FOLDERS = "audio.scan.folders";
	// the captures' hint ({0} how many): the players installed ({1}, one after the other as PLAYERS joins two), or, none
	// installed, the two to choose from ({1}, {2}: both play A2 captures)
	private static final String CAPTURES = "audio.scan.captures";
	private static final String NO_PLAYER = "audio.scan.captures.no-player";
	private static final String PLAYERS = "audio.scan.players";
	public static final TGPluginScan NONE = new TGPluginScan(Collections.<Supplier<String>>singletonList(() -> say(NOT_YET)));

	private final List<Supplier<String>> lines;

	private TGPluginScan(List<Supplier<String>> lines) {
		this.lines = lines;
	}

	// the folders scanned, the plugins found, the modules skipped, and the NAM captures in the folders
	public static TGPluginScan of(List<File> folders, List<TGVst3Plugin> plugins, List<TGVst3SkippedModule> skipped, List<File> captures) {
		List<Supplier<String>> lines = new ArrayList<Supplier<String>>();
		long instruments = plugins.stream().filter(TGVst3Plugin::isInstrument).count();
		long effects = plugins.size() - instruments;
		lines.add(() -> say(FOUND, effects, instruments));
		if (!captures.isEmpty()) {
			lines.add(captures(captures.size(), TGCapturePlayer.installed(plugins)));
		}
		skipped.forEach(module -> lines.add(() -> say(SKIPPED, module.getModule().getName(), module.getReason())));
		String scanned = folders.stream().map(File::getPath).collect(Collectors.joining(", "));
		lines.add(() -> say(FOLDERS, scanned));
		return new TGPluginScan(lines);
	}

	public static TGPluginScan failed(String reason) {
		return new TGPluginScan(Collections.<Supplier<String>>singletonList(() -> say(FAILED, reason)));
	}

	// what a capture is, which player plays it (or which to install), and where the player goes
	private static Supplier<String> captures(int count, List<TGCapturePlayer> players) {
		if (players.isEmpty()) {
			return () -> say(NO_PLAYER, count, TGCapturePlayer.GATEWAY.getLabel(), TGCapturePlayer.TONE3000.getLabel());
		}
		return () -> say(CAPTURES, count, players.stream().map(TGCapturePlayer::getLabel).reduce((one, other) -> say(PLAYERS, one, other)).get());
	}

	// the message as TuxGuitar's language says it, the arguments in place of {0}, {1}...
	private static String say(String message, Object... arguments) {
		return TGMessagesManager.getProperty(message, arguments);
	}

	public List<String> getLines() {
		return Collections.unmodifiableList(this.lines.stream().map(Supplier::get).collect(Collectors.toList()));
	}

	// the lines one under the other
	public String toString() {
		return String.join("\n", this.getLines());
	}
}
