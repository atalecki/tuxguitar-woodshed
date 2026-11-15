package app.tuxguitar.editor.action.transport;

import org.junit.jupiter.api.AfterEach;

import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.player.base.MidiPlayer;

/**
 * A test of an action that practises live: the document's song has 4 measures, and its player is stopped after each
 * test (Stop ends practice under way).
 */
abstract class PracticeActionTest {

	final SongActionFixture fixture = SongActionFixture.onDocument();
	final MidiPlayer player = MidiPlayer.getInstance(fixture.context);

	PracticeActionTest() {
		fixture.measureCount(4);
	}

	@AfterEach
	public void stop() {
		player.stop();
	}
}
