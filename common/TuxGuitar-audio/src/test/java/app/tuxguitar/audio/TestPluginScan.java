package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.vst3.TGVst3Plugin;
import app.tuxguitar.vst3.TGVst3SkippedModule;

/**
 * What a scan of the VST3 folders tells the user, a line each: the plugins found, the modules skipped and why, which
 * plugin plays the NAM captures found there, and the folders. Read as an English user reads them: with TuxGuitar's
 * English messages, none again after.
 */
class TestPluginScan {

	private static final List<File> FOLDERS = Arrays.asList(new File("common"), new File("local"));
	private static final List<File> NO_CAPTURES = Collections.<File>emptyList();
	private static final List<TGVst3SkippedModule> NONE_SKIPPED = Collections.<TGVst3SkippedModule>emptyList();
	// the captures' hint when no plugin that plays them is installed: what a capture is, the two players to choose from,
	// and where the player goes
	private static final String NOT_INSTALLED = "NAM captures found: 2. A .nam capture is a model of an amp or pedal, not a plugin:"
		+ " a NAM capture player plays it, and the scan recognised none. Install Gateway (the official Neural Amp Modeler plugin,"
		+ " free from the Neural Amp Modeler website) or the TONE3000 plugin (free from tone3000.com); A2 captures need one of"
		+ " these two. Add the player to the track as a VST3 effect after the instrument, then load the capture in its editor.";

	// the captures' hint when the players named are installed
	private static String installed(String players) {
		return "NAM captures found: 2. A .nam capture is a model of an amp or pedal, not a plugin: " + players + " plays it. Add "
			+ players + " to the track as a VST3 effect after the instrument, then load the capture in its editor.";
	}

	@BeforeEach
	void speakEnglish() {
		TuxGuitarMessages.speak(Map.of());
	}

	@AfterEach
	void speakNone() {
		TGMessagesManager.getInstance().setResources(null);
	}

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
		assertEquals(times(2, installed("the Neural Amp Modeler plugin")), captureLine(Arrays.asList(plugin("NeuralAmpModeler", false))) + " | " + captureLine(Arrays.asList(plugin("Gain", false), plugin("neural amp modeler", false))));
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
		assertEquals(times(3, installed("the TONE3000 plugin")), captureLines("TONE3000", "Tone 3000 Player", "tone3000-nam"));
	}

	// Gateway is the Neural Amp Modeler's new name; the scan reads no vendor and a noise gate may well be called Gateway,
	// so the name must say NAM, Neural Amp Modeler or Atkinson as well; and those words without Gateway play nothing
	@Test
	void gatewayPlaysCapturesOnlyWhenItsNameAlsoSaysNamNeuralAmpModelerOrAtkinson() {
		assertEquals(times(4, installed("Gateway")) + " / " + times(4, NOT_INSTALLED),
			captureLines("NAM Gateway", "Gateway (NAM)", "Gateway Neural Amp Modeler", "Atkinson Gateway") + " / "
			+ captureLines("Gateway", "Dynamic Gateway", "Atkinson Tuner", "Neural Amp Modeler Tuner"));
	}

	// several players installed: each named once, in the order TuxGuitar prefers them, whatever order the scan found them
	@Test
	void withSeveralCapturePlayersInstalledEachIsNamedOnceGatewayFirst() {
		assertEquals(installed("Gateway or the TONE3000 plugin"),
			captureLine(Arrays.asList(plugin("TONE3000", false), plugin("NAM Gateway", false), plugin("Gateway NAM", false))));
	}

	@Test
	void beforeTheFirstScanAndWhenTheScanFailsTheUserIsToldSo() {
		assertEquals("The VST3 folders have not been scanned yet. | The VST3 folders could not be scanned: no VST3 host is configured",
			TGPluginScan.NONE + " | " + TGPluginScan.failed("no VST3 host is configured"));
	}

	// a made-up German: what a scan says, and the capture players' names, as the language TuxGuitar shows says them
	private static final Map<String, String> GERMAN = Map.ofEntries(
		Map.entry("audio.scan.found", "VST3-Effekte: {0}, VST3-Instrumente: {1}."),
		Map.entry("audio.scan.skipped", "Übersprungen: {0} ({1})"),
		Map.entry("audio.scan.folders", "Durchsuchte Ordner: {0}"),
		Map.entry("audio.scan.not-yet", "Die VST3-Ordner sind noch nicht durchsucht."),
		Map.entry("audio.scan.failed", "Die VST3-Ordner ließen sich nicht durchsuchen: {0}"),
		Map.entry("audio.scan.captures", "NAM-Captures: {0}. {1} spielt sie."),
		Map.entry("audio.scan.captures.no-player", "NAM-Captures: {0}. Installiere {1} oder {2}."),
		Map.entry("audio.scan.players", "{0} oder {1}"),
		Map.entry("audio.capture-player.gateway", "Gateway (NAM)"),
		Map.entry("audio.capture-player.tone3000", "das TONE3000-Plugin"),
		Map.entry("audio.capture-player.neural-amp-modeler", "das Neural-Amp-Modeler-Plugin"));

	// the lines are said when they are read, in the language TuxGuitar shows then
	@Test
	void aScanIsSaidAsTheLanguageTuxGuitarShowsSaysIt() {
		List<TGVst3Plugin> plugins = Arrays.asList(plugin("TONE3000", false), plugin("NAM Gateway", false), plugin("Piano", true));
		List<TGVst3SkippedModule> skipped = Arrays.asList(new TGVst3SkippedModule(new File("common", "Broken.vst3"), "the VST3 host stopped"));
		TGPluginScan scan = TGPluginScan.of(FOLDERS, plugins, skipped, captures(2));
		TuxGuitarMessages.speak(GERMAN);
		assertEquals(Arrays.asList("VST3-Effekte: 2, VST3-Instrumente: 1.", "NAM-Captures: 2. Gateway (NAM) oder das TONE3000-Plugin spielt sie.",
			"Übersprungen: Broken.vst3 (the VST3 host stopped)", "Durchsuchte Ordner: " + FOLDERS.get(0) + ", " + FOLDERS.get(1)), scan.getLines());
	}

	@Test
	void beforeTheFirstScanWhenItFailsAndWithoutAPlayerTheUserIsToldSoInTuxGuitarsLanguage() {
		TuxGuitarMessages.speak(GERMAN);
		assertEquals("Die VST3-Ordner sind noch nicht durchsucht. | Die VST3-Ordner ließen sich nicht durchsuchen: no VST3 host is configured"
			+ " | NAM-Captures: 2. Installiere Gateway (NAM) oder das TONE3000-Plugin. | NAM-Captures: 2. das Neural-Amp-Modeler-Plugin spielt sie.",
			TGPluginScan.NONE + " | " + TGPluginScan.failed("no VST3 host is configured") + " | " + captureLine(Collections.<TGVst3Plugin>emptyList())
			+ " | " + captureLine(Arrays.asList(plugin("NeuralAmpModeler", false))));
	}

	@Test
	void theScanReadsItsLinesOneUnderTheOther() {
		assertEquals("VST3 effects found: 0. VST3 instruments found: 0.\nFolders scanned: " + FOLDERS.get(0) + ", " + FOLDERS.get(1),
			TGPluginScan.of(FOLDERS, Collections.<TGVst3Plugin>emptyList(), NONE_SKIPPED, NO_CAPTURES).toString());
	}
}
