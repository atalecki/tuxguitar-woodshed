package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.midi.synth.TGSynthAudioExporter;
import app.tuxguitar.midi.synth.TGSynthManager;

/**
 * The audio export an app's own export writes through once the live audio is installed (an extension of the synth
 * manager): the formats offered, by name, and the song (one measure of the guitar) written in the one named.
 */
class TestAudioExport {

	private final SongActionFixture fixture = SongActionFixture.onDocument();

	TestAudioExport() {
		TGLiveAudio.install(this.fixture.context, id -> false);
	}

	private TGSynthAudioExporter exporter() {
		List<TGSynthAudioExporter> exporters = TGSynthManager.getInstance(this.fixture.context).getExtensions(TGSynthAudioExporter.class);
		assertEquals(1, exporters.size());
		return exporters.get(0);
	}

	@Test
	void theFormatsOfferedByNameWithTheirFilesExtension() {
		TGSynthAudioExporter exporter = exporter();
		assertEquals("[WAVE 16-bit, WAVE 24-bit, WAVE 32-bit float, AIFF 16-bit, AU 16-bit] wav au", exporter.getFormats() + " " + exporter.getExtension("WAVE 24-bit") + " " + exporter.getExtension("AU 16-bit"));
	}

	// what the user is shown of each, as an English user reads it
	@Test
	void theFormatsAreShownByTheirLabels() {
		TGSynthAudioExporter exporter = exporter();
		assertEquals("WAVE 16-bit | WAVE 24-bit | WAVE 32-bit float | AIFF 16-bit | AU 16-bit", TuxGuitarMessages.english(() -> labels(exporter)));
	}

	// as the language TuxGuitar shows says the words (a made-up German), the types as they are; the names stay
	@Test
	void theFormatsAreShownInTuxGuitarsLanguageAndChosenByTheirNames() {
		TGSynthAudioExporter exporter = exporter();
		Map<String, String> german = Map.of("audio.export.format.pcm-16", "{0} mit 16 Bit", "audio.export.format.pcm-24", "{0} mit 24 Bit",
			"audio.export.format.float-32", "{0} mit 32-Bit-Gleitkomma");
		assertEquals("WAVE mit 16 Bit | WAVE mit 24 Bit | WAVE mit 32-Bit-Gleitkomma | AIFF mit 16 Bit | AU mit 16 Bit [WAVE 16-bit, WAVE 24-bit, WAVE 32-bit float, AIFF 16-bit, AU 16-bit]",
			TuxGuitarMessages.said(german, () -> labels(exporter) + " " + exporter.getFormats()));
	}

	private static String labels(TGSynthAudioExporter exporter) {
		return exporter.getFormats().stream().map(exporter::getLabel).collect(Collectors.joining(" | "));
	}

	@Test
	void theSongIsWrittenInTheFormatNamed() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		exporter().export(this.fixture.song, "AU 16-bit", out);
		AudioFiles file = AudioFiles.read(out.toByteArray());
		assertEquals("AU 44100 Hz 16 bits PCM_SIGNED true", file.describe() + " " + (file.getFrameLength() >= 44100));
	}

	@Test
	void aFormatNotOfferedIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> exporter().getExtension("MP3"));
	}
}
