package app.tuxguitar.player.base;

import app.tuxguitar.song.models.TGTimeSignature;
import app.tuxguitar.song.models.TGVelocities;
import app.tuxguitar.util.configuration.TGConfigManager;

/**
 * The click sound: the General MIDI drum note and the velocity that Count In and the Metronome play. A TuxGuitar
 * setting (the player's), not part of the song. Immutable: a note from 0 to 127 at a velocity from 1 to 127.
 *
 * Beat 1 of each measure is accented (isAccented): a synthesizer's own drums (MidiSampleClock.DRUMS, the TuxGuitar
 * Synthesizer's built-in click, which plays whatever the note) are sent ACCENTED for it and PLAIN for the other beats;
 * any other channel the drum note on every beat, as a General MIDI port has always played it.
 */
public final class MidiClick {

	// Side Stick at the default velocity, the click TuxGuitar has always played
	public static final MidiClick DEFAULT = new MidiClick(37, TGVelocities.DEFAULT);

	// the keys it is kept under in a configuration
	public static final String NOTE = "midi.click.note";
	public static final String VELOCITY = "midi.click.velocity";

	// the notes of an accented and a plain click on a synthesizer's own drums (General MIDI's high and low wood block)
	public static final int ACCENTED = 76;
	public static final int PLAIN = 77;

	private static final int MAX = 127;

	private final int note;
	private final int velocity;

	public MidiClick(int note, int velocity) {
		if (!isMidi(note, velocity)) {
			throw new IllegalArgumentException("not a MIDI note and velocity: " + note + ", " + velocity);
		}
		this.note = note;
		this.velocity = velocity;
	}

	// the click kept in a configuration (the app's, shared by every song); the default when none is, or when what is
	// kept is not a MIDI note and velocity (a configuration edited by hand)
	public static MidiClick read(TGConfigManager config) {
		int note = config.getIntegerValue(NOTE, DEFAULT.note);
		int velocity = config.getIntegerValue(VELOCITY, DEFAULT.velocity);
		return (isMidi(note, velocity) ? new MidiClick(note, velocity) : DEFAULT);
	}

	public void write(TGConfigManager config) {
		config.setValue(NOTE, this.note);
		config.setValue(VELOCITY, this.velocity);
	}

	private static boolean isMidi(int note, int velocity) {
		return (note >= 0 && note <= MAX && velocity >= 1 && velocity <= MAX);
	}

	public int getNote() {
		return this.note;
	}

	public int getVelocity() {
		return this.velocity;
	}

	// the note of a click on the channel: on a synthesizer's own drums, ACCENTED or PLAIN; elsewhere the drum note
	public int getNote(int channel, boolean accented) {
		return (channel != MidiSampleClock.DRUMS ? this.note : (accented ? ACCENTED : PLAIN));
	}

	/**
	 * Whether a click is accented: of clicks counted in measures of that many beats (the Metronome's beats of a
	 * measure; Count In's, see isCountInAccented), the first of each measure is beat 1. The click counts from 0, the
	 * clicks before it from -1 back.
	 */
	public static boolean isAccented(int click, int beats) {
		return (click % beats == 0);
	}

	/**
	 * Whether a Count In click is accented, live and offline, before the player's start and a practice turn's: of so
	 * many clicks (counting from 0) a beat apart before a start that many ticks into a measure of the time signature,
	 * those on beat 1, counted backwards from the start, so the accents fall on the bar lines before it. 6 clicks before
	 * beat 1 of 4/4 are "3 4 | 1 2 3 4", 2 before 3/4 "2 3" (no accent), 4 before beat 3 of 4/4 "3 4 | 1 2". Before a
	 * start between two beats no click falls on a beat of the song: its first click is accented, then the first of
	 * each measure's worth after it.
	 */
	public static boolean isCountInAccented(int click, int clicks, TGTimeSignature signature, long from) {
		long beat = signature.getDenominator().getTime();
		int first = (from % beat == 0 ? (int) (from / beat) - clicks : 0);
		return isAccented(first + click, signature.getNumerator());
	}
}
