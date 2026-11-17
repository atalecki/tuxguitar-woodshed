package app.tuxguitar.audio.features;

import java.util.HashMap;
import java.util.Map;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;

/**
 * Steps on the language TuxGuitar shows its texts in: English (messages.properties) but for the messages a scenario's
 * language says its own way, as a language's file says them over messages.properties. Each scenario starts and ends in
 * English.
 */
public class LanguageSteps {

	private final Map<String, String> said = new HashMap<String, String>();

	@Given("TuxGuitar's language says {string} for the message {string}")
	public void says(String text, String message) {
		this.said.put(message, text);
		AudioWorld.speak(this.said);
	}

	@After
	public void speakEnglish() {
		if (!this.said.isEmpty()) {
			AudioWorld.speak(Map.of());
		}
	}
}
