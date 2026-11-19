package app.tuxguitar.editor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import app.tuxguitar.resource.TGResourceBundle;
import app.tuxguitar.util.TGMessagesManager;

/**
 * TuxGuitar's messages for a test that reads a text shown to the user: English (common/resources/lang/messages.properties;
 * Maven runs the tests in the module folder), but for those a made-up language says its own way, as a language's file
 * says them over messages.properties. After the reading, no messages again: every message reads as its key, as the
 * other tests expect.
 */
public final class TuxGuitarMessages {

	private static final String MESSAGES = "../resources/lang/messages.properties";
	private static final Properties ENGLISH = new Properties();

	static {
		try (Reader in = new InputStreamReader(new FileInputStream(MESSAGES), StandardCharsets.UTF_8)) {
			ENGLISH.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private TuxGuitarMessages() {
	}

	// TuxGuitar's messages from now on: English, but for those the language says its own way; none, English
	public static void speak(Map<String, String> said) {
		Properties properties = new Properties();
		properties.putAll(ENGLISH);
		properties.putAll(said);
		TGMessagesManager.getInstance().setResources(new TGResourceBundle(Locale.ENGLISH, properties));
	}

	// what the reading reads in English, but for the messages the language says its own way; then no messages again
	public static String said(Map<String, String> said, Supplier<String> reading) {
		speak(said);
		try {
			return reading.get();
		} finally {
			TGMessagesManager.getInstance().setResources(null);
		}
	}

	// what the reading reads in English
	public static String english(Supplier<String> reading) {
		return said(Map.of(), reading);
	}
}
