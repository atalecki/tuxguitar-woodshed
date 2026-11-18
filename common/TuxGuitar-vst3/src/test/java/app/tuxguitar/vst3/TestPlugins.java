package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;

/**
 * Where the native build (native/build.cmd or build.sh, run by Maven before the tests) put the host process and the test
 * plugins: the system property tg.vst3.native names its directory.
 */
public final class TestPlugins {

	private TestPlugins() {
	}

	public static File nativeBuild() {
		String directory = System.getProperty("tg.vst3.native");
		if (directory == null) {
			throw new IllegalStateException("tg.vst3.native is not set: run the tests through Maven, which builds the VST3 host first");
		}
		return new File(directory);
	}

	// the host executable, with or without the Windows extension
	public static File host() {
		File bin = new File(nativeBuild(), "bin");
		File windows = new File(bin, "tuxguitar-vst3-host.exe");
		return (windows.isFile() ? windows : new File(bin, "tuxguitar-vst3-host"));
	}

	// the folder holding the test plugins' bundles
	public static File folder() {
		return new File(new File(nativeBuild(), "VST3"), "Release");
	}

	public static File bundle(String name) {
		return new File(folder(), name + ".vst3");
	}

	// a block of frames, each side at a constant level
	public static float[][] block(int frames, float left, float right) {
		float[][] block = new float[2][frames];
		java.util.Arrays.fill(block[0], left);
		java.util.Arrays.fill(block[1], right);
		return block;
	}

	// ends the process, as a crash would, and waits until it is gone
	public static void kill(long pid) {
		ProcessHandle process = ProcessHandle.of(pid).get();
		process.destroyForcibly();
		process.onExit().join();
	}

	// the folder holding Old Amp, a VST 2 plugin
	public static File vst2Folder() {
		return new File(nativeBuild(), "vst2");
	}

	// a NAM capture "Gain 3.nam" in the folder, multiplying the signal by 3
	public static File gain3(File folder) throws Exception {
		File capture = new File(folder, "Gain 3.nam");
		Files.write(capture.toPath(), ("{\"version\": \"0.7.0\", \"architecture\": \"Linear\", \"config\": {\"receptive_field\": 1, \"bias\": false, "
			+ "\"in_channels\": 1, \"out_channels\": 1}, \"weights\": [3.0], \"sample_rate\": 44100}").getBytes(StandardCharsets.UTF_8));
		return capture;
	}

	// its status: the profile, the refusal, the notice and the capture being loaded, a line break between
	public static String profile(TGVst3Instance woodshed) throws TGVst3Exception {
		return new String(woodshed.message("Woodshed.profile", Collections.<String, Long>emptyMap(), Collections.<String, byte[]>emptyMap()), StandardCharsets.UTF_8);
	}

	// waits, 10 seconds at most, until it says it restores its state no more (its host asked meanwhile)
	public static void untilRestored(TGVst3Instance instance) {
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			while (instance.isRestoring(Duration.ofMinutes(1).toMillis())) {
				Thread.onSpinWait();
			}
		});
	}
}
