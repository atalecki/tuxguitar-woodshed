package app.tuxguitar.vst3;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The VST3 modules in folders: every file or bundle named *.vst3, in any subfolder (a bundle's own content is not
 * searched), folder by folder, in name order. Other files in them (NAM captures, *.nam) are found the same way: a file
 * or folder named so is one found, and what it holds is not searched.
 */
public final class TGVst3Folders {

	private static final String EXTENSION = ".vst3";

	private TGVst3Folders() {
	}

	/**
	 * The folders VST3 plugins are installed in on the system (os: its name, as os.name says it), in the order the VST3
	 * SDK lists them: on Windows the common files' and the user's programs' (from the environment, its names in any case
	 * as Windows has them; a variable missing leaves its folder out), on macOS the system's and the user's library,
	 * elsewhere the user's and the system's.
	 */
	public static List<File> standard(String os, Map<String, String> env, File home) {
		String system = os.toLowerCase(Locale.ROOT);
		if (system.startsWith("windows")) {
			Map<String, String> variables = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			variables.putAll(env);
			List<File> folders = new ArrayList<File>();
			under(folders, variables.get("COMMONPROGRAMFILES"), "VST3");
			under(folders, variables.get("LOCALAPPDATA"), "Programs/Common/VST3");
			return folders;
		}
		if (system.startsWith("mac")) {
			return Arrays.asList(new File("/Library/Audio/Plug-Ins/VST3"), new File(home, "Library/Audio/Plug-Ins/VST3"));
		}
		return Arrays.asList(new File(home, ".vst3"), new File("/usr/lib/vst3"), new File("/usr/local/lib/vst3"));
	}

	/**
	 * The folders TuxGuitar scans: the system's (standard), then TuxGuitar's own plugins folder, vst3 next to the folder
	 * of its native libraries (in the install directory: lib and vst3), where the plugins it ships are.
	 */
	public static List<File> scanned(String os, Map<String, String> env, File home, File nativeFolder) {
		List<File> folders = new ArrayList<File>(standard(os, env, home));
		folders.add(new File(nativeFolder.getAbsoluteFile().getParentFile(), "vst3"));
		return folders;
	}

	// the path under the folder, unless there is no folder
	private static void under(List<File> folders, String folder, String path) {
		if (folder != null) {
			folders.add(new File(folder, path));
		}
	}

	public static List<File> modules(List<File> folders) {
		return find(folders, EXTENSION);
	}

	// the files and folders named *<extension> (lower case; any case matches)
	public static List<File> find(List<File> folders, String extension) {
		List<File> found = new ArrayList<File>();
		for (File folder : folders) {
			collect(folder, extension, found);
		}
		return found;
	}

	private static void collect(File folder, String extension, List<File> found) {
		File[] children = folder.listFiles();
		if (children == null) {
			return;
		}
		Arrays.sort(children);
		for (File child : children) {
			if (child.getName().toLowerCase(Locale.ROOT).endsWith(extension)) {
				found.add(child);
			} else if (child.isDirectory()) {
				collect(child, extension, found);
			}
		}
	}
}
