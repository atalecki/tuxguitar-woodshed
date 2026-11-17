package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.audio.action.TGEffectAction;
import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.vst3.TGVst3EditorWindow;
import app.tuxguitar.vst3.TGVst3EffectProcessor;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Instance;
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
		world.fixture().actionContext.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_OWNER, tuxGuitarsWindow());
		openTheEditor(effect, track);
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

	@Then("the plugin's own editor of VST3 effect {int} of track {int} is open")
	public void itsOwnEditorIsOpen(int effect, int track) {
		assertTrue(editor(effect, track).isOwn());
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
		return saying.equals("saying so");
	}

	// a capture loaded in the plugin's own editor (no gesture), the plugin saying it is dirty or not; then the app drains
	@Given("the plugin has loaded the capture {int} into VST3 effect {int} of track {int} {saying}")
	@When("the plugin loads the capture {int} into VST3 effect {int} of track {int} {saying}")
	public void thePluginLoadsTheCapture(int capture, int effect, int track, boolean dirty) throws TGVst3Exception {
		running(effect, track).message("TG.load", Map.of("capture", (long) capture, "dirty", dirty ? 1L : 0L), Collections.<String, byte[]>emptyMap());
		live().drain();
	}

	// its close box hides the window; the hide comes with a reply of the plugin's host, then the app drains
	@When("I close the plugin's own editor of VST3 effect {int} of track {int} by its close box")
	public void closeTheEditorByItsCloseBox(int effect, int track) throws TGVst3Exception {
		TGVst3Instance plugin = running(effect, track);
		plugin.message("TG.close-window", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		long end = System.currentTimeMillis() + 2000;
		while (plugin.getEditorWindow().isShown() && System.currentTimeMillis() < end) {
			java.util.concurrent.locks.LockSupport.parkNanos(5000000);
		}
		live().drain();
	}

	private static int capture(TGVst3Instance plugin) throws TGVst3Exception {
		byte[] capture = plugin.message("TG.capture", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		return java.nio.ByteBuffer.wrap(capture).order(java.nio.ByteOrder.nativeOrder()).getInt();
	}

	// of the song: the plugin opened afresh from the element
	@Then("the capture of VST3 effect {int} of track {int} is {int}")
	public void theCaptureIs(int effect, int track, int capture) throws TGVst3Exception {
		TGVst3EffectProcessor processor = new TGVst3EffectProcessor(TGVst3Hosting.getInstance(world.fixture().context));
		try {
			processor.restoreParameters(effect(effect, track).getParameters());
			assertEquals(capture, capture(processor.getInstance()));
		} finally {
			processor.close();
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
