package app.tuxguitar.audio.ui;

import java.util.List;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.audio.TGDefaultInstrumentSource;
import app.tuxguitar.audio.TGLiveAudio;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.TGSoundChainRows.Control;
import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UIListBoxSelect;
import app.tuxguitar.ui.widget.UIReadOnlyTextBox;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.log.TGLog;
import app.tuxguitar.vst3.TGVst3Hosting;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * A track's sound chain (its channel's): the source and the effects in processing order (TGSoundChainRows), and the
 * changes of it, each one undo step (the sound chain actions): add a VST3 effect, make a VST3 instrument or the default
 * instrument of the track's kind the source (its row saying what plays and why), keep the default instrument that plays
 * in the song as the track's own ("Keep in song", only while one plays), move the picked effect up, remove it, enable or disable it, open the picked plugin's editor
 * (its own, else TuxGuitar's parameter editor, TGPluginParametersDialog). The plugins offered are those the last scan of
 * the VST3 folders found, and the status under them says what that scan found and skipped, and why (TGPluginScan); Scan
 * again scans them off the UI thread, then offers what it found. What can be done with the row picked is decided there;
 * the rows are read again after each change. An editor that cannot open is refused by its action, whose message the
 * app shows as it shows every action's error.
 */
public class TGSoundChainDialog {

	private static final TGLog LOG = TGLog.get(TGSoundChainDialog.class);

	private final TGContext context;
	private final TGTrack track;
	private final UIFactory factory;
	private UIWindow dialog;
	private UIListBoxSelect<Integer> rows;
	private UIDropDownSelect<TGVst3Plugin> effects;
	private UIDropDownSelect<TGVst3Plugin> instruments;
	private final TGSoundChainButtons buttons = new TGSoundChainButtons();
	// each change runs on the action's thread; the rows are read again on the UI thread once it is done (the dialog still
	// open)
	private final TGSoundChainCommands commands;
	private UIButton scan;
	// what the last scan found, skipped and why (TGPluginScan)
	private UIReadOnlyTextBox status;

	public TGSoundChainDialog(TGContext context, TGTrack track) {
		this.context = context;
		this.track = track;
		this.factory = TGApplication.getInstance(context).getFactory();
		this.commands = new TGSoundChainCommands(context, () -> track, () -> this.later(this::refresh));
	}

	public void open() {
		this.dialog = this.factory.createWindow(TGWindow.getInstance(this.context).getWindow(), true, false);
		this.dialog.setLayout(new UITableLayout());
		this.dialog.setText(TuxGuitar.getProperty("audio.sound-chain.title") + ": " + this.track.getName());
		this.rows = this.factory.createListBoxSelect(this.dialog);
		this.rows.addSelectionListener(event -> this.updateButtons());
		this.layout().set(this.rows, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 4, 360f, 160f, null);
		this.effects = this.plugins(2, "audio.sound-chain.effect");
		this.buttons.put(Control.ADD_EFFECT, this.button(2, 4, "audio.sound-chain.add-effect", () -> this.commands.addEffect(this.effects.getSelectedValue())));
		this.instruments = this.plugins(3, "audio.sound-chain.instrument");
		this.buttons.put(Control.USE_AS_SOURCE, this.button(3, 4, "audio.sound-chain.choose-instrument", () -> this.commands.useAsSource(this.instruments.getSelectedValue())));
		this.buttons.put(Control.KEEP, this.button(4, 3, "audio.sound-chain.keep", this.commands::keep));
		this.buttons.put(Control.DEFAULT_INSTRUMENT, this.button(4, 4, "audio.sound-chain.default-instrument", this.commands::useDefault));
		this.buttons.put(Control.MOVE_UP, this.button(5, 1, "audio.sound-chain.up", () -> this.commands.moveUp(this.picked())));
		this.buttons.put(Control.REMOVE, this.button(5, 2, "audio.sound-chain.remove", () -> this.commands.remove(this.picked())));
		this.buttons.put(Control.TOGGLE, this.button(5, 3, "audio.sound-chain.disable", () -> this.commands.toggle(this.picked())));
		this.buttons.put(Control.EDIT, this.button(5, 4, "audio.sound-chain.edit", this::openEditor));
		this.status = this.factory.createReadOnlyTextBox(this.dialog, true, false);
		this.layout().set(this.status, 6, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 4, 360f, 120f, null);
		this.scan = this.button(7, 1, "audio.sound-chain.scan", this::scanAgain);
		this.button(7, 4, "close", this.dialog::dispose);
		this.showPlugins();
		this.refresh();
		TGDialogUtil.openDialog(this.dialog, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}

	private UITableLayout layout() {
		return (UITableLayout) this.dialog.getLayout();
	}

	// a drop-down of plugins, with its label, on the row
	private UIDropDownSelect<TGVst3Plugin> plugins(int row, String key) {
		TGDialogParts.label(this.factory, this.dialog, row, key);
		UIDropDownSelect<TGVst3Plugin> select = this.factory.createDropDownSelect(this.dialog);
		select.addSelectionListener(event -> this.updateButtons());
		this.layout().set(select, row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, false, 1, 2, 200f, null, null);
		return select;
	}

	// the drop-downs offer the plugins the last scan found, each its first picked; the status says what it found
	private void showPlugins() {
		List<TGVst3Plugin> plugins = TGVst3Hosting.getInstance(this.context).getPlugins();
		TGSoundChainButtons.offer(this.effects, TGSoundChainRows.effects(plugins));
		TGSoundChainButtons.offer(this.instruments, TGSoundChainRows.instruments(plugins));
		this.status.setText(TGLiveAudio.getInstance(this.context).getPluginScan().toString());
	}

	// the VST3 folders scanned again off the UI thread (a plugin installed while TuxGuitar runs), then offered here; the
	// default instruments applied at once unless the song plays
	private void scanAgain() {
		this.scan.setEnabled(false);
		Thread scanning = new Thread(() -> {
			LOG.info(TGLiveAudio.getInstance(this.context).scanAgain());
			this.later(this::scanned);
		}, "TuxGuitar VST3 scan");
		scanning.setDaemon(true);
		scanning.start();
	}

	private void scanned() {
		this.showPlugins();
		this.scan.setEnabled(true);
		this.updateButtons();
	}

	private void later(Runnable done) {
		TGSoundChainButtons.later(this.context, this.dialog, done);
	}

	private UIButton button(int row, int column, String key, Runnable action) {
		UIButton button = this.factory.createButton(this.dialog);
		button.setText(TuxGuitar.getProperty(key));
		button.addSelectionListener(event -> action.run());
		this.layout().set(button, row, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, 100f, 25f, null);
		return button;
	}

	// the track's channel as the song has it now (an undo may have replaced it)
	private TGChannel channel() {
		TGDocumentManager document = TGDocumentManager.getInstance(this.context);
		return document.getSongManager().getChannel(document.getSong(), this.track.getChannelId());
	}

	// the rows read again, the row picked kept
	private void refresh() {
		Integer picked = this.rows.getSelectedValue();
		this.rows.removeItems();
		List<String> read = this.chain().getRows(TuxGuitar.getProperty("audio.sound-chain.source"));
		for (int row = 0; row < read.size(); row++) {
			this.rows.addItem(new UISelectItem<Integer>(read.get(row), row));
		}
		this.rows.setSelectedValue(picked);
		this.updateButtons();
	}

	// the chain as the song has it now, a default source saying what plays and why
	private TGSoundChainRows chain() {
		TGChannel channel = this.channel();
		return TGSoundChainRows.of(channel, TGDefaultInstrumentSource.install(this.context).play(channel));
	}

	private void updateButtons() {
		this.buttons.apply(this.chain(), this.picked(), this.effects.getSelectedValue(), this.instruments.getSelectedValue());
	}

	private int picked() {
		return TGSoundChainRows.row(this.rows.getSelectedValue());
	}

	// the picked plugin's own editor opens (a window of its host process titled with the plugin and this track, owned
	// by TuxGuitar's main window: it stays in front of it), or else TuxGuitar's parameter editor here
	private void openEditor() {
		TGSoundChainButtons.edit(this.context, this.commands, this.track, this.picked(), this::later, this.dialog);
	}
}
