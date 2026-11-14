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
 * that the Neural Amp Modeler plugin plays them, and whether it is installed; each module it skipped, and why (long:
 * after the hint); last, the folders scanned.
 */
public final class TGPluginScan {

	// the plugin that plays NAM captures, named without regard to spaces and case (it installs as "NeuralAmpModeler")
	public static final String CAPTURE_PLAYER = "Neural Amp Modeler";
	// what the name of a NAM capture's file ends with
	public static final String CAPTURE = ".nam";
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

	private static String captures(int count, List<TGVst3Plugin> plugins) {
		String player = (plugins.stream().anyMatch(TGPluginScan::playsCaptures) ? ". Add it" : ", and it is not installed. Install it, add it");
		return "NAM captures found: " + count + ". A NAM capture is not a plugin: the " + CAPTURE_PLAYER + " plugin plays it" + player
			+ " as a VST3 effect, then load the capture in its editor.";
	}

	private static boolean playsCaptures(TGVst3Plugin plugin) {
		return plugin.getName().replace(" ", "").equalsIgnoreCase(CAPTURE_PLAYER.replace(" ", ""));
	}

	public List<String> getLines() {
		return this.lines;
	}

	// the lines one under the other
	public String toString() {
		return String.join("\n", this.lines);
	}
}
