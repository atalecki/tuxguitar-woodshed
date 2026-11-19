package app.tuxguitar.audio.ui;

import app.tuxguitar.action.TGActionContext;
import app.tuxguitar.action.TGActionManager;
import app.tuxguitar.app.action.TGActionAdapterManager;
import app.tuxguitar.app.action.listener.cache.controller.TGUpdateItemsController;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.util.TGContext;

/**
 * Shows the Track panel and its editor area, or hides them (View > Sound Chain): on the UI thread, bindable to a key (F8 by default),
 * the menus updated after it.
 */
final class TGToggleSoundChainPanelAction extends TGActionBase {

	static final String NAME = "action.audio.toggle-sound-chain-panel";

	private final TGTrackPanel panel;

	private TGToggleSoundChainPanelAction(TGContext context, TGTrackPanel panel) {
		super(context, NAME);
		this.panel = panel;
	}

	protected void processAction(TGActionContext context) {
		this.panel.toggle();
	}

	static void map(TGContext context, TGTrackPanel panel) {
		TGActionAdapterManager adapter = TGActionAdapterManager.getInstance(context);
		TGActionManager.getInstance(context).mapAction(NAME, new TGToggleSoundChainPanelAction(context, panel));
		adapter.getKeyBindingActionIds().addActionId(NAME);
		adapter.getSyncThreadInterceptor().addActionId(NAME);
		adapter.getUpdatableActionListener().getControllers().set(NAME, new TGUpdateItemsController());
	}

	static void unmap(TGContext context) {
		TGActionAdapterManager adapter = TGActionAdapterManager.getInstance(context);
		TGActionManager.getInstance(context).unmapAction(NAME);
		adapter.getKeyBindingActionIds().removeActionId(NAME);
		adapter.getSyncThreadInterceptor().removeActionId(NAME);
	}
}
