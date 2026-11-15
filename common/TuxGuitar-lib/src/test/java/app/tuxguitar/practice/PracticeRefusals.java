package app.tuxguitar.practice;

/** What starting practice comes to, for the practice tests. */
final class PracticeRefusals {

	private PracticeRefusals() {
	}

	/** Starting practice as set: it may be refused. */
	interface Starting {
		void start() throws TGPracticeRefusal;
	}

	// "accepted", or the reason starting is refused
	static String outcome(Starting starting) {
		try {
			starting.start();
			return "accepted";
		} catch (TGPracticeRefusal refusal) {
			return refusal.getMessage();
		}
	}
}
