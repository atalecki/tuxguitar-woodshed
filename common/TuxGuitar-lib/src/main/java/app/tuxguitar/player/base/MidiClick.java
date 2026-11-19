package app.tuxguitar.player.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import app.tuxguitar.song.models.TGTimeSignature;
import app.tuxguitar.song.models.TGVelocities;
import app.tuxguitar.util.configuration.TGConfigManager;

/**
 * The click Count In and the Metronome play: beat 1's sound, the other beats' sound and the click volume. A TuxGuitar
 * setting (the player's), not part of the song. Immutable: two sounds (beat 1's may be the other beats' sound played
 * accented) and a volume from MIN_VOLUME to MAX_VOLUME dB, 0 dB being the loudest click a General MIDI velocity gives.
 *
 * Beat 1 of each measure is accented (isAccented). A synthesizer's own drums (MidiSampleClock.DRUMS, the TuxGuitar
 * Synthesizer's built-in click) are sent ACCENTED for it and PLAIN for the other beats, at full velocity, and told the
 * sounds and the volume as parameters (toParameters); any other channel the General MIDI drum note of the beat's sound
 * at the velocity of the volume, as a General MIDI port has always played it.
 */
public final class MidiClick {

	public static final int MIN_VOLUME = -30;
	public static final int MAX_VOLUME = 12;

	// beat 1 played as the other beats' sound, accented: 3 dB louder, and a recording 2 semitones higher
	public static final int ACCENT_DECIBELS = 3;
	public static final int ACCENT_SEMITONES = 2;

	// the keys it is kept under in a configuration, the same in the parameters of a synthesizer's drums
	public static final String BEAT_ONE = "midi.click.beat-one";
	public static final String OTHER = "midi.click.other";
	public static final String VOLUME = "midi.click.volume";
	// the keys it was kept under before, a General MIDI note and velocity: read once, then removed
	public static final String NOTE = "midi.click.note";
	public static final String VELOCITY = "midi.click.velocity";
	// every key a configuration keeps it under
	public static final List<String> KEYS = List.of(BEAT_ONE, OTHER, VOLUME, NOTE, VELOCITY);
	// the parameter of the drums naming the folder of the user's own sounds (see MidiClickSound.own)
	public static final String FOLDER = "midi.click.folder";
	// beat 1's sound when it is the other beats' sound, accented
	public static final String SAME_ACCENTED = "same-accented";

	// the notes of an accented and a plain click on a synthesizer's own drums (General MIDI's high and low wood block)
	public static final int ACCENTED = 76;
	public static final int PLAIN = 77;

	private static final int MAX_VELOCITY = 127;
	private static final int OLD_NOTE = 37;

	// the code-made Click, beat 1 accented, at the volume of TuxGuitar's click before (Side Stick at the default velocity)
	public static final MidiClick DEFAULT = ofMidi(OLD_NOTE, TGVelocities.DEFAULT);

	// null: the other beats' sound, accented
	private final MidiClickSound beatOne;
	private final MidiClickSound other;
	private final int volume;

	public MidiClick(MidiClickSound beatOne, MidiClickSound other, int volume) {
		this(Optional.of(given(beatOne)), other, volume);
	}

	private MidiClick(Optional<MidiClickSound> beatOne, MidiClickSound other, int volume) {
		if (volume < MIN_VOLUME || volume > MAX_VOLUME) {
			throw new IllegalArgumentException("not a click volume: " + volume);
		}
		this.beatOne = beatOne.orElse(null);
		this.other = given(other);
		this.volume = volume;
	}

	private static MidiClickSound given(MidiClickSound sound) {
		if (sound == null) {
			throw new IllegalArgumentException("no click sound");
		}
		return sound;
	}

	// beat 1 the other beats' sound, accented
	public static MidiClick sameAccented(MidiClickSound other, int volume) {
		return new MidiClick(Optional.empty(), other, volume);
	}

	// the click of a General MIDI note at a velocity (how TuxGuitar kept its click before): the drum note's built-in sound
	// (else the Click), beat 1 accented, the velocity as a volume (round(20 * log10(velocity / 127)) dB, from MIN_VOLUME)
	public static MidiClick ofMidi(int note, int velocity) {
		MidiClickSound sound = Objects.requireNonNullElse(MidiClickSound.ofGmNote(note), MidiClickSound.CLICK);
		return sameAccented(sound, within((int) Math.round(20 * Math.log10(velocity / (double) MAX_VELOCITY))));
	}

