package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * A turn of interleaved practice as MidiSequenceParser makes it (parseTurn): from the turn's tick, its Count In (a click
 * a beat on the Metronome's channel, on the info track, in its first measure's time signature and tempo at the
 * Percentage of Tempo), then the unit's measures, each once in score order whatever repeats and alternate endings say,
 * moved to follow the Count In; the channels' setup only for the sequence's first turn.
 *
 * The song (ParserFixture) has 5 measures of 4/4 at 120 bpm, a quarter note on the first beat of each, fret = its
 * number (string 1: key 64 + fret); measure 2 is repeated once, measure 3 its first ending, 4 its second and 5 a third
 * that no pass reaches. The Metronome's channel is 9.
 */
public class TestSequenceParserTurn {

	private static final long MEASURE = 4 * TGDuration.QUARTER_TIME;

	private final ParserFixture song = new ParserFixture();

	public TestSequenceParserTurn() {
		for (int index = 0; index < 5; index++) {
			song.note(index, 0, TGDuration.QUARTER, 1, index + 1, 0);
		}
		song.measure(1).getHeader().setRepeatOpen(true);
		ending(3, 1);
		ending(4, 2);
		ending(5, 4);
		song.metronomeChannelId = 9;
	}

	// the measure is the ending, closing the repeat
	private void ending(int number, int alternative) {
		TGMeasureHeader header = song.measure(number - 1).getHeader();
		header.setRepeatAlternative(alternative);
		header.setRepeatClose(1);
	}

	private MidiPracticeTurn turn(int first, int last, long start, int clicks) {
		return new MidiPracticeTurn(song.song.getMeasureHeader(first - 1), song.song.getMeasureHeader(last - 1), start, clicks);
	}

	// the turn's events (MidiEventRecorder lines) of the kinds given, comma separated
	private String parsed(MidiPracticeTurn turn, boolean setup, int percent, String... kinds) {
		MidiSequenceParser parser = new MidiSequenceParser(song.song, song.songManager, MidiSequenceParser.ADD_MIXER_MESSAGES);
		parser.setMetronomeChannelId(song.metronomeChannelId);
		parser.setTempoPercent(percent);
		MidiEventRecorder recorder = new MidiEventRecorder(song.song.countTracks() + 2);
		parser.parseTurn(recorder, turn, setup);
		return String.join(", ", recorder.events(kinds));
	}

	@Test
	public void aTurnClicksItsCountInThenPlaysItsMeasuresOnceEachInScoreOrder() {
		long start = 10 * MEASURE;
		assertEquals("ts 38400 t0 4/4, tempo 38400 t0 500000, on 38400 t0 c9 k37 v95 s-1 b0, on 39360 t0 c9 k37 v95 s-1 b0, ts 40320 t0 4/4, tempo 40320 t0 500000,"
			+ " on 40320 t1 c1 k66, on 44160 t1 c1 k67, on 48000 t1 c1 k68, on 51840 t1 c1 k69",
			parsed(turn(2, 5, start, 2), false, 100, "ts", "tempo", "on").replaceAll(" v95 s1 b0", ""));
	}

	@Test
	public void aTurnWithoutCountInStartsWithItsFirstMeasure() {
		assertEquals("on 3840 t1 c1 k67 v95 s1 b0, off 4800 t1 c1 k67 v95 s1 b0",
			parsed(turn(3, 3, 3840, 0), false, 100, "on", "off"));
	}

	@Test
	public void theCountInIsInTheFirstMeasuresTimeSignatureAndTempoAtThePercentage() {
		TGMeasureHeader third = song.song.getMeasureHeader(2);
		third.getTimeSignature().setNumerator(6);
		third.getTimeSignature().getDenominator().setValue(TGDuration.EIGHTH);
		third.getTempo().setQuarterValue(90);
		assertEquals("ts 960 t0 6/8, tempo 960 t0 1333332, on 960 t0 c9 k37 v95 s-1 b0, off 1440 t0 c9 k37 v95 s-1 b0, on 1440 t0 c9 k37 v95 s-1 b0,"
			+ " off 1920 t0 c9 k37 v95 s-1 b0, ts 1920 t0 6/8, tempo 1920 t0 1333332", parsed(turn(3, 3, 960, 2), false, 50, "ts", "tempo", "on 960", "off 1440", "on 1440", "off 1920"));
	}

	// as in the song's sequence, a muted track is left out
	@Test
	public void aMutedTracksNotesAreLeftOut() {
		song.track.setMute(true);
		assertEquals("", parsed(turn(3, 3, 3840, 0), false, 100, "on"));
	}

	@Test
	public void onlyTheFirstTurnSetsTheChannelsUp() {
		assertEquals("program 960 t1 c1 25, name 960 t1 track 1 | ",
			parsed(turn(1, 1, 960, 0), true, 100, "program", "name") + " | " + parsed(turn(1, 1, 960, 0), false, 100, "program", "name", "bend"));
	}
}
