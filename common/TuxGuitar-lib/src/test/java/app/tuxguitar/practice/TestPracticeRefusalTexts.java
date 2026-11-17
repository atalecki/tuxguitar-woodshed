package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * The words of the refusals of interleaved practice and click-up as an English user reads them, for every kind of unit:
 * a count of one unit or more ("1 measure", "2 measures"), and the unit or block a refusal names ("Line 2", "The block of
 * lines 1–2", "The block of measure 3 beat 1 to measure 3 beat 4"). The songs are of 4/4, 4 measures a line, 4 lines a
 * page.
 */
public class TestPracticeRefusalTexts {

	private final TGPracticeSettings settings = new TGPracticeSettings();

	// a session of so many turns over the pool of the kind from to, blocks of so many units, in the song
	private String session(TGSong song, TGPracticeUnitKind kind, int from, int to, int unitsPerBlock, int turns) {
		settings.setUnit(kind);
		settings.setPool(from, to);
		settings.setUnitsPerBlock(unitsPerBlock);
		settings.setTurns(turns);
		return PracticeRefusals.outcome(() -> TGPracticeSession.start(settings, song));
	}

	// a click-up of the passage of the kind from to, in a song of so many measures
	private static String clickUp(int measures, TGPracticeUnitKind kind, int from, int to) {
		TGClickUpSettings clickUp = new TGClickUpSettings();
		clickUp.setUnit(kind);
		clickUp.setPassageFrom(from);
		clickUp.setPassageTo(to);
		TGSong song = PracticeSongs.song(measures);
		return PracticeRefusals.outcome(() -> TGClickUpLadder.of(clickUp, song));
	}

	@Test
	public void aBlockNeedsAtLeastOneUnitOfItsKind() {
		assertEquals("A block needs at least 1 line. | A block needs at least 1 page.", session(PracticeSongs.song(16), TGPracticeUnitKind.LINE, 1, 2, 0, 1)
			+ " | " + session(PracticeSongs.song(16), TGPracticeUnitKind.PAGE, 1, 1, 0, 1));
	}

	@Test
	public void aBlockThatDoesNotFitCountsItsUnitsOneOrMore() {
		assertEquals("A block of 2 measures does not fit in the pool's 1 measure. | A block of 3 lines does not fit in the pool's 2 lines. | "
			+ "A block of 2 pages does not fit in the pool's 1 page.", session(PracticeSongs.song(16), TGPracticeUnitKind.MEASURE, 1, 1, 2, 1) + " | "
			+ session(PracticeSongs.song(16), TGPracticeUnitKind.LINE, 1, 2, 3, 1) + " | " + session(PracticeSongs.song(16), TGPracticeUnitKind.PAGE, 1, 1, 2, 1));
	}

	// measure 2 of 1/4: a pool of one beat
	@Test
	public void aRefusalNamesTheUnitThatWouldComeTwice() {
		TGSong song = PracticeSongs.song(4);
		PracticeSongs.signature(song, 2, 1, 4);
		assertEquals("Line 2 would come twice in a row. | Page 1 would come twice in a row. | Measure 2 beat 1 would come twice in a row.",
			session(PracticeSongs.song(16), TGPracticeUnitKind.LINE, 2, 2, 1, 2) + " | " + session(PracticeSongs.song(16), TGPracticeUnitKind.PAGE, 1, 1, 1, 2)
				+ " | " + session(song, TGPracticeUnitKind.BEAT, 2, 2, 1, 2));
	}

	@Test
	public void aRefusalNamesTheBlockThatWouldComeTwiceByItsFirstAndLastUnit() {
		assertEquals("The block of lines 1–2 would come twice in a row. | The block of pages 1–2 would come twice in a row. | "
			+ "The block of measure 3 beat 1 to measure 3 beat 4 would come twice in a row.", session(PracticeSongs.song(16), TGPracticeUnitKind.LINE, 1, 2, 2, 2)
			+ " | " + session(PracticeSongs.song(32), TGPracticeUnitKind.PAGE, 1, 2, 2, 2) + " | " + session(PracticeSongs.song(16), TGPracticeUnitKind.BEAT, 3, 3, 4, 2));
	}

	@Test
	public void aPoolOrPassageIsRefusedInTheKindItIsGivenIn() {
		assertEquals("The pool starts before measure 1. | The pool starts before page 1. | The passage starts before page 1. | "
			+ "The passage reaches past the song's 1 line.", session(PracticeSongs.song(16), TGPracticeUnitKind.BEAT, 0, 1, 1, 1) + " | "
			+ session(PracticeSongs.song(16), TGPracticeUnitKind.PAGE, 0, 1, 1, 1) + " | " + clickUp(16, TGPracticeUnitKind.PAGE, 0, 1) + " | "
			+ clickUp(4, TGPracticeUnitKind.LINE, 1, 2));
	}
}
