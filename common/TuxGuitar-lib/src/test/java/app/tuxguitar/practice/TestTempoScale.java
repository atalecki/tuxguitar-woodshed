package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.models.TGTempo;

/**
 * A step's tempo is an exact fraction of the written one: p% plays the song at p/100 of it, b bpm at b over the written
 * bpm it is read against. A quarter note then lasts the written microseconds over that fraction, rounded down as the
 * player's sequence rounds the Percentage of Tempo, and is heard as so many bpm, rounded to the nearest. Unrounded, the
 * audio's sample time hears it, and the session's schedule reckons so many ticks of it, rounded down at the end.
 */
public class TestTempoScale {

	private static TGTempo tempo(int bpm) {
		TGTempo tempo = new TGFactory().newTempo();
		tempo.setQuarterValue(bpm);
		return tempo;
	}

	// the sequence's own formula for a quarter note at the Percentage of Tempo (MidiSequenceParser.usq, before it read a
	// scale)
	private static int sequenceUsq(TGTempo tempo, int percent) {
		return (int) (tempo.getInUSQ() * 100.00 / percent);
	}

	// for how many written tempos (1 to 400 bpm) and percents (1 to 1000) a percent scale reads otherwise than the
	// Percentage of Tempo did
	private static int differ(BiPredicate<TGTempo, Integer> same) {
		int differ = 0;
		for (int bpm = 1; bpm <= 400; bpm++) {
			TGTempo tempo = tempo(bpm);
			for (int percent = 1; percent <= 1000; percent++) {
				differ += (same.test(tempo, percent) ? 0 : 1);
			}
		}
		return differ;
	}

	@Test
	public void aPercentLastsAsLongAsThePercentageOfTempoDoesForEveryPercentAndTempo() {
		assertEquals(0, differ((tempo, percent) -> TGTempoScale.percent(percent).usq(tempo) == sequenceUsq(tempo, percent)));
	}

	// the audio's formula (TGSampleTime.tempo, before it read a scale)
	@Test
	public void aPercentIsHeardAtTheTempoTimesThePercentageForEveryPercentAndTempo() {
		assertEquals(0, differ((tempo, percent) -> TGTempoScale.percent(percent).exactBpm(tempo.getQuarterValue()) == tempo.getQuarterValue() * percent / 100.0));
	}

	// the schedule's formula (TGPracticeSchedule.micros, before it read a scale), for a 4/4 measure and an odd few ticks
	@Test
	public void aPercentLastsAsLongAsTheScheduleReckonedForEveryPercentAndTempo() {
		assertEquals(0, differ((tempo, percent) -> scheduled(3840, tempo, percent) && scheduled(7, tempo, percent)));
	}

	private static boolean scheduled(long ticks, TGTempo tempo, int percent) {
		return TGTempoScale.percent(percent).micros(ticks, tempo) == ticks * 60000000L * 100 / (960L * tempo.getQuarterValue() * percent);
	}

	// 120 at 65% is 78 exactly; 97 at 65% is 63.05; 120 at 90 bpm is 90; 100 at 2/3 is 66.67, not rounded
	@Test
	public void aTempoIsHeardExactlyBeforeRounding() {
		assertEquals("78.0 63.05 90.0 66.66666666666667", TGTempoScale.percent(65).exactBpm(120) + " " + TGTempoScale.percent(65).exactBpm(97) + " "
			+ TGTempoScale.bpm(90, 120).exactBpm(120) + " " + TGTempoScale.bpm(80, 120).exactBpm(100));
	}

	// a quarter (960 ticks) at 120 bpm: 1 s at 50%, 666666.67 at 90 bpm; a 4/4 measure at 150%: 1333333.33
	@Test
	public void ticksLastTheWrittenTimeOverTheScaleRoundedDownOnce() {
		assertEquals("1000000 666666 1333333", TGTempoScale.percent(50).micros(960, tempo(120)) + " " + TGTempoScale.bpm(90, 120).micros(960, tempo(120)) + " "
			+ TGTempoScale.percent(150).micros(3840, tempo(120)));
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
