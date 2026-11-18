package app.tuxguitar.audio;

import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * What Record needs, as it is at one moment: an armed track, the TuxGuitar Synthesizer as the MIDI Port, an audio
 * device (the session's: none when the native audio library did not load or the devices did not open). Why Record is
 * refused, and whether that still holds, is read from it (TGRecordRefusal).
 */
public final class TGRecordConditions {

	private final boolean armed;
	private final boolean synthesizerPort;
	private final boolean device;

	TGRecordConditions(boolean armed, boolean synthesizerPort, boolean device) {
		this.armed = armed;
		this.synthesizerPort = synthesizerPort;
		this.device = device;
	}

	// as they are now, for the song
	public static TGRecordConditions of(TGContext context, TGSong song) {
		TGAudioSession session = TGAudioSession.getInstance(context);
		return new TGRecordConditions(!session.getArmedTracks(song).isEmpty(), MidiPlayer.getInstance(context).isOutputPortOpen(TGAudioSession.SYNTHESIZER_PORT_KEY),
			session.getDevice() != null);
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
