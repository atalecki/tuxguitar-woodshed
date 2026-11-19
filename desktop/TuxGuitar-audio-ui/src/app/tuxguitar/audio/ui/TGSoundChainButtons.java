package app.tuxguitar.audio.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.audio.TGPluginEditor;
import app.tuxguitar.audio.TGSoundChainCommands;
import app.tuxguitar.audio.TGSoundChainRows;
import app.tuxguitar.audio.TGSoundChainRows.Control;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGSynchronizer;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The buttons of a sound chain editor (the Sound Chain dialog, the Sound Chain panel), one per control, each enabled or
 * greyed as the rows decide it (TGSoundChainRows.isEnabled) for the row picked; the enable/disable toggle says what it
 * would do. With what both editors do alike: offer the plugins the scan found in a drop-down, open the picked plugin's
 * editor, run what follows a change on the UI thread while the editor is still there. Glue only.
 */
final class TGSoundChainButtons {

	private final Map<Control, UIButton> buttons = new EnumMap<Control, UIButton>(Control.class);

	// the control's button
	UIButton put(Control control, UIButton button) {
		this.buttons.put(control, button);
		return button;
	}

	void apply(TGSoundChainRows rows, int row, TGVst3Plugin effect, TGVst3Plugin instrument) {
		this.buttons.forEach((control, button) -> button.setEnabled(rows.isEnabled(control, row, effect, instrument)));
		this.buttons.get(Control.TOGGLE).setText(TuxGuitar.getProperty(rows.toggleKey(row)));
	}

	// the plugins in the drop-down, the first picked
	static void offer(UIDropDownSelect<TGVst3Plugin> select, List<TGVst3Plugin> plugins) {
		select.removeItems();
		plugins.forEach(plugin -> select.addItem(new UISelectItem<TGVst3Plugin>(plugin.getName(), plugin)));
		plugins.stream().findFirst().ifPresent(select::setSelectedValue);
	}

	// the row's plugin's own editor opens floating over TuxGuitar's main window, or else TuxGuitar's parameter editor,
	// over the window given
	static void edit(TGContext context, TGSoundChainCommands commands, TGTrack track, int row, Consumer<Runnable> later, UIWindow parent) {
		commands.edit(row, TGNativeWindow.floating(context), editor -> later.accept(() -> parameters(context, track, commands, row, editor, parent)));
	}

	private static void parameters(TGContext context, TGTrack track, TGSoundChainCommands commands, int row, TGPluginEditor editor, UIWindow parent) {
		if (!editor.isOwn()) {
			new TGPluginParametersDialog(context, track, commands, row, editor).open(parent);
		}
	}

	// done on the UI thread once it gets there, unless the editor was closed meanwhile (a scan, or a plugin loading or
	// opening its editor, can take longer than the editor stays open)
	static void later(TGContext context, UIControl editor, Runnable done) {
		TGSynchronizer.getInstance(context).executeLater(() -> {
			if (!editor.isDisposed()) {
				done.run();
			}
		});
	}
}
