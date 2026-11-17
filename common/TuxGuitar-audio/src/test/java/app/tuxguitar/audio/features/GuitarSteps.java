package app.tuxguitar.audio.features;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import app.tuxguitar.vst3.TGVst3Folders;
import app.tuxguitar.vst3.TGVst3Hosting;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;

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

	// a capture of the architecture Linear (NeuralAmpModelerCore's A1 format) whose single weight is the gain
	private void capture(String name, String architecture, double gain, int rate) throws IOException {
		String json = "{\"version\": \"0.5.4\", \"architecture\": \"" + architecture + "\", \"config\": {\"receptive_field\": 1, \"bias\": false}, "
			+ "\"weights\": [" + gain + "], \"sample_rate\": " + rate + "}";
		Files.write(folder().resolve(name), json.getBytes(StandardCharsets.UTF_8));
	}

	@Given("there is a NAM capture {string} that multiplies the signal by {double}")
	public void thereIsANamCapture(String name, double gain) throws IOException {
		capture(name, "Linear", gain, 44100);
	}

	@Given("there is a NAM capture {string} that multiplies the signal by {double}, made at {int} Hz")
	public void thereIsANamCaptureMadeAt(String name, double gain, int rate) throws IOException {
		capture(name, "Linear", gain, rate);
	}

	@Given("there is a NAM capture {string} of the architecture {string}")
	public void thereIsANamCaptureOfTheArchitecture(String name, String architecture) throws IOException {
		capture(name, architecture, 1.0, 44100);
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
