package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.editor.TuxGuitarMessages;
import app.tuxguitar.midi.synth.TGInstrumentKind;
import app.tuxguitar.vst3.TGVst3Plugin;

/**
 * The quiet hint that a default instrument did not play and General MIDI plays instead: told the first time it happens
 * for a song (and again once the defaults change), for 5 seconds or until its cause no longer holds (the default is
 * another one now, or the scan finds the one not installed), whichever comes first; said in TuxGuitar's language
 * (English here). The song is any object: a new one is a song opened.
 */
class TestFallbackNotice {

	private static final String DRUMS_ID = "7A1C0009544743434C49434B00000009";
	private static final TGDefaultInstrument DRUMS = TGDefaultInstrument.parse(DRUMS_ID, "Test Drums").get();
	private static final TGDefaultInstruments DEFAULTS = TGDefaultInstruments.NONE.with(TGInstrumentKind.DRUMS, DRUMS);
	private static final TGFallback NOT_INSTALLED = new TGFallback(TGInstrumentKind.DRUMS, DRUMS, TGFallback.Why.NOT_INSTALLED);
	private static final TGFallback NOT_LOADED = new TGFallback(TGInstrumentKind.DRUMS, DRUMS, TGFallback.Why.NOT_LOADED);
	private static final Object SONG = "song";

	private final TGFallbackNotice notice = new TGFallbackNotice();
	private TGDefaultInstruments defaults = DEFAULTS;
	private List<TGVst3Plugin> scanned = Collections.emptyList();

	// what the notice reads at each of the times, in milliseconds
	private String readAt(long... times) {
		return TuxGuitarMessages.english(() -> {
			List<String> read = new ArrayList<String>();
			for (long time : times) {
				read.add(this.notice.getText(time, this.defaults, this.scanned));
			}
			return String.join(" | ", read);
		});
	}

	@Test
	void aDefaultNotInstalledIsToldForFiveSeconds() {
		this.notice.fellBack(NOT_INSTALLED, SONG, 1000);
		assertEquals("Test Drums is not installed: drum tracks play General MIDI. | Test Drums is not installed: drum tracks play General MIDI. | null",
			readAt(1000, 5999, 6000));
	}

	@Test
	void aDefaultThatDidNotLoadIsToldForItsKind() {
		this.defaults = this.defaults.with(TGInstrumentKind.BASS, DRUMS);
		this.notice.fellBack(new TGFallback(TGInstrumentKind.BASS, DRUMS, TGFallback.Why.NOT_LOADED), SONG, 0);
		assertEquals("Test Drums could not be loaded: bass tracks play General MIDI.", readAt(0));
	}

	// played again (each Play, each channel), it is not told again for the same song; another kind's, or another reason,
	// is another fallback
	@Test
	void aFallbackIsToldOncePerSong() {
		boolean first = this.notice.fellBack(NOT_INSTALLED, SONG, 0);
		boolean again = this.notice.fellBack(new TGFallback(TGInstrumentKind.DRUMS, DRUMS, TGFallback.Why.NOT_INSTALLED), SONG, 6000);
		boolean bass = this.notice.fellBack(new TGFallback(TGInstrumentKind.BASS, DRUMS, TGFallback.Why.NOT_INSTALLED), SONG, 6000);
		boolean loaded = this.notice.fellBack(NOT_LOADED, SONG, 6000);
		assertEquals("true false true true", first + " " + again + " " + bass + " " + loaded);
	}

	// told at 0, then what happens between, then the same fallback again at 6 s playing the song: whether it was told
	// again, and what the notice reads then
	private String toldAgain(Runnable between, Object song) {
		this.notice.fellBack(NOT_INSTALLED, SONG, 0);
		between.run();
		return this.notice.fellBack(NOT_INSTALLED, song, 6000) + " " + readAt(6000);
	}

	@Test
	void aSongOpenedTellsItAgain() {
		assertEquals("true Test Drums is not installed: drum tracks play General MIDI.", toldAgain(() -> {
		}, "another song"));
	}

	@Test
	void theDefaultsChangedTellItAgain() {
		assertEquals("true Test Drums is not installed: drum tracks play General MIDI.", toldAgain(this.notice::defaultsChanged, SONG));
	}

	// another fallback of the same song is told as it happens
	@Test
	void theLastFallbackToldIsRead() {
		this.notice.fellBack(NOT_INSTALLED, SONG, 0);
		this.notice.fellBack(NOT_LOADED, SONG, 10);
		assertEquals("Test Drums could not be loaded: drum tracks play General MIDI.", readAt(10));
	}

	@Test
	void itIsNoLongerToldOnceTheDefaultIsAnotherOne() {
		this.notice.fellBack(NOT_LOADED, SONG, 0);
		String before = readAt(0);
		this.defaults = TGDefaultInstruments.NONE;
		assertEquals("Test Drums could not be loaded: drum tracks play General MIDI. | null", before + " | " + readAt(1));
	}

	@Test
	void aDefaultNotInstalledIsNoLongerToldOnceTheScanFindsIt() {
		this.notice.fellBack(NOT_INSTALLED, SONG, 0);
		String before = readAt(0);
		this.scanned = Arrays.asList(new TGVst3Plugin("C:/VST3/Test Drums.vst3", DRUMS_ID, "Test Drums", true));
		assertEquals("Test Drums is not installed: drum tracks play General MIDI. | null", before + " | " + readAt(1));
	}

	// guitar and other tracks have words of their own
	@Test
	void eachKindIsNamed() {
		this.defaults = this.defaults.with(TGInstrumentKind.GUITAR, DRUMS).with(TGInstrumentKind.OTHER, DRUMS);
		this.notice.fellBack(new TGFallback(TGInstrumentKind.GUITAR, DRUMS, TGFallback.Why.NOT_INSTALLED), SONG, 0);
		String guitar = readAt(0);
		this.notice.fellBack(new TGFallback(TGInstrumentKind.OTHER, DRUMS, TGFallback.Why.NOT_INSTALLED), SONG, 0);
		assertEquals("Test Drums is not installed: guitar tracks play General MIDI. | Test Drums is not installed: other tracks play General MIDI.", guitar + " | " + readAt(0));
	}

	// a fallback is the same as another of the same kind, instrument and reason, whichever told it
	@Test
	void fallbacksAreTheSameByKindInstrumentAndReason() {
		TGFallback same = new TGFallback(TGInstrumentKind.DRUMS, TGDefaultInstrument.parse(DRUMS_ID, "named otherwise").get(), TGFallback.Why.NOT_INSTALLED);
		assertEquals("true false false", NOT_INSTALLED.equals(same) + " " + NOT_INSTALLED.equals(NOT_LOADED) + " " + NOT_INSTALLED.equals(DRUMS));
	}
}
