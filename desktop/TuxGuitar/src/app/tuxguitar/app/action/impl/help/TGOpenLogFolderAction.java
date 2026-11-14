package app.tuxguitar.app.action.impl.help;

import java.net.MalformedURLException;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionException;
import app.tuxguitar.app.system.log.TGLogAdapter;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.util.TGContext;

// shows the folder of TuxGuitar's log files in the system's file manager
public class TGOpenLogFolderAction extends TGActionBase {

	public static final String NAME = "action.help.open-log-folder";

	public TGOpenLogFolderAction(TGContext context) {
		super(context, NAME);
	}

	protected void processAction(TGActionContext context) {
		try {
			TGLogAdapter.folder().mkdirs();
			TGApplication.getInstance(getContext()).getApplication().openUrl(TGLogAdapter.folder().toURI().toURL());
		} catch (MalformedURLException e) {
			throw new TGActionException(e);
		}
	}
}
