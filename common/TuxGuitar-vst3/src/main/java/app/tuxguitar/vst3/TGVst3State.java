package app.tuxguitar.vst3;

import java.util.Arrays;

/**
 * What a plugin saves: its component's state and, when its controller is apart from it, the controller's.
 */
public final class TGVst3State {

	private final byte[] component;
	private final byte[] controller;

	public TGVst3State(byte[] component, byte[] controller) {
		this.component = component;
		this.controller = controller;
	}

	public byte[] getComponent() {
		return this.component;
	}

	public byte[] getController() {
		return this.controller;
	}

	// the same bytes saved
	public boolean equals(Object other) {
		return (other instanceof TGVst3State && Arrays.equals(this.component, ((TGVst3State) other).component) && Arrays.equals(this.controller, ((TGVst3State) other).controller));
	}

	public int hashCode() {
		return Arrays.deepHashCode(new Object[] {this.component, this.controller});
	}
}
