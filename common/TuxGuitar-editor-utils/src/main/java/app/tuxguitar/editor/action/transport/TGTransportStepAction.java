package app.tuxguitar.editor.action.transport;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.util.TGContext;

/**
 * Repeat step and Previous step of the interleaved click-up under way (MidiPlayer.repeatStep, previousStep): Repeat step
 * plays the step under way again, Previous step the one before it (during the first, the first again), at once from its
 * Count In, then the steps after it; paused, Play plays it so. With no click-up under way it does nothing, and the app
 * greys it out (isEnabled). One action per command, each its own name.
 */
public class TGTransportStepAction extends TGActionBase {

	public static final String NAME_REPEAT = "action.transport.repeat-step";
	public static final String NAME_PREVIOUS = "action.transport.previous-step";

	/** What the command asks the player. */
	private interface Step {
		void choose(MidiPlayer player) throws MidiPlayerException;
	}

	private final Step step;

	private TGTransportStepAction(TGContext context, String name, Step step) {
		super(context, name);
		this.step = step;
	}

	// Repeat step
	public static TGTransportStepAction repeat(TGContext context) {
		return new TGTransportStepAction(context, NAME_REPEAT, MidiPlayer::repeatStep);
	}

	// Previous step
	public static TGTransportStepAction previous(TGContext context) {
		return new TGTransportStepAction(context, NAME_PREVIOUS, MidiPlayer::previousStep);
	}

	// whether there are steps to go back over: an interleaved click-up under way, playing or paused
	public static boolean isEnabled(TGContext context) {
		return MidiPlayer.getInstance(context).hasSteps();
	}

	protected void processAction(TGActionContext context) {
		try {
			this.step.choose(MidiPlayer.getInstance(getContext()));
		} catch (MidiPlayerException e) {
			throw new TGActionException(e);
		}
	}
}
