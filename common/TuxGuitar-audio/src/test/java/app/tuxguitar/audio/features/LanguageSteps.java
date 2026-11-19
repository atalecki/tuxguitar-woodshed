package app.tuxguitar.audio.features;

import java.util.HashMap;
import java.util.Map;

import app.tuxguitar.editor.TuxGuitarMessages;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;

/**
 * Steps on the language TuxGuitar shows its texts in: English (messages.properties) but for the messages a scenario's
 * language says its own way, as a language's file says them over messages.properties. Each scenario starts and ends in
 * English.
 */
public class LanguageSteps {

	private final Map<String, String> said = new HashMap<String, String>();
	private final AudioWorld world;
	// whether a step showed TuxGuitar's texts in another language
	private boolean shown;

	public LanguageSteps(AudioWorld world) {
		this.world = world;
	}

	// TuxGuitar's texts as a language's (its tag: "de", "pt_BR"), the app telling the plugins (its language listener)
	@Given("TuxGuitar shows its texts in {string}")
	public void showsItsTextsIn(String tag) {
		String[] parts = tag.split("_");
		java.util.Locale locale = (parts.length > 1 ? new java.util.Locale(parts[0], parts[1]) : new java.util.Locale(parts[0]));
		app.tuxguitar.util.TGMessagesManager.getInstance().setResources(new app.tuxguitar.resource.TGResourceBundle(locale, new java.util.Properties()));
		app.tuxguitar.audio.TGLivePlugins.getInstance(this.world.fixture().context).languageChanged();
		this.shown = true;
	}

	@Given("TuxGuitar's language says {string} for the message {string}")
	public void says(String text, String message) {
		this.said.put(message, text);
		TuxGuitarMessages.speak(this.said);
	}

	@After
	public void speakEnglish() {
		if (!this.said.isEmpty() || this.shown) {
			TuxGuitarMessages.speak(Map.of());
		}
	}
}
