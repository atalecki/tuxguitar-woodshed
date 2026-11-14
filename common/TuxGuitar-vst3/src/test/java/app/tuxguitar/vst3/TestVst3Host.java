package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
		assertEquals("the VST3 host speaks version 4, not 3", refused.getMessage());
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
		assertFalse(open("Test Clip 0.5").openEditor(0));
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

	private static boolean inALoopOfItsOwn(TGVst3Instance plugin) throws TGVst3Exception {
		return plugin.message("TG.in-modal", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap())[0] == 1;
	}

	// whether each of 10 blocks played while the plugin's editor runs a loop of its own or waits (as the message says)
	// is answered in time, then whether it still runs or waits
	private static String answeredWhileItsEditorWaits(TGVst3Instance plugin, Map<String, Long> wait) throws TGVst3Exception {
		plugin.message("TG.modal", wait, Collections.<String, byte[]>emptyMap());
		assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
			while (!inALoopOfItsOwn(plugin)) {
				Thread.sleep(5);
			}
		});
		List<Boolean> answered = new ArrayList<Boolean>();
		for (int i = 0; i < 10; i++) {
			answered.add(plugin.process(block(0.25f, 0.25f), FRAMES, TGVst3Transport.STOPPED, Collections.<TGVst3NoteEvent>emptyList()));
		}
		return answered + " " + inALoopOfItsOwn(plugin);
	}

	// a plugin's editor that runs a loop of its own (a file dialog, its window dragged) does not keep the host from
	// answering: the blocks played meanwhile are answered in time
	@Test
	public void theHostAnswersInTimeWhileAPluginsEditorRunsALoopOfItsOwn() throws TGVst3Exception {
		assertEquals(Collections.nCopies(10, true) + " true", answeredWhileItsEditorWaits(open("Test Gain"), Collections.singletonMap("millis", 3000L)));
	}

	// the plugin opened in the other host, its editor open
	private static TGVst3Instance withItsEditor(TGVst3Host other, String name) throws TGVst3Exception {
		TGVst3Instance plugin = other.open(other.scan(TestPlugins.bundle(name)).get(0), 44100f, FRAMES);
		plugin.openEditor(0);
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

	@Test
	public void anEditorOpensAgainOnceClosed() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		assertTrue(gain.openEditor(0));
		gain.closeEditor();
		assertTrue(gain.openEditor(0));
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
		gain.openEditor(0);
		long window = inFront(gain).getWindow();
		gain.message("TG.close-window", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		boolean hidden = !once(gain, editor -> !editor.isShown()).isShown();
		boolean stays = !gain.getEditorWindow().isShown();
		gain.openEditor(0);
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
			assertTrue(gain.openEditor(owner.getWindow()));
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
		gain.openEditor(0);
		TGVst3EditorWindow editor = inFront(gain);
		assertEquals("true true 200x100", editor.isShown() + " " + editor.isFront() + " " + editor.getInside());
	}

	// Edit on an editor that is open brings that window to the front rather than opening another
	@Test
	public void anEditorOpenedAgainIsTheSameWindowBroughtToTheFront() throws TGVst3Exception {
		TGVst3Instance first = open("Test Gain");
		TGVst3Instance second = open("Test Gain");
		first.openEditor(0);
		long window = inFront(first).getWindow();
		second.openEditor(0);
		inFront(second);
		boolean behind = first.getEditorWindow().isFront();
		assertTrue(first.openEditor(0));
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
			boolean opened = assertTimeoutPreemptively(Duration.ofMillis(1000), () -> gain.openEditor(owner));
			assertEquals("true " + owner, opened + " " + gain.getEditorWindow().getOwner());
		}
	}

	// closing an editor shown in front (the active window when Windows let the host make it so) is answered at once
	// while the thread of the owner window answers no messages: the window is destroyed once the reply is sent
	@Test
	public void anEditorInFrontOwnedByAWindowWhoseThreadIsBusyClosesAtOnce() throws TGVst3Exception {
		try (TGVst3Host other = TGVst3Host.start(TestPlugins.host())) {
			TGVst3Instance hang = withItsEditor(other, "Test Hang");
			TGVst3Instance gain = open("Test Gain");
			gain.openEditor(hang.getEditorWindow().getWindow());
			inFront(gain);
			hangItsHost(hang);
			assertTimeoutPreemptively(Duration.ofMillis(1000), gain::closeEditor);
			assertEquals(0L, gain.getEditorWindow().getWindow());
		}
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
		assertTrue(unowned.openEditor(0));
		assertTrue(stale.openEditor(gone));
		assertEquals("0 0", unowned.getEditorWindow().getOwner() + " " + stale.getEditorWindow().getOwner());
		assertNotEquals(0L, stale.getEditorWindow().getWindow());
	}

	@Test
	public void aPluginWithoutAnEditorOpenHasNoEditorWindow() throws TGVst3Exception {
		TGVst3EditorWindow none = open("Test Gain").getEditorWindow();
		assertEquals("0 0 false", none.getWindow() + " " + none.getOwner() + " " + none.isShown());
	}

	@Test
	public void aClosedInstanceIsGone() throws TGVst3Exception {
		TGVst3Instance gain = open("Test Gain");
		gain.close();
		TGVst3Exception gone = assertThrows(TGVst3Exception.class, () -> process(gain, block(0.25f, 0.25f)));
		assertTrue(gone.getMessage().startsWith("no plugin"), gone.getMessage());
	}
}
