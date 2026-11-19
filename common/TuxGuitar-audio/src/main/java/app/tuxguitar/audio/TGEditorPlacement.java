package app.tuxguitar.audio;

import java.util.function.LongSupplier;

import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.editor.action.TGActionProcessor;

/**
 * Where a plugin's own editor opens when a sound chain editor's Edit asks for it (TGSoundChainCommands.edit): set on the
 * action that opens it (TGOpenPluginEditorAction). Floating: a window of its own owned by the app's window (its native
 * handle, read when Edit is pressed), so it stays in front of it. The seam where an editor placed inside the app's
 * window comes in later.
 */
public interface TGEditorPlacement {

	void place(TGActionProcessor processor);

	static TGEditorPlacement floating(LongSupplier owner) {
		return processor -> processor.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_OWNER, owner.getAsLong());
	}
}
