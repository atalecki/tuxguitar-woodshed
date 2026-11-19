package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * The shortcuts a plugin's editor inside the Sound Chain panel hands to TuxGuitar: those with Ctrl or Alt, and those on
 * a function key; plain keys stay with the plugin. Each passed as a Windows virtual-key code with its modifiers
 * (virtual key | modifiers << 8: 1 Shift, 2 Ctrl, 4 Alt).
 */
public class TestEditorKeys {

	// each row: shortcuts as TuxGuitar's key bindings write them (tuxguitar-shortcuts.xml, and more), a comma between,
	// then the keys passed as "Ctrl+5a" says (modifiers, then the virtual-key code in hex), in the shortcuts' order
	private static final String[][] PASSED = {
		// with Ctrl or Alt
		{"Ctrl z, Ctrl 1, Ctrl Del, Alt Left, Alt F4, Ctrl Shift p, Alt Shift Right", "Ctrl+5a Ctrl+31 Ctrl+2e Alt+25 Alt+73 Shift+Ctrl+50 Shift+Alt+27"},
		// function keys, with or without modifiers
		{"F1, F8, F12, F24, Shift F7", "70 77 7b 87 Shift+76"},
		// plain keys, alone or with Shift only, stay with the plugin: here only Ctrl+Z goes
		{"Space, a, 0, Left, Up, Enter, Del, Ins, PgUp, Shift Ins, Shift Left, ESC, BackSpace, Tab, Ctrl z", "Ctrl+5a"},
		// no virtual-key code here (punctuation: its key depends on the keyboard's layout), only modifiers, or a modifier
		// of another system (Command): here only Ctrl+A goes
		{"Ctrl +, Ctrl -, Alt /, Ctrl, Ctrl Shift, Command s, Ctrl a", "Ctrl+41"},
		// every named key; letters (either case) and digits are their own codes
		{"Ctrl Space, Ctrl Enter, Ctrl Tab, Ctrl BackSpace, Ctrl ESC, Ctrl Ins, Ctrl Del, Ctrl Home, Ctrl End, Ctrl PgUp, Ctrl PgDn, Ctrl Up, Ctrl Down, Ctrl Left, Ctrl Right, Ctrl Pause, Ctrl a, Ctrl Z, Ctrl 0, Ctrl 9",
			"Ctrl+20 Ctrl+d Ctrl+9 Ctrl+8 Ctrl+1b Ctrl+2d Ctrl+2e Ctrl+24 Ctrl+23 Ctrl+21 Ctrl+22 Ctrl+26 Ctrl+28 Ctrl+25 Ctrl+27 Ctrl+13 Ctrl+41 Ctrl+5a Ctrl+30 Ctrl+39"},
		// a shortcut bound twice (one bound in the user's file, one by default) goes once
		{"F8, Ctrl z, F8, Ctrl Z", "77 Ctrl+5a"},
	};

	private static List<List<String>> shortcuts(String written) {
		return Arrays.stream(written.split(", ")).map(keys -> Arrays.asList(keys.split(" "))).collect(Collectors.toList());
	}

	private static String passed(String written) {
		return Arrays.stream(TGEditorKeys.passed(shortcuts(written))).mapToObj(TestEditorKeys::said).collect(Collectors.joining(" "));
	}

	private static String said(int key) {
		String modifiers = ((key & 0x100) != 0 ? "Shift+" : "") + ((key & 0x200) != 0 ? "Ctrl+" : "") + ((key & 0x400) != 0 ? "Alt+" : "");
		return modifiers + Integer.toHexString(key & 0xFF);
	}

	@Test
	public void theShortcutsWithCtrlAltOrAFunctionKeyArePassedPlainKeysStay() {
		List<String> expected = Arrays.stream(PASSED).map(row -> row[1]).collect(Collectors.toList());
		assertEquals(expected, Arrays.stream(PASSED).map(row -> passed(row[0])).collect(Collectors.toList()));
	}
}
