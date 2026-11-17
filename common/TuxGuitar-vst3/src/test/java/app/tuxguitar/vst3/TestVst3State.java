package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A plugin's saved state is its bytes: two states saved the same are one state (the song and a running plugin agree on
 * it), whichever arrays hold them.
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
}
