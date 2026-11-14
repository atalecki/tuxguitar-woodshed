package app.tuxguitar.util.log;

import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The user's home folder as the log writes it: "~", wherever a line holds it (a message, a host's error message, a
 * stack trace), so that a log sent in shows no user name. Either slash separates its folders; on Windows its case does
 * not matter. A folder whose name only starts or ends like it (C:\Users\bobby, /mnt/home/bob, /home/bob.old) is kept:
 * the home folder must not be glued to a letter, digit, '.', '_' or '-' on either side. A home folder with no letter or
 * digit at all ("/", "?") shortens nothing.
 */
final class TGLogHome {

	static final String SHORT = "~";

	private static final String SEPARATOR = "[\\\\/]";
	private static final String NAME_CHARACTER = "[\\p{L}\\p{N}._-]";

	// null: nothing is shortened
	private final Pattern home;

	TGLogHome(String home, boolean ignoreCase) {
		this.home = compile(home == null ? "" : home.replaceAll(SEPARATOR + "+$", ""), ignoreCase);
	}

	// the home folder of that system (Java's system properties)
	static TGLogHome of(Properties system) {
		return new TGLogHome(system.getProperty("user.home"), system.getProperty("os.name", "").startsWith("Windows"));
	}

	String shorten(String text) {
		return (this.home != null ? this.home.matcher(text).replaceAll(SHORT) : text);
	}

	private static Pattern compile(String folder, boolean ignoreCase) {
		if (!folder.matches(".*[\\p{L}\\p{N}].*")) {
			return null;
		}
		String path = Arrays.stream(folder.split(SEPARATOR)).map(Pattern::quote).collect(Collectors.joining(SEPARATOR));
		return Pattern.compile((ignoreCase ? "(?iu)" : "") + "(?<!" + NAME_CHARACTER + ")" + path + "(?!" + NAME_CHARACTER + ")");
	}
}
