package app.tuxguitar.audio.features;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.tuxguitar.audio.TGLivePlugins;
import app.tuxguitar.audio.TGSoundChain;
import app.tuxguitar.io.base.TGFileFormatException;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.vst3.TGVst3Element;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Folders;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Instance;
import app.tuxguitar.vst3.TGVst3InstrumentProcessor;
import app.tuxguitar.vst3.TGVst3State;
import io.cucumber.java.After;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps on Woodshed Guitar, the VST3 instrument TuxGuitar ships, and the NAM captures it plays: the captures are files
 * of a folder of the scenario's own, made as the scenario says.
 */
public class GuitarSteps {

	private final AudioWorld world;
	private final SavingSteps saving;
	// the scenario's folder: its NAM captures and other files, and a TuxGuitar installed there
	private Path folder;

	public GuitarSteps(AudioWorld world, SavingSteps saving) {
		this.world = world;
		this.saving = saving;
	}

	private Path folder() throws IOException {
		if (this.folder == null) {
			this.folder = Files.createTempDirectory("woodshed-");
		}
		return this.folder;
	}

	@After
	public void removeTheFolder() throws IOException {
		if (this.folder != null) {
			try (Stream<Path> paths = Files.walk(this.folder)) {
				paths.sorted(Comparator.reverseOrder()).forEach(GuitarSteps::delete);
			}
		}
	}

