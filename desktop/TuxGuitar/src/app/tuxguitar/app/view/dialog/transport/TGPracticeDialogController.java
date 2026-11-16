package app.tuxguitar.app.view.dialog.transport;

import app.tuxguitar.app.view.controller.TGOpenViewController;
import app.tuxguitar.app.view.controller.TGViewContext;

public class TGPracticeDialogController implements TGOpenViewController {

	public void openView(TGViewContext context) {
		new TGPracticeDialog(context).show();
	}
}
