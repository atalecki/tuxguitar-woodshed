package app.tuxguitar.app.action.listener.cache.controller;

import java.util.List;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.impl.edit.TGOpenMeasureErrorsDialogAction;
import app.tuxguitar.app.util.TGMessageDialogUtil;
import app.tuxguitar.app.view.dialog.message.TGMessageDialog;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.editor.action.file.TGReadSongAction;
import app.tuxguitar.util.TGContext;

public class TGUpdateReadSongController extends TGUpdateItemsController {
	
	public TGUpdateReadSongController() {
		super();
	}
	
	@Override
	public void update(TGContext context, TGActionContext actionContext) {
		TGDocumentManager tgDocumentManager = TGDocumentManager.getInstance(context);
		if (tgDocumentManager.getSongManager().hasMajorError(tgDocumentManager.getSong())) {
			TGActionProcessor tgActionProcessor = new TGActionProcessor(context, TGOpenMeasureErrorsDialogAction.NAME);
			tgActionProcessor.process();
		}
		warn(context, actionContext.getAttribute(TGReadSongAction.WARNINGS));
	}

	// what the reader warned about the song read (a take left out), once the song is open: one warning dialog for all
	private static void warn(TGContext context, List<String> warnings) {
		if (!warnings.isEmpty()) {
			TGMessageDialogUtil.showMessage(context, TGWindow.getInstance(context).getWindow(), TuxGuitar.getProperty("warning"), String.join("\n", warnings),
				TGMessageDialog.STYLE_WARNING);
		}
	}

}
