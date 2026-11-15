package app.tuxguitar.editor.practice;

import java.util.function.ToIntFunction;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.action.TGActionInterceptor;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.action.edit.TGRedoAction;
import app.tuxguitar.editor.action.edit.TGUndoAction;
import app.tuxguitar.editor.undo.TGUndoableEdit;
import app.tuxguitar.editor.undo.TGUndoableManager;
import app.tuxguitar.editor.undo.impl.custom.TGUndoableDifficulty;
import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * What an edit does to the interleaved practice session under way (TGPracticeUnderWay). Flagging a measure, and undoing
 * or redoing a flag, goes on with it when its next cycles can play the weights that makes (the chooser checks it), and
 * counts from the next cycle; else the action is refused saying why (a TGActionException), and changes nothing. Any other
 * edit of the song (an undo step made, undone or redone) ends the session. One per context, installed by the app.
 */
public final class TGPracticeEdits implements TGActionInterceptor {

	private final TGContext context;

	private TGPracticeEdits(TGContext context) {
		this.context = context;
	}

	public static void install(TGContext context) {
		TGSingletonUtil.getInstance(context, TGPracticeEdits.class.getName(), TGPracticeEdits::installed);
	}

	private static TGPracticeEdits installed(TGContext context) {
		TGPracticeEdits edits = new TGPracticeEdits(context);
		TGActionManager.getInstance(context).addInterceptor(edits);
		TGUndoableManager.getInstance(context).addListener(edits::edited);
		return edits;
	}

	// refuses a flag the session under way could not play; intercepts nothing
	public boolean intercept(String id, TGActionContext actionContext) throws TGActionException {
		TGPracticeChooser chooser = TGPracticeUnderWay.getInstance(this.context).getChooser();
		Flag flag = (chooser != null ? this.flagOf(id, actionContext) : null);
		if (flag != null) {
			flag.check(chooser);
		}
		return false;
	}

	// an edit other than a flag's ends the session under way
	private void edited(TGUndoableEdit edit) {
		TGPracticeChooser chooser = TGPracticeUnderWay.getInstance(this.context).getChooser();
		if (chooser != null && edit != null && !(edit instanceof TGUndoableDifficulty)) {
			chooser.getSession().end();
		}
	}

	// the flag the action sets; null for one that sets none
	private Flag flagOf(String id, TGActionContext actionContext) {
		TGUndoableManager undo = TGUndoableManager.getInstance(this.context);
		if (TGUndoAction.NAME.equals(id)) {
			return this.flagOf(undo.getEditToBeUndone(), TGUndoableDifficulty::getUndoableDifficulty);
		}
		if (TGRedoAction.NAME.equals(id)) {
			return this.flagOf(undo.getEditToBeRedone(), TGUndoableDifficulty::getRedoableDifficulty);
		}
		if (TGChangeDifficultyAction.NAME.equals(id)) {
			TGMeasureHeader header = actionContext.getAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER);
			return new Flag(header, ((Integer) actionContext.getAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY)).intValue());
		}
		return null;
	}

	// the flag undoing or redoing the edit sets again; null when it is no flag's
	private Flag flagOf(TGUndoableEdit edit, ToIntFunction<TGUndoableDifficulty> difficulty) {
		if (!(edit instanceof TGUndoableDifficulty)) {
			return null;
		}
		TGUndoableDifficulty flag = (TGUndoableDifficulty) edit;
		TGDocumentManager document = TGDocumentManager.getInstance(this.context);
		return new Flag(document.getSongManager().getMeasureHeaderAt(document.getSong(), flag.getPosition()), difficulty.applyAsInt(flag));
	}

	private static final class Flag {
		private final TGMeasureHeader header;
		private final int difficulty;

		Flag(TGMeasureHeader header, int difficulty) {
			this.header = header;
			this.difficulty = difficulty;
		}

		// a flag outside 1x to 5x is the flag action's to refuse
		void check(TGPracticeChooser chooser) throws TGActionException {
			try {
				if (TGMeasureHeader.isDifficulty(this.difficulty)) {
					chooser.checkFlag(this.header.getNumber(), this.difficulty);
				}
			} catch (TGPracticeRefusal refusal) {
				throw new TGActionException(refusal.getMessage());
			}
		}
	}
}
