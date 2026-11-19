package app.tuxguitar.player.base;

import java.util.List;
import java.util.Objects;

/**
 * A sound Count In and the Metronome click with (see MidiClick): a built-in one, the code-made Click or one of the
 * recordings the TuxGuitar Synthesizer ships, or a WAV file of the user's own in the click sounds folder, named by its
 * file name there. Each has the General MIDI drum note other MIDI Ports play for it: an own sound's is Side Stick.
 * Immutable; kept as its id ("click", "wood-block", ..., "file:" and the file name), read back with parse once where it
 * comes in.
 */
public final class MidiClickSound {

	// the drum note of an own sound on other MIDI Ports: Side Stick
	public static final int OWN_GM_NOTE = 37;

	private static final String OWN_PREFIX = "file:";
	private static final String WAV = ".wav";

	// the code-made click: TuxGuitar's click until another sound is chosen
	public static final MidiClickSound CLICK = new MidiClickSound("click", 37, null);
	public static final MidiClickSound WOOD_BLOCK = new MidiClickSound("wood-block", 76, null);
	public static final MidiClickSound COWBELL = new MidiClickSound("cowbell", 56, null);
	public static final MidiClickSound SNARE = new MidiClickSound("snare", 38, null);
	public static final MidiClickSound SIDE_STICK = new MidiClickSound("side-stick", 37, null);
	public static final MidiClickSound CLAVES = new MidiClickSound("claves", 75, null);
	public static final MidiClickSound HI_HAT = new MidiClickSound("hi-hat", 42, null);
	public static final MidiClickSound CLAP = new MidiClickSound("clap", 39, null);

	// in the order they are offered
	public static final List<MidiClickSound> BUILT_IN = List.of(CLICK, WOOD_BLOCK, COWBELL, SNARE, SIDE_STICK, CLAVES, HI_HAT, CLAP);

	private final String id;
	private final int gmNote;
	// an own sound's file name in the click sounds folder; null for a built-in one
	private final String fileName;

	private MidiClickSound(String id, int gmNote, String fileName) {
		this.id = id;
		this.gmNote = gmNote;
		this.fileName = fileName;
	}

	// the user's own sound in the file of that name in the click sounds folder
	public static MidiClickSound own(String fileName) {
		if (!isOwnFileName(fileName)) {
			throw new IllegalArgumentException("not a WAV file name: " + fileName);
		}
		return new MidiClickSound(OWN_PREFIX + fileName, OWN_GM_NOTE, fileName);
	}

	// whether it names a WAV file in the click sounds folder: a name ending in .wav (any case), not in a folder of its own
	public static boolean isOwnFileName(String fileName) {
		return (fileName != null && fileName.length() > WAV.length() && fileName.toLowerCase().endsWith(WAV)
			&& fileName.chars().noneMatch(c -> c == '/' || c == '\\' || c == ':'));
	}

	// the sound of the id; null for what names none
	public static MidiClickSound parse(String id) {
		if (id != null && id.startsWith(OWN_PREFIX)) {
			String fileName = id.substring(OWN_PREFIX.length());
			return (isOwnFileName(fileName) ? own(fileName) : null);
		}
		return BUILT_IN.stream().filter(sound -> sound.id.equals(id)).findFirst().orElse(null);
	}

	// the first built-in sound of the drum note; null for none
	public static MidiClickSound ofGmNote(int gmNote) {
		return BUILT_IN.stream().filter(sound -> sound.gmNote == gmNote).findFirst().orElse(null);
	}

	public String getId() {
		return this.id;
	}

	public int getGmNote() {
		return this.gmNote;
	}

	// an own sound's file name in the click sounds folder; null for a built-in sound
	public String getFileName() {
		return this.fileName;
	}

	// whether it plays a recording (a built-in one or the user's own); only the code-made Click does not
	public boolean isRecording() {
		return (this != CLICK);
	}

	public boolean equals(Object other) {
		return (other instanceof MidiClickSound && ((MidiClickSound) other).id.equals(this.id));
	}

	public int hashCode() {
		return Objects.hash(this.id);
	}

	public String toString() {
		return this.id;
	}
}
