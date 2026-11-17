package app.tuxguitar.player.base;

/**
 * Which channel the clicks (Count In, the Metronome) go to, for a synthesizer with or without drums of its own (the
 * TuxGuitar Synthesizer's built-in click: MidiSampleClock.DRUMS) and a song with or without a percussion channel. A
 * synthesizer's drums, when it has them, take every click; otherwise a song with a percussion channel clicks on it, and
 * one without on CLICKS, a percussion channel of the player's own outside the song (General MIDI channel 10 on a port).
 */
public final class MidiClickChannels {

	// no channel
	public static final int NONE = -1;

	// the player's own percussion channel for the clicks; no song channel, nor MidiSampleClock.DRUMS, has this id
	public static final int CLICKS = -3;

	private final boolean drums;
	private final int percussion;

	// drums: whether the synthesizer has drums of its own; percussion: the id of the song's percussion channel, NONE
	// without one
	MidiClickChannels(boolean drums, int percussion) {
		this.drums = drums;
		this.percussion = percussion;
	}

	// the player's own channel that the clicks need, which it keeps open: the synthesizer's drums when it has them, else
	// CLICKS without a percussion channel; NONE when the song's percussion channel takes them all
	int own() {
		return (this.drums ? MidiSampleClock.DRUMS : (this.percussion == NONE ? CLICKS : NONE));
	}

	// the one Count In and the Metronome click on: the synthesizer's drums when it has them, else the song's percussion
	// channel, else the player's own
	int clicks() {
		return (this.drums || this.percussion == NONE ? this.own() : this.percussion);
	}
}
