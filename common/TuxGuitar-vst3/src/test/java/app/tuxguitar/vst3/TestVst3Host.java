package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The VST3 host process, driven as TuxGuitar drives it, with the real test plugins.
 */
public class TestVst3Host {

	private static final int FRAMES = 512;

	private TGVst3Host host;

	@BeforeEach
	public void start() throws TGVst3Exception {
		this.host = TGVst3Host.start(TestPlugins.host());
	}

	@AfterEach
	public void stop() {
		this.host.close();
	}

	private TGVst3Plugin plugin(String name) throws TGVst3Exception {
		return this.host.scan(TestPlugins.bundle(name)).get(0);
	}

	private TGVst3Instance open(String name) throws TGVst3Exception {
		return this.host.open(plugin(name), 44100f, FRAMES);
	}

	private static float[][] block(float left, float right) {
		return TestPlugins.block(FRAMES, left, right);
	}

	private static float[][] process(TGVst3Instance instance, float[][] block) throws TGVst3Exception {
		instance.process(block, FRAMES, TGVst3Transport.STOPPED, Collections.<TGVst3NoteEvent>emptyList());
		return block;
	}

	@Test
	public void theHostRunsInAProcessOfItsOwn() {
		assertTrue(this.host.isRunning());
		assertNotEquals(ProcessHandle.current().pid(), this.host.getPid());
		assertTrue(ProcessHandle.of(this.host.getPid()).isPresent());
	}

	// as the native build names it on this system, and on Windows and elsewhere
	@Test
	public void theHostsExecutableIsNamedForTheSystem() {
		File bin = new File(TestPlugins.nativeBuild(), "bin");
		assertEquals(TestPlugins.host() + " | " + new File("lib", "tuxguitar-vst3-host.exe") + " " + new File("lib", "tuxguitar-vst3-host"), TGVst3Hosting.executable(bin, System.getProperty("os.name")) + " | " + TGVst3Hosting.executable(new File("lib"), "Windows 11") + " " + TGVst3Hosting.executable(new File("lib"), "Linux"));
	}

	@Test
	public void aHostOfAnotherProtocolVersionIsRefused() throws TGVst3Exception {
		assertEquals(42L, TGVst3Protocol.greeted(TGVst3Protocol.VERSION, 42));
		TGVst3Exception refused = assertThrows(TGVst3Exception.class, () -> TGVst3Protocol.greeted(TGVst3Protocol.VERSION + 1, 42));
		assertEquals("the VST3 host speaks version 11, not 10", refused.getMessage());
	}

	@Test
	public void theHostTellsWhichPluginsItOpened() throws TGVst3Exception {
		open("Test Gain");
		open("Test Sine");
		assertEquals(Arrays.asList("Test Gain", "Test Sine"), this.host.getOpened());
	}

	@Test
	public void theHostListensOnNoNetworkPort() throws TGVst3Exception {
		assertEquals(0, this.host.networkListeners(this.host.getPid()));
	}

	@Test
	public void scanningABundleListsItsEffectOrInstrument() throws TGVst3Exception {
		TGVst3Plugin gain = plugin("Test Gain");
		TGVst3Plugin sine = plugin("Test Sine");
		assertEquals("Test Gain", gain.getName());
		assertFalse(gain.isInstrument());
		assertEquals("Test Sine", sine.getName());
		assertTrue(sine.isInstrument());
		assertEquals(TestPlugins.bundle("Test Gain").getPath(), gain.getModule());
		assertNotEquals(gain.getClassId(), sine.getClassId());
	}

	@Test
	public void aVst2PluginIsNotAVst3Module() {
		File oldAmp = new File(TestPlugins.vst2Folder(), "Old Amp.dll");
		TGVst3Exception refused = assertThrows(TGVst3Exception.class, () -> this.host.scan(oldAmp));
		assertTrue(refused.getMessage().startsWith("not a VST3 module"), refused.getMessage());
	}

	// the host's reason is one line, as an app shows it (Windows ends its system messages with a line break)
	@Test
	public void aRefusalReadsOnOneLine() {
		File missing = new File(TestPlugins.vst2Folder(), "Missing.vst3");
		String refused = assertThrows(TGVst3Exception.class, () -> this.host.scan(missing)).getMessage();
		assertEquals("not a VST3 module: " + missing.getPath() + " (" + " | false | false | true", refused.substring(0, ("not a VST3 module: " + missing.getPath() + " (").length()) + " | "
			+ (refused.contains("\n") || refused.contains("\r")) + " | " + refused.contains(" )") + " | " + refused.endsWith(")"));
	}

	@Test
	public void anEffectProcessesABlockThroughTheSharedFile() throws TGVst3Exception {
		float[][] block = process(open("Test Gain"), block(0.25f, -0.1f));
		assertEquals(0.5f, block[0][0]);
		assertEquals(-0.2f, block[1][FRAMES - 1], 1e-7f);
	}

