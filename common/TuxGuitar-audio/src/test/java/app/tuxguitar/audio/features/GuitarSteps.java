package app.tuxguitar.audio.features;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.vst3.TGVst3Exception;
import app.tuxguitar.vst3.TGVst3Folders;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Instance;
import app.tuxguitar.vst3.TGVst3InstrumentProcessor;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps on Woodshed Guitar, the VST3 instrument TuxGuitar ships, and the NAM captures it plays: the captures are files
 * of a folder of the scenario's own, made as the scenario says.
 */
public class GuitarSteps {

	private final AudioWorld world;
	// the scenario's folder: its NAM captures and other files, and a TuxGuitar installed there
	private Path folder;

	public GuitarSteps(AudioWorld world) {
		this.world = world;
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
		assertEquals(name + "\n", status(running(track)));
	}

	// the profile's name, a line break, the refusal of the last file given to it
	private static String status(TGVst3Instance plugin) throws TGVst3Exception {
		return new String(plugin.message("Woodshed.profile", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()), StandardCharsets.UTF_8);
	}

	@Then("Woodshed Guitar of track {int} refuses it, saying something starting {string}")
	public void refusesIt(int track, String refusal) throws TGVst3Exception {
		String status = status(running(track));
		String said = status.substring(status.indexOf('\n') + 1);
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
			return status(plugin);
		} catch (TGVst3Exception e) {
			throw new IllegalStateException(e);
		}
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
