package app.tuxguitar.midi.synth;

import app.tuxguitar.song.models.TGChannel;

/**
 * What a channel plays, as its General MIDI instrument says it: the percussion bank is drums, the Guitar programs (Nylon
 * Guitar to Guitar Harmonics, 24 to 31) guitar, the Bass programs (Acoustic Bass to Synth Bass 2, 32 to 39) bass, any
 * other program other. The kind picks the default instrument of a channel without one of its own.
 */
public enum TGInstrumentKind {
	GUITAR, BASS, DRUMS, OTHER;

	private static final int GUITARS = 24;
	private static final int BASSES = 32;
	private static final int FAMILY = 8;

	public static TGInstrumentKind of(int bank, int program) {
		if (bank == TGChannel.DEFAULT_PERCUSSION_BANK) {
			return DRUMS;
		}
		if (program >= GUITARS && program < GUITARS + FAMILY) {
			return GUITAR;
		}
		return (program >= BASSES && program < BASSES + FAMILY ? BASS : OTHER);
	}
}
