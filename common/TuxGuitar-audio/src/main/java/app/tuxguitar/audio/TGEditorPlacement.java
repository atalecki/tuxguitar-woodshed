package app.tuxguitar.audio;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

import app.tuxguitar.audio.action.TGOpenPluginEditorAction;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.vst3.TGEditorTarget;

/**
 * Where a plugin's own editor opens when a sound chain editor's Edit asks for it (TGSoundChainCommands.edit): set on the
 * action that opens it (TGOpenPluginEditorAction.ATTRIBUTE_TARGET), the native handles read when Edit is pressed.
 * Floating: a window of its own owned by the app's window, so it stays in front of it (the Sound Chain dialog).
 * Embedded: inside an area of the app's window, at most TGEditorArea.MAX_SHARE of that window high, the app's window
 * kept as the owner of a window of its own later (the Track panel), the keys given (TGEditorKeys: TuxGuitar's
 * shortcuts, read when Edit is pressed) passed to that area while the editor has the focus.
 */
public interface TGEditorPlacement {

	void place(TGActionProcessor processor);

	static TGEditorPlacement floating(LongSupplier owner) {
		return processor -> processor.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_TARGET, TGEditorTarget.floating(owner.getAsLong()));
	}

	static TGEditorPlacement embedded(LongSupplier area, LongSupplier owner, Supplier<int[]> keys) {
		return processor -> processor.setAttribute(TGOpenPluginEditorAction.ATTRIBUTE_TARGET, TGEditorTarget.embedded(area.getAsLong(), owner.getAsLong(), TGEditorArea.MAX_SHARE).passing(keys.get()));
	}
}
