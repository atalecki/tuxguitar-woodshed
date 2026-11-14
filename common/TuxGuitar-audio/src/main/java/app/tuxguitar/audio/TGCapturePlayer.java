package app.tuxguitar.audio;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The VST3 effects known to play NAM captures (.nam files: models of an amp or pedal, which are not plugins), the
 * capture players, in the order TuxGuitar prefers them. A scan knows a plugin by its name only (TGVst3Plugin has no
 * vendor), read lowercased with only its letters and digits. Only NEURAL_AMP_MODELER's name is confirmed; the names
 * GATEWAY and TONE3000 install as are not, so their rules are guesses that err on the side of not counting a plugin.
 */
public enum TGCapturePlayer {

	// Atkinson Advanced Modeling's free plugin, the Neural Amp Modeler's new name; plays A1 and A2 captures. UNCONFIRMED
	// name. "Gateway" alone proves nothing (a noise gate may be called that, and the scan reads no vendor), so the name
	// must also say NAM, Neural Amp Modeler or Atkinson
	GATEWAY("Gateway", name -> name.contains("gateway") && Stream.of("namgateway", "gatewaynam", "neuralampmodeler", "atkinson").anyMatch(name::contains)),
	// TONE3000's free player plugin; plays A2 captures. UNCONFIRMED name: any that holds TONE3000
	TONE3000("the TONE3000 plugin", name -> name.contains("tone3000")),
	// the plugin Gateway was before (sdatkinson/NeuralAmpModelerPlugin): it installs as "NeuralAmpModeler"
	NEURAL_AMP_MODELER("the Neural Amp Modeler plugin", name -> name.equals("neuralampmodeler"));

	// how the user is told its name
	private final String label;
	// whether a plugin's name, lowercased with only its letters and digits, is this player's
	private final Predicate<String> named;

	private TGCapturePlayer(String label, Predicate<String> named) {
		this.label = label;
		this.named = named;
	}

	public String getLabel() {
		return this.label;
	}

	public boolean plays(TGVst3Plugin plugin) {
		return this.named.test(plugin.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
	}

	// the capture players among the plugins, in the order TuxGuitar prefers them
	public static List<TGCapturePlayer> installed(List<TGVst3Plugin> plugins) {
		return Arrays.stream(values()).filter(player -> plugins.stream().anyMatch(player::plays)).collect(Collectors.toList());
	}
}
