package app.tuxguitar.app.view.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.singleton.TGSingletonUtil;

/**
 * The check items plugins add to the View menu, after Fretboard, one per context: each plugin adds its item when it
 * connects and removes it when it disconnects, then has the menus made again (TGItemManager.createMenu). An item is
 * named by its message key, runs its action (shown with its key binding), and is checked and enabled as its suppliers
 * say each time the menus update.
 */
public final class TGViewMenuExtensions {

	private final List<Item> items = new CopyOnWriteArrayList<Item>();

	private TGViewMenuExtensions() {
	}

	public static TGViewMenuExtensions getInstance(TGContext context) {
		return TGSingletonUtil.getInstance(context, TGViewMenuExtensions.class.getName(), key -> new TGViewMenuExtensions());
	}

	public void add(Item item) {
		this.items.add(item);
	}

	public void remove(Item item) {
		this.items.remove(item);
	}

	// in the order the plugins added them
	public List<Item> getItems() {
		return new ArrayList<Item>(this.items);
	}

	public static final class Item {

		private final String key;
		private final String action;
		private final BooleanSupplier checked;
		private final BooleanSupplier enabled;

		public Item(String key, String action, BooleanSupplier checked, BooleanSupplier enabled) {
			this.key = key;
			this.action = action;
			this.checked = checked;
			this.enabled = enabled;
		}

		public String getKey() {
			return this.key;
		}

		public String getAction() {
			return this.action;
		}

		public boolean isChecked() {
			return this.checked.getAsBoolean();
		}

		public boolean isEnabled() {
			return this.enabled.getAsBoolean();
		}
	}
}
