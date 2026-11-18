package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Woodshed Guitar (TuxGuitar's own plugin, C++) has check programs of its own, built by the native build with the
 * plugin, each saying which of its checks fail; none should. One checks how it chooses its texts from a table of its
 * own (native/plugins/woodshed_guitar/text.h): the system's UI language, English for a language or a text it lacks.
 * Another checks how loud it plays a capture (capture.h): evened out to NAM's target loudness when the capture stores
 * its own, else at the capture's own level. The last checks where its editor puts its controls (layout.h): scaled to the
 * screen's DPI, only the sliders and text lines stretched, the label column as wide as its widest label.
 */
class TestWoodshedChecks {

	// the check program, with or without the Windows extension
	private static File check(String name) {
		File bin = new File(TestPlugins.nativeBuild(), "bin");
		File windows = new File(bin, name + ".exe");
		return (windows.isFile() ? windows : new File(bin, name));
	}

	// its exit code, then what it printed: "0 " when every check passed
	private static String run(String name) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(check(name).getPath()).redirectErrorStream(true).start();
		String failed = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		return process.waitFor() + " " + failed;
	}

	// woodshed-guitar-text-test: how it chooses its texts; woodshed-guitar-level-test: how loud it plays a capture;
	// woodshed-guitar-layout-test: where its editor puts its controls
	@TestFactory
	Stream<DynamicTest> everyCheckOfEachCheckProgramPasses() {
		return Stream.of("woodshed-guitar-text-test", "woodshed-guitar-level-test", "woodshed-guitar-layout-test")
			.map(name -> DynamicTest.dynamicTest(name, () -> assertEquals("0 ", run(name))));
	}
}
