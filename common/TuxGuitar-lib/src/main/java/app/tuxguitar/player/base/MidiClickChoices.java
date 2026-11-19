package app.tuxguitar.player.base;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.tuxguitar.util.TGMessagesManager;

/**
 * The click being chosen in Settings > Sound's "Count In / Metronome click", not yet kept: beat 1's sound ("Same sound,
 * accented" first, then every sound), the other beats' (every sound: the built-in ones, then the user's own, as the
 * folder of own sounds has them now), the click volume. getClick is what Try plays and OK keeps. Add copies a WAV file
 * in and chooses it for the other beats; Remove deletes the own sound chosen for the other beats (only that one), and
 * whatever played it plays the Click. Each sound is named in the user's language (name), the volume in signed dB.
 */
public final class MidiClickChoices {

	private static final String SOUND = "metronome.sound.";

	private final MidiClickSoundsFolder folder;
	private MidiClick click;

	public MidiClickChoices(MidiClick click, MidiClickSoundsFolder folder) {
		this.click = click;
		this.folder = folder;
	}

	// the other beats' choices: every sound
	public List<MidiClickSound> getSounds() {
		return this.folder.sounds();
	}

	// beat 1's choices: the other beats' sound accented (none), then every sound
	public List<Optional<MidiClickSound>> getBeatOneChoices() {
		List<Optional<MidiClickSound>> choices = new ArrayList<Optional<MidiClickSound>>();
		choices.add(Optional.empty());
		this.getSounds().forEach(sound -> choices.add(Optional.of(sound)));
		return choices;
	}

	public MidiClick getClick() {
		return this.click;
	}

	public void chooseBeatOne(Optional<MidiClickSound> beatOne) {
		this.click = this.click.withBeatOne(beatOne);
	}

	public void chooseOther(MidiClickSound other) {
		this.click = this.click.withOther(other);
	}

	public void chooseVolume(int volume) {
		this.click = this.click.withVolume(volume);
	}

	// whether the other beats' sound is an own one
	public boolean canRemove() {
		return (this.click.getOther().getFileName() != null);
	}

	// a copy of the file added, and chosen for the other beats
	public MidiClickSound add(File file) throws MidiClickSoundsFolder.Refused {
		MidiClickSound added = this.folder.add(file);
		this.chooseOther(added);
		return added;
	}

	// the other beats' sound (an own one, see canRemove) deleted, the Click where it played
	public void remove() throws MidiClickSoundsFolder.Refused {
		MidiClickSound removed = this.click.getOther();
		this.folder.remove(removed);
		this.click = this.click.without(removed);
	}

	// beat 1's choice: its sound's name, else "Same sound, accented"
	public static String name(Optional<MidiClickSound> beatOne) {
		return beatOne.map(MidiClickChoices::name).orElseGet(() -> TGMessagesManager.getProperty(SOUND + "same-accented"));
	}

	// a built-in sound's name in the user's language, an own one's file name without .wav
	public static String name(MidiClickSound sound) {
		String file = sound.getFileName();
		return (file != null ? file.substring(0, file.length() - ".wav".length()) : TGMessagesManager.getProperty(SOUND + sound.getId()));
	}

	// "+6 dB", "0 dB", "-3 dB"
	public static String volume(int decibels) {
		return TGMessagesManager.getProperty(SOUND + "decibels", new Object[] {(decibels > 0 ? "+" : "") + decibels});
	}
}
