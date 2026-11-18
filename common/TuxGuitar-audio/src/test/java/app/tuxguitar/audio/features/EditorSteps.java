package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.audio.TGEditorArea;
import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.audio.action.TGEffectAction;
import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.midi.synth.TGAudioProcessor;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.vst3.TGEditorTarget;
import app.tuxguitar.vst3.TGVst3EditorOpened;
import app.tuxguitar.vst3.TGVst3EditorWindow;
import app.tuxguitar.vst3.TGVst3EffectProcessor;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Instance;
import app.tuxguitar.vst3.TGVst3InstrumentProcessor;
import app.tuxguitar.vst3.TGVst3PluginProcessor;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps on a plugin's editors: opening them, and what the plugin does from its own editor ("the plugin sets X to v":
 * one gesture; "the plugin loads its preset P"), played through the test plugins' messages on the plugin as it runs.
 */
public class EditorSteps {

	// the presets of the test plugins, in order (Test Gain's: README)
	private static final List<String> PRESETS = Arrays.asList("Default", "Loud");

	private final AudioWorld world;
	private final PlaySteps play;
	private final RecordingSteps recording;
	// the state of a plugin before the plugin loaded a preset
	private byte[] beforePreset;
	// the native handle of TuxGuitar's window, once a step opened one
	private long tuxGuitarsWindow;
	// the window of a plugin's own editor as a step noted it: its native handle and its frame on the screen
	private String noted;
	// TuxGuitar's window as the editor area: the test plugins' stand-in, made by a plugin of a host process of its own
	// (closed after the scenario), and its native handle (0 until a step made it)
	private TGVst3Instance standInMaker;
	private long standIn;
	// the editor a step opened inside TuxGuitar's window
	private TGPluginEditor inside;
	// the process id of the host of a plugin whose editor a step made hang
	private long hungHost;

	public EditorSteps(AudioWorld world, PlaySteps play, RecordingSteps recording) {
		this.world = world;
		this.play = play;
		this.recording = recording;
	}

	private TGLivePlugins live() {
		return TGLivePlugins.getInstance(world.fixture().context);
	}

	private TGChannel channel(int track) {
		return world.channel(world.track(track));
	}

	private TGProgramElement effect(int effect, int track) {
		return TGSoundChain.get(channel(track)).getOutput(effect - 1);
	}

	// the plugin of the effect as it runs
	private TGVst3Instance running(int effect, int track) throws TGVst3Exception {
		return live().get(channel(track).getChannelId(), effect(effect, track).getId());
	}

	// the effect by its number, the source by none (null), for the track
	private void openTheEditor(Integer effect, int track) {
		openTheEditor(effect, track, world.track(track));
	}

