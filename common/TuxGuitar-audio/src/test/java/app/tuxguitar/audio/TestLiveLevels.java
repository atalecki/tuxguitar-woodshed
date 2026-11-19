package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.tuxguitar.midi.synth.TGLevels;

/**
 * The live levels: those of the TuxGuitar Synthesizer playing, measured while anyone shows meters.
 */
public class TestLiveLevels {

	// whether the TuxGuitar Synthesizer is the MIDI Port
	private boolean heard = true;
	private final TGLiveLevels live = new TGLiveLevels(() -> this.heard);
	private final TGLevels levels = new TGLevels();
	private final TGLevels other = new TGLevels();

	@Test
	void beforeTheTuxGuitarSynthesizerOpensThereAreNone() {
		assertNull(this.live.get());
	}

	@Test
	void theSynthesizerPlayingHasThem() {
		this.live.play(this.levels);
		assertSame(this.levels, this.live.get());
	}

	@Test
	void whileTheMidiPortIsAnotherThereAreNone() {
		this.live.play(this.levels);
		this.heard = false;
		assertNull(this.live.get());
	}

	@Test
	void theyAreMeasuredWhileAnyoneWatches() {
		this.live.play(this.levels);
		this.live.watch();
		this.live.watch();
		this.live.unwatch();
		assertTrue(this.levels.isOn());
		this.live.unwatch();
		assertFalse(this.levels.isOn());
		// unwatching more than watched leaves nobody watching
		this.live.unwatch();
		this.live.watch();
		assertTrue(this.levels.isOn());
	}

	// watching carries over to the synthesizer opened next (another MIDI Port and back, a new device)
	@Test
	void watchingCarriesOverToTheNextSynthesizer() {
		this.live.watch();
		this.live.play(this.levels);
		assertTrue(this.levels.isOn());
		this.live.play(this.other);
		assertEquals("false true", this.levels.isOn() + " " + this.other.isOn());
	}

}
