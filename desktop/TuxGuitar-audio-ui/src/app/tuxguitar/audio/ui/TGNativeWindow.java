package app.tuxguitar.audio.ui;

import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.audio.TGEditorPlacement;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.util.TGContext;

/**
 * The native handle of an app window or panel, for a window of another process that it owns or holds (a VST3 plugin's
 * editor): the handle of an SWT control (on Windows its window handle), read without depending on SWT, as the audio UI
 * runs with the other toolkits too; 0 when the toolkit has none. A plugin's editor floats in a window of its own, owned
 * by TuxGuitar's main window (floating), or is inside the Sound Chain panel's editor area.
 */
final class TGNativeWindow {

	private TGNativeWindow() {
	}

	static long handle(UIControl window) {
		try {
			Object control = window.getClass().getMethod("getControl").invoke(window);
			return control.getClass().getField("handle").getLong(control);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return 0;
		}
	}

	static TGEditorPlacement floating(TGContext context) {
		return TGEditorPlacement.floating(() -> handle(TGWindow.getInstance(context).getWindow()));
	}
}
