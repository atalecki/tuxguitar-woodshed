package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.models.TGTempo;

/**
 * A step's tempo is an exact fraction of the written one: p% plays the song at p/100 of it, b bpm at b over the written
 * bpm it is read against. A quarter note then lasts the written microseconds over that fraction, rounded down as the
 * player's sequence rounds the Percentage of Tempo, and is heard as so many bpm, rounded to the nearest.
 */
public class TestTempoScale {

	private static TGTempo tempo(int bpm) {
		TGTempo tempo = new TGFactory().newTempo();
		tempo.setQuarterValue(bpm);
		return tempo;
	}

	// the sequence's own formula for a quarter note at the Percentage of Tempo (MidiSequenceParser.usq)
	private static int sequenceUsq(TGTempo tempo, int percent) {
		return (int) (tempo.getInUSQ() * 100.00 / percent);
	}

	@Test
	public void aPercentLastsAsLongAsThePercentageOfTempoDoesForEveryPercentAndTempo() {
		int differ = 0;
		for (int bpm = 1; bpm <= 400; bpm++) {
			TGTempo tempo = tempo(bpm);
			for (int percent = 1; percent <= 1000; percent++) {
				differ += (TGTempoScale.percent(percent).usq(tempo) == sequenceUsq(tempo, percent) ? 0 : 1);
			}
		}
		assertEquals(0, differ);
	}

	// 500000 microseconds a quarter at 120 bpm
	@Test
	public void aQuarterLastsTheWrittenTimeOverTheScaleRoundedDown() {
		assertEquals("1000000 666666 500000 333333 769230", TGTempoScale.percent(50).usq(tempo(120)) + " " + TGTempoScale.bpm(90, 120).usq(tempo(120))
			+ " " + TGTempoScale.bpm(120, 120).usq(tempo(120)) + " " + TGTempoScale.percent(150).usq(tempo(120)) + " " + TGTempoScale.percent(65).usq(tempo(120)));
	}

	@Test
	public void aBpmScaleIsTheBpmOverTheWrittenBpm() {
		assertEquals("45 30 90 180", TGTempoScale.bpm(45, 90).bpm(90) + " " + TGTempoScale.bpm(45, 90).bpm(60) + " " + TGTempoScale.bpm(90, 60).bpm(60)
			+ " " + TGTempoScale.bpm(90, 60).bpm(120));
	}

	// 120 at 65% is 78; 97 at 50% is 48.5, heard as 49; 97 at 33% is 32.01, heard as 32; 100 at 2/3 is 66.7, heard as 67
	@Test
	public void aTempoIsHeardAsTheNearestWholeBpm() {
		assertEquals("78 49 32 67", TGTempoScale.percent(65).bpm(120) + " " + TGTempoScale.percent(50).bpm(97) + " " + TGTempoScale.percent(33).bpm(97)
			+ " " + TGTempoScale.bpm(80, 120).bpm(100));
	}

	@Test
	public void scalesOfTheSameFractionAreEqual() {
		assertEquals("true true true", TGTempoScale.percent(50).equals(TGTempoScale.bpm(60, 120)) + " "
			+ (TGTempoScale.percent(50).hashCode() == TGTempoScale.bpm(45, 90).hashCode()) + " " + TGTempoScale.percent(150).equals(TGTempoScale.bpm(3, 2)));
	}

	@Test
	public void scalesOfAnotherFractionOrNoScaleAreNotEqual() {
		TGTempoScale half = TGTempoScale.percent(50);
		assertEquals("false false false false false", half.equals(TGTempoScale.percent(51)) + " " + half.equals(TGTempoScale.bpm(40, 120)) + " "
			+ half.equals(TGTempoScale.percent(150)) + " " + half.equals("1/2") + " " + half.equals(null));
	}

	@Test
	public void aScaleReadsAsItsFraction() {
		assertEquals("1/2 13/20 3/4 7/5 2/1", TGTempoScale.percent(50) + " " + TGTempoScale.percent(65) + " " + TGTempoScale.bpm(90, 120) + " " + TGTempoScale.bpm(140, 100)
			+ " " + TGTempoScale.bpm(2, 1));
	}

	@Test
	public void aScaleIsAboveNothing() {
		assertEquals("refused refused refused refused", refused(() -> TGTempoScale.percent(0)) + " " + refused(() -> TGTempoScale.bpm(0, 120))
			+ " " + refused(() -> TGTempoScale.bpm(60, 0)) + " " + refused(() -> TGTempoScale.percent(-5)));
	}

	private static String refused(Runnable scale) {
		try {
			scale.run();
			return "accepted";
		} catch (IllegalArgumentException refused) {
			return "refused";
		}
	}
}
