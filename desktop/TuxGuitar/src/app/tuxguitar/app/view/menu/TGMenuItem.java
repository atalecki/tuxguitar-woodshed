package app.tuxguitar.app.view.menu;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.TGActionProcessorListener;
import app.tuxguitar.app.system.keybindings.KeyBindingActionManager;
import app.tuxguitar.ui.menu.UIMenuActionItem;
import app.tuxguitar.ui.menu.UIMenuItem;
import app.tuxguitar.ui.resource.UIImage;
import app.tuxguitar.util.TGContext;

public abstract class TGMenuItem {

	// the items named so far (see named), in order
	private final List<Named> named = new ArrayList<Named>();

	// each item's state (the app updates menus through TGMenuGreying.refresh)
	public abstract void update();

	// the item runs the listener's action when selected, greyed out while a practice session refuses it
	public <T extends UIMenuActionItem> T act(T item, TGActionProcessorListener listener) {
		item.addSelectionListener(listener);
		return this.greyable(item, listener.getActionName());
	}

	// the item, greyed out while a practice session refuses the action (see TGMenuGreying)
	public <T extends UIMenuItem> T greyable(T item, String action) {
		TGMenuGreying.getInstance(this.findContext()).add(item, action);
		return item;
	}

	public abstract void loadProperties();

	public abstract void showItems();

	public void setMenuItemTextAndAccelerator(UIMenuItem menuItem, String key,String action) {
		menuItem.setKeyCombination(action != null ? KeyBindingActionManager.getInstance(this.findContext()).getKeyBindingForAction(action) : null);
		menuItem.setText(TuxGuitar.getProperty(key));
	}

	public TGActionProcessorListener createActionProcessor(String actionId) {
		return new TGActionProcessorListener(findContext(), actionId);
	}

	// the item, its text the property of the key, its key binding the action's (none: null), its image the icon's (none:
	// null), set by loadNames and loadImages, each time they are called
	public <T extends UIMenuItem> T named(T item, String key, String action, String icon) {
		this.named.add(new Named(item, key, action, icon));
		return item;
	}

	public void loadNames() {
		for (Named item : this.named) {
			this.setMenuItemTextAndAccelerator(item.item, item.key, item.action);
		}
	}

	public void loadImages() {
		for (Named item : this.named) {
			item.loadImage(this);
		}
	}

	private static final class Named {
		private final UIMenuItem item;
		private final String key;
		private final String action;
		private final String icon;

		Named(UIMenuItem item, String key, String action, String icon) {
			this.item = item;
			this.key = key;
			this.action = action;
			this.icon = icon;
		}

		void loadImage(TGMenuItem menu) {
			if (this.icon != null) {
				this.item.setImage(menu.icon(this.icon));
			}
		}
	}

	// the skin's image of the name (see TGIconManager)
	public UIImage icon(String name) {
		return TuxGuitar.getInstance().getIconManager().getImageByName(name);
	}

	public TGContext findContext() {
		return TuxGuitar.getInstance().getContext();
	}
}
