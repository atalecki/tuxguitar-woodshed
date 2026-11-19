package app.tuxguitar.midi.synth.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import app.tuxguitar.player.base.MidiClickSound;
import app.tuxguitar.util.log.TGLog;

/**
 * Where the recording of a click sound is: a built-in one in click-sounds (see its README), next to this class; the
 * user's own in the folder of own click sounds the drums are told (MidiClick.FOLDER), by its file name. One that cannot
 * be read (gone, not a WAVE file the wave reader reads, silent, no folder told) is logged, and the code-made Click
 * plays in its place.
 */
final class TGClickSounds {

	private static final TGLog LOG = TGLog.get(TGClickSounds.class);

	private TGClickSounds() {
	}

	// the recording of the sound, evened; null for the code-made Click, and for one that cannot be read (logged)
	static TGClickRecording recording(MidiClickSound sound, String folder) {
		if (!sound.isRecording()) {
			return null;
		}
		try (InputStream in = open(sound, folder)) {
			return TGClickRecording.read(in);
		} catch (IOException | RuntimeException unreadable) {
			LOG.warn("The click sound " + sound + " could not be read, the Click plays in its place: " + unreadable.getMessage());
			return null;
		}
	}

	// the file of a recording
	static InputStream open(MidiClickSound sound, String folder) throws IOException {
		if (sound.getFileName() != null) {
			if (folder == null) {
				throw new IOException("no folder of own click sounds");
			}
			return new FileInputStream(new File(folder, sound.getFileName()));
		}
		InputStream in = TGClickSounds.class.getResourceAsStream("click-sounds/" + sound.getId() + ".wav");
		if (in == null) {
			throw new IOException("not built in");
		}
		return in;
	}
}
