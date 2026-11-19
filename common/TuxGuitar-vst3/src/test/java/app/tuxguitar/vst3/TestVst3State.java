package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.song.models.TGChannelParameter;

/**
 * A plugin's saved state is its bytes, and the parts it keeps apart (its resources, a capture say, by name): two states
 * saved the same are one state (the song and a running plugin agree on it), whichever arrays hold them. A sound chain's
 * element keeps a state's resources as shared values, which a TuxGuitar file keeps once however many elements hold them.
 */
public class TestVst3State {

	private static TGVst3State state(int component, int controller) {
		return new TGVst3State(new byte[] {1, (byte) component}, new byte[] {(byte) controller});
	}

	private static String compared(TGVst3State one, Object other) {
		return one.equals(other) + (other != null && one.hashCode() == other.hashCode() ? " same hash" : " other hash");
	}

	@Test
	public void statesOfTheSameBytesAreEqualWithOneHashOthersAreNot() {
		assertEquals("true same hash | false other hash | false other hash | false other hash | false other hash",
			compared(state(2, 3), state(2, 3)) + " | " + compared(state(2, 3), state(4, 3)) + " | " + compared(state(2, 3), state(2, 4))
			+ " | " + compared(state(2, 3), state(3, 2)) + " | " + compared(state(2, 3), null));
	}

	// a state equal to an object of another kind would let anything pass for the song's state
	@Test
	public void aStateIsNotEqualToItsBytes() {
		TGVst3State state = state(2, 3);
		assertEquals(false, state.equals(state.getComponent()));
	}

	private static TGVst3State withResource(String name, int value) {
		return new TGVst3State(new byte[] {1}, new byte[0], Collections.singletonMap(name, new byte[] {(byte) value}));
	}

	@Test
	public void statesOfTheSameResourcesAreEqualWithOneHashOthersAreNot() {
		assertEquals("true same hash | false other hash | false other hash | false other hash",
			compared(withResource("a", 2), withResource("a", 2)) + " | " + compared(withResource("a", 2), withResource("a", 3))
			+ " | " + compared(withResource("a", 2), withResource("b", 2)) + " | " + compared(withResource("a", 2), state(1, 0)));
	}

	// the parameters the element keeps of the state, by key, its resources' values read as what follows SHARED
	private static Map<String, String> kept(TGProgramElement element) {
		Map<String, String> kept = new TreeMap<String, String>();
		element.getParameters().forEach((key, value) -> kept.put(key, value.startsWith(TGChannelParameter.SHARED) ? "shared " + value.substring(TGChannelParameter.SHARED.length()) : value));
		return kept;
	}

	@Test
	public void anElementKeepsAStatesResourcesAsSharedValuesAndGivesTheStateBackWhole() {
		TGProgramElement element = new TGProgramElement();
		TGVst3State state = new TGVst3State(new byte[] {1}, new byte[] {2}, Collections.singletonMap("profile", new byte[] {3, 4}));
		TGVst3Element.setState(element, state);
		assertEquals("{vst3.controller=Ag==, vst3.resource.profile=shared AwQ=, vst3.state=AQ==} true", kept(element) + " " + state.equals(TGVst3Element.state(element.getParameters())));
	}

	// a state of other resources replaces those the element kept
	@Test
	public void anElementKeepsOnlyTheResourcesOfItsLastState() {
		TGProgramElement element = new TGProgramElement();
		TGVst3Element.setState(element, withResource("profile", 3));
		TGVst3Element.setState(element, withResource("impulse", 4));
		assertEquals(Arrays.asList("vst3.controller", "vst3.resource.impulse", "vst3.state"), Arrays.asList(kept(element).keySet().toArray()));
	}
}
