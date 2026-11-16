package app.tuxguitar.editor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Items of the app's actions (menu items, buttons) greyed out while a rule refuses their action, over the state their
 * own updates give them: refresh runs an update between giving back the state it took from the items it greyed out (each
 * enabled before) and greying out again, so any update of any of the items may run through it. An item gone (disposed)
 * is let alone. One thread (the UI's) runs it.
 */
public final class TGGreying<T> {

	private final Predicate<T> enabled;
	private final BiConsumer<T, Boolean> enabler;
	private final Predicate<T> gone;
	private final Predicate<String> refused;
	private final List<Greyable> greyables = new ArrayList<Greyable>();
	// the items it greyed out last
	private final List<T> greyed = new ArrayList<T>();

	// whether an item is enabled, how one is enabled or not, whether one is gone, and whether the rule refuses an action
	public TGGreying(Predicate<T> enabled, BiConsumer<T, Boolean> enabler, Predicate<T> gone, Predicate<String> refused) {
		this.enabled = enabled;
		this.enabler = enabler;
		this.gone = gone;
		this.refused = refused;
	}

	// the item of the action
	public void add(T item, String action) {
		this.greyables.add(new Greyable(item, action));
	}

	// the update, then the items of refused actions greyed out
	public void refresh(Runnable update) {
		this.greyables.removeIf(greyable -> this.gone.test(greyable.item));
		this.greyed.removeIf(this.gone);
		this.greyed.forEach(item -> this.enabler.accept(item, true));
		this.greyed.clear();
		update.run();
		this.greyables.stream().filter(this::greysOut).forEach(this::greyOut);
	}

	private boolean greysOut(Greyable greyable) {
		return (this.enabled.test(greyable.item) && this.refused.test(greyable.action));
	}

	private void greyOut(Greyable greyable) {
		this.enabler.accept(greyable.item, false);
		this.greyed.add(greyable.item);
	}

	private final class Greyable {
		private final T item;
		private final String action;

		Greyable(T item, String action) {
			this.item = item;
			this.action = action;
		}
	}
}