	@Test
	public void aParameterIsSetByItsPlainValue() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		assertEquals(0.25, gain.setParameter(gain.findParameter("Gain"), 0.25));
		assertEquals(0.0625f, process(gain, block(0.25f, 0.25f))[0][10]);
		assertEquals(0.25, gain.getParameter(gain.findParameter("Gain")));
	}

	@Test
	public void aParameterNotThereIsNotFound() throws TGVst3Exception {
		assertEquals(-1, open("Test Gain").findParameter("Volume"));
	}

	// what Test Tempo was told in its first block: tempo, numerator, denominator, playing, position, sample
	private static String told(byte[] report) {
		java.nio.ByteBuffer told = java.nio.ByteBuffer.wrap(report).order(java.nio.ByteOrder.nativeOrder());
		return told.getDouble() + " " + told.getInt() + "/" + told.getInt() + " " + told.getInt() + " " + told.getDouble() + " " + told.getLong();
	}

	@Test
	public void aPluginIsToldTheTransport() throws TGVst3Exception {
		TGVst3Instance tempo = open("Test Tempo");
		tempo.process(block(0f, 0f), FRAMES, new TGVst3Transport(true, 96.0, 3, 4, 4.5, 1000), Collections.<TGVst3NoteEvent>emptyList());
		byte[] report = tempo.message("TG.report", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		assertEquals("96.0 3/4 1 4.5 1000", told(report));
		TGVst3Instance stopped = open("Test Tempo");
		process(stopped, block(0f, 0f));
		assertEquals("120.0 4/4 0 0.0 0", told(stopped.message("TG.report", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap())));
	}

	// each with its plain values at either end and its steps (none: continuous); Test Gain's two presets are one step
	@Test
	public void aPluginListsItsParametersWithTheirValues() throws TGVst3Exception {
		List<String> read = new ArrayList<String>();
		for (TGVst3Parameter parameter : open("Test Gain").getParameters()) {
			read.add(parameter.getTitle() + "=" + parameter.getPlain() + " " + parameter.getMinimum() + ".." + parameter.getMaximum() + " in " + parameter.getSteps()
				+ (parameter.choosesPresets() ? " (presets)" : ""));
		}
		assertEquals(Arrays.asList("Gain=2.0 0.0..16.0 in 0", "Preset=0.0 0.0..1.0 in 1 (presets)"), read);
	}

	@Test
	public void anInstanceReportsItsBusesAndEditor() throws TGVst3Exception {
		TGVst3Instance stereo = open("Test Stereo");
		TGVst3Instance clip = open("Test Clip 0.5");
		assertEquals(1, stereo.getInputs());
		assertEquals(2, stereo.getOutputs());
		assertTrue(stereo.hasEditor());
		assertFalse(clip.hasEditor());
	}

	@Test
	public void aMonoEffectTakesTheMeanOfBothSides() throws TGVst3Exception {
		float[][] block = process(open("Test Stereo"), block(0.4f, 0.2f));
		assertEquals(0.3f, block[0][0], 1e-6f);
		assertEquals(0.15f, block[1][0], 1e-6f);
	}

	@Test
	public void latencyAndTailAreReported() throws TGVst3Exception {
		TGVst3Instance latency = open("Test Latency");
		assertEquals(0, latency.getLatency());
		latency.setParameter(latency.findParameter("Latency"), 2048);
		assertEquals(2048, latency.getLatency());
		assertEquals(13230, open("Test Echo").getTail());
		assertEquals(TGVst3Instance.INFINITE_TAIL, open("Test Drone").getTail());
		assertEquals(0, open("Test Gain").getTail());
	}

	@Test
	public void aPluginDelaysByItsLatency() throws TGVst3Exception {
		TGVst3Instance latency = open("Test Latency");
		latency.setParameter(latency.findParameter("Latency"), 100);
		float[][] block = new float[2][FRAMES];
		block[0][0] = 1f;
		process(latency, block);
		assertEquals(0f, block[0][0]);
		assertEquals(1f, block[0][100]);
	}

	@Test
	public void anInstrumentPlaysNotesOnTheirSamples() throws TGVst3Exception {
		TGVst3Instance click = open("Test Click");
		float[][] block = new float[2][FRAMES];
		click.process(block, FRAMES, TGVst3Transport.STOPPED, Arrays.asList(TGVst3NoteEvent.noteOn(300, 60, 1, 1f)));
		assertEquals(0f, block[0][299]);
		assertEquals(0.5f, block[0][300]);
		assertEquals(0.5f, block[1][300]);
	}

	@Test
	public void aStateRestoredInAnotherInstanceSoundsTheSame() throws TGVst3Exception {
		TGVst3Instance first = open("Test Gain");
		first.setParameter(first.findParameter("Gain"), 3.0);
		TGVst3State state = first.getState();
		TGVst3Instance second = open("Test Gain");
		second.setState(state);
		assertEquals(1024, state.getComponent().length);
		assertEquals(0.75f, process(second, block(0.25f, 0.25f))[0][0]);
		assertArrayEquals(state.getComponent(), second.getState().getComponent());
	}

	// Woodshed Guitar (TuxGuitar's own plugin) keeps its profile's contents apart, as a part of its state named "profile";
	// a state given back with it plays that profile
	@Test
	public void thePartsAPluginKeepsApartComeBackWithItsState(@TempDir File folder) throws Exception {
		File capture = TestPlugins.gain3(folder);
		TGVst3Instance first = this.woodshed();
		load(first, capture);
		// it loads on a thread of its own: once it says it loads nothing (its status's fourth line), it has
		while (!TestPlugins.profile(first).endsWith("\n")) {
			Thread.onSpinWait();
		}
		TGVst3State state = first.getState();
		TGVst3Instance second = this.woodshed();
		second.setState(state);
		assertEquals("[profile] true Gain 3.nam", state.getResources().keySet() + " " + Arrays.equals(Files.readAllBytes(capture.toPath()), state.getResources().get("profile"))
			+ " " + TestPlugins.profile(second).split("\n")[0]);
	}

	// a profile loaded as the editor's Load profile... loads it is a change of the plugin, which tells its host it is
	// dirty; the same profile loaded because the host asked for it ("host": the profile a default instrument's element
	// names, loaded again after an Undo) is no change the user made, and is not told
	@Test
	public void aProfileTheHostAsksWoodshedGuitarToLoadIsLoadedAndToldAsNoChange(@TempDir File folder) throws Exception {
		File capture = TestPlugins.gain3(folder);

		assertEquals("Gain 3.nam: DIRTY 0 | Gain 3.nam: ", loadHeard(capture, Collections.<String, Long>emptyMap()) + " | " + loadHeard(capture, Map.of("host", 1L)));
	}

	// a Woodshed Guitar asked to load the capture: once it has, the profile it holds and the changes it told
	private String loadHeard(File capture, Map<String, Long> asked) throws TGVst3Exception {
		TGVst3Instance woodshed = this.woodshed();
		List<TGVst3Notice> notices = Collections.synchronizedList(new ArrayList<TGVst3Notice>());
		woodshed.setListener(notice -> Optional.of(notice).filter(dirty -> dirty.getKind() == TGVst3Notice.Kind.DIRTY).ifPresent(notices::add));
		woodshed.message("Woodshed.load", asked, Collections.singletonMap("path", capture.getPath().getBytes(StandardCharsets.UTF_8)));
		while (!TestPlugins.profile(woodshed).endsWith("\n")) {
			Thread.onSpinWait();
		}
		return TestPlugins.profile(woodshed).split("\n")[0] + ": " + heard(notices);
	}

	// Woodshed Guitar, TuxGuitar's own plugin
	private TGVst3Instance woodshed() throws TGVst3Exception {
		TGVst3Plugin woodshed = this.host.scan(new File(new File(TestPlugins.nativeBuild(), "plugins/VST3"), "Woodshed Guitar.vst3")).get(0);
		return this.host.open(woodshed, 44100f, FRAMES);
	}

	private static void load(TGVst3Instance woodshed, File capture) throws TGVst3Exception {
		woodshed.message("Woodshed.load", Collections.<String, Long>emptyMap(), Collections.singletonMap("path", capture.getPath().getBytes(StandardCharsets.UTF_8)));
	}

	// a plugin restoring its state in the background (it told its host, IProgress) is restoring until it says it has
	// finished; one that never began is not
	@Test
	public void aPluginRestoringItsStateInTheBackgroundIsRestoringUntilItSaysItHasFinished() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		boolean before = gain.isRestoring();
		restoring(gain, 1L);
		boolean during = gain.isRestoring();
		restoring(gain, 0L);
		assertEquals("false true false", before + " " + during + " " + gain.isRestoring());
	}

	private static void restoring(TGVst3Instance instance, long underway) throws TGVst3Exception {
		instance.message("TG.restoring", Collections.singletonMap("underway", underway), Collections.<String, byte[]>emptyMap());
	}

	// a plugin that never says it has finished is waited for no longer than the wait, from when it was first heard
	@Test
	public void aPluginThatNeverFinishesRestoringIsNoLongerRestoringOnceTheWaitHasPassed() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		restoring(gain, 1L);
		long heard = System.nanoTime();
		boolean within = gain.isRestoring(300);
		LockSupport.parkNanos(Duration.ofMillis(100).toNanos());
		boolean still = gain.isRestoring(300);
		// a park may end early: until the wait has passed
		while (System.nanoTime() - heard < Duration.ofMillis(300).toNanos()) {
			LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
		}
		assertEquals("true true false", within + " " + still + " " + gain.isRestoring(300));
	}

	// a plugin restoring its state whose host has stopped has nothing left to wait for
	@Test
	public void aPluginRestoringItsStateWhoseHostStoppedIsNoLongerRestoring() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		restoring(gain, 1L);
		TestPlugins.kill(this.host.getPid());
		assertFalse(gain.isRestoring());
	}

	// a task the plugin's editor runs in the background (IProgress) restores nothing: it is not waited for, and its end
	// does not end a restoration under way
	@Test
	public void aTaskOfThePluginsEditorInTheBackgroundIsNoRestorationAndEndsNone() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.message("TG.background", Collections.singletonMap("underway", 1L), Collections.<String, byte[]>emptyMap());
		boolean task = gain.isRestoring();
		restoring(gain, 1L);
		gain.message("TG.background", Collections.singletonMap("underway", 0L), Collections.<String, byte[]>emptyMap());
		assertEquals("false true", task + " " + gain.isRestoring());
	}

	// a plugin restoring its state says what it restores when it begins (IProgress's description; nothing said: empty),
	// asked of its host however long it has; null when it is not restoring
	@Test
	public void aPluginRestoringItsStateSaysWhatItRestoresUntilItHasFinished() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		String before = gain.getRestoring();
		gain.message("TG.restoring", Collections.singletonMap("underway", 1L), Collections.singletonMap("what", "Big Room ü.wav".getBytes(StandardCharsets.UTF_8)));
		String during = gain.getRestoring();
		restoring(gain, 0L);
		String after = gain.getRestoring();
		restoring(gain, 1L);
		assertEquals("null Big Room ü.wav null []", before + " " + during + " " + after + " [" + gain.getRestoring() + "]");
	}

	// asked of a host that stopped, a plugin restores nothing
	@Test
	public void aPluginRestoringItsStateWhoseHostStoppedRestoresNothing() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		restoring(gain, 1L);
		TestPlugins.kill(this.host.getPid());
		assertNull(gain.getRestoring());
	}

	@Test
	public void aPluginIsNamedAsItsModuleNamesIt() throws TGVst3Exception {
		assertEquals("Test Gain", open("Test Gain").getName());
	}

	// Woodshed Guitar says the file of the profile it loads; a load given while one is under way, its own file
	@Test
	public void woodshedGuitarRestoringItsStateSaysTheFileItLoads(@TempDir File folder) throws Exception {
		File large = large(folder);
		File capture = TestPlugins.gain3(folder);
		TGVst3Instance woodshed = this.woodshed();
		load(woodshed, large);
		String first = woodshed.getRestoring();
		load(woodshed, capture);
		String second = woodshed.getRestoring();
		TestPlugins.untilRestored(woodshed);
		assertEquals("Large.nam, Gain 3.nam, null", first + ", " + second + ", " + woodshed.getRestoring());
	}

	// Woodshed Guitar restores its state while it loads a profile on a thread of its own: its host, asked, hears when it
	// has finished, and the profile is then loaded; the next load restores it again
	@Test
	public void woodshedGuitarRestoresItsStateUntilTheProfileItLoadsIsReady(@TempDir File folder) throws Exception {
		TGVst3Instance woodshed = this.woodshed();
		File capture = TestPlugins.gain3(folder);
		load(woodshed, capture);
		boolean loading = woodshed.isRestoring();
		TestPlugins.untilRestored(woodshed);
		String loaded = TestPlugins.profile(woodshed);
		load(woodshed, capture);
		assertEquals("true Gain 3.nam\n\n\n true", loading + " " + loaded + " " + woodshed.isRestoring());
	}

	// loads given one after the other, the first not ready yet, are one restoration, which ends once the last is ready
	@Test
	public void woodshedGuitarLoadingTwiceInARowRestoresItsStateUntilTheLastIsReady(@TempDir File folder) throws Exception {
		File capture = large(folder);
		TGVst3Instance woodshed = this.woodshed();
		load(woodshed, capture);
		load(woodshed, capture);
		TestPlugins.untilRestored(woodshed);
		assertEquals("Large.nam\n\n\n", TestPlugins.profile(woodshed));
	}

	// a NAM capture "Large.nam" in the folder, multiplying the signal by 3, with 5 MB of notes: it takes a while to load
	private static File large(File folder) throws Exception {
		File capture = new File(folder, "Large.nam");
		byte[] gain3 = Files.readAllBytes(TestPlugins.gain3(folder).toPath());
		Files.write(capture.toPath(), Arrays.copyOf(gain3, gain3.length - 1));
		Files.write(capture.toPath(), (", \"metadata\": {\"notes\": \"" + String.join("", Collections.nCopies(5 * 1024 * 1024, "n")) + "\"}}").getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
		return capture;
	}

	// what was delayed past the block still sounds in the next one, unless the plugin was reset
	private float tailAfter(boolean reset) throws TGVst3Exception {
		TGVst3Instance latency = open("Test Latency");
		latency.setParameter(latency.findParameter("Latency"), 600);
		process(latency, block(1f, 1f));
		if (reset) {
			latency.reset();
		}
		return process(latency, new float[2][FRAMES])[0][100];
	}

	@Test
	public void resettingClearsTheTail() throws TGVst3Exception {
		assertEquals(1f, tailAfter(false));
		assertEquals(0f, tailAfter(true));
	}

	@Test
	public void aGestureInThePluginsEditorIsHeardAsEdits() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		List<String> heard = new ArrayList<String>();
		gain.setListener(notice -> heard.add(notice.getKind() + " " + notice.getParameter() + " " + notice.getPlain()));
		int id = gain.findParameter("Gain");
		gain.message("TG.gesture", Collections.singletonMap("id", (long) id), Collections.singletonMap("values", TGVst3Instance.doubles(2.5, 3.0)));
		assertEquals(Arrays.asList("BEGIN_EDIT 1 0.0", "PERFORM_EDIT 1 2.5", "PERFORM_EDIT 1 3.0", "END_EDIT 1 0.0"), heard);
		assertEquals(3.0, gain.getParameter(id));
	}

	private static String heard(List<TGVst3Notice> notices) {
		return notices.stream().map(notice -> notice.getKind() + " " + notice.getParameter()).collect(java.util.stream.Collectors.joining(", "));
	}

	// a capture loaded in the plugin's own editor is no edit: a plugin that says it is dirty is heard saying so, one that
	// says nothing is not heard; either way the capture is in its state
	@Test
	public void aCaptureLoadedInThePluginsEditorIsHeardOnlyWhenThePluginSaysItIsDirty() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		List<TGVst3Notice> notices = new ArrayList<TGVst3Notice>();
		gain.setListener(notices::add);
		gain.message("TG.load", Map.of("capture", 7L, "dirty", 0L), Collections.<String, byte[]>emptyMap());
		String silent = heard(notices);
		gain.message("TG.load", Map.of("capture", 8L, "dirty", 1L), Collections.<String, byte[]>emptyMap());
		TGVst3Instance second = open("Test Gain");
		second.setState(gain.getState());
		byte[] capture = second.message("TG.capture", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		assertEquals(" | DIRTY 0 | 8", silent + " | " + heard(notices) + " | " + java.nio.ByteBuffer.wrap(capture).order(java.nio.ByteOrder.nativeOrder()).getInt());
	}

	// its close box hides the editor's window, which the plugin's host is told: the editor may have changed the plugin
	@Test
	public void anEditorClosedByItsCloseBoxIsHeardHidden() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		List<TGVst3Notice> notices = new ArrayList<TGVst3Notice>();
		gain.setListener(notices::add);
		gain.openEditor(0, "");
		inFront(gain);
		gain.message("TG.close-window", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		once(gain, editor -> !editor.isShown());
		assertEquals("EDITOR_HIDDEN 0", heard(notices));
	}

	// a gesture ends when the edit that began it ends: a preset choice begins the preset and every parameter it sets
	@Test
	public void aGestureAndAPresetChoiceAreEachHeardAsOneGestureOnceItEnds() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		int id = gain.findParameter("Gain");
		List<Double> ended = new ArrayList<Double>();
		gain.setListener(new TGVst3Gestures(() -> ended.add(this.parameter(gain, id))));
		gain.message("TG.gesture", Collections.singletonMap("id", (long) id), Collections.singletonMap("values", TGVst3Instance.doubles(2.5, 3.0)));
		gain.message("TG.preset", Collections.singletonMap("index", 1L), Collections.<String, byte[]>emptyMap());
		gain.message("TG.gesture", Collections.singletonMap("id", (long) id), Collections.singletonMap("values", TGVst3Instance.doubles(5.0)));
		assertEquals(Arrays.asList(3.0, 4.0, 5.0), ended);
	}

	private double parameter(TGVst3Instance instance, int id) {
		try {
			return instance.getParameter(id);
		} catch (TGVst3Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	public void aPluginsReportComesBack() throws TGVst3Exception {
		TGVst3Instance tempo = open("Test Tempo");
		process(tempo, block(0f, 0f));
		process(tempo, block(0f, 0f));
		// two records of 36 bytes: tempo, numerator, denominator, playing, position, sample
		assertEquals(72, tempo.message("TG.report", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()).length);
	}

	@Test
	public void theEditorOfAPluginWithoutOneDoesNotOpen() throws TGVst3Exception {
		assertFalse(open("Test Clip 0.5").openEditor(0, ""));
	}

	@Test
	public void aHostThatExitedIsReportedAndRefusesCalls() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		TestPlugins.kill(this.host.getPid());
		assertFalse(this.host.isRunning());
		TGVst3Exception stopped = assertThrows(TGVst3Exception.class, () -> process(gain, block(0.25f, 0.25f)));
		assertFalse(this.host.isRunning());
		assertTrue(stopped.getMessage().startsWith("the VST3 host stopped"), stopped.getMessage());
		assertTrue(this.host.getExitReport().startsWith("the VST3 host stopped"), this.host.getExitReport());
		assertThrows(TGVst3Exception.class, () -> plugin("Test Gain"));
	}

	@Test
	public void aPluginThatCrashesTakesOnlyTheHostDown() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		TGVst3Exception crashed = assertThrows(TGVst3Exception.class, () -> gain.message("TG.crash", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()));
		assertEquals("the VST3 host stopped (exit code 70)", crashed.getMessage());
		assertFalse(this.host.isRunning());
		assertEquals(crashed.getMessage(), this.host.getExitReport());
	}

	// what the test plugins' module says of an editor's loop of its own: whether it runs (1), whether a plugin or an
	// editor's view went while it ran (1), how many plugins of the module are alive
	private static byte[] loopState(TGVst3Instance plugin) throws TGVst3Exception {
		return plugin.message("TG.in-modal", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
	}

	private static boolean inALoopOfItsOwn(TGVst3Instance plugin) throws TGVst3Exception {
		return loopState(plugin)[0] == 1;
	}

	// until the loop runs, or no longer does (at most 3 s)
	private static void awaitLoop(TGVst3Instance plugin, boolean running) {
		assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
			while (inALoopOfItsOwn(plugin) != running) {
				Thread.sleep(5);
			}
		});
	}

	// whether each of 10 blocks played while the plugin's editor runs a loop of its own or waits (as the message says)
	// is answered in time, then whether it still runs or waits
	private static String answeredWhileItsEditorWaits(TGVst3Instance plugin, Map<String, Long> wait) throws TGVst3Exception {
		plugin.message("TG.modal", wait, Collections.<String, byte[]>emptyMap());
		awaitLoop(plugin, true);
		List<Boolean> answered = new ArrayList<Boolean>();
		for (int i = 0; i < 10; i++) {
			answered.add(plugin.process(block(0.25f, 0.25f), FRAMES, TGVst3Transport.STOPPED, Collections.<TGVst3NoteEvent>emptyList()));
		}
		return answered + " " + inALoopOfItsOwn(plugin);
	}

	// closing a plugin's editor, or the plugin, while an editor runs a loop of its own (a file dialog) is answered at
	// once, but the view and the plugin go only once that loop has returned: their code may be running in it. An editor
	// closed and opened again meanwhile stays open
	@Test
	public void aPluginClosedWhileAnEditorRunsALoopOfItsOwnGoesOnceTheLoopReturns() throws TGVst3Exception {
		TGVst3Instance closed = open("Test Gain");
		TGVst3Instance editorClosed = open("Test Gain");
		TGVst3Instance reopened = open("Test Gain");
		closed.openEditor(0, "");
		editorClosed.openEditor(0, "");
		reopened.openEditor(0, "");
		closed.message("TG.modal", Collections.singletonMap("millis", 1500L), Collections.<String, byte[]>emptyMap());
		awaitLoop(reopened, true);
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
			closed.closeEditor();
			closed.close();
			editorClosed.closeEditor();
			reopened.closeEditor();
			reopened.openEditor(0, "");
		});
		String during = Arrays.toString(loopState(reopened)) + " " + (editorClosed.getEditorWindow().getWindow() != 0);
		awaitLoop(reopened, false);
		String after = Arrays.toString(loopState(reopened)) + " " + (editorClosed.getEditorWindow().getWindow() != 0) + " " + (reopened.getEditorWindow().getWindow() != 0);
		assertEquals("[1, 0, 3] true | [0, 0, 2] false true", during + " | " + after);
	}

	// a plugin's editor that runs a loop of its own (a file dialog, its window dragged) does not keep the host from
	// answering: the blocks played meanwhile are answered in time
	@Test
	public void theHostAnswersInTimeWhileAPluginsEditorRunsALoopOfItsOwn() throws TGVst3Exception {
		assertEquals(Collections.nCopies(10, true) + " true", answeredWhileItsEditorWaits(open("Test Gain"), Collections.singletonMap("millis", 3000L)));
	}

	// nor a ping, the watchdog's question whether the host still answers (the thread that runs the editors answers it)
	@Test
	public void aPingIsAnsweredWhileAnEditorRunsALoopOfItsOwn() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "");
		gain.message("TG.modal", Collections.singletonMap("millis", 3000L), Collections.<String, byte[]>emptyMap());
		awaitLoop(gain, true);
		List<Boolean> answered = new ArrayList<Boolean>();
		for (int i = 0; i < 10; i++) {
			answered.add(this.host.ping());
		}
		assertEquals(Collections.nCopies(10, true) + " true", answered + " " + inALoopOfItsOwn(gain));
	}

	// the plugin opened in the other host, its editor open
	private static TGVst3Instance withItsEditor(TGVst3Host other, String name) throws TGVst3Exception {
		TGVst3Instance plugin = other.open(other.scan(TestPlugins.bundle(name)).get(0), 44100f, FRAMES);
		plugin.openEditor(0, "");
		return plugin;
	}

	// its host's thread hangs in a block for 5 s from now (that block's reply is owed): its windows answer no messages
	private static void hangItsHost(TGVst3Instance hang) throws TGVst3Exception {
		hang.setParameter(hang.findParameter("Hang at"), 0);
		hang.setParameter(hang.findParameter("Hang for"), 5000);
		process(hang, block(0.25f, 0.25f));
		assertTrue(hang.getHost().isLate());
	}

	// the window of a Test Hang editor in the other host, whose thread then hangs
	private static long windowOfAHungHost(TGVst3Host other) throws TGVst3Exception {
		TGVst3Instance hang = withItsEditor(other, "Test Hang");
		long window = hang.getEditorWindow().getWindow();
		hangItsHost(hang);
		return window;
	}

	// nor does a wait on a window of another process that does not answer yet, as an editor TuxGuitar's window owns
	// waits on it while TuxGuitar waits for a reply (here the window of an editor whose host hangs in a block)
	@Test
	public void theHostAnswersInTimeWhileItWaitsOnAWindowOfAnotherProcess() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			long window = windowOfAHungHost(other);
			assertEquals(Collections.nCopies(10, true) + " true", answeredWhileItsEditorWaits(open("Test Gain"), Collections.singletonMap("window", window)));
		}
	}

	private static float[][] play(TGVst3Instance instrument, TGVst3NoteEvent... events) throws TGVst3Exception {
		float[][] block = new float[2][FRAMES];
		instrument.process(block, FRAMES, TGVst3Transport.STOPPED, Arrays.asList(events));
		return block;
	}

	// a plugin that hangs in its process call for the milliseconds, from its first block
	private TGVst3Instance hanging(double millis) throws TGVst3Exception {
		TGVst3Instance hang = open("Test Hang");
		hang.setParameter(hang.findParameter("Hang at"), 0);
		hang.setParameter(hang.findParameter("Hang for"), millis);
		return hang;
	}

	private static boolean inTime(TGVst3Instance instance, float[][] block) throws TGVst3Exception {
		return instance.process(block, FRAMES, TGVst3Transport.STOPPED, Collections.<TGVst3NoteEvent>emptyList());
	}

	// a block whose reply is late is silent, and so is every block until the reply comes, without waiting for it; the
	// late reply is then dropped, and the next block's reply is its own
	@Test
	public void aBlockAnsweredLateIsSilentAndTheBlockAfterTheReplyIsItsOwn() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			TGVst3Instance hang = hanging(400);
			float[][] late = block(0.25f, 0.25f);
			float[][] owed = block(0.25f, 0.25f);
			boolean lateAnswered = inTime(hang, late);
			long start = System.nanoTime();
			boolean owedAnswered = inTime(hang, owed);
			long waited = (System.nanoTime() - start) / 1000000;
			Thread.sleep(600);
			float[][] own = process(hang, block(0.5f, -0.5f));
			assertEquals("false false 0.0 0.0", lateAnswered + " " + owedAnswered + " " + late[0][0] + " " + owed[1][0]);
			assertTrue(waited < 50, "waited " + waited + " ms");
			assertEquals(0.5f, own[0][0]);
			assertEquals(-0.5f, own[1][FRAMES - 1]);
		});
	}

	// a host that has not answered for 2 seconds is ended, its process tree with it, and reported
	@Test
	public void aHostThatDoesNotAnswerForLongIsEndedAndReported() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			TGVst3Instance hang = hanging(1e9);
			assertFalse(inTime(hang, block(0.25f, 0.25f)));
			Thread.sleep(2100);
			TGVst3Exception silent = assertThrows(TGVst3Exception.class, () -> inTime(hang, block(0.25f, 0.25f)));
			assertEquals("the VST3 host did not answer for 2 s", silent.getMessage());
			assertEquals(silent.getMessage(), this.host.getExitReport());
			assertFalse(ProcessHandle.of(this.host.getPid()).isPresent());
		});
	}

	// a plugin's editor whose code hangs keeps its host from answering anything: pinged every 100 ms while nothing plays
	// (the watchdog's question), the host is ended within 2.5 s, as not answering, and whoever listens is told once
	@Test
	public void aHostWhoseEditorHangsIsEndedWithin2500msWhileNothingPlays() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			TGVst3Instance gain = open("Test Gain");
			gain.openEditor(0, "");
			List<String> told = new ArrayList<String>();
			this.host.whenStopped(() -> told.add("stopped"));
			gain.message("TG.hang-view", Collections.singletonMap("millis", 10000L), Collections.<String, byte[]>emptyMap());
			long start = System.nanoTime();
			while (this.host.isRunning() && (System.nanoTime() - start) / 1000000 < 5000) {
				pingQuietly(this.host);
				Thread.sleep(100);
			}
			long ended = (System.nanoTime() - start) / 1000000;
			pingQuietly(this.host);
			assertTrue(ended < 2500, "ended after " + ended + " ms");
			assertEquals("false true [stopped] the VST3 host did not answer for 2 s", this.host.isRunning() + " " + this.host.wasSilent() + " " + told + " " + this.host.getExitReport());
		});
	}

	private static void pingQuietly(TGVst3Host host) {
		try {
			host.ping();
		} catch (TGVst3Exception e) {
			// ended: the loop sees it
		}
	}

	// a request TuxGuitar's window makes of an editor (fit it, close it) gives up on a host whose editor hangs within 2.5 s,
	// not 30, and the host is ended
	@Test
	public void anEditorRequestToAHostWhoseEditorHangsGivesUpWithin2500ms() {
		assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
			List<String> waited = new ArrayList<String>();
			for (boolean closing : new boolean[] {false, true}) {
				try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
					TGVst3Instance gain = withItsEditor(other, "Test Gain");
					gain.message("TG.hang-view", Collections.singletonMap("millis", 10000L), Collections.<String, byte[]>emptyMap());
					Thread.sleep(200);
					long start = System.nanoTime();
					assertThrows(TGVst3Exception.class, () -> editorRequest(gain, closing));
					long millis = (System.nanoTime() - start) / 1000000;
					waited.add((millis >= 1900 && millis < 2500) + " " + other.isRunning());
				}
			}
			assertEquals("[true false, true false]", waited.toString());
		});
	}

	private static void editorRequest(TGVst3Instance plugin, boolean closing) throws TGVst3Exception {
		if (closing) {
			plugin.closeEditor();
		} else {
			plugin.fitEditor();
		}
	}

	// a host that crashes tells whoever listens, once, as stopped (not as not answering); one TuxGuitar closes tells
	// nothing
	@Test
	public void aHostThatStopsTellsWhoListensOnceAndOneClosedTellsNothing() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		List<String> told = new ArrayList<String>();
		this.host.whenStopped(() -> told.add("crashed"));
		assertThrows(TGVst3Exception.class, () -> gain.message("TG.crash", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()));
		this.host.isRunning();
		assertThrows(TGVst3Exception.class, () -> gain.getParameter(1));
		try (TGVst3Host closed = TGVst3Host.start(TestPlugins.host())) {
			closed.whenStopped(() -> told.add("closed"));
			closed.close();
			closed.isRunning();
		}
		assertEquals("[crashed] false", told + " " + this.host.wasSilent());
	}

	// another request while a reply is owed waits for it only what is left of the 2 seconds
	@Test
	public void aRequestWhileAReplyIsOwedWaitsOnlyTheRestOfTheLimit() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			TGVst3Instance hang = hanging(1e9);
			assertFalse(inTime(hang, block(0.25f, 0.25f)));
			Thread.sleep(1500);
			long start = System.nanoTime();
			assertThrows(TGVst3Exception.class, () -> hang.getParameter(hang.findParameter("Hang at")));
			long waited = (System.nanoTime() - start) / 1000000;
			assertTrue(waited < 1200, "waited " + waited + " ms");
		});
	}

	private static boolean offline(TGVst3Instance instance, float[][] block) throws TGVst3Exception {
		return instance.process(block, FRAMES, TGVst3Transport.STOPPED, Collections.<TGVst3NoteEvent>emptyList(), true);
	}

	// offline (an export) a block waits for its reply however late it is, and is never left silent
	@Test
	public void offlineABlockWaitsForALateReply() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			float[][] block = block(0.25f, -0.25f);
			boolean answered = offline(hanging(400), block);
			assertEquals("true 0.25 -0.25", answered + " " + block[0][0] + " " + block[1][FRAMES - 1]);
		});
	}

	// offline, a host that has not answered a block for 2 seconds is ended, its process tree with it, and reported
	@Test
	public void offlineAHostThatDoesNotAnswerABlockFor2SecondsIsEnded() {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			TGVst3Instance hang = hanging(1e9);
			long start = System.nanoTime();
			TGVst3Exception silent = assertThrows(TGVst3Exception.class, () -> offline(hang, block(0.25f, 0.25f)));
			long waited = (System.nanoTime() - start) / 1000000;
			assertTrue(waited >= 1900 && waited < 4000, "waited " + waited + " ms");
			assertEquals("the VST3 host did not answer for 2 s", silent.getMessage());
			assertFalse(ProcessHandle.of(this.host.getPid()).isPresent());
		});
	}

	@Test
	public void aNoteStopsOnItsSample() throws TGVst3Exception {
		float[][] block = play(open("Test Sine"), TGVst3NoteEvent.noteOn(0, 69, 7, 1f), TGVst3NoteEvent.noteOff(256, 69, 7));
		assertNotEquals(0f, block[0][255]);
		assertEquals(0f, block[0][256]);
	}

	@Test
	public void aNotesTuningMovesItsPitch() throws TGVst3Exception {
		float[][] tuned = play(open("Test Sine"), TGVst3NoteEvent.noteOn(0, 57, 7, 1f), TGVst3NoteEvent.tuning(0, 7, 12f));
		float[][] octave = play(open("Test Sine"), TGVst3NoteEvent.noteOn(0, 69, 7, 1f));
		assertArrayEquals(octave[0], tuned[0], 1e-4f);
	}

	// Test Tone: Test Sine's tones at its parameter Level, an instrument without an editor of its own
	@Test
	public void anInstrumentWithoutAnEditorOfItsOwnPlaysAtItsLevel() throws TGVst3Exception {
		TGVst3Instance tone = open("Test Tone");
		tone.setParameter(tone.findParameter("Level"), 0.5);
		float[][] half = play(tone, TGVst3NoteEvent.noteOn(0, 69, 7, 1f));
		float[][] sine = play(open("Test Sine"), TGVst3NoteEvent.noteOn(0, 69, 7, 1f));
		assertFalse(tone.openEditor(0, ""));
		assertEquals(sine[0][100] / 2, half[0][100], 1e-6f);
	}

	@Test
	public void anEditorOpensAgainOnceClosed() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		assertTrue(gain.openEditor(0, ""));
		gain.closeEditor();
		assertTrue(gain.openEditor(0, ""));
	}

	// the window of a Test Gain editor open in another host process, as TuxGuitar's window is a window of another process
	private static TGVst3EditorWindow editorOfAnotherProcess(TGVst3Host other) throws TGVst3Exception {
		return withItsEditor(other, "Test Gain").getEditorWindow();
	}

	private static long windowOfAnotherProcess(TGVst3Host other) throws TGVst3Exception {
		return editorOfAnotherProcess(other).getWindow();
	}

	// its editor's window once it is as the condition says, or as it is after 2 s: the host shows a window right after
	// its reply to Edit, and handles a click on the window's close box once it waits for messages
	private static TGVst3EditorWindow once(TGVst3Instance plugin, Predicate<TGVst3EditorWindow> condition) throws TGVst3Exception {
		long end = System.currentTimeMillis() + 2000;
		TGVst3EditorWindow editor = plugin.getEditorWindow();
		while (!condition.test(editor) && System.currentTimeMillis() < end) {
			LockSupport.parkNanos(5000000);
			editor = plugin.getEditorWindow();
		}
		return editor;
	}

	private static TGVst3EditorWindow inFront(TGVst3Instance plugin) throws TGVst3Exception {
		return once(plugin, editor -> editor.isShown() && editor.isFront());
	}

	// its close box hides the window, the plugin's view still in it, and the window stays hidden; Edit shows that window
	// again
	@Test
	public void anEditorClosedByItsCloseBoxIsHiddenUntilEditShowsItAgain() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "");
		long window = inFront(gain).getWindow();
		gain.message("TG.close-window", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		boolean hidden = !once(gain, editor -> !editor.isShown()).isShown();
		boolean stays = !gain.getEditorWindow().isShown();
		gain.openEditor(0, "");
		TGVst3EditorWindow again = inFront(gain);
		assertEquals("true true true true", hidden + " " + stays + " " + (again.getWindow() == window) + " " + again.isShown());
	}

	// owned, it stays in front of that window, minimises with it and has no taskbar button of its own; it opens centred on
	// that window (here a window of the same size: the same frame)
	@Test
	public void anEditorOpensInAWindowOwnedByTheWindowGivenCentredOnIt() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			TGVst3EditorWindow owner = editorOfAnotherProcess(other);
			TGVst3Instance gain = open("Test Gain");
			assertTrue(gain.openEditor(owner.getWindow(), ""));
			TGVst3EditorWindow editor = inFront(gain);
			assertNotEquals(0L, owner.getWindow());
			assertNotEquals(0L, editor.getWindow());
			assertEquals(owner.getWindow(), editor.getOwner());
			assertEquals(owner.getFrame(), editor.getFrame());
		}
	}

	// the inside of its window is the size of the plugin's view (Test Gain's: 200 by 100)
	@Test
	public void anEditorOpensShownInFrontSizedForThePluginsView() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "");
		TGVst3EditorWindow editor = inFront(gain);
		assertEquals("true true 200x100", editor.isShown() + " " + editor.isFront() + " " + editor.getInside());
	}

	// its window's caption is the title given (any text: UTF-8 on the wire), and Edit again gives it the title then
	@Test
	public void anEditorsWindowIsTitledAsEditSays() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "Test Gain – Track 1");
		String first = gain.getEditorWindow().getTitle();
		gain.openEditor(0, "Test Gain – Lead é");
		assertEquals("Test Gain – Track 1 | Test Gain – Lead é", first + " | " + gain.getEditorWindow().getTitle());
	}

	// a test plugin's message on its editor's view, whose report is doubles
	private static double[] viewMessage(TGVst3Instance plugin, String id, Map<String, Long> integers) throws TGVst3Exception {
		ByteBuffer report = ByteBuffer.wrap(plugin.message(id, integers, Collections.<String, byte[]>emptyMap())).order(ByteOrder.nativeOrder());
		double[] values = new double[report.remaining() / Double.BYTES];
		report.asDoubleBuffer().get(values);
		return values;
	}

	// what the view was last told: its content scale, width, height; and whether it is attached to a window (1, else 0)
	private static double[] view(TGVst3Instance plugin) throws TGVst3Exception {
		return viewMessage(plugin, "TG.view", Collections.<String, Long>emptyMap());
	}

	// the host is aware of each screen's own DPI (per monitor, version 2): the plugin's view is told the content scale of
	// its window's screen (its DPI over 96)
	@Test
	public void aPluginsViewIsToldTheContentScaleOfItsWindowsScreen() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "");
		TGVst3EditorWindow editor = gain.getEditorWindow();
		assertEquals("true " + (editor.getDpi() / 96.0), editor.isPerMonitorDpiAware() + " " + view(gain)[0]);
	}

	private static Map<String, Long> size(long width, long height) {
		Map<String, Long> size = new java.util.HashMap<String, Long>();
		size.put("width", width);
		size.put("height", height);
		return size;
	}

	// what the window and the view say of the size: the window's inside, the view's size, whether the window has a
	// border the user can drag
	private static String sizes(TGVst3Instance plugin) throws TGVst3Exception {
		TGVst3EditorWindow editor = plugin.getEditorWindow();
		double[] view = view(plugin);
		return editor.getInside() + " " + (int) view[1] + "x" + (int) view[2] + " " + editor.isResizable();
	}

	// a view that asks its host for another size (IPlugFrame.resizeView) gets it: the window's inside takes it, and the
	// view is told it; a view that cannot be resized gives its window no border to drag
	// the sizes once the test message ("TG.resize", "TG.drag-to") of that size is played
	private static String sizesAfter(TGVst3Instance plugin, String message, long width, long height) throws TGVst3Exception {
		plugin.message(message, size(width, height), Collections.<String, byte[]>emptyMap());
		return sizes(plugin);
	}

	@Test
	public void aViewThatAsksForAnotherSizeGetsItsWindowResized() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "");
		String before = sizes(gain);
		assertEquals("200x100 200x100 false | 300x150 300x150 false", before + " | " + sizesAfter(gain, "TG.resize", 300, 150));
	}

	// a view that can be resized has a window with a border the user can drag; the view is told each size, down to the
	// smallest it accepts (Test Gain's made resizable: 150 by 80)
	@Test
	public void aResizableViewsWindowIsResizedByTheUserWithinWhatTheViewAccepts() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.message("TG.resizable", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		gain.openEditor(0, "");
		String larger = sizesAfter(gain, "TG.drag-to", 260, 140);
		assertEquals("260x140 260x140 true | 150x80 150x80 true", larger + " | " + sizesAfter(gain, "TG.drag-to", 100, 40));
	}

	// a window gone to a screen of another DPI takes the frame Windows suggests, and its view is told the new scale
	@Test
	public void anEditorsWindowGoneToAScreenOfAnotherDpiTakesTheFrameSuggestedAndTellsItsView() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(0, "");
		long dpi = inFront(gain).getDpi() + 48;
		double[] suggested = viewMessage(gain, "TG.dpi-changed", Collections.singletonMap("dpi", dpi));
		String frame = (int) suggested[0] + "," + (int) suggested[1] + " " + (int) suggested[2] + "," + (int) suggested[3];
		assertEquals((dpi / 96.0) + " " + frame, view(gain)[0] + " " + gain.getEditorWindow().getFrame());
	}

	// Edit on an editor that is open brings that window to the front rather than opening another
	@Test
	public void anEditorOpenedAgainIsTheSameWindowBroughtToTheFront() throws TGVst3Exception {
		TGVst3Instance first = open("Test Gain");
		TGVst3Instance second = open("Test Gain");
		first.openEditor(0, "");
		long window = inFront(first).getWindow();
		second.openEditor(0, "");
		inFront(second);
		boolean behind = first.getEditorWindow().isFront();
		assertTrue(first.openEditor(0, ""));
		TGVst3EditorWindow again = inFront(first);
		assertEquals("false true true false", behind + " " + (again.getWindow() == window) + " " + again.isFront() + " " + second.getEditorWindow().isFront());
	}

	// Edit is answered at once while the thread of the owner window answers no messages, as TuxGuitar's UI thread waits
	// for that reply: the window is shown in front once the reply is sent (here the owner is the window of an editor
	// whose host hangs in a block)
	@Test
	public void anEditorOwnedByAWindowWhoseThreadIsBusyOpensAtOnce() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			long owner = windowOfAHungHost(other);
			TGVst3Instance gain = open("Test Gain");
			boolean opened = assertTimeoutPreemptively(Duration.ofMillis(1000), () -> gain.openEditor(owner, ""));
			assertEquals("true " + owner, opened + " " + gain.getEditorWindow().getOwner());
		}
	}

	// closing an editor shown in front (the active window when Windows let the host make it so) is answered at once
	// while the thread of the owner window answers no messages; the window goes once nothing waits on that thread (here
	// once the owner's host has ended)
	@Test
	public void anEditorInFrontOwnedByAWindowWhoseThreadIsBusyClosesAtOnce() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			TGVst3Instance hang = withItsEditor(other, "Test Hang");
			gain.openEditor(hang.getEditorWindow().getWindow(), "");
			inFront(gain);
			hangItsHost(hang);
			assertTimeoutPreemptively(Duration.ofMillis(1000), gain::closeEditor);
		}
		assertEquals(0L, once(gain, editor -> editor.getWindow() == 0).getWindow());
	}

	// a handle that is no window any more (that window closed) is no owner
	@Test
	public void anEditorGivenNoWindowOrOneThatIsGoneOpensInAWindowOfItsOwn() throws TGVst3Exception {
		long gone;
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			gone = windowOfAnotherProcess(other);
		}
		TGVst3Instance unowned = open("Test Gain");
		TGVst3Instance stale = open("Test Gain");
		assertTrue(unowned.openEditor(0, ""));
		assertTrue(stale.openEditor(gone, ""));
		assertEquals("0 0", unowned.getEditorWindow().getOwner() + " " + stale.getEditorWindow().getOwner());
		assertNotEquals(0L, stale.getEditorWindow().getWindow());
	}

	// a window of another host process standing in for TuxGuitar's (the test plugins' "TG.window"), of a DPI awareness,
	// its inside of a size; resized as the user resizes TuxGuitar's
	private static final class StandIn {
		private final TGVst3Instance maker;
		private final long window;

		StandIn(TGVst3Host other, int awareness, long width, long height) throws TGVst3Exception {
			this.maker = other.open(other.scan(TestPlugins.bundle("Test Gain")).get(0), 44100f, FRAMES);
			Map<String, Long> asked = size(width, height);
			asked.put("awareness", (long) awareness);
			this.window = ByteBuffer.wrap(this.maker.message("TG.window", asked, Collections.<String, byte[]>emptyMap())).order(ByteOrder.nativeOrder()).getLong();
		}

		void resize(long width, long height) throws TGVst3Exception {
			Map<String, Long> asked = size(width, height);
			asked.put("window", this.window);
			this.maker.message("TG.window-size", asked, Collections.<String, byte[]>emptyMap());
		}

		// inside it, at most that share of its height
		TGEditorTarget at(double share) {
			return TGEditorTarget.embedded(this.window, 0, share);
		}

		// whether the window given (of any process) is still a window, as the stand-in's host sees it
		boolean sees(long window) throws TGVst3Exception {
			return this.maker.message("TG.is-window", Collections.singletonMap("window", window), Collections.<String, byte[]>emptyMap())[0] == 1;
		}
	}

	// Test Gain, its views made resizable (down to 150 by 80)
	private TGVst3Instance resizableGain() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.message("TG.resizable", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		return gain;
	}

	// once the host has put it inside the window (after its reply): whether it is there, shown, the size of its inside,
	// and what its view was told
	private static String inside(TGVst3Instance plugin, StandIn app) throws TGVst3Exception {
		TGVst3EditorWindow editor = once(plugin, TGVst3EditorWindow::isEmbedded);
		double[] view = view(plugin);
		return (editor.getParent() == app.window) + " " + editor.isShown() + " " + editor.getInside() + " " + (int) view[1] + "x" + (int) view[2];
	}

	// TuxGuitar's shortcuts (virtual key | modifiers << 8: 1 Shift, 2 Ctrl, 4 Alt) as the area passes them: Ctrl+Z, F8,
	// Alt+Left
	private static final int SHIFT = 1 << 8;
	private static final int CTRL = 2 << 8;
	private static final int ALT = 4 << 8;
	// for the test: the plugin looks at the key typed without taking it (PeekMessage, PM_NOREMOVE)
	private static final int PEEKED = 1 << 16;
	// for the test: the key is typed in a standard text box of the plugin's view, which has the focus: an Edit, a RichEdit
	private static final int IN_EDIT = 1 << 17;
	private static final int IN_RICH_EDIT = 2 << 17;
	// the same under class names of the plugin's own (superclassed), or in a list box of the view (no text box)
	private static final int IN_RENAMED_EDIT = 3 << 17;
	private static final int IN_RENAMED_RICH_EDIT = 4 << 17;
	private static final int IN_LIST_BOX = 5 << 17;
	private static final int[] SHORTCUTS = {'Z' | CTRL, 0x77, 0x25 | ALT};
	// the keys a text box edits with, as TuxGuitar's shortcuts too: copy, cut, paste, select all, undo, redo (Ctrl+Y,
	// Ctrl+Shift+Z), a word left or right (Ctrl+Left, Ctrl+Right), selected (with Shift too), deleted (Ctrl+Backspace,
	// Ctrl+Delete), and Ctrl+Shift+Delete (TuxGuitar's Remove Track, kept so a slip while typing removes no track; typed
	// before the paste, while the box is empty, as a text box may cut with it)
	private static final int[] EDITING = {'C' | CTRL, 'X' | CTRL, 0x2E | CTRL | SHIFT, 'V' | CTRL, 'A' | CTRL, 'Z' | CTRL, 'Y' | CTRL, 'Z' | CTRL | SHIFT,
		0x25 | CTRL, 0x27 | CTRL, 0x25 | CTRL | SHIFT, 0x27 | CTRL | SHIFT, 0x08 | CTRL, 0x2E | CTRL};
	// TuxGuitar's shortcuts, those a text box edits with, and two it does not (Ctrl+Shift+C, Ctrl+Up)
	private static final int[] WITH_EDITING = IntStream.concat(IntStream.concat(IntStream.of(SHORTCUTS), IntStream.of(EDITING)), IntStream.of('C' | CTRL | SHIFT, 0x26 | CTRL))
		.toArray();

	// where a key typed in the plugin's view (with the modifiers held) went once it got there: "window <key>" when the
	// stand-in heard it, as typed there with the modifiers its thread saw held, "view <key>" when the view did (both,
	// joined by "and", when both did), or "nowhere" after 2 s
	private static String typed(TGVst3Instance plugin, StandIn app, int key) throws TGVst3Exception {
		Map<String, Long> typed = new java.util.HashMap<String, Long>();
		typed.put("key", (long) (key & 0xFF));
		typed.put("ctrl", (long) ((key & CTRL) != 0 ? 1 : 0));
		typed.put("alt", (long) ((key & ALT) != 0 ? 1 : 0));
		typed.put("shift", (long) ((key & SHIFT) != 0 ? 1 : 0));
		typed.put("peek", (long) ((key & PEEKED) != 0 ? 1 : 0));
		typed.put("box", (long) (key >> 17));
		plugin.message("TG.type", typed, Collections.<String, byte[]>emptyMap());
		long end = System.currentTimeMillis() + 2000;
		String went = "";
		while (went.isEmpty() && System.currentTimeMillis() < end) {
			LockSupport.parkNanos(5000000);
			went = went(heard(app.maker)[0], heard(plugin)[1]);
		}
		return (went.isEmpty() ? "nowhere" : went);
	}

	private static String went(String window, String view) {
		String both = (!window.isEmpty() && !view.isEmpty() ? " and " : "");
		return (window.isEmpty() ? "" : "window " + window) + both + (view.isEmpty() ? "" : "view " + view);
	}

	// what the windows of the plugin's host process heard since last asked: the stand-in's, the views'
	private static String[] heard(TGVst3Instance plugin) throws TGVst3Exception {
		byte[] report = plugin.message("TG.keys", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		return new String(report, StandardCharsets.UTF_8).split(" \\| ", -1);
	}

	private static String typedAll(TGVst3Instance plugin, StandIn app, int... keys) throws TGVst3Exception {
		List<String> went = new ArrayList<String>();
		for (int key : keys) {
			went.add(typed(plugin, app, key));
		}
		return String.join(", ", went);
	}

	// Test Gain's editor inside the stand-in, passing the shortcuts (SHORTCUTS)
	private TGVst3Instance passingShortcuts(StandIn app) throws TGVst3Exception {
		return passing(app, SHORTCUTS);
	}

	private TGVst3Instance passing(StandIn app, int[] keys) throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.openEditor(app.at(1.0).passing(keys), "");
		once(gain, TGVst3EditorWindow::isEmbedded);
		return gain;
	}

	// the keys, typed in the plugin's control of that kind (IN_EDIT, IN_RICH_EDIT, ...)
	private static int[] in(int box, int... keys) {
		return Arrays.stream(keys).map(key -> key | box).toArray();
	}

	// while a standard text box of the plugin's editor has the focus (an Edit, a RichEdit: a key comes to the window with
	// the focus), the shortcuts passed that it edits with (EDITING) stay with it; the other shortcuts passed still go to
	// the window, and so do those typed elsewhere in the editor (a plugin drawing its own text boxes)
	@Test
	public void aStandardTextBoxWithTheFocusKeepsTheEditingKeysOthersStillGoToTheWindow() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = passing(app, WITH_EDITING);
			String edit = typedAll(gain, app, in(IN_EDIT, EDITING)) + ", " + typedAll(gain, app, in(IN_EDIT, 0x77, 0x25 | ALT, 'C' | CTRL | SHIFT, 0x26 | CTRL));
			String rich = typedAll(gain, app, in(IN_RICH_EDIT, 'V' | CTRL, 'Y' | CTRL, 0x08 | CTRL, 0x77));
			assertEquals("view C+ctrl, view X+ctrl, view #46+ctrl+shift, view V+ctrl, view A+ctrl, view Z+ctrl, view Y+ctrl, view Z+ctrl+shift, view #37+ctrl, view #39+ctrl,"
				+ " view #37+ctrl+shift, view #39+ctrl+shift, view #8+ctrl, view #46+ctrl, window F8, window #37+alt, window C+ctrl+shift, window #38+ctrl"
				+ " | view V+ctrl, view Y+ctrl, view #8+ctrl, window F8 | window C+ctrl, window Z+ctrl, window #37+ctrl, window #8+ctrl, window #46+ctrl+shift",
				edit + " | " + rich + " | " + typedAll(gain, app, 'C' | CTRL, 'Z' | CTRL, 0x25 | CTRL, 0x08 | CTRL, 0x2E | CTRL | SHIFT));
		}
	}

	// a standard text box under a class name of the plugin's own (an Edit or a RichEdit superclassed, as Delphi's TEdit and
	// WinForms' WindowsForms10.EDIT... are) keeps the keys it edits with too; a standard control that edits no text (a
	// list box) does not
	@Test
	public void aTextBoxUnderAnotherClassNameKeepsTheEditingKeysAListBoxDoesNot() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = passing(app, WITH_EDITING);
			String edit = typedAll(gain, app, in(IN_RENAMED_EDIT, 'A' | CTRL, 'Z' | CTRL, 0x25 | CTRL, 0x77));
			String rich = typedAll(gain, app, in(IN_RENAMED_RICH_EDIT, 'Y' | CTRL, 0x27 | CTRL, 0x77));
			assertEquals("view A+ctrl, view Z+ctrl, view #37+ctrl, window F8 | view Y+ctrl, view #39+ctrl, window F8 | window A+ctrl, window Z+ctrl, window #37+ctrl",
				edit + " | " + rich + " | " + typedAll(gain, app, in(IN_LIST_BOX, 'A' | CTRL, 'Z' | CTRL, 0x25 | CTRL)));
		}
	}

	// the shortcuts passed, typed in an editor inside a window of TuxGuitar (in another process), go to that window as if
	// typed there, their modifiers held; other keys, and those keys with other modifiers, stay with the plugin's view
	@Test
	public void theKeysPassedGoToTheWindowTheEditorIsInTheOthersStayWithTheView() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = passingShortcuts(app);
			assertEquals("window Z+ctrl, view Z, window F8, view Space, window #37+alt, view A+ctrl, view #37, view Z+ctrl+shift",
				typedAll(gain, app, 'Z' | CTRL, 'Z', 0x77, ' ', 0x25 | ALT, 'A' | CTRL, 0x25, 'Z' | CTRL | SHIFT));
		}
	}

	// a key passed that the plugin's code looks at before its host takes it (a loop of its own peeking at its queue)
	// still goes to the window once, when it is taken
	@Test
	public void aKeyPassedIsPassedOnceThoughThePluginPeeksAtIt() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = passingShortcuts(app);
			assertEquals("window F8, view Space", typedAll(gain, app, 0x77 | PEEKED, ' '));
		}
	}

	// embedded again in the same window, an editor passes the keys given then; a window of its own passes none
	@Test
	public void anEditorPassesTheKeysGivenLastAndNoneInAWindowOfItsOwn() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = passingShortcuts(app);
			gain.openEditor(app.at(1.0).passing(new int[] {0x77}), "");
			String again = typedAll(gain, app, 'Z' | CTRL, 0x77);
			gain.openEditor(TGEditorTarget.floating(0).passing(SHORTCUTS), "");
			once(gain, editor -> !editor.isEmbedded());
			assertEquals("view Z+ctrl, window F8 | view F8", again + " | " + typed(gain, app, 0x77));
		}
	}

	// an editor put inside a window of TuxGuitar is a child of that window, as wide as its inside when its view can be
	// resized, as high as its view
	@Test
	public void anEditorEmbeddedInAWindowIsItsChildFillingItsWidth() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = resizableGain();
			TGVst3EditorOpened opened = gain.openEditor(app.at(1.0), "Test Gain – Guitar");
			assertEquals("EMBEDDED 500x100 resizable in 500 | true true 500x100 500x100", opened + " in " + opened.getAreaWidth() + " | " + inside(gain, app));
		}
	}

	// a view that cannot be resized keeps its own size, at the left of the window's inside
	@Test
	public void anEmbeddedViewThatCannotResizeKeepsItsSize() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = open("Test Gain");
			TGVst3EditorOpened opened = gain.openEditor(app.at(1.0), "");
			assertEquals("EMBEDDED 200x100 fixed | true true 200x100 200x100", opened + " | " + inside(gain, app));
		}
	}

	// an editor higher than the limit (a share of the height of the inside of TuxGuitar's window), or wider than the window when it cannot shrink to its width, is not put there: the
	// host says the size it would take and keeps nothing open (TuxGuitar opens it in a window of its own)
	@Test
	public void anEditorTallerThanTheLimitOrWiderThanTheWindowIsNotEmbeddedAndSaysItsSize() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			StandIn narrow = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 120, 300);
			TGVst3Instance gain = open("Test Gain");
			String taller = gain.openEditor(app.at(0.33), "") + " " + gain.getEditorWindow().getWindow();
			String wider = gain.openEditor(narrow.at(1.0), "") + " " + gain.getEditorWindow().getWindow();
			String shrinking = resizableGain().openEditor(narrow.at(1.0), "").toString();
			String fits = gain.openEditor(app.at(1.0 / 3), "").toString();
			assertEquals("TOO_LARGE 200x100 fixed 0 | TOO_LARGE 200x100 fixed 0 | TOO_LARGE 150x100 resizable | EMBEDDED 200x100 fixed", taller + " | " + wider + " | " + shrinking + " | " + fits);
		}
	}

	// it takes the DPI awareness of the window it is in (a per-monitor child cannot be in a window of another
	// awareness), its view told the scale of that window
	@Test
	public void anEmbeddedEditorTakesTheDpiAwarenessOfTheWindowItIsIn() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			List<String> taken = new ArrayList<String>();
			for (int awareness : new int[] {TGVst3EditorWindow.UNAWARE, TGVst3EditorWindow.AWARE_SYSTEM, TGVst3EditorWindow.AWARE_PER_MONITOR_V2}) {
				TGVst3Instance gain = open("Test Gain");
				StandIn app = new StandIn(other, awareness, 500, 300);
				gain.openEditor(app.at(1.0), "");
				TGVst3EditorWindow editor = once(gain, TGVst3EditorWindow::isEmbedded);
				taken.add(editor.getAwareness() + "/" + editor.getOwnerAwareness() + " " + (editor.getParent() == app.window) + " " + (view(gain)[0] == editor.getDpi() / 96.0));
			}
			assertEquals("[0/0 true true, 1/1 true true, 3/3 true true]", taken.toString());
		}
	}

	// the window's thread busy (TuxGuitar's UI thread waits for the reply), putting an editor in it is answered at once,
	// and the host goes on answering
	@Test
	public void embeddingIsAnsweredAtOnceWhileTheParentsThreadIsBusy() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			hangItsHost(other.open(other.scan(TestPlugins.bundle("Test Hang")).get(0), 44100f, FRAMES));
			TGVst3Instance gain = open("Test Gain");
			TGVst3EditorOpened opened = assertTimeoutPreemptively(Duration.ofMillis(1000), () -> gain.openEditor(app.at(1.0), ""));
			assertEquals("EMBEDDED 200x100 fixed true", opened + " " + this.host.ping());
		}
	}

	// so is closing it; the view goes at once, its window once nothing waits on that thread
	@Test
	public void closingAnEmbeddedEditorIsAnsweredAtOnceWhileTheParentsThreadIsBusy() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance hang = other.open(other.scan(TestPlugins.bundle("Test Hang")).get(0), 44100f, FRAMES);
			gain.openEditor(app.at(1.0), "");
			once(gain, TGVst3EditorWindow::isEmbedded);
			hangItsHost(hang);
			assertTimeoutPreemptively(Duration.ofMillis(1000), gain::closeEditor);
			assertTrue(this.host.ping());
		}
		assertEquals(0L, once(gain, editor -> editor.getWindow() == 0).getWindow());
	}

	// its window goes at once too, the parent's thread still busy: destroying it sends that thread nothing
	@Test
	public void anEmbeddedEditorsWindowGoesAtOnceWhileTheParentsThreadIsBusy() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance hang = other.open(other.scan(TestPlugins.bundle("Test Hang")).get(0), 44100f, FRAMES);
			TGVst3Instance gain = open("Test Gain");
			gain.openEditor(app.at(1.0), "");
			long frame = once(gain, TGVst3EditorWindow::isEmbedded).getWindow();
			hang.setParameter(hang.findParameter("Hang at"), 0);
			hang.setParameter(hang.findParameter("Hang for"), 1500);
			process(hang, block(0.25f, 0.25f));
			gain.closeEditor();
			assertFalse(seenFrom(gain, frame));
		}
	}

	// whether the window (of any process) is still a window, as the plugin's host sees it
	private static boolean seenFrom(TGVst3Instance plugin, long window) throws TGVst3Exception {
		return plugin.message("TG.is-window", Collections.singletonMap("window", window), Collections.<String, byte[]>emptyMap())[0] == 1;
	}

	// an editor inside a window closed while an editor runs a loop of its own goes once the loop has returned, its window
	// with it, with no request to the host after
	@Test
	public void anEmbeddedEditorClosedWhileALoopRunsGoesWithItsWindowOnceTheLoopReturns() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = open("Test Gain");
			gain.openEditor(app.at(1.0), "");
			long frame = once(gain, TGVst3EditorWindow::isEmbedded).getWindow();
			gain.message("TG.modal", Collections.singletonMap("millis", 500L), Collections.<String, byte[]>emptyMap());
			awaitLoop(gain, true);
			gain.closeEditor();
			boolean during = app.sees(frame);
			LockSupport.parkNanos(1000000000L);
			assertEquals("true false", during + " " + app.sees(frame));
		}
	}

	// a plugin closed while its editor is inside a window: its window goes (once the reply is sent), and the host goes on
	@Test
	public void aPluginClosedWhileItsEditorIsInsideAWindowTakesItsWindowAlong() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = open("Test Gain");
			gain.openEditor(app.at(1.0), "");
			long frame = once(gain, TGVst3EditorWindow::isEmbedded).getWindow();
			gain.close();
			long end = System.currentTimeMillis() + 2000;
			while (app.sees(frame) && System.currentTimeMillis() < end) {
				LockSupport.parkNanos(5000000);
			}
			assertEquals("false true", app.sees(frame) + " " + this.host.ping());
		}
	}

	// the window it is in gone (TuxGuitar ended), the editor is closed: no view is left in a window that is gone
	@Test
	public void anEmbeddedEditorWhoseWindowIsGoneIsClosed() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			gain.openEditor(new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300).at(1.0), "");
			once(gain, TGVst3EditorWindow::isEmbedded);
		}
		assertEquals("0 0.0 true", once(gain, editor -> editor.getWindow() == 0).getWindow() + " " + view(gain)[3] + " " + this.host.ping());
	}

	// a view asking for another size inside the window gets it, and TuxGuitar hears it (the panel's height follows)
	@Test
	public void aViewAskingForAnotherSizeInsideAWindowIsSizedAndTold() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = open("Test Gain");
			List<String> heard = new ArrayList<String>();
			gain.setListener(notice -> heard.add(notice.getKind() + " " + notice.getParameter() + "x" + (int) notice.getPlain()));
			gain.openEditor(app.at(1.0), "");
			once(gain, TGVst3EditorWindow::isEmbedded);
			gain.message("TG.resize", size(300, 150), Collections.<String, byte[]>emptyMap());
			assertEquals("true true 300x150 300x150 [EDITOR_RESIZED 300x150]", inside(gain, app) + " " + heard);
		}
	}

	// TuxGuitar's window resized, the editor is fitted to it again: a view that can be resized takes the new width, one
	// that cannot keeps its size
	@Test
	public void anEmbeddedEditorFitsTheWidthOfTheWindowItIsInOnceThatWindowIsResized() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance stretched = resizableGain();
			TGVst3Instance fixed = open("Test Gain");
			stretched.openEditor(app.at(1.0), "");
			fixed.openEditor(app.at(1.0), "");
			app.resize(700, 300);
			assertEquals("EMBEDDED 700x100 resizable true true 700x100 700x100 | EMBEDDED 200x100 fixed", stretched.fitEditor() + " " + inside(stretched, app) + " | " + fixed.fitEditor());
		}
	}

	// an editor not inside a window has nothing to fit
	@Test
	public void anEditorNotEmbeddedHasNothingToFit() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		String closed = gain.fitEditor().toString();
		gain.openEditor(0, "");
		assertEquals("NONE 0x0 fixed | NONE 0x0 fixed", closed + " | " + gain.fitEditor());
	}

	// a plugin has one view at a time: opened in a window of its own it leaves the window it was in, and put back in
	// it leaves its own window (each a new view); put in the same window again it stays the same
	@Test
	public void anEditorMovesBetweenAWindowOfItsOwnAndAWindowOfTuxGuitarAsANewView() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			StandIn app = new StandIn(other, TGVst3EditorWindow.AWARE_PER_MONITOR_V2, 500, 300);
			TGVst3Instance gain = open("Test Gain");
			gain.openEditor(app.at(1.0), "");
			long embedded = once(gain, TGVst3EditorWindow::isEmbedded).getWindow();
			gain.openEditor(app.at(1.0), "");
			boolean same = (gain.getEditorWindow().getWindow() == embedded);
			gain.openEditor(app.window, "Test Gain – Guitar");
			TGVst3EditorWindow floating = inFront(gain);
			gain.openEditor(app.at(1.0), "");
			TGVst3EditorWindow back = once(gain, TGVst3EditorWindow::isEmbedded);
			assertEquals("true | false " + app.window + " true | true " + app.window, same + " | " + floating.isEmbedded() + " " + floating.getOwner() + " " + (floating.getWindow() != embedded)
					+ " | " + back.isEmbedded() + " " + back.getParent());
		}
	}

	// a test plugin's one-byte answer about the window its editor is in
	private static boolean says(TGVst3Instance plugin, String message) throws TGVst3Exception {
		return plugin.message(message, Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap())[0] == 1;
	}

	// a window of its own opens in the frame given (TuxGuitar remembers where the user left it): at its place, at its size
	// when the view can be resized (else at the view's size); a frame off every screen opens on the nearest screen
	@Test
	public void anEditorOpensInTheFrameGivenOnAVisibleScreen() throws TGVst3Exception {
		TGVst3Instance stretched = resizableGain();
		TGVst3Instance fixed = open("Test Gain");
		TGVst3Instance lost = open("Test Gain");
		stretched.openEditor(TGEditorTarget.floating(0).at(new int[] {300, 200, 800, 500}), "");
		fixed.openEditor(TGEditorTarget.floating(0).at(new int[] {310, 210, 900, 900}), "");
		lost.openEditor(TGEditorTarget.floating(0).at(new int[] {-30000, -30000, -29700, -29800}), "");
		String frames = inFront(stretched).getFrame() + " | " + inFront(fixed).getFrame().startsWith("310,210 ") + " " + inFront(fixed).getInside();
		assertEquals("300,200 800,500 | true 200x100 | true", frames + " | " + says(lost, "TG.on-screen"));
	}

	// a plugin asking its host to show its editor (VST3 requestOpenEditor) is heard
	@Test
	public void aPluginAskingToShowItsEditorIsHeard() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		List<String> heard = new ArrayList<String>();
		gain.setListener(notice -> heard.add(notice.getKind().toString()));
		gain.message("TG.request-editor", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		assertEquals("[EDITOR_REQUESTED]", heard.toString());
	}

	@Test
	public void aPluginWithoutAnEditorOpenHasNoEditorWindow() throws TGVst3Exception {
		TGVst3EditorWindow none = open("Test Gain").getEditorWindow();
		assertEquals("0 0 0 false false -1 -1", none.getWindow() + " " + none.getOwner() + " " + none.getParent() + " " + none.isShown() + " " + none.isEmbedded() + " "
				+ none.getAwareness() + " " + none.getOwnerAwareness());
	}

	// a floating editor is in no window (parent 0, not embedded); its window is per-monitor DPI aware (version 2: 3), and
	// so is the window owning it here (another host's editor window; -1 when none owns it)
	@Test
	public void aFloatingEditorSaysTheDpiAwarenessOfItsWindowAndOfItsOwner() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			TGVst3Instance owned = open("Test Gain");
			TGVst3Instance unowned = open("Test Gain");
			owned.openEditor(windowOfAnotherProcess(other), "");
			unowned.openEditor(0, "");
			TGVst3EditorWindow editor = owned.getEditorWindow();
			assertEquals("0 false 3 3 | 3 -1", editor.getParent() + " " + editor.isEmbedded() + " " + editor.getAwareness() + " " + editor.getOwnerAwareness() + " | "
					+ unowned.getEditorWindow().getAwareness() + " " + unowned.getEditorWindow().getOwnerAwareness());
		}
	}

	@Test
	public void aClosedInstanceIsGone() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.close();
		TGVst3Exception gone = assertThrows(TGVst3Exception.class, () -> process(gain, block(0.25f, 0.25f)));
		assertTrue(gone.getMessage().startsWith("no plugin"), gone.getMessage());
	}
}
