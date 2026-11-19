package app.tuxguitar.audio;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGPracticeCourse;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * What Record needs, as it is at one moment: no practice session under way (interleaved practice or click-up, playing
 * or paused), an armed track, the TuxGuitar Synthesizer as the MIDI Port, an audio device (the session's: none when the
 * native audio library did not load or the devices did not open). Why Record is refused, and whether that still holds,
 * is read from it (TGRecordRefusal).
 */
public final class TGRecordConditions {

	// the practice session under way, as it names itself in TuxGuitar's language; null when none is
	private final String practice;
	private final boolean armed;
	private final boolean synthesizerPort;
	private final boolean device;

	TGRecordConditions(String practice, boolean armed, boolean synthesizerPort, boolean device) {
		this.practice = practice;
		this.armed = armed;
		this.synthesizerPort = synthesizerPort;
		this.device = device;
	}

	// as they are now, for the song
	public static TGRecordConditions of(TGContext context, TGSong song) {
		TGPracticeCourse course = TGPracticeUnderWay.getInstance(context).getCourse();
		TGAudioSession session = TGAudioSession.getInstance(context);
		return new TGRecordConditions((course != null ? course.getModeName() : null), !session.getArmedTracks(song).isEmpty(),
			MidiPlayer.getInstance(context).isOutputPortOpen(TGAudioSession.SYNTHESIZER_PORT_KEY), session.getDevice() != null);
	}

	String getPractice() {
		return this.practice;
	}

	boolean isArmed() {
		return this.armed;
	}

	boolean isSynthesizerPort() {
		return this.synthesizerPort;
	}

	boolean hasDevice() {
		return this.device;
	}
}
