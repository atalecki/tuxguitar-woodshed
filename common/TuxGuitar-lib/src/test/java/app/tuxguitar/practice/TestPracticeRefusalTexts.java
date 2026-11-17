package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import app.tuxguitar.song.models.TGSong;

/**
 * The words of the refusals of interleaved practice and click-up as an English user reads them, for every kind of unit:
 * a count of one unit or more ("1 measure", "2 measures"), and the unit or block a refusal names ("Line 2", "The block of
 * lines 1–2", "The block of measure 3 beat 1 to measure 3 beat 4"). They are said in TuxGuitar's language: a language
 * that says the messages its own way (here a made-up German) reads them so, each part too. The songs are of 4/4, 4
 * measures a line, 4 lines a page.
 */
public class TestPracticeRefusalTexts {

	private static final Map<String, String> ENGLISH = Map.of();

	private final TGPracticeSettings settings = new TGPracticeSettings();

	// a session of so many turns over the pool of the kind from to, blocks of so many units, in the song, in English
	// but for the messages the language says its own way
	private String session(Map<String, String> said, TGSong song, TGPracticeUnitKind kind, int from, int to, int unitsPerBlock, int turns) {
		settings.setUnit(kind);
		settings.setPool(from, to);
		settings.setUnitsPerBlock(unitsPerBlock);
		settings.setTurns(turns);
		return PracticeRefusals.outcome(said, () -> TGPracticeSession.start(settings, song));
	}

	// the same in English, in a song of so many measures
	private String session(int measures, TGPracticeUnitKind kind, int from, int to, int unitsPerBlock, int turns) {
		return session(ENGLISH, PracticeSongs.song(measures), kind, from, to, unitsPerBlock, turns);
	}

	// a click-up of the passage of the kind from to, in a song of so many measures, in English
	private static String clickUp(int measures, TGPracticeUnitKind kind, int from, int to) {
		TGClickUpSettings clickUp = new TGClickUpSettings();
		clickUp.setUnit(kind);
		clickUp.setPassageFrom(from);
		clickUp.setPassageTo(to);
		TGSong song = PracticeSongs.song(measures);
		return PracticeRefusals.outcome(() -> TGClickUpLadder.of(clickUp, song));
	}

	// a song of 4 measures, measure 2 of 1/4: a pool of measure 2 holds one beat
	private static TGSong oneBeatInMeasure2() {
		TGSong song = PracticeSongs.song(4);
		PracticeSongs.signature(song, 2, 1, 4);
		return song;
	}

	// a click-up whose ladder in bpm starts at 0, in English but for the messages the language says its own way
	private static String belowOneBpm(Map<String, String> said) {
		TGClickUpSettings clickUp = new TGClickUpSettings();
		clickUp.setTempoMode(TGClickUpTempoMode.BPM);
		clickUp.setStart(0);
		TGSong song = PracticeSongs.song(4);
		return PracticeRefusals.outcome(said, () -> TGClickUpLadder.of(clickUp, song));
	}

	private static DynamicTest reads(String expected, Supplier<String> refusal) {
		return DynamicTest.dynamicTest(expected, () -> assertEquals(expected, refusal.get()));
	}

	@TestFactory
	public Stream<DynamicTest> aRefusalReadsAsAnEnglishUserReadsIt() {
		return Stream.of(
			reads("A block needs at least 1 line.", () -> session(16, TGPracticeUnitKind.LINE, 1, 2, 0, 1)),
			reads("A block needs at least 1 page.", () -> session(16, TGPracticeUnitKind.PAGE, 1, 1, 0, 1)),
			reads("A block of 2 measures does not fit in the pool's 1 measure.", () -> session(16, TGPracticeUnitKind.MEASURE, 1, 1, 2, 1)),
			reads("A block of 3 lines does not fit in the pool's 2 lines.", () -> session(16, TGPracticeUnitKind.LINE, 1, 2, 3, 1)),
			reads("A block of 2 pages does not fit in the pool's 1 page.", () -> session(16, TGPracticeUnitKind.PAGE, 1, 1, 2, 1)),
			reads("Line 2 would come twice in a row.", () -> session(16, TGPracticeUnitKind.LINE, 2, 2, 1, 2)),
			reads("Page 1 would come twice in a row.", () -> session(16, TGPracticeUnitKind.PAGE, 1, 1, 1, 2)),
			reads("Measure 2 beat 1 would come twice in a row.", () -> session(ENGLISH, oneBeatInMeasure2(), TGPracticeUnitKind.BEAT, 2, 2, 1, 2)),
			reads("The block of lines 1–2 would come twice in a row.", () -> session(16, TGPracticeUnitKind.LINE, 1, 2, 2, 2)),
			reads("The block of pages 1–2 would come twice in a row.", () -> session(32, TGPracticeUnitKind.PAGE, 1, 2, 2, 2)),
			reads("The block of measure 3 beat 1 to measure 3 beat 4 would come twice in a row.", () -> session(16, TGPracticeUnitKind.BEAT, 3, 3, 4, 2)),
			reads("The pool starts before measure 1.", () -> session(16, TGPracticeUnitKind.BEAT, 0, 1, 1, 1)),
			reads("The pool starts before page 1.", () -> session(16, TGPracticeUnitKind.PAGE, 0, 1, 1, 1)),
			reads("The passage starts before page 1.", () -> clickUp(16, TGPracticeUnitKind.PAGE, 0, 1)),
			reads("The passage reaches past the song's 1 line.", () -> clickUp(4, TGPracticeUnitKind.LINE, 1, 2)));
	}

	// a block of lines that would come twice, named in the language too; a pool past the song's end, its measures
	// counted in the language too; a ladder in bpm that starts below 1 bpm, the tempo said in the language too
	@TestFactory
	public Stream<DynamicTest> aRefusalIsSaidInTuxGuitarsLanguage() {
		return Stream.of(
			reads("Der Block der Zeilen 1 bis 2 käme zweimal hintereinander.", () -> session(Map.of("transport.practice.refusal.repeat",
				"{0} käme zweimal hintereinander.", "transport.practice.refusal.block.line", "Der Block der Zeilen {0} bis {1}"), PracticeSongs.song(16),
				TGPracticeUnitKind.LINE, 1, 2, 2, 2)),
			reads("Der Pool reicht über die 16 Takte des Songs hinaus.", () -> session(Map.of("transport.practice.refusal.pool-past-end",
				"Der Pool reicht über die {0} {1} des Songs hinaus.", "transport.practice.nouns.measure", "Takte"), PracticeSongs.song(16),
				TGPracticeUnitKind.MEASURE, 1, 17, 1, 1)),
			reads("Die Leiter beginnt unter 1 Schläge/min.", () -> belowOneBpm(Map.of("transport.click-up.refusal.start", "Die Leiter beginnt unter {0}.",
				"transport.click-up.tempo.bpm", "{0} Schläge/min"))));
	}
}
