package app.tuxguitar.midi.synth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.StringJoiner;

import org.junit.jupiter.api.Test;

/**
 * A channel's kind, from its General MIDI instrument: the percussion bank is drums, the Guitar programs (Nylon to Guitar
 * Harmonics) guitar, the Bass programs (Acoustic Bass to Synth Bass 2) bass, anything else other.
 */
public class TestInstrumentKind {

	// "bank:program=KIND" for each
	private static String kinds(int[][] channels) {
		StringJoiner kinds = new StringJoiner(" ");
		for (int[] channel : channels) {
			kinds.add(channel[0] + ":" + channel[1] + "=" + TGInstrumentKind.of(channel[0], channel[1]));
		}
		return kinds.toString();
	}

	@Test
	public void thePercussionBankIsDrumsWhateverItsProgram() {
		assertEquals("128:0=DRUMS 128:25=DRUMS 128:33=DRUMS", kinds(new int[][] {{128, 0}, {128, 25}, {128, 33}}));
	}

	@Test
	public void theGuitarProgramsAreGuitarOnAnyOtherBank() {
		assertEquals("0:24=GUITAR 0:25=GUITAR 0:31=GUITAR 1:25=GUITAR", kinds(new int[][] {{0, 24}, {0, 25}, {0, 31}, {1, 25}}));
	}

	@Test
	public void theBassProgramsAreBass() {
		assertEquals("0:32=BASS 0:39=BASS", kinds(new int[][] {{0, 32}, {0, 39}}));
	}

	// on either side of the guitars and basses, the ends of the programs, and no program yet
	@Test
	public void everyOtherProgramIsOther() {
		assertEquals("0:23=OTHER 0:40=OTHER 0:0=OTHER 0:127=OTHER 0:-1=OTHER", kinds(new int[][] {{0, 23}, {0, 40}, {0, 0}, {0, 127}, {0, -1}}));
	}
}
