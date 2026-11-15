package app.tuxguitar.editor.action.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.tuxguitar.action.TGActionException;
import app.tuxguitar.player.base.MidiSequencer;
import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import app.tuxguitar.practice.TGClickUpSettings;

/**
 * TGTransportClickUpAction starts interleaved click-up live, as its dialog runs it: the click-up as set plays on
 * TuxGuitar's sequencer, its steps to go back over (Repeat step, Previous step: TGTransportStepAction); one that cannot
 * be played, or a sequencer that cannot play it, is refused with the reason (a TGActionException, the app's error
 * message). The settings of the last start, refused or not, are the ones the dialog shows next (at first measures 1 to
 * 4, from 60% to 100% by 5%, 1 pass a step). The song has 4 measures.
 */
public class TestTransportClickUpAction extends PracticeActionTest {

	// the player's sequencer at first, which cannot play practice
	private final MidiSequencer first = player.getSequencer();

	public TestTransportClickUpAction() {
		player.loadSequencer(new MidiSequencerImpl(fixture.context));
	}

	// measures from one to another, from 50% to 100% by 25%
	private static TGClickUpSettings settings(int from, int to) {
		TGClickUpSettings settings = new TGClickUpSettings();
		settings.setPassageFrom(from);
		settings.setPassageTo(to);
		settings.setStart(50);
		settings.setIncrement(25);
		return settings;
	}

	// starts the click-up: the refusal's message, or "started"; then whether there are steps to go back over, and the
	// settings shown next
	private String start(TGClickUpSettings settings) {
		fixture.actionContext.setAttribute(TGTransportClickUpAction.ATTRIBUTE_SETTINGS, settings);
		String outcome = "started";
		try {
			fixture.execute(new TGTransportClickUpAction(fixture.context));
		} catch (TGActionException refused) {
			outcome = refused.getMessage();
		}
		return outcome + " | " + TGTransportStepAction.isEnabled(fixture.context) + " | " + shown();
	}

	private String shown() {
		TGClickUpSettings last = TGTransportClickUpAction.getLastSettings(fixture.context);
		return last.getUnit().getName() + " " + last.getPassageFrom() + "-" + last.getPassageTo() + " " + last.getTempoMode().reading(last.getStart()) + "-"
			+ last.getGoal() + " by " + last.getIncrement() + " x" + last.getPasses();
	}

	@Test
	public void aClickUpAsSetStartsOnTuxGuitarsSequencer() {
		assertEquals("started | true | measure 2-3 50%-100 by 25 x1 | success", start(settings(2, 3)) + " | " + fixture.outcome());
	}

	// at first the dialog shows measures 1 to 4, from 60% to 100% by 5%, 1 pass a step; then the settings refused
	@Test
	public void aClickUpThatCannotBePlayedIsRefusedSayingWhyAndShownNext() {
		String first = shown();
		assertEquals("measure 1-4 60%-100 by 5 x1 | The passage reaches past the song's 4 measures. | false | measure 3-9 50%-100 by 25 x1",
			first + " | " + start(settings(3, 9)));
	}

	@Test
	public void aSequencerThatCannotPlayAClickUpRefusesItSayingWhy() {
		player.loadSequencer(first);
		assertEquals("Interleaved click-up needs a sequencer that takes turns added while it plays, and \"" + player.getSequencer().getName()
			+ "\" does not: choose the TuxGuitar Sequencer in Settings > Sound. | false | measure 1-2 50%-100 by 25 x1", start(settings(1, 2)));
	}
}
