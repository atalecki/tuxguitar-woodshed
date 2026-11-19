package app.tuxguitar.vst3;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * What a plugin saves: its component's state, when its controller is apart from it the controller's, and the parts of
 * it the plugin keeps apart in TuxGuitar (its resources, by name: a capture, say), which the song stores once however
 * many plugins hold them.
 */
public final class TGVst3State {

	private final byte[] component;
	private final byte[] controller;
	private final Map<String, byte[]> resources;

	public TGVst3State(byte[] component, byte[] controller) {
		this(component, controller, Collections.<String, byte[]>emptyMap());
	}

	public TGVst3State(byte[] component, byte[] controller, Map<String, byte[]> resources) {
		this.component = component;
		this.controller = controller;
		this.resources = Collections.unmodifiableMap(new TreeMap<String, byte[]>(resources));
	}

	public byte[] getComponent() {
		return this.component;
	}

	public byte[] getController() {
		return this.controller;
	}

	// by name, in the order of their names
	public Map<String, byte[]> getResources() {
		return this.resources;
	}

	// the same bytes saved
	public boolean equals(Object other) {
		return (other instanceof TGVst3State && Arrays.deepEquals(this.saved(), ((TGVst3State) other).saved()));
	}

	public int hashCode() {
		return Arrays.deepHashCode(this.saved());
	}

	// all it saved, in order: the component's bytes, the controller's, then each resource's name and bytes
	private Object[] saved() {
		Object[] saved = new Object[2 + 2 * this.resources.size()];
		saved[0] = this.component;
		saved[1] = this.controller;
		int at = 2;
		for (Map.Entry<String, byte[]> resource : this.resources.entrySet()) {
			saved[at++] = resource.getKey();
			saved[at++] = resource.getValue();
		}
		return saved;
	}
}