	// the click kept in a configuration (the app's, shared by every song): kept before as a note and velocity, migrated
	public static MidiClick read(TGConfigManager config) {
		return read(config::getStringValue);
	}

	// the click the parameters of a synthesizer's drums give
	public static MidiClick fromParameters(Map<String, String> parameters) {
		return read(parameters::get);
	}

	// what is kept under each key: a sound that is none is the Click (beat 1's, the other beats' accented), a volume that
	// is no number the default one, outside the range the nearest end; without the other beats' sound, the note and
	// velocity kept before (the default click when they are no MIDI note and velocity)
	private static MidiClick read(UnaryOperator<String> kept) {
		if (kept.apply(OTHER) == null) {
			return migrated(integer(kept.apply(NOTE), OLD_NOTE), integer(kept.apply(VELOCITY), TGVelocities.DEFAULT));
		}
		MidiClickSound other = Objects.requireNonNullElse(MidiClickSound.parse(kept.apply(OTHER)), MidiClickSound.CLICK);
		return new MidiClick(Optional.ofNullable(MidiClickSound.parse(kept.apply(BEAT_ONE))), other, within(integer(kept.apply(VOLUME), DEFAULT.volume)));
	}

	private static MidiClick migrated(int note, int velocity) {
		boolean midi = (note >= 0 && note <= MAX_VELOCITY && velocity >= 1 && velocity <= MAX_VELOCITY);
		return (midi ? ofMidi(note, velocity) : DEFAULT);
	}

	private static int integer(String text, int otherwise) {
		try {
			return Integer.parseInt(text.trim());
		} catch (RuntimeException notANumber) {
			return otherwise;
		}
	}

	private static int within(int volume) {
		return Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
	}

	// kept under its keys, the note and velocity kept before removed
	public void write(TGConfigManager config) {
		this.toParameters(null).forEach(config::setValue);
		config.remove(NOTE);
		config.remove(VELOCITY);
	}

	// what a synthesizer's drums are told: the sounds and the volume, and the folder of the user's own sounds when given
	public Map<String, String> toParameters(String folder) {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(BEAT_ONE, (this.beatOne != null ? this.beatOne.getId() : SAME_ACCENTED));
		parameters.put(OTHER, this.other.getId());
		parameters.put(VOLUME, Integer.toString(this.volume));
		if (folder != null) {
			parameters.put(FOLDER, folder);
		}
		return parameters;
	}

	// beat 1's sound; none when it is the other beats' sound, accented
	public Optional<MidiClickSound> getBeatOne() {
		return Optional.ofNullable(this.beatOne);
	}

	public MidiClickSound getOther() {
		return this.other;
	}

	public int getVolume() {
		return this.volume;
	}

	// the volume's gain: 10^(dB / 20)
	public double getGain() {
		return gain(this.volume);
	}

	private static double gain(int decibels) {
		return Math.pow(10, decibels / 20.0);
	}

	// the note of a click on the channel: on a synthesizer's own drums, ACCENTED or PLAIN; elsewhere the drum note of
	// the beat's sound
	public int getNote(int channel, boolean accented) {
		if (channel == MidiSampleClock.DRUMS) {
			return (accented ? ACCENTED : PLAIN);
		}
		return (accented && this.beatOne != null ? this.beatOne : this.other).getGmNote();
	}

	// the velocity of a click on the channel: on a synthesizer's own drums, full (they play the volume they are told);
	// elsewhere the volume's, 127 * 10^(dB / 20) up to 127 (at MIN_VOLUME, 4), ACCENT_DECIBELS louder for beat 1 played
	// accented
	public int getVelocity(int channel, boolean accented) {
		if (channel == MidiSampleClock.DRUMS) {
			return MAX_VELOCITY;
		}
		int decibels = this.volume + (accented && this.beatOne == null ? ACCENT_DECIBELS : 0);
		return (int) Math.min(MAX_VELOCITY, Math.round(MAX_VELOCITY * gain(decibels)));
	}

	public boolean equals(Object other) {
		if (!(other instanceof MidiClick)) {
			return false;
		}
		MidiClick click = (MidiClick) other;
		return (Objects.equals(click.beatOne, this.beatOne) && click.other.equals(this.other) && click.volume == this.volume);
	}

	public int hashCode() {
		return Objects.hash(this.beatOne, this.other, this.volume);
	}

	public String toString() {
		return this.toParameters(null).toString();
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
