package app.tuxguitar.audio;

import java.util.List;

import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.util.TGMessagesManager;

/**
 * Why Record, pressed, cannot start a recording: no track is armed, the MIDI Port is not the TuxGuitar Synthesizer, or
 * there is no live audio device. The log reads it in English (toString); the user is told what to do about it in
 * TuxGuitar's language (say), on the app's status line (TGStatusHint).
 */
public enum TGRecordRefusal {

	NO_ARMED_TRACK("no track is armed", "audio.record.refusal.no-armed-track"),
	MIDI_PORT("the MIDI port is not the TuxGuitar Synthesizer", "audio.record.refusal.midi-port"),
	NO_DEVICE("no live audio device", "audio.record.refusal.no-device");

	private final String english;
	// what the user is told, its message
	private final String message;

	TGRecordRefusal(String english, String message) {
		this.english = english;
		this.message = message;
	}

	// why a recording of the armed tracks cannot start, the first that holds; null when it can
	public static TGRecordRefusal of(List<TGTrack> armed, boolean synthesizerPort, TGAudioDevice device) {
		if (armed.isEmpty()) {
			return NO_ARMED_TRACK;
		}
		if (!synthesizerPort) {
			return MIDI_PORT;
		}
		return (device == null ? NO_DEVICE : null);
	}

	// what the user is told, as TuxGuitar's language says it
	public String say() {
		return TGMessagesManager.getProperty(this.message);
	}

	// as the log reads it, in English
	public String toString() {
		return this.english;
	}
}
