package app.tuxguitar.practice;

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

import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.resource.TGResourceBundle;
import app.tuxguitar.util.TGMessagesManager;

/**
 * What starting practice comes to, for the practice tests, in TuxGuitar's language: its English messages
 * (common/resources/lang/messages.properties; Maven runs the tests in the module folder), but for those a made-up
 * language says its own way, as a language's file says them over messages.properties.
 */
public final class PracticeRefusals {

	private static final String MESSAGES = "../resources/lang/messages.properties";
	private static final Properties ENGLISH = new Properties();

	static {
		try (Reader in = new InputStreamReader(new FileInputStream(MESSAGES), StandardCharsets.UTF_8)) {
			ENGLISH.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private PracticeRefusals() {
	}

	/** Starting practice as set: it may be refused. */
	public interface Starting {
		void start() throws TGPracticeRefusal;
	}

	/** Starting practice on the player: it may be refused, and the player may fail (which fails the test). */
	public interface Playing {
		void start() throws TGPracticeRefusal, MidiPlayerException;
	}

	// what the reading reads with TuxGuitar's messages English, but for those the language says its own way; then no
	// messages again (every message reads as its key, as the other tests expect)
	public static String said(Map<String, String> said, Supplier<String> reading) {
		Properties properties = new Properties();
		properties.putAll(ENGLISH);
		properties.putAll(said);
		TGMessagesManager.getInstance().setResources(new TGResourceBundle(Locale.ENGLISH, properties));
		try {
			return reading.get();
		} finally {
			TGMessagesManager.getInstance().setResources(null);
		}
	}

	// what the reading reads with TuxGuitar's English messages
	public static String english(Supplier<String> reading) {
		return said(Map.of(), reading);
	}

	// "accepted", or the reason the player refuses to start, in English
	public static String played(Playing playing) {
		return outcome(() -> {
			try {
				playing.start();
			} catch (MidiPlayerException e) {
				throw new AssertionError(e);
			}
		});
	}

	// "accepted", or the reason starting is refused, in English
	public static String outcome(Starting starting) {
		return outcome(Map.of(), starting);
	}

	// "accepted", or the reason starting is refused, in English but for the messages the language says its own way
	public static String outcome(Map<String, String> said, Starting starting) {
		return said(said, () -> {
			try {
				starting.start();
				return "accepted";
			} catch (TGPracticeRefusal refusal) {
				return refusal.getMessage();
			}
		});
	}
}
