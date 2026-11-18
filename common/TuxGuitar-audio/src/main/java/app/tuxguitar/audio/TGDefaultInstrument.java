package app.tuxguitar.audio;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A VST3 instrument chosen as the default instrument of a kind of channel: by its class id (32 hexadecimal digits, in
 * upper case), which finds it in the scan wherever its module is installed, and its name, for the user to read.
 */
public final class TGDefaultInstrument {

	private static final Pattern CLASS_ID = Pattern.compile("[0-9A-F]{32}");

	private final String classId;
	private final String name;

	// a class id checked (see parse)
	TGDefaultInstrument(String classId, String name) {
		this.classId = classId;
		this.name = name;
	}

	// the instrument of the class id, in any case, and the name (the class id when there is none); none when it is no
	// class id
	public static Optional<TGDefaultInstrument> parse(String classId, String name) {
		String upper = (classId != null ? classId.trim().toUpperCase(Locale.ROOT) : "");
		if (!CLASS_ID.matcher(upper).matches()) {
			return Optional.empty();
		}
		return Optional.of(new TGDefaultInstrument(upper, (name != null && !name.trim().isEmpty() ? name : upper)));
	}

	public String getClassId() {
		return this.classId;
	}

	public String getName() {
		return this.name;
	}
}
