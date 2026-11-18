package app.tuxguitar.audio;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.vst3.TGEditorTarget;

/**
 * Where a plugin's own editor opens when a sound chain editor's Edit asks for it (TGSoundChainCommands.edit): set on the
 * action that opens it (TGOpenPluginEditorAction.ATTRIBUTE_TARGET), the native handles read when Edit is pressed.
 * Floating: a window of its own owned by the app's window, so it stays in front of it (the Sound Chain dialog).
 * Embedded: inside an area of the app's window, at most the height given in device pixels, the app's window kept as
 * the owner of a window of its own later (the Sound Chain panel).
 */
public interface TGEditorPlacement {

	void place(TGActionProcessor processor);

	static TGEditorPlacement floating(LongSupplier owner) {
		return processor -> processor.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_TARGET, TGEditorTarget.floating(owner.getAsLong()));
	}

	static TGEditorPlacement embedded(LongSupplier area, LongSupplier owner, IntSupplier maxHeight) {
		return processor -> processor.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_TARGET, TGEditorTarget.embedded(area.getAsLong(), owner.getAsLong(), maxHeight.getAsInt()));
	}
}
