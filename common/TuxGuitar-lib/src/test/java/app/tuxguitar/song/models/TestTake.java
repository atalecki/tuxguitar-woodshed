package app.tuxguitar.song.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * A take whose file entry could not be read, as the file reader makes it: no samples to play, the entry kept as it was
 * for saving, numbered 0 until the file gives it its number, as a take a codec reads is.
 */
public class TestTake {

	private static String entry(TGTake take) throws IOException {
		InputStream entry = take.getUnreadEntry();
		return (entry != null ? new String(entry.readAllBytes(), StandardCharsets.US_ASCII) : "none");
	}

	@Test
	public void aTakeThatCouldNotBeReadHasNoSamplesAndKeepsItsEntryUnderAnyNumber() throws IOException {
		TGTake unreadable = TGTake.unreadable("RIFF?".getBytes(StandardCharsets.US_ASCII));
		TGTake renumbered = unreadable.renumbered(3);

		assertEquals("take 0: 1 channel of 0 samples, entry RIFF? | take 3: entry RIFF? | recorded: entry none",
			"take " + unreadable.getNumber() + ": " + unreadable.countChannels() + " channel of " + unreadable.countSamples() + " samples, entry "
				+ entry(unreadable) + " | take " + renumbered.getNumber() + ": entry " + entry(renumbered) + " | recorded: entry "
				+ entry(new TGTake(1, new float[] {0.5f}, 44100)));
	}
}
