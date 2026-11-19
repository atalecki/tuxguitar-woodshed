package app.tuxguitar.practice;

/**
 * Interleaved practice refuses what cannot be played as set, saying why (the message the app shows), in TuxGuitar's
 * language: the message of a key of its messages (transport.practice.refusal.*, transport.click-up.refusal.*), English
 * where the language does not say it.
 */
public class TGPracticeRefusal extends Exception {

	private static final long serialVersionUID = 1L;

	// the message of the key, the arguments in place of {0}, {1}...
	public TGPracticeRefusal(String key, String... arguments) {
		super(TGPracticeWording.APP.get(key, arguments));
	}
}
