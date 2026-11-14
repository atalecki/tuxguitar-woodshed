package app.tuxguitar.audio;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import app.tuxguitar.vst3.TGVst3Plugin;
import app.tuxguitar.vst3.TGVst3SkippedModule;

/**
 * What a scan of the VST3 folders found, as an app shows it to the user, a line each: how many VST3 effects and VST3
 * instruments; when the folders hold NAM captures (CAPTURE files: models of an amp or pedal, which are not plugins),
 * which plugin plays them, and whether one is installed (TGCapturePlayer); each module it skipped, and why (long:
 * after the hint); last, the folders scanned.
 */
public final class TGPluginScan {

	// what the name of a NAM capture's file ends with
	public static final String CAPTURE = ".nam";
	// the captures' hint when no capture player is installed: the two to choose from (both play A2 captures)
	private static final String NO_PLAYER = "a NAM capture player plays it, and the scan recognised none. Install "
		+ TGCapturePlayer.GATEWAY.getLabel() + " (the official Neural Amp Modeler plugin, free from the Neural Amp Modeler website) or "
		+ TGCapturePlayer.TONE3000.getLabel() + " (free from tone3000.com); A2 captures need one of these two. Add the player";
	public static final TGPluginScan NONE = new TGPluginScan(Collections.singletonList("The VST3 folders have not been scanned yet."));

	private final List<String> lines;

	private TGPluginScan(List<String> lines) {
		this.lines = Collections.unmodifiableList(lines);
	}

	// the folders scanned, the plugins found, the modules skipped, and the NAM captures in the folders
	public static TGPluginScan of(List<File> folders, List<TGVst3Plugin> plugins, List<TGVst3SkippedModule> skipped, List<File> captures) {
		List<String> lines = new ArrayList<String>();
		long instruments = plugins.stream().filter(TGVst3Plugin::isInstrument).count();
		lines.add("VST3 effects found: " + (plugins.size() - instruments) + ". VST3 instruments found: " + instruments + ".");
		if (!captures.isEmpty()) {
			lines.add(captures(captures.size(), plugins));
		}
		skipped.forEach(module -> lines.add("Skipped " + module.getModule().getName() + ": " + module.getReason()));
		lines.add("Folders scanned: " + folders.stream().map(File::getPath).collect(Collectors.joining(", ")));
		return new TGPluginScan(lines);
	}

	public static TGPluginScan failed(String reason) {
		return new TGPluginScan(Collections.singletonList("The VST3 folders could not be scanned: " + reason));
	}

	// what a capture is, which player plays it (or which to install), and where the player goes
	private static String captures(int count, List<TGVst3Plugin> plugins) {
		String players = TGCapturePlayer.installed(plugins).stream().map(TGCapturePlayer::getLabel).collect(Collectors.joining(" or "));
		String player = (players.isEmpty() ? NO_PLAYER : players + " plays it. Add " + players);
		return "NAM captures found: " + count + ". A .nam capture is a model of an amp or pedal, not a plugin: " + player
			+ " to the track as a VST3 effect after the instrument, then load the capture in its editor.";
	}

	public List<String> getLines() {
		return this.lines;
	}

	// the lines one under the other
	public String toString() {
		return String.join("\n", this.lines);
	}
}
