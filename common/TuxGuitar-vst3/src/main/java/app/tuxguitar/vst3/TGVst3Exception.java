package app.tuxguitar.vst3;

import app.tuxguitar.util.TGMessagesManager;

/**
 * A VST3 host request that failed: refused by the host process (the reason it gave), or lost with the process. Its
 * message is what the log reads. What the user is told is its localized message: the host process's own reasons as it
 * gave them, and TuxGuitar's reasons the host failed (it stopped, did not answer, did not start) as TuxGuitar's language
 * says them, when they are read, while the log keeps them in English.
 */
public class TGVst3Exception extends Exception {

	private static final long serialVersionUID = 1L;

	// the message the user is told it by, in TuxGuitar's language (null: its message as given), and what goes in its
	// {0}, {1}...
	private final String said;
	private final Object[] arguments;

	public TGVst3Exception(String message) {
		this(message, null);
	}

	public TGVst3Exception(String message, Throwable cause) {
		this(message, cause, null);
	}

	// logged as the English message, told the user as TuxGuitar's language says the message said, with the arguments
	TGVst3Exception(String message, Throwable cause, String said, Object... arguments) {
		super(message, cause);
		this.said = said;
		this.arguments = arguments;
	}

	// the same failure, thrown again: this one its cause
	TGVst3Exception again() {
		return new TGVst3Exception(this.getMessage(), this, this.said, this.arguments);
	}

	// what the user is told
	@Override
	public String getLocalizedMessage() {
		return (this.said != null ? TGMessagesManager.getProperty(this.said, this.arguments) : this.getMessage());
	}
}
