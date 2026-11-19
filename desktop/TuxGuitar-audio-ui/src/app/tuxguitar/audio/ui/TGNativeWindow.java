package app.tuxguitar.audio.ui;

import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.audio.TGEditorPlacement;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.TGContext;

/**
 * The native handle of an app window, for a window of another process that it owns (a VST3 plugin's editor): the
 * handle of an SWT shell (on Windows its window handle), read without depending on SWT, as the audio UI runs with the
 * other toolkits too; 0 when the window's toolkit has none. A plugin's editor floats in a window of its own, owned by
 * TuxGuitar's main window (floating).
 */
final class TGNativeWindow {

	private TGNativeWindow() {
	}

	static long handle(UIWindow window) {
		try {
			Object shell = window.getClass().getMethod("getControl").invoke(window);
			return shell.getClass().getField("handle").getLong(shell);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return 0;
		}
	}

	static TGEditorPlacement floating(TGContext context) {
		return TGEditorPlacement.floating(() -> handle(TGWindow.getInstance(context).getWindow()));
	}
}
