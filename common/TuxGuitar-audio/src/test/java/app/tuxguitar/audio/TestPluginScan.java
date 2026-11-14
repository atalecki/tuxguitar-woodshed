package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.vst3.TGVst3Plugin;
import app.tuxguitar.vst3.TGVst3SkippedModule;

/**
 * What a scan of the VST3 folders tells the user, a line each: the plugins found, the modules skipped and why, which
 * plugin plays the NAM captures found there, and the folders.
 */
class TestPluginScan {

	private static final List<File> FOLDERS = Arrays.asList(new File("common"), new File("local"));
	private static final List<File> NO_CAPTURES = Collections.<File>emptyList();
	private static final List<TGVst3SkippedModule> NONE_SKIPPED = Collections.<TGVst3SkippedModule>emptyList();
	// the captures' hint when no plugin that plays them is installed, and when one is
	private static final String NOT_INSTALLED = "NAM captures found: 2. A NAM capture is not a plugin: the Neural Amp Modeler plugin plays it, and it is not installed."
		+ " Install it, add it as a VST3 effect, then load the capture in its editor.";
	private static final String INSTALLED = "NAM captures found: 2. A NAM capture is not a plugin: the Neural Amp Modeler plugin plays it."
		+ " Add it as a VST3 effect, then load the capture in its editor.";

	private static TGVst3Plugin plugin(String name, boolean instrument) {
		return new TGVst3Plugin(name + ".vst3", "class", name, instrument);
	}

	private static List<File> captures(int count) {
		return Collections.nCopies(count, new File("Lead.nam"));
	}

	private static String captureLine(List<TGVst3Plugin> plugins) {
		return TGPluginScan.of(FOLDERS, plugins, NONE_SKIPPED, captures(2)).getLines().get(1);
	}

	@Test
	void aScanTellsHowManyEffectsAndInstrumentsItFoundEachModuleItSkippedAndWhereItLooked() {
		List<TGVst3Plugin> plugins = Arrays.asList(plugin("Amp", false), plugin("Piano", true), plugin("Echo", false));
		List<TGVst3SkippedModule> skipped = Arrays.asList(new TGVst3SkippedModule(new File("local", "NamModels.vst3"), "not a VST3 module: local/NamModels.vst3"),
			new TGVst3SkippedModule(new File("common", "Broken.vst3"), "the VST3 host stopped"));
		assertEquals(Arrays.asList("VST3 effects found: 2. VST3 instruments found: 1.",
			"Skipped NamModels.vst3: not a VST3 module: local/NamModels.vst3",
			"Skipped Broken.vst3: the VST3 host stopped",
			"Folders scanned: " + FOLDERS.get(0) + ", " + FOLDERS.get(1)),
			TGPluginScan.of(FOLDERS, plugins, skipped, NO_CAPTURES).getLines());
	}

	// what the user most likely looks for comes first: the captures' hint before the long reasons of the modules skipped
	@Test
	void theCapturesHintComesBeforeTheModulesSkipped() {
		List<TGVst3SkippedModule> skipped = Arrays.asList(new TGVst3SkippedModule(new File("local", "NamModels.vst3"), "not a VST3 module"));
		List<String> lines = TGPluginScan.of(FOLDERS, Collections.<TGVst3Plugin>emptyList(), skipped, captures(1)).getLines();
		assertEquals("NAM captures found: 1. | Skipped NamModels.vst3: not a VST3 module", lines.get(1).substring(0, lines.get(1).indexOf('.') + 1) + " | " + lines.get(2));
	}

	// a capture is a model of an amp or pedal, not a plugin: the user learns which plugin plays it
	@Test
	void nAMCapturesInTheFoldersAreSaidToBePlayedByTheNeuralAmpModelerWhichIsNotInstalled() {
		assertEquals(NOT_INSTALLED, captureLine(Arrays.asList(plugin("Neural Amp", false))));
	}

	// the old plugin installs as "NeuralAmpModeler"; its name is matched without spaces and case
	@Test
	void withTheNeuralAmpModelerInstalledTheUserIsToldToLoadTheCaptureInIt() {
		assertEquals(INSTALLED + " | " + INSTALLED, captureLine(Arrays.asList(plugin("NeuralAmpModeler", false))) + " | " + captureLine(Arrays.asList(plugin("Gain", false), plugin("neural amp modeler", false))));
	}

	// the hint of two captures with each plugin named installed beside a "Gain", one after the other
	private static String captureLines(String... names) {
		return Arrays.stream(names).map(name -> captureLine(Arrays.asList(plugin("Gain", false), plugin(name, false)))).collect(Collectors.joining(" | "));
	}

	private static String times(int count, String line) {
		return String.join(" | ", Collections.nCopies(count, line));
	}

	// TONE3000's player plugin: its exact VST3 name is not known, so any name holding TONE3000, whatever case and spacing
	@Test
	void theTone3000PluginPlaysCapturesWhateverTheCaseAndSpacingOfItsName() {
		assertEquals(times(3, INSTALLED), captureLines("TONE3000", "Tone 3000 Player", "tone3000-nam"));
	}

	// Gateway is the Neural Amp Modeler's new name; the scan reads no vendor and a noise gate may well be called Gateway,
	// so the name must say NAM, Neural Amp Modeler or Atkinson as well; and those words without Gateway play nothing
	@Test
	void gatewayPlaysCapturesOnlyWhenItsNameAlsoSaysNamNeuralAmpModelerOrAtkinson() {
		assertEquals(times(4, INSTALLED) + " / " + times(4, NOT_INSTALLED),
			captureLines("NAM Gateway", "Gateway (NAM)", "Gateway Neural Amp Modeler", "Atkinson Gateway") + " / "
			+ captureLines("Gateway", "Dynamic Gateway", "Atkinson Tuner", "Neural Amp Modeler Tuner"));
	}

	@Test
	void beforeTheFirstScanAndWhenTheScanFailsTheUserIsToldSo() {
		assertEquals("The VST3 folders have not been scanned yet. | The VST3 folders could not be scanned: no VST3 host is configured",
			TGPluginScan.NONE + " | " + TGPluginScan.failed("no VST3 host is configured"));
	}

	@Test
	void theScanReadsItsLinesOneUnderTheOther() {
		assertEquals("VST3 effects found: 0. VST3 instruments found: 0.\nFolders scanned: " + FOLDERS.get(0) + ", " + FOLDERS.get(1),
			TGPluginScan.of(FOLDERS, Collections.<TGVst3Plugin>emptyList(), NONE_SKIPPED, NO_CAPTURES).toString());
	}
}
