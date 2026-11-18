package app.tuxguitar.audio.ui;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGSynchronizer;

/**
 * TuxGuitar's parameter editor of a plugin without an editor of its own (TGPluginEditor's rows), for a row of a track's
 * sound chain (TGSoundChainRows: 0 the source, then each effect), in a window of its own: the rows in one column
 * (TGPluginParameterRowsView), each value set by one undoable change of the song (TGSoundChainCommands.set). Each time
 * the plugin may hold other values (an undo, a redo) the rows show what it holds; once it no longer runs, the window
 * closes. Glue only.
 */
public class TGPluginParametersDialog {

	private final TGContext context;
	private final TGTrack track;
	private final TGSoundChainCommands commands;
	private final int row;
	private final TGPluginEditor editor;
	private TGPluginParameterRowsView rows;
	private UIWindow dialog;

	public TGPluginParametersDialog(TGContext context, TGTrack track, TGSoundChainCommands commands, int row, TGPluginEditor editor) {
		this.context = context;
		this.track = track;
		this.commands = commands;
		this.row = row;
		this.editor = editor;
	}

	public void open(UIWindow parent) {
		UIFactory factory = TGApplication.getInstance(this.context).getFactory();
		this.dialog = factory.createWindow(parent, true, false);
		this.dialog.setLayout(new UITableLayout());
		this.dialog.setText(TuxGuitar.getProperty("audio.sound-chain.parameters") + ": " + this.track.getName());
		this.rows = new TGPluginParameterRowsView(this.context, factory, this.dialog, this.editor, (parameter, plain) -> this.commands.set(this.row, parameter, plain));
		((UITableLayout) this.dialog.getLayout()).set(this.rows.getControl(), 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);
		TGDialogParts.button(factory, this.dialog, 2, 1, "close", this.dialog::dispose);
		this.editor.watch(() -> TGSynchronizer.getInstance(this.context).executeLater(this::follow));
		this.dialog.addDisposeListener(event -> this.editor.watch(() -> { }));
		TGDialogUtil.openDialog(this.dialog, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}

	// on the UI thread, once the plugin may hold other values (an undo, a redo) or no longer runs
	private void follow() {
		if (!this.dialog.isDisposed()) {
			this.followEditor();
		}
	}

	// the rows show the values the plugin holds now; a plugin that no longer runs closes the window
	private void followEditor() {
		if (!this.rows.follow()) {
			this.dialog.dispose();
		}
	}
}