	private static void delete(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// a capture of the architecture Linear (NeuralAmpModelerCore 0.6.0's format, one input and one output) whose single
	// weight is the gain, of the file version and made at the rate
	private void capture(String name, String version, String architecture, double gain, int rate) throws IOException {
		String json = "{\"version\": \"" + version + "\", \"architecture\": \"" + architecture + "\", "
			+ "\"config\": {\"receptive_field\": 1, \"bias\": false, \"in_channels\": 1, \"out_channels\": 1}, "
			+ "\"weights\": [" + gain + "], \"sample_rate\": " + rate + "}";
		Files.write(folder().resolve(name), json.getBytes(StandardCharsets.UTF_8));
	}

	@Given("there is a NAM capture {string} that multiplies the signal by {double}")
	public void thereIsANamCapture(String name, double gain) throws IOException {
		capture(name, "0.7.0", "Linear", gain, 44100);
	}

	@Given("there is a NAM capture {string} that multiplies the signal by {double}, made at {int} Hz")
	public void thereIsANamCaptureMadeAt(String name, double gain, int rate) throws IOException {
		capture(name, "0.7.0", "Linear", gain, rate);
	}

	@Given("there is a NAM capture {string} of the architecture {string}")
	public void thereIsANamCaptureOfTheArchitecture(String name, String architecture) throws IOException {
		capture(name, "0.7.0", architecture, 1.0, 44100);
	}

	@Given("there is a NAM capture {string} of the file version {string}")
	public void thereIsANamCaptureOfTheFileVersion(String name, String version) throws IOException {
		capture(name, version, "Linear", 1.0, 44100);
	}

	// the test captures (README), from the test resources
	@Given("there is the test NAM capture {string}")
	public void thereIsTheTestNamCapture(String name) throws IOException {
		try (java.io.InputStream capture = GuitarSteps.class.getResourceAsStream("/captures/" + name)) {
			Files.copy(capture, folder().resolve(name));
		}
	}

	@Given("the NAM capture {string} has been moved away")
	public void theNamCaptureHasBeenMovedAway(String name) throws IOException {
		Files.delete(folder().resolve(name));
	}

	@Given("the NAM capture {string} has been changed to multiply the signal by {double}")
	public void theNamCaptureHasBeenChanged(String name, double gain) throws IOException {
		assertTrue(Files.exists(folder().resolve(name)), name);
		capture(name, "0.7.0", "Linear", gain, 44100);
	}

	@Given("the NAM capture {string} has been left as it was")
	public void theNamCaptureHasBeenLeft(String name) throws IOException {
		assertTrue(Files.exists(folder().resolve(name)), name);
	}

	// the plugin of the track's source as it runs
	private TGVst3Instance running(int track) throws TGVst3Exception {
		return TGLivePlugins.getInstance(world.fixture().context).get(world.channel(world.track(track)).getChannelId(), "source");
	}

	// as its editor's Load profile... does, with the file of the scenario's folder; then the app drains what the plugin
	// told it
	@When("Woodshed Guitar of track {int} loads the NAM capture {string}")
	@When("Woodshed Guitar of track {int} loads the file {string}")
	public void loads(int track, String name) throws TGVst3Exception, IOException {
		byte[] path = folder().resolve(name).toString().getBytes(StandardCharsets.UTF_8);
		running(track).message("Woodshed.load", Collections.<String, Long>emptyMap(), Map.of("path", path));
		TGLivePlugins.getInstance(world.fixture().context).drain();
	}

	@Given("Woodshed Guitar of track {int} has loaded the NAM capture {string}")
	public void hasLoaded(int track, String name) throws TGVst3Exception, IOException {
		loads(track, name);
		assertEquals(name + "\n", profileAndRefusal(running(track)));
	}

	// what the plugin says of its profile, a line each: the profile's name, the refusal of the last file given to it, and
	// what it says of the profile's file
	private static String[] status(TGVst3Instance plugin) throws TGVst3Exception {
		return lines(plugin.message("Woodshed.profile", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()));
	}

	private static String[] lines(byte[] report) {
		return new String(report, StandardCharsets.UTF_8).split("\n", -1);
	}

	// the profile's name, a line break, the refusal
	private static String profileAndRefusal(TGVst3Instance plugin) throws TGVst3Exception {
		String[] status = status(plugin);
		return status[0] + "\n" + status[1];
	}

	// what it says of the profile's file
	@Then("Woodshed Guitar of track {int} says {string}")
	public void says(int track, String notice) throws TGVst3Exception {
		assertEquals(notice, status(running(track))[2]);
	}

	@Then("Woodshed Guitar of track {int} refuses it, saying something starting {string}")
	public void refusesIt(int track, String refusal) throws TGVst3Exception {
		String said = status(running(track))[1];
		assertTrue(said.startsWith(refusal), said);
	}

	// of the song: the plugin opened afresh from the source's element, as a song opened elsewhere is
	private <T> T ofTheSong(int track, Function<TGVst3Instance, T> look) throws TGVst3Exception {
		TGProgramElement source = TGSoundChain.get(world.channel(world.track(track))).getReceiver();
		TGVst3InstrumentProcessor processor = new TGVst3InstrumentProcessor(TGVst3Hosting.getInstance(world.fixture().context));
		try {
			processor.restoreParameters(source.getParameters());
			return look.apply(processor.getInstance());
		} finally {
			processor.close();
		}
	}

	@Then("Woodshed Guitar of track {int} plays the profile {string}")
	public void playsTheProfile(int track, String name) throws TGVst3Exception {
		assertEquals(name + "\n", ofTheSong(track, GuitarSteps::statusOf));
	}

	@Then("Woodshed Guitar of track {int} plays no profile")
	public void playsNoProfile(int track) throws TGVst3Exception {
		assertEquals("\n", ofTheSong(track, GuitarSteps::statusOf));
	}

	private static String statusOf(TGVst3Instance plugin) {
		try {
			return profileAndRefusal(plugin);
		} catch (TGVst3Exception e) {
			throw new IllegalStateException(e);
		}
	}

	// its lines, read from its window: the profile, the refusal, what it says of the profile's file, and the text of the
	// button it offers there ("" when it shows none)
	// two of them read as the scenario words them: 'shows "<profile>" and "<refusal>"' or 'says "<notice>", offering
	// "<button>"'; each is the first line read and the line after it, a line break between
	@ParameterType("shows \"([^\"]*)\" and \"([^\"]*)\"|says \"([^\"]*)\", offering \"([^\"]*)\"")
	public Map.Entry<Integer, String> editorLines(String profile, String refusal, String notice, String button) {
		return (profile != null ? Map.entry(0, profile + "\n" + refusal) : Map.entry(2, notice + "\n" + button));
	}

	@Then("Woodshed Guitar's editor of track {int} {editorLines}")
	public void itsEditorShows(int track, Map.Entry<Integer, String> lines) throws TGVst3Exception {
		String[] shown = lines(running(track).message("Woodshed.shown", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()));
		assertEquals(lines.getValue(), shown[lines.getKey()] + "\n" + shown[lines.getKey() + 1]);
	}

	// as the user clicks the button its editor shows with that text (it shows none: the step fails); then the app drains
	// what the plugin told it
	@Given("I have clicked {string} in Woodshed Guitar's editor of track {int}")
	@When("I click {string} in Woodshed Guitar's editor of track {int}")
	public void clickInTheEditor(String button, int track) throws TGVst3Exception {
		byte[] clicked = running(track).message("Woodshed.click", Collections.<String, Long>emptyMap(), Map.of("button", button.getBytes(StandardCharsets.UTF_8)));
		assertEquals(button, new String(clicked, StandardCharsets.UTF_8), "the editor shows the button");
		TGLivePlugins.getInstance(world.fixture().context).drain();
	}

	// as the user moves the control of the parameter in the open editor (its plain value: dB, or Quality's index); then
	// the app drains the gesture
	@When("I set {string} to {double} in Woodshed Guitar's editor of track {int}")
	public void setInTheEditor(String title, double value, int track) throws TGVst3Exception {
		TGVst3Instance plugin = running(track);
		plugin.message("Woodshed.move", Map.of("id", (long) plugin.findParameter(title)), Map.of("value", TGVst3Instance.doubles(value)));
		TGLivePlugins.getInstance(world.fixture().context).drain();
	}

	@Then("Woodshed Guitar of track {int} has {string} at {double}")
	public void hasAt(int track, String title, double value) throws TGVst3Exception {
		assertEquals(value, (double) ofTheSong(track, plugin -> parameter(plugin, title)), 1e-9);
	}

	private static double parameter(TGVst3Instance plugin, String title) {
		try {
			return plugin.getParameter(plugin.findParameter(title));
		} catch (TGVst3Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Then("Woodshed Guitar's editor of track {int} shows the controls {string}")
	public void itsEditorShowsTheControls(int track, String controls) throws TGVst3Exception {
		byte[] shown = running(track).message("Woodshed.controls", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap());
		assertEquals(controls, new String(shown, StandardCharsets.UTF_8));
	}

	@Then("Woodshed Guitar of track {int} reports a latency of {int} samples")
	public void reportsALatencyOf(int track, int latency) throws TGVst3Exception {
		assertEquals(latency, (int) ofTheSong(track, TGVst3Instance::getLatency));
	}

	@Then("Woodshed Guitar of track {int} reports a latency above 0 samples")
	public void reportsALatencyAboveZero(int track) throws TGVst3Exception {
		int latency = ofTheSong(track, TGVst3Instance::getLatency);
		assertTrue(latency > 0, "latency " + latency);
	}

	// the TuxGuitar file the song is now, as the app saves it, against the one saved last
	@Then("the song saved now is larger than the song saved before by less than a tenth of the size of {string}")
	public void theSongSavedNowIsLargerByLessThan(String name) throws IOException {
		long grown = this.saving.write().length - world.saved().length;
		assertTrue(grown < Files.size(folder().resolve(name)) / 10, "grown by " + grown + " bytes");
	}

	// as the commits before captures were stored once saved it: Woodshed Guitar's state is "WSG1", Input gain, Output
	// level and Quality (doubles), then the capture's path and contents (each its length in 8 bytes, then its bytes), all
	// little-endian; the song written as it is, the plugins not asked for their states
	@Given("I have saved the song as a TuxGuitar file, Woodshed Guitar of track {int} holding {string} in its own state")
	public void haveSavedHoldingItsOwnState(int track, String name) throws IOException, TGFileFormatException {
		byte[] path = folder().resolve(name).toString().getBytes(StandardCharsets.UTF_8);
		byte[] contents = Files.readAllBytes(folder().resolve(name));
		ByteBuffer state = ByteBuffer.allocate(4 + 3 * Double.BYTES + Long.BYTES + path.length + Long.BYTES + contents.length).order(ByteOrder.LITTLE_ENDIAN);
		state.put("WSG1".getBytes(StandardCharsets.US_ASCII)).putDouble(0.0).putDouble(0.0).putDouble(0.0);
		state.putLong(path.length).put(path).putLong(contents.length).put(contents);
		TGChannel channel = world.channel(world.track(track));
		TGProgram chain = TGSoundChain.get(channel);
		TGVst3Element.setState(chain.getReceiver(), new TGVst3State(state.array(), new byte[0]));
		TGSoundChain.save(channel, chain, world.fixture().songManager.getFactory());
		world.setSaved(this.saving.writeAsItIs());
	}

	@Given("there is a text file {string}")
	public void thereIsATextFile(String name) throws IOException {
		Files.write(folder().resolve(name), "a text, not a capture".getBytes(StandardCharsets.UTF_8));
	}

	// TuxGuitar's install directory: lib (its native libraries), and vst3, its own plugins folder holding the bundle the
	// native build made; the system's VST3 folders as on a Windows without them (no variable names one)
	@Given("TuxGuitar is installed with Woodshed Guitar in its own plugins folder, and no VST3 plugin is installed on the system")
	public void tuxGuitarIsInstalledWithWoodshedGuitar() throws IOException {
		Path install = folder().resolve("TuxGuitar");
		Path bundle = PluginSteps.ownPlugins().toPath().resolve("Woodshed Guitar.vst3");
		copy(bundle, install.resolve("vst3").resolve("Woodshed Guitar.vst3"));
		File lib = Files.createDirectories(install.resolve("lib")).toFile();
		TGVst3Hosting.getInstance(world.fixture().context).configure(PluginSteps.host(), TGVst3Folders.scanned("Windows 11", Map.of(), install.toFile(), lib));
	}

	private static void copy(Path from, Path to) throws IOException {
		Files.createDirectories(to.getParent());
		try (Stream<Path> paths = Files.walk(from)) {
			for (Path path : (Iterable<Path>) paths::iterator) {
				Files.copy(path, to.resolve(from.relativize(path).toString()));
			}
		}
	}
}