	private void openTheEditor(Integer effect, int track, TGTrack forTrack) {
		world.fixture().actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_CHANNEL, channel(track));
		world.fixture().actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, forTrack);
		world.fixture().actionContext.setAttribute(TGEffectAction.ATTRIBUTE_EFFECT, effect);
		world.attempt(new TGOpenPluginEditorAction(world.fixture().context));
	}

	@Given("I have opened the editor of VST3 effect {int} of track {int}")
	@When("I open the editor of VST3 effect {int} of track {int}")
	public void openTheEditorOfAnEffect(int effect, int track) {
		openTheEditor(effect, track);
	}

	@When("I open the editor of VST3 effect {int} of track {int} for no track in particular")
	public void openTheEditorForNoTrack(int effect, int track) {
		openTheEditor(effect, track, null);
	}

	@Given("I have opened the editor of the source of track {int}")
	@When("I open the editor of the source of track {int}")
	public void openTheEditorOfTheSource(int track) {
		openTheEditor(null, track);
	}

	// TuxGuitar's window: the window of a Test Gain editor in a host process of its own (the hosts close after the
	// scenario)
	private long tuxGuitarsWindow() throws TGVst3Exception {
		TGVst3Hosting hosting = TGVst3Hosting.getInstance(world.fixture().context);
		TGVst3Instance window = hosting.open(hosting.find("Test Gain"), 44100f, 512);
		window.openEditor(0, "");
		this.tuxGuitarsWindow = window.getEditorWindow().getWindow();
		return this.tuxGuitarsWindow;
	}

	@When("I open the editor of VST3 effect {int} of track {int} from TuxGuitar's window")
	public void openTheEditorFromTuxGuitarsWindow(int effect, int track) throws TGVst3Exception {
		world.fixture().actionContext.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_TARGET, TGEditorTarget.floating(tuxGuitarsWindow()));
		openTheEditor(effect, track);
	}

	@Given("TuxGuitar's window is {int} by {int} inside")
	public void tuxGuitarsWindowIs(int width, int height) throws TGVst3Exception {
		TGVst3Hosting hosting = TGVst3Hosting.getInstance(world.fixture().context);
		this.standInMaker = hosting.open(hosting.find("Test Gain"), 44100f, 512);
		byte[] handle = this.standInMaker.message("TG.window", Map.of("awareness", 3L, "width", (long) width, "height", (long) height), Collections.<String, byte[]>emptyMap());
		this.standIn = ByteBuffer.wrap(handle).order(ByteOrder.nativeOrder()).getLong();
	}

	@When("TuxGuitar's window becomes {int} by {int} inside")
	public void tuxGuitarsWindowBecomes(int width, int height) throws TGVst3Exception {
		this.standInMaker.message("TG.window-size", Map.of("window", this.standIn, "width", (long) width, "height", (long) height), Collections.<String, byte[]>emptyMap());
	}

	// inside TuxGuitar's window (600 by 400 unless a step made it), as the Sound Chain panel opens it in its area
	@Given("I have opened the editor of VST3 effect {int} of track {int} inside TuxGuitar's window")
	@When("I open the editor of VST3 effect {int} of track {int} inside TuxGuitar's window")
	public void openTheEditorInside(int effect, int track) throws TGVst3Exception {
		if (this.standIn == 0) {
			tuxGuitarsWindowIs(600, 400);
		}
		world.fixture().actionContext.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_TARGET, TGEditorTarget.embedded(this.standIn, 0, TGEditorArea.MAX_SHARE));
		openTheEditor(effect, track);
		this.inside = editor(effect, track);
	}

	@Then("the plugin's own editor of {plugin} is inside TuxGuitar's window, {int} by {int}")
	public void itsOwnEditorIsInside(Map.Entry<Integer, String> plugin, int width, int height) throws TGVst3Exception {
		TGVst3EditorWindow window = running(plugin).getEditorWindow();
		long end = System.currentTimeMillis() + 2000;
		while (!window.isEmbedded() && System.currentTimeMillis() < end) {
			java.util.concurrent.locks.LockSupport.parkNanos(5000000);
			window = running(plugin).getEditorWindow();
		}
		assertEquals("true " + width + "x" + height, (window.getParent() == this.standIn) + " " + window.getInside());
	}

	@Then("the editor of {plugin} is too large for TuxGuitar's window, {string}")
	public void itsEditorIsTooLarge(Map.Entry<Integer, String> plugin, String size) {
		assertEquals("TOO_LARGE " + size, live().getEditor(plugin.getKey(), plugin.getValue()).getOpened().toString());
	}

	@Then("no editor of {plugin} is open")
	public void noEditorIsOpen(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		assertEquals(0L, running(plugin).getEditorWindow().getWindow());
	}

	@Then("the editor opened inside TuxGuitar's window has moved out of it")
	public void theEditorInsideHasMoved() {
		assertEquals(TGPluginEditor.State.MOVED, this.inside.getState());
	}

	@When("I close the editor opened inside TuxGuitar's window")
	public void closeTheEditorInside() {
		live().close(this.inside);
	}

	@Then("the editor opened inside TuxGuitar's window is closed, with nothing left to fit")
	public void theEditorInsideIsClosed() throws TGVst3Exception {
		assertEquals("CLOSED " + TGVst3EditorOpened.NONE, this.inside.getState() + " " + live().fit(this.inside));
	}

	// its code hangs for 20 s (a deadlock in its UI), the host process it runs in noted
	@When("the editor of {plugin} stops responding")
	public void itsEditorStopsResponding(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		TGVst3Instance hanging = running(plugin);
		this.hungHost = hanging.getHost().getPid();
		hanging.message("TG.hang-view", Collections.singletonMap("millis", 20000L), Collections.<String, byte[]>emptyMap());
	}

	// as the app polls every 100 ms, until the editor opened inside TuxGuitar's window is no longer open
	@When("TuxGuitar watches its plugins for up to {int} seconds")
	public void tuxGuitarWatchesItsPlugins(int seconds) throws InterruptedException {
		long end = System.currentTimeMillis() + seconds * 1000L;
		while (this.inside.isOpen() && System.currentTimeMillis() < end) {
			live().poll();
			Thread.sleep(100);
		}
	}

	@Then("the editor opened inside TuxGuitar's window says its plugin stopped responding and was started again")
	public void theEditorInsideSaysItsPluginWasStartedAgain() {
		assertEquals("STOPPED true true", this.inside.getState() + " " + this.inside.isHung() + " " + this.inside.isRestarted());
	}

	@Then("the plugin of {plugin} runs in another host process, its {string} at {double}")
	public void itRunsInAnotherHostProcess(Map.Entry<Integer, String> plugin, String title, double value) throws TGVst3Exception {
		TGVst3Instance now = running(plugin);
		assertEquals("true " + value, (now.getHost().getPid() != this.hungHost && now.getHost().isRunning()) + " " + now.getParameter(now.findParameter(title)));
	}

	@Given("the plugin of {plugin} makes its editor resizable")
	public void itMakesItsEditorResizable(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		running(plugin).message("TG.resizable", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
	}

	@When("the editor opened inside TuxGuitar's window is fitted again")
	public void theEditorInsideIsFittedAgain() throws TGVst3Exception {
		live().fit(this.inside);
	}

	// as a crash ends it
	@Given("the host process of VST3 effect {int} of track {int} has stopped")
	public void theHostProcessHasStopped(int effect, int track) throws TGVst3Exception {
		ProcessHandle process = ProcessHandle.of(running(effect, track).getHost().getPid()).get();
		process.destroyForcibly();
		process.onExit().join();
	}

	private TGPluginEditor editor(int effect, int track) {
		return live().getEditor(channel(track).getChannelId(), effect(effect, track).getId());
	}

	// a plugin of a track's sound chain as a step names it: "VST3 effect N of track M", "the source of track M", or "the
	// default instrument of track M" (what the track plays by default, as its source or not); read as its channel's id
	// and its element's
	@ParameterType("VST3 effect \\d+ of track \\d+|the source of track \\d+|the default instrument of track \\d+")
	public Map.Entry<Integer, String> plugin(String words) {
		Matcher named = Pattern.compile("(?:VST3 effect (\\d+)|the (source|default instrument)) of track (\\d+)").matcher(words);
		named.matches();
		int track = Integer.parseInt(named.group(3));
		String element = (named.group(1) != null ? effect(Integer.parseInt(named.group(1)), track).getId() : instrument(named.group(2), track));
		return Map.entry(channel(track).getChannelId(), element);
	}

	private String instrument(String instrument, int track) {
		return (instrument.equals("source") ? "source" : TGDefaultInstrumentSource.install(world.fixture().context).play(channel(track)).getElement().getId());
	}

	private TGVst3Instance running(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		return live().get(plugin.getKey(), plugin.getValue());
	}

	@Then("the plugin's own editor of {plugin} is open")
	public void itsOwnEditorIsOpen(Map.Entry<Integer, String> plugin) {
		assertTrue(live().getEditor(plugin.getKey(), plugin.getValue()).isOwn());
	}

	@Given("I have noted the window of the plugin's own editor of {plugin}")
	public void noteTheWindow(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		this.noted = window(plugin);
	}

	private String window(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		TGVst3EditorWindow window = running(plugin).getEditorWindow();
		return window.getWindow() + " " + window.getFrame() + " shown " + window.isShown();
	}

	@Then("the plugin's own editor of {plugin} is open in the window noted, where it was")
	public void itsOwnEditorIsOpenInTheWindowNoted(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		itsOwnEditorIsOpen(plugin);
		assertEquals(this.noted, window(plugin));
	}

	// the plugin as the synthesizer runs it
	@Then("the running plugin of {plugin} plays the capture {int}")
	public void theRunningPluginPlaysTheCapture(Map.Entry<Integer, String> plugin, int capture) throws TGVst3Exception {
		assertEquals(capture, capture(running(plugin)));
	}

	@Then("the window of the plugin's own editor of VST3 effect {int} of track {int} is titled {string}")
	public void itsOwnEditorsWindowIsTitled(int effect, int track, String title) throws TGVst3Exception {
		assertEquals(title, running(effect, track).getEditorWindow().getTitle());
	}

	@Then("the plugin's own editor of VST3 effect {int} of track {int} is in a window of its own")
	public void itsOwnEditorIsInAWindowOfItsOwn(int effect, int track) throws TGVst3Exception {
		TGVst3EditorWindow editor = running(effect, track).getEditorWindow();
		assertEquals("true 0", (editor.getWindow() != 0) + " " + editor.getOwner());
	}

	@Then("the plugin's own editor of VST3 effect {int} of track {int} is in a window TuxGuitar's window owns")
	public void itsOwnEditorIsInAWindowTuxGuitarsWindowOwns(int effect, int track) throws TGVst3Exception {
		assertEquals(this.tuxGuitarsWindow, running(effect, track).getEditorWindow().getOwner());
	}

	@Then("TuxGuitar's parameter editor of VST3 effect {int} of track {int} is open")
	public void theParameterEditorIsOpen(int effect, int track) {
		assertFalse(editor(effect, track).isOwn());
	}

	@Then("TuxGuitar's parameter editor of {plugin} is open, reading {string}")
	public void theParameterEditorIsOpenReading(Map.Entry<Integer, String> plugin, String parameters) {
		TGPluginEditor editor = live().getEditor(plugin.getKey(), plugin.getValue());
		assertEquals("false " + parameters, editor.isOwn() + " " + String.join(", ", editor.getParameters()));
	}

	@Then("the parameters of VST3 effect {int} of track {int} read {string}")
	public void theParametersRead(int effect, int track, String expected) {
		assertEquals(expected, String.join(", ", editor(effect, track).getParameters()));
	}

	// one gesture in the plugin's own editor, through the values
	private void gesture(String title, int effect, int track, double... values) throws TGVst3Exception {
		gesture(running(effect, track), title, values);
	}

	// then the app drains it at once, as its UI thread does within a few milliseconds
	private void gesture(TGVst3Instance instance, String title, double... values) throws TGVst3Exception {
		instance.message("TG.gesture", Map.of("id", (long) instance.findParameter(title)), Map.of("values", TGVst3Instance.doubles(values)));
		live().drain();
	}

	// the gesture, while the song plays, once it reaches the position
	private void schedule(String title, int effect, int track, double value, SongPosition at) {
		double quarters = (double) (at.tick(world.song()) - TGDuration.QUARTER_TIME) / TGDuration.QUARTER_TIME;
		world.reporting().schedule(() -> this.runningOrNone(effect, track), quarters, plugin -> gesture(plugin, title, value));
	}

	private TGVst3Instance runningOrNone(int effect, int track) {
		try {
			return running(effect, track);
		} catch (TGVst3Exception e) {
			return null;
		}
	}

	@When("the plugin sets {string} of VST3 effect {int} of track {int} to {double} at {position}")
	public void thePluginSetsWhilePlaying(String title, int effect, int track, double value, SongPosition at) {
		schedule(title, effect, track, value, at);
		this.play.playUnderWay();
		assertFalse(world.reporting().pending(), "the plugin did the gesture while the song played");
	}

	@Given("I have recorded from {position} {stop} while the plugin set {string} of VST3 effect {int} of track {int} to {double} at {position}")
	public void haveRecordedWhileThePluginSet(SongPosition from, RecordingStop stop, String title, int effect, int track, double value, SongPosition at) {
		schedule(title, effect, track, value, at);
		this.recording.haveRecorded(from, stop);
		assertFalse(world.reporting().pending(), "the plugin did the gesture while recording");
	}

	@Given("the plugin has set {string} of VST3 effect {int} of track {int} to {double}")
	@When("the plugin sets {string} of VST3 effect {int} of track {int} to {double}")
	public void thePluginSets(String title, int effect, int track, double value) throws TGVst3Exception {
		gesture(title, effect, track, value);
	}

	@Given("the plugin has set {string} of VST3 effect {int} of track {int} through {double} to {double}")
	public void thePluginHasSetThrough(String title, int effect, int track, double through, double value) throws TGVst3Exception {
		gesture(title, effect, track, through, value);
	}

	@Given("the plugin has loaded its preset {string} for VST3 effect {int} of track {int}")
	@When("the plugin loads its preset {string} for VST3 effect {int} of track {int}")
	public void thePluginLoadsItsPreset(String preset, int effect, int track) throws TGVst3Exception {
		this.beforePreset = state(effect(effect, track));
		running(effect, track).message("TG.preset", Map.of("index", (long) PRESETS.indexOf(preset)), Collections.<String, byte[]>emptyMap());
		live().drain();
	}

	// whether the plugin says it is dirty
	@ParameterType("saying so|without saying so")
	public Boolean saying(String saying) {
		return !saying.startsWith("without");
	}

	// a capture loaded in the plugin's own editor (no gesture), the plugin saying it is dirty or not; then the app drains
	@Given("the plugin has loaded the capture {int} into {plugin} {saying}")
	@When("the plugin loads the capture {int} into {plugin} {saying}")
	public void thePluginLoadsTheCapture(int capture, Map.Entry<Integer, String> plugin, boolean dirty) throws TGVst3Exception {
		running(plugin).message("TG.load", Map.of("capture", (long) capture, "dirty", dirty ? 1L : 0L), Collections.<String, byte[]>emptyMap());
		live().drain();
	}

	// then the app drains
	@When("I close the plugin's own editor of {plugin} by its close box")
	public void closeTheEditorByItsCloseBox(Map.Entry<Integer, String> plugin) throws TGVst3Exception {
		closeBox(running(plugin));
		live().drain();
	}

	// its close box hides the window; the hide comes with a reply of the plugin's host
	public static void closeBox(TGVst3Instance plugin) throws TGVst3Exception {
		plugin.message("TG.close-window", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		long end = System.currentTimeMillis() + 2000;
		while (plugin.getEditorWindow().isShown() && System.currentTimeMillis() < end) {
			java.util.concurrent.locks.LockSupport.parkNanos(5000000);
		}
	}

	// the capture a test plugin plays
	public static int capture(TGVst3Instance plugin) throws TGVst3Exception {
		byte[] capture = plugin.message("TG.capture", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		return java.nio.ByteBuffer.wrap(capture).order(java.nio.ByteOrder.nativeOrder()).getInt();
	}

	// of the song: the plugin opened afresh from the element
	@Then("the capture of VST3 effect {int} of track {int} is {int}")
	public void theCaptureIs(int effect, int track, int capture) throws TGVst3Exception {
		theCaptureIs(new TGVst3EffectProcessor(TGVst3Hosting.getInstance(world.fixture().context)), effect(effect, track), capture);
	}

	@Then("the capture of the source of track {int} is {int}")
	public void theSourcesCaptureIs(int track, int capture) throws TGVst3Exception {
		theCaptureIs(new TGVst3InstrumentProcessor(TGVst3Hosting.getInstance(world.fixture().context)), TGSoundChain.get(channel(track)).getReceiver(), capture);
	}

	private static void theCaptureIs(TGVst3PluginProcessor processor, TGProgramElement element, int capture) throws TGVst3Exception {
		try {
			((TGAudioProcessor) processor).restoreParameters(element.getParameters());
			assertEquals(capture, capture(processor.getInstance()));
		} finally {
			((TGAudioProcessor) processor).close();
		}
	}

	// the state of the plugin opened from the element
	private byte[] state(TGProgramElement element) throws TGVst3Exception {
		TGVst3EffectProcessor processor = new TGVst3EffectProcessor(TGVst3Hosting.getInstance(world.fixture().context));
		try {
			processor.restoreParameters(element.getParameters());
			return processor.getInstance().getState().getComponent();
		} finally {
			processor.close();
		}
	}

	@Then("the state of VST3 effect {int} of track {int} is the preset {string}")
	public void theStateIsThePreset(int effect, int track, String preset) throws TGVst3Exception {
		TGVst3Instance fresh = TGVst3Hosting.getInstance(world.fixture().context).open(TGVst3Element.plugin(effect(effect, track).getParameters(), false), 44100f, 512);
		try {
			fresh.message("TG.preset", Map.of("index", (long) PRESETS.indexOf(preset)), Collections.<String, byte[]>emptyMap());
			assertArrayEquals(fresh.getState().getComponent(), state(effect(effect, track)));
		} finally {
			fresh.close();
		}
	}

	@Then("the state of VST3 effect {int} of track {int} is byte for byte what it was before the preset")
	public void theStateIsWhatItWasBeforeThePreset(int effect, int track) throws TGVst3Exception {
		assertArrayEquals(this.beforePreset, state(effect(effect, track)));
	}
}
