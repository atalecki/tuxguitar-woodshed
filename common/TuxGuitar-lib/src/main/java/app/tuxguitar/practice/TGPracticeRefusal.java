package app.tuxguitar.practice;

/**
 * Interleaved practice refuses what cannot be played as set, saying why (the message the app shows).
 */
public class TGPracticeRefusal extends Exception {

	private static final long serialVersionUID = 1L;

	public TGPracticeRefusal(String message) {
		super(message);
	}
}
