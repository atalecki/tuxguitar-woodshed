package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Where a plugin's own editor goes: floating in a window of its own, or inside a window of TuxGuitar (the Sound Chain
 * panel's editor area), at most so high.
 */
public class TestEditorTarget {

	private static String described(TGEditorTarget target) {
		return target.isEmbedded() + " " + target.getParent() + " " + target.getOwner() + " " + target.getMaxHeight() + " (" + target + ")";
	}

	// floating: owned by the window given, in none; embedded: inside the area, at most the height given, its owner kept
	// for a pop-out; a toolkit whose windows have no native handle (the area: 0) has nothing to put an editor in: it
	// floats
	@Test
	public void anEditorFloatsOrIsInsideTheAreaGiven() {
		String described = Arrays.asList(TGEditorTarget.floating(42), TGEditorTarget.embedded(5, 42, 300), TGEditorTarget.embedded(0, 42, 300)).stream()
				.map(TestEditorTarget::described).collect(Collectors.joining(" | "));
		assertEquals("false 0 42 0 (floating, owned by 42) | true 5 42 300 (inside 5, at most 300 px high, owned by 42) | false 0 42 0 (floating, owned by 42)", described);
	}
}
