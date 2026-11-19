package app.tuxguitar.player.base;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import app.tuxguitar.io.wave.TGWaveReader;
import app.tuxguitar.util.TGMessagesManager;
import app.tuxguitar.util.TGUserFileUtils;

/**
 * The folder of the user's own click sounds (see MidiClickSound.own): its WAV files ARE the own sounds, one dropped there
 * by hand too. add copies a file in, under a numbered name when its name is taken ("Beep (2).wav"), and refuses what
 * the click could not play, nothing copied (Refused, saying why in the user's language); remove deletes the copy.
 * The user's is the folder NAME in TuxGuitar's user folder (user()).
 */
public final class MidiClickSoundsFolder {

	public static final String NAME = "click-sounds";

	private static final String WAV = ".wav";

	// why a sound was not added (nothing copied) or not removed; its message is metronome.sound.refusal.* ({0} the file's
	// name, {1} what the system said)
	public enum Reason {
		NOT_WAVE, NOT_SAMPLES, ENDS_EARLY, SILENT, NOT_READ, NOT_COPIED, NOT_REMOVED;

		private String key() {
			return "metronome.sound.refusal." + this.name().toLowerCase(Locale.ROOT).replace('_', '-');
		}
	}

	// a file the wave reader refuses, refused for the same reason
	private static final Map<TGWaveReader.Refusal, Reason> WAVE_REFUSALS = Map.of(TGWaveReader.Refusal.NOT_WAVE, Reason.NOT_WAVE,
		TGWaveReader.Refusal.NOT_SAMPLES, Reason.NOT_SAMPLES, TGWaveReader.Refusal.ENDS_EARLY, Reason.ENDS_EARLY);

	public static final class Refused extends IOException {

		private static final long serialVersionUID = 1L;

		private final Reason reason;
		private final String fileName;
		private final String detail;

		public Refused(Reason reason, String fileName, String detail) {
			super(reason.key());
			this.reason = reason;
			this.fileName = fileName;
			this.detail = detail;
		}

		public Reason getReason() {
			return this.reason;
		}

		// why, in TuxGuitar's language when it is asked
		public String getMessage() {
			return TGMessagesManager.getProperty(this.reason.key(), new Object[] {this.fileName, this.detail});
		}
	}

	private final File folder;

	public MidiClickSoundsFolder(File folder) {
		this.folder = folder;
	}

	// the user's, in TuxGuitar's user folder
	public static MidiClickSoundsFolder user() {
		return new MidiClickSoundsFolder(new File(TGUserFileUtils.PATH_USER_DIR, NAME));
	}

	public String getPath() {
		return this.folder.getAbsolutePath();
	}

	// the own sounds: its WAV files, by name ignoring case; none while it is not there
	public List<MidiClickSound> own() {
		return Optional.ofNullable(this.folder.list()).map(Stream::of).orElseGet(Stream::empty).filter(MidiClickSound::isOwnFileName).filter(name -> new File(this.folder, name).isFile())
			.sorted(String.CASE_INSENSITIVE_ORDER).map(MidiClickSound::own).collect(Collectors.toList());
	}

	// every sound offered: the built-in ones in their order, then the own ones
	public List<MidiClickSound> sounds() {
		List<MidiClickSound> sounds = new ArrayList<MidiClickSound>(MidiClickSound.BUILT_IN);
		sounds.addAll(this.own());
		return sounds;
	}

	// the name a copy of a file of that name gets: a WAV file's (".wav" added when it has none), in the folder itself
	static String copyName(String name) {
		String plain = name.replace(':', '_').replace('\\', '_').replace('/', '_');
		return (MidiClickSound.isOwnFileName(plain) ? plain : plain + WAV);
	}

	// a copy of the file, the folder made when it is not there; refused, nothing copied, when the click could not play it
	public MidiClickSound add(File file) throws Refused {
		if (silent(samples(file))) {
			throw new Refused(Reason.SILENT, file.getName(), null);
		}
		File copy = this.free(copyName(file.getName()));
		try {
			Files.createDirectories(this.folder.toPath());
			Files.copy(file.toPath(), copy.toPath());
		} catch (IOException failed) {
			throw new Refused(Reason.NOT_COPIED, file.getName(), failed.getMessage());
		}
		return MidiClickSound.own(copy.getName());
	}

	// the file of that name in the folder, else of that name numbered from 2, the first that is not there
	private File free(String name) {
		String stem = name.substring(0, name.length() - WAV.length());
		String extension = name.substring(stem.length());
		File free = new File(this.folder, name);
		for (int number = 2; free.exists(); number++) {
			free = new File(this.folder, stem + " (" + number + ")" + extension);
		}
		return free;
	}

	// the file's samples, a channel each; refused when the wave reader refuses it, or it cannot be read
	private static float[][] samples(File file) throws Refused {
		try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
			return TGWaveReader.readSamples(in, TGWaveReader.readFormat(in));
		} catch (TGWaveReader.Refused refused) {
			throw new Refused(WAVE_REFUSALS.get(refused.getRefusal()), file.getName(), null);
		} catch (IOException unread) {
			throw new Refused(Reason.NOT_READ, file.getName(), unread.getMessage());
		}
	}

	private static boolean silent(float[][] channels) {
		return Stream.of(channels).allMatch(channel -> IntStream.range(0, channel.length).allMatch(i -> channel[i] == 0));
	}

	// the copy of the own sound deleted (nothing happens when it is not there)
	public void remove(MidiClickSound sound) throws Refused {
		try {
			Files.deleteIfExists(new File(this.folder, sound.getFileName()).toPath());
		} catch (IOException failed) {
			throw new Refused(Reason.NOT_REMOVED, sound.getFileName(), failed.getMessage());
		}
	}
}
