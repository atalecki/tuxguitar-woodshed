package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * How wide the Track panel docked at the right of the main window is: 300 the first time, then as last left, within 220
 * to 600; a drag of its divider to the right narrows it by as much, to the left widens it, within the same range.
 */
class TestTrackPanelWidth {

	@Test
	void theFirstTimeTheDefaultThenAsLastLeftWithinTheRange() {
		assertEquals("300.0 300.0 450.0 220.0 600.0", TGTrackPanelWidth.shown(0f) + " " + TGTrackPanelWidth.shown(-1f) + " "
			+ TGTrackPanelWidth.shown(450f) + " " + TGTrackPanelWidth.shown(100f) + " " + TGTrackPanelWidth.shown(900f));
	}

	@Test
	void aDragToTheRightNarrowsToTheLeftWidensWithinTheRange() {
		assertEquals("250.0 350.0 220.0 600.0 220.0 600.0", TGTrackPanelWidth.dragged(300f, 50f) + " " + TGTrackPanelWidth.dragged(300f, -50f) + " "
			+ TGTrackPanelWidth.dragged(300f, 200f) + " " + TGTrackPanelWidth.dragged(300f, -400f) + " " + TGTrackPanelWidth.dragged(220f, 0.5f) + " " + TGTrackPanelWidth.dragged(600f, -0.5f));
	}
}
