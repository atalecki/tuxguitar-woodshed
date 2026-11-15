package app.tuxguitar.app.action.impl.caret;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.config.TGConfigKeys;
import app.tuxguitar.app.system.config.TGConfigManager;
import app.tuxguitar.app.view.component.tab.Caret;
import app.tuxguitar.app.view.component.tab.Tablature;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.util.TGContext;

/**
 * What moving the caret by an action does besides: the selection goes unless the action keeps it, and the beat the
 * caret reaches is played when the settings say so.
 */
final class TGCaretMoved {

	private TGCaretMoved() {
	}

	static void clearSelection(TGActionContext context, Tablature tablature) {
		if (!Boolean.TRUE.equals(context.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_KEEP_SELECTION))) {
			tablature.getSelector().clearSelection();
		}
	}

	// the caret's beat played, with "play when moving" on
	static void playBeat(TGContext context, Caret caret) {
		if (TGConfigManager.getInstance(context).getBooleanValue(TGConfigKeys.PLAY_WHEN_MOVING)) {
			playSelected(caret);
		}
	}

	private static void playSelected(Caret caret) {
		TGBeat beat = (TGBeat) caret.getSelectedBeat();
		if (beat != null) {
			TuxGuitar.getInstance().playBeat(beat);
		}
	}
}
