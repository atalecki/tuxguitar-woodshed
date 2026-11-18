package app.tuxguitar.audio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Which of TuxGuitar's shortcuts a plugin's editor inside the Sound Chain panel hands to TuxGuitar while it has the focus
 * (Q5): those with Ctrl or Alt, and those on a function key (F8, F5...), so they always work; plain keys (Space,
 * letters, arrows, Enter, with or without Shift) stay with the plugin. A shortcut is its keys' names as TuxGuitar's key
 * bindings write them ("Ctrl", "Shift", "Alt", "F8", "z", "Left"...); a key passed is a Windows virtual-key code with
 * its modifiers (virtual key | modifiers << 8: 1 Shift, 2 Ctrl, 4 Alt), as the VST3 host matches the keys typed in the
 * editor. A key without a virtual-key code here (punctuation, whose key differs from one keyboard layout to another)
 * is not passed.
 */
public final class TGEditorKeys {

	private static final int CODE = 0xFF;
	private static final int CTRL_OR_ALT = (2 | 4) << 8;
	private static final int F1 = 0x70;
	private static final int F24 = 0x87;

	// the modifiers by name, as bits of a key passed
	private static final Map<String, Integer> MODIFIERS = new HashMap<String, Integer>();
	static {
		MODIFIERS.put("Shift", 1 << 8);
		MODIFIERS.put("Ctrl", 2 << 8);
		MODIFIERS.put("Alt", 4 << 8);
	}

	// the virtual-key code of each key by name; letters (either case) and digits are their own codes
	private static final Map<String, Integer> CODES = new HashMap<String, Integer>();
	static {
		String[] named = {"Space", "Enter", "Tab", "BackSpace", "ESC", "Ins", "Del", "Home", "End", "PgUp", "PgDn", "Up", "Down", "Left", "Right", "Pause"};
		int[] codes = {0x20, 0x0D, 0x09, 0x08, 0x1B, 0x2D, 0x2E, 0x24, 0x23, 0x21, 0x22, 0x26, 0x28, 0x25, 0x27, 0x13};
		IntStream.range(0, named.length).forEach(i -> CODES.put(named[i], codes[i]));
		IntStream.rangeClosed(F1, F24).forEach(code -> CODES.put("F" + (code - F1 + 1), code));
		IntStream.rangeClosed('A', 'Z').forEach(code -> CODES.put(String.valueOf((char) code), code));
		IntStream.rangeClosed('A', 'Z').forEach(code -> CODES.put(String.valueOf((char) Character.toLowerCase(code)), code));
		IntStream.rangeClosed('0', '9').forEach(code -> CODES.put(String.valueOf((char) code), code));
	}

	private TGEditorKeys() {
	}

	// the keys passed for those shortcuts, each once, in the shortcuts' order
	public static int[] passed(List<List<String>> shortcuts) {
		return shortcuts.stream().mapToInt(TGEditorKeys::key).filter(TGEditorKeys::isPassed).distinct().toArray();
	}

	// the shortcut as a key (virtual key | modifiers << 8); 0 unless it has one key besides its modifiers, with a code
	private static int key(List<String> keys) {
		int modifiers = keys.stream().mapToInt(name -> MODIFIERS.getOrDefault(name, 0)).reduce(0, (held, more) -> held | more);
		List<Integer> codes = keys.stream().filter(name -> !MODIFIERS.containsKey(name)).map(name -> CODES.getOrDefault(name, 0)).collect(Collectors.toList());
		return (codes.size() == 1 ? codes.get(0) | modifiers : 0);
	}

	// with Ctrl or Alt, or on a function key (a key with no code is none of these)
	private static boolean isPassed(int key) {
		int code = (key & CODE);
		return ((key & CTRL_OR_ALT) != 0 && code != 0) || (code >= F1 && code <= F24);
	}
}
