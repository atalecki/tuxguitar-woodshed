package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Woodshed Guitar (TuxGuitar's own plugin, C++) chooses its texts from a table of its own (native/plugins/
 * woodshed_guitar/text.h): the system's UI language, English for a language or a text it lacks. Its check program,
 * built by the native build with the plugin, says which of its checks fail; none should.
 */
class TestWoodshedText {

	// the check program, with or without the Windows extension
	private static File check() {
		File bin = new File(TestPlugins.nativeBuild(), "bin");
		File windows = new File(bin, "woodshed-guitar-text-test.exe");
		return (windows.isFile() ? windows : new File(bin, "woodshed-guitar-text-test"));
	}

	@Test
	void everyCheckOfHowItChoosesItsTextsPasses() throws IOException, InterruptedException {
		Process process = new ProcessBuilder(check().getPath()).redirectErrorStream(true).start();
		String failed = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		assertEquals("0 ", process.waitFor() + " " + failed);
	}
}
