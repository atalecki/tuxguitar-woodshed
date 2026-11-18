package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The editor area's height in the toolkit's units, from the pixels the host reports: the area as wide as 800 units is
 * 800 pixels wide at 100 %, 1200 at 150 %, 1600 at 200 %.
 */
public class TestEditorArea {

	@Test
	public void theAreaIsAsHighAsTheViewAtEachScale() {
		String heights = TGEditorArea.height(230, 800, 800) + " " + TGEditorArea.height(345, 1200, 800) + " " + TGEditorArea.height(460, 1600, 800);
		assertEquals("230 230 230", heights);
	}

	// a part of a unit is a whole one: the view is never cut at its foot
	@Test
	public void aPartOfAUnitIsAWholeOne() {
		assertEquals("154 1", TGEditorArea.height(231, 1200, 800) + " " + TGEditorArea.height(1, 1600, 800));
	}

	// before the area has a width in pixels (not laid out yet) nothing can be said: no height
	@Test
	public void anAreaWithoutAWidthHasNoHeight() {
		assertEquals("0 0", TGEditorArea.height(230, 0, 800) + " " + TGEditorArea.height(230, -1, 800));
	}
}
