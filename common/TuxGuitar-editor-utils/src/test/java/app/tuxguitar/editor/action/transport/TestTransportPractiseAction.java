package app.tuxguitar.editor.action.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import app.tuxguitar.action.TGActionException;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import app.tuxguitar.practice.TGPracticeSettings;

/**
 * TGTransportPractiseAction starts an interleaved practice session live, as the Interleaved Practice dialog runs it:
 * the session as set plays on TuxGuitar's sequencer; one that cannot be played, or a sequencer that cannot play it, is
 * refused with the reason (a TGActionException, the app's error message). The settings of the last start, refused or
 * not, are the ones the dialog shows next (at first measures 1 to 4, 12 turns). The song has 4 measures.
 */
public class TestTransportPractiseAction {

	private final SongActionFixture fixture = SongActionFixture.onDocument();
	private final MidiPlayer player = MidiPlayer.getInstance(fixture.context);

	public TestTransportPractiseAction() {
		fixture.measureCount(4);
	}

	@AfterEach
	public void stop() {
		player.stop();
	}

	private static TGPracticeSettings settings(int from, int to, int turns) {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(from, to);
		settings.setTurns(turns);
		return settings;
	}

	// starts the session: the refusal's message, or "started"; then whether a session is held and the settings shown next
	private String start(TGPracticeSettings settings) {
		fixture.actionContext.setAttribute(TGTransportPractiseAction.ATTRIBUTE_SETTINGS, settings);
		String outcome = "started";
		try {
			fixture.execute(new TGTransportPractiseAction(fixture.context));
		} catch (TGActionException refused) {
			outcome = refused.getMessage();
		}
		return outcome + " | " + player.isPractising() + " | " + shown();
	}

	private String shown() {
		TGPracticeSettings last = TGTransportPractiseAction.getLastSettings(fixture.context);
		return last.getUnit().getName() + " " + last.getPoolFrom() + "-" + last.getPoolTo() + " " + last.getTurns();
	}

	@Test
	public void atFirstTheDialogShowsMeasures1To4For12Turns() {
		assertEquals("measure 1-4 12", shown());
	}

	@Test
	public void aSessionAsSetStartsOnTuxGuitarsSequencer() {
		player.loadSequencer(new MidiSequencerImpl(fixture.context));
		assertEquals("started | true | measure 2-3 5 | success", start(settings(2, 3, 5)) + " | " + fixture.outcome());
	}

	@Test
	public void aSessionThatCannotBePlayedIsRefusedSayingWhy() {
		player.loadSequencer(new MidiSequencerImpl(fixture.context));
		assertEquals("The pool reaches past the song's 4 measures. | false | measure 3-9 6", start(settings(3, 9, 6)));
	}

	@Test
	public void aSequencerThatCannotPlayASessionRefusesItSayingWhy() {
		assertEquals("Interleaved practice needs a sequencer that takes turns added while it plays, and \"" + player.getSequencer().getName()
			+ "\" does not: choose the TuxGuitar Sequencer in Settings > Sound. | false | measure 1-4 6", start(settings(1, 4, 6)));
	}
}
