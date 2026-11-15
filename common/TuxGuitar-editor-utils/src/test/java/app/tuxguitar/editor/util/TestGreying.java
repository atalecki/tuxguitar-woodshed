package app.tuxguitar.editor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Items greyed out while a rule refuses their action, over the state their own updates give them (TGGreying): item A
 * of action "a", item B of action "b"; the rule refuses the actions in a set. An item reads "on" (enabled) or "off".
 */
public class TestGreying {

	private static final class Item {
		private boolean enabled = true;
		private boolean disposed;

		void setEnabled(boolean enabled) {
			if (this.disposed) {
				throw new IllegalStateException("disposed");
			}
			this.enabled = enabled;
		}

		@Override
		public String toString() {
			return (this.enabled ? "on" : "off");
		}
	}

	private final Set<String> refused = new HashSet<String>();
	private final TGGreying<Item> greying = new TGGreying<Item>(item -> item.enabled, Item::setEnabled, item -> item.disposed, this.refused::contains);
	private final Item itemA = new Item();
	private final Item itemB = new Item();

	public TestGreying() {
		this.greying.add(this.itemA, "a");
		this.greying.add(this.itemB, "b");
	}

	private String items() {
		return this.itemA + " " + this.itemB;
	}

	private String refreshed(Runnable update) {
		this.greying.refresh(update);
		return this.items();
	}

	// the update runs first: the item of a refused action is greyed out after it, the other stays as it set it
	@Test
	public void theItemOfARefusedActionIsGreyedOutAfterTheUpdate() {
		this.refused.add("a");

		assertEquals("off on", this.refreshed(() -> this.itemA.setEnabled(true)));
	}

	// once its action is no longer refused, an item greyed out gets back the state it had (its update need not set it);
	// one its own update turned off stays off
	@Test
	public void anItemGreyedOutGetsItsOwnStateBackOnceNotRefused() {
		this.refused.add("a");
		this.refused.add("b");
		String greyed = this.refreshed(() -> this.itemB.setEnabled(false));
		this.refused.clear();

		assertEquals("off off | on off", greyed + " | " + this.refreshed(() -> { }));
	}

	// an item gone (disposed) is let alone, greyed out or not
	@Test
	public void anItemGoneIsLetAlone() {
		this.refused.add("a");
		this.greying.refresh(() -> { });
		this.itemA.disposed = true;
		this.refused.clear();

		assertEquals("off on", this.refreshed(() -> { }));
	}
}
