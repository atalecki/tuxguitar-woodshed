package app.tuxguitar.song.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.factory.TGFactory;

/**
 * A measure's difficulty flag (interleaved practice's weight): 1 (not flagged) to 5, nothing else, kept with the
 * other header fields when a header is copied or cloned.
 */
public class TestMeasureDifficulty {

	private final TGFactory factory = new TGFactory();

	@Test
	public void aNewMeasureIsNotFlagged() {
		assertEquals(1, factory.newHeader().getDifficulty());
	}

	@Test
	public void aMeasureCanBeFlaggedFromOneToFive() {
		TGMeasureHeader header = factory.newHeader();
		StringBuilder flagged = new StringBuilder();
		for (int difficulty = TGMeasureHeader.DIFFICULTY_NONE; difficulty <= TGMeasureHeader.DIFFICULTY_MAX; difficulty++) {
			header.setDifficulty(difficulty);
			flagged.append(header.getDifficulty());
		}
		assertEquals("12345", flagged.toString());
	}

	@Test
	public void aDifficultyOutsideOneToFiveIsRefusedAndChangesNothing() {
		TGMeasureHeader header = factory.newHeader();
		header.setDifficulty(3);
		assertThrows(IllegalArgumentException.class, () -> header.setDifficulty(0));
		assertThrows(IllegalArgumentException.class, () -> header.setDifficulty(6));
		assertEquals(3, header.getDifficulty());
	}

	@Test
	public void whatIsADifficulty() {
		assertEquals("false true true false", TGMeasureHeader.isDifficulty(0) + " " + TGMeasureHeader.isDifficulty(1)
			+ " " + TGMeasureHeader.isDifficulty(5) + " " + TGMeasureHeader.isDifficulty(6));
	}

	@Test
	public void aCloneKeepsTheDifficulty() {
		TGMeasureHeader header = factory.newHeader();
		header.setDifficulty(4);
		assertEquals(4, header.clone(factory).getDifficulty());
	}
}
