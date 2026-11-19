package app.tuxguitar.audio;

import java.util.Arrays;
import java.util.function.Predicate;

import app.tuxguitar.util.TGMessagesManager;

/**
 * Why Record, pressed, cannot start a recording: a practice session is under way, no track is armed, the MIDI Port is
 * not the TuxGuitar Synthesizer, or there is no live audio device: each a cause that holds, or not, in what Record needs as it is (TGRecordConditions).
 * The log reads it in English (toString); the user is told what to do about it in TuxGuitar's language (say), on the
 * app's status line (TGStatusHint).
 */
public enum TGRecordRefusal {

	// {0} in its message: the practice under way
	PRACTICE("a practice session is under way", "transport.practice.refusal.record", now -> now.getPractice() != null),
	NO_ARMED_TRACK("no track is armed", "audio.record.refusal.no-armed-track", now -> !now.isArmed()),
	MIDI_PORT("the MIDI port is not the TuxGuitar Synthesizer", "audio.record.refusal.midi-port", now -> !now.isSynthesizerPort()),
	NO_DEVICE("no live audio device", "audio.record.refusal.no-device", now -> !now.hasDevice());

	private final String english;
	// what the user is told, its message
	private final String message;
	private final Predicate<TGRecordConditions> cause;

	TGRecordRefusal(String english, String message, Predicate<TGRecordConditions> cause) {
		this.english = english;
		this.message = message;
		this.cause = cause;
	}

	// why a recording cannot start, the first that holds; null when it can
	public static TGRecordRefusal of(TGRecordConditions now) {
		return Arrays.stream(values()).filter(refusal -> refusal.holds(now)).findFirst().orElse(null);
	}

	// whether its cause holds
	public boolean holds(TGRecordConditions now) {
		return this.cause.test(now);
	}

	// what the user is told while it holds, as TuxGuitar's language says it
	public String say(TGRecordConditions now) {
		return TGMessagesManager.getProperty(this.message, new Object[] {now.getPractice()});
	}

	// as the log reads it, in English
	public String toString() {
		return this.english;
	}
}
