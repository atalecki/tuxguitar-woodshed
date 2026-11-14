package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The folders VST3 plugins are installed in on each system (the VST3 SDK's "Plug-in Locations"), searched in the order
 * the SDK lists them. On Windows they are read from the environment; a variable missing leaves its folder out.
 */
class TestVst3Folders {

	private static final File HOME = new File("/home/me");
	private static final Map<String, String> WINDOWS = Map.of("COMMONPROGRAMFILES", "/common", "LOCALAPPDATA", "/local");

	// the folders on the system with the environment, joined by ", ", with "/" between names
	private static String folders(String os, Map<String, String> env) {
		return TGVst3Folders.standard(os, env, HOME).stream().map(folder -> folder.getPath().replace(File.separatorChar, '/')).collect(Collectors.joining(", "));
	}

	@Test
	void eachSystemsFoldersInTheOrderTheSdkListsThem() {
		assertEquals("Windows: /common/VST3, /local/Programs/Common/VST3"
			+ " | macOS: /Library/Audio/Plug-Ins/VST3, /home/me/Library/Audio/Plug-Ins/VST3"
			+ " | Linux: /home/me/.vst3, /usr/lib/vst3, /usr/local/lib/vst3",
			"Windows: " + folders("Windows 11", WINDOWS) + " | macOS: " + folders("Mac OS X", WINDOWS) + " | Linux: " + folders("Linux", WINDOWS));
	}

	@Test
	void onWindowsAFolderWhoseVariableIsMissingIsLeftOut() {
		assertEquals("/local/Programs/Common/VST3 | /common/VST3 | ", folders("Windows 10", Map.of("LOCALAPPDATA", "/local")) + " | " + folders("Windows 10", Map.of("COMMONPROGRAMFILES", "/common")) + " | " + folders("Windows 10", Collections.<String, String>emptyMap()));
	}

	private static void file(File folder, String path) throws IOException {
		File file = new File(folder, path);
		file.getParentFile().mkdirs();
		file.createNewFile();
	}

	// the files found under the folders, by their paths below the root, with "/" between names
	private static String found(List<File> folders, String extension, File root) {
		return TGVst3Folders.find(folders, extension).stream().map(file -> root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')).collect(Collectors.joining(", "));
	}

	// a VST3 bundle is one module: what it holds is not searched for modules, but it is for other files (NAM captures in
	// a folder named like a bundle); a folder that is not there holds nothing
	@Test
	void theFilesOfAKindAreFoundInEverySubfolderInNameOrder(@TempDir File root) throws IOException {
		file(root, "one/Amp.vst3/Contents/x86_64-win/Amp.vst3");
		file(root, "one/NamModels.vst3/Clean.nam");
		file(root, "one/Loose.NAM");
		file(root, "one/sub/Old.vst3");
		file(root, "two/Lead.nam");
		List<File> folders = Arrays.asList(new File(root, "one"), new File(root, "missing"), new File(root, "two"));
		assertEquals("one/Amp.vst3, one/NamModels.vst3, one/sub/Old.vst3 | one/Loose.NAM, one/NamModels.vst3/Clean.nam, two/Lead.nam",
			found(folders, ".vst3", root) + " | " + found(folders, ".nam", root));
	}

	// Windows sets CommonProgramFiles in that case, and Java's System.getenv() map does not ignore the case of names
	@Test
	void onWindowsTheVariablesAreFoundInAnyCase() {
		assertEquals("/common/VST3, /local/Programs/Common/VST3", folders("Windows 11", Map.of("CommonProgramFiles", "/common", "LocalAppData", "/local")));
	}
}
