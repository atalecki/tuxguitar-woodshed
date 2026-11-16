package app.tuxguitar.editor.action.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import app.tuxguitar.practice.TGPracticeSettings;

/**
 * TGTransportStepAction, Repeat step and Previous step, as the app's commands run them: each its own action, enabled
 * only while an interleaved click-up is under way (the app greys them out otherwise), doing nothing to an interleaved
 * practice session, whose turns are no steps. What they play is the player's (MidiPlayer.repeatStep, previousStep; the
 * audio scenarios play them live). The song has 4 measures; TuxGuitar's sequencer plays.
 */
public class TestTransportStepAction extends PracticeActionTest {

	public TestTransportStepAction() {
		player.loadSequencer(new MidiSequencerImpl(fixture.context));
	}

	@Test
	public void eachCommandIsAnActionOfItsOwn() {
		assertEquals("action.transport.repeat-step action.transport.previous-step",
			TGTransportStepAction.repeat(fixture.context).getName() + " " + TGTransportStepAction.previous(fixture.context).getName());
	}

	// "the steps enabled | the turns laid", before and during the session
	@Test
	public void anInterleavedPracticeSessionHasNoStepsAndKeepsItsTurns() {
		boolean before = TGTransportStepAction.isEnabled(fixture.context);
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setPool(1, 4);
		settings.setTurns(3);
		fixture.actionContext.setAttribute(TGTransportPractiseAction.ATTRIBUTE_SETTINGS, settings);
		fixture.execute(new TGTransportPractiseAction(fixture.context));
		String laid = laid();
		fixture.execute(TGTransportStepAction.repeat(fixture.context));
		fixture.execute(TGTransportStepAction.previous(fixture.context));
		assertEquals("false false " + laid + " | true", before + " " + TGTransportStepAction.isEnabled(fixture.context) + " " + laid() + " | " + player.isPractising());
	}

	private String laid() {
		return player.getPracticeTurns().stream().map(turn -> turn.getTurn() + "@" + turn.getStart()).collect(Collectors.joining(" "));
	}
}
