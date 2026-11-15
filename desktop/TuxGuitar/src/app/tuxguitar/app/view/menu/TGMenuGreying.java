package app.tuxguitar.app.view.menu;

import app.tuxguitar.app.action.TGActionAdapterManager;
import app.tuxguitar.editor.util.TGGreying;
import app.tuxguitar.ui.menu.UIMenuItem;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The menu items of the app's actions (TGMenuItem.act and greyable), greyed out while a practice session refuses their
 * action, paused too (TGDisableOnPlayInterceptor.greys), over the state the menus' updates give them (see TGGreying):
 * the menus update through refresh. One per context.
 */
public final class TGMenuGreying {

	private final TGGreying<UIMenuItem> greying;

	private TGMenuGreying(TGContext context) {
		this.greying = new TGGreying<UIMenuItem>(UIMenuItem::isEnabled, UIMenuItem::setEnabled, UIMenuItem::isDisposed,
			action -> TGActionAdapterManager.getInstance(context).getDisableOnPlayInterceptor().greys(action));
	}

	public static TGMenuGreying getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGMenuGreying.class.getName(), TGMenuGreying::new);
	}

	void add(UIMenuItem item, String action) {
		this.greying.add(item, action);
	}

	// the menus' update, then the items of refused actions greyed out
	public void refresh(Runnable update) {
		this.greying.refresh(update);
	}
}
