package app.tuxguitar.app.action.impl.transport;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.app.system.config.TGConfigManager;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiClickSound;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.util.TGContext;

/**
 * Transport > Metronome Sound: the other beats' sound (ATTRIBUTE_SOUND), beat 1 as it is (its own sound, or the other
 * beats' accented), at once: the player is given the click (the TuxGuitar Synthesizer's drums play it from the next
 * click, also while playing) and it is kept in the configuration.
 */
public class TGSetMetronomeSoundAction extends TGActionBase {

	public static final String NAME = "action.transport.metronome-sound";

	public static final String ATTRIBUTE_SOUND = MidiClickSound.class.getName();

	public TGSetMetronomeSoundAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context) {
		MidiPlayer player = MidiPlayer.getInstance(getContext());
		MidiClick click = player.getClick().withOther(context.getAttribute(ATTRIBUTE_SOUND));
		player.setClick(click);
		TGConfigManager config = TGConfigManager.getInstance(getContext());
		click.write(config);
		config.save();
	}
}
