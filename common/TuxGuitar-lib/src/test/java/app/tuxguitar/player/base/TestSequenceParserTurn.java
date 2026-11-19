package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.practice.TGPracticeGrouping;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSpan;
import app.tuxguitar.practice.TGPracticeUnit;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;

/**
 * A turn of interleaved practice as MidiSequenceParser makes it (parseTurn): from the turn's tick, its Count In (a click
 * a beat on the Metronome's channel, on the info track, in its first measure's time signature and tempo), then the
 * unit's measures, each once in score order whatever repeats and alternate endings say, moved to follow the Count In,
 * all at the turn's tempo scale, whatever Percentage of Tempo the parser had; the channels' setup only for the
 * sequence's first turn.
 *
 * The song (ParserFixture) has 5 measures of 4/4 at 120 bpm, a quarter note on the first beat of each, fret = its
 * number (string 1: key 64 + fret); measure 2 is repeated once, measure 3 its first ending, 4 its second and 5 a third
 * that no pass reaches. The Metronome's channel is 9. A turn of beats (TGPracticeSpan) plays the notes starting on its
 * beats and its joint, each cut at the joint or the joint's end, its measures' tempo and time signature from where it
 * plays them, the Metronome's clicks on the beats it plays.
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

	// the turn's events (MidiEventRecorder lines) of the kinds given, comma separated, by a parser set to another
	// Percentage of Tempo (80), which the turn's scale replaces
	private String parsed(MidiPracticeTurn turn, boolean setup, String... kinds) {
		return parsed(MidiSequenceParser.ADD_MIXER_MESSAGES, turn, setup, kinds);
	}

	// as parsed(turn, setup, kinds), by a parser of the flags given
	private String parsed(int flags, MidiPracticeTurn turn, boolean setup, String... kinds) {
		MidiSequenceParser parser = new MidiSequenceParser(song.song, song.songManager, flags);
		parser.setMetronomeChannelId(song.metronomeChannelId);
		parser.setTempoPercent(80);
		MidiEventRecorder recorder = new MidiEventRecorder(song.song.countTracks() + 2);
		parser.parseTurn(recorder, turn, setup);
		return String.join(", ", recorder.events(kinds));
	}

	@Test
	public void aTurnClicksItsCountInThenPlaysItsMeasuresOnceEachInScoreOrder() {
		long start = 10 * MEASURE;
		assertEquals("ts 38400 t0 4/4, tempo 38400 t0 500000, on 38400 t0 c9 k37 v95 s-1 b0, on 39360 t0 c9 k37 v95 s-1 b0, ts 40320 t0 4/4, tempo 40320 t0 500000,"
			+ " on 40320 t1 c1 k66, on 44160 t1 c1 k67, on 48000 t1 c1 k68, on 51840 t1 c1 k69",
			parsed(turn(2, 5, start, 2), false, "ts", "tempo", "on").replaceAll(" v95 s1 b0", ""));
	}

	// a break of 1920 ticks before a Count In of 2: the time signature and tempo from the break's start, the clicks after it
	@Test
	public void aTurnAfterABreakTakesItsTempoAtTheBreakAndClicksAfterIt() {
		MidiPracticeTurn turn = new MidiPracticeTurn(song.song.getMeasureHeader(2), song.song.getMeasureHeader(2), 960, 1920, 2, 2, TGTempoScale.percent(100));
		assertEquals("ts 960 t0 4/4, tempo 960 t0 500000, on 2880 t0 c9 k37 v95 s-1 b0, on 3840 t0 c9 k37 v95 s-1 b0, ts 4800 t0 4/4, tempo 4800 t0 500000, on 4800 t1 c1 k67",
			parsed(turn, false, "ts", "tempo", "on").replaceAll(" v95 s1 b0", ""));
	}

	// the notes of the turn's Count In clicks (on the info track) on a synthesizer's own drums (the built-in click)
	private String drumClicks(MidiPracticeTurn turn) {
		song.metronomeChannelId = MidiSampleClock.DRUMS;
		return java.util.Arrays.stream(parsed(turn, false, "on").split(", ")).filter(event -> event.contains(" t0 "))
			.map(event -> event.replaceAll(".* (k\\d+) .*", "$1")).collect(java.util.stream.Collectors.joining(" "));
	}

	// a turn's Count In is accented as the player's (MidiClick.isCountInAccented): counted backwards from the turn's
	// first measure, 6 clicks in 4/4 are "3 4 | 1 2 3 4"
	@Test
	public void onTheDrumsATurnsCountInAccentsTheBarLineBeforeItsFirstMeasure() {
		assertEquals("k77 k77 k76 k77 k77 k77", drumClicks(turn(2, 2, 0, 6)));
	}

	// a turn of beats from beat 3 (a click-up step's) is led into beat 3: 4 clicks in 4/4 are "3 4 | 1 2"
	@Test
	public void onTheDrumsATurnFromBeat3CountsInToBeat3() throws TGPracticeRefusal {
		assertEquals("k77 k77 k76 k77", drumClicks(beats(1, 3, 4, 960, 4)));
	}

	@Test
	public void aTurnWithoutCountInStartsWithItsFirstMeasure() {
		assertEquals("on 3840 t1 c1 k67 v95 s1 b0, off 4800 t1 c1 k67 v95 s1 b0",
			parsed(turn(3, 3, 3840, 0), false, "on", "off"));
	}

	// a turn at 50%
	@Test
	public void theCountInIsInTheFirstMeasuresTimeSignatureAndTempoAtTheTurnsScale() {
		TGMeasureHeader third = song.song.getMeasureHeader(2);
		third.getTimeSignature().setNumerator(6);
		third.getTimeSignature().getDenominator().setValue(TGDuration.EIGHTH);
		third.getTempo().setQuarterValue(90);
		assertEquals("ts 960 t0 6/8, tempo 960 t0 1333332, on 960 t0 c9 k37 v95 s-1 b0, off 1440 t0 c9 k37 v95 s-1 b0, on 1440 t0 c9 k37 v95 s-1 b0,"
			+ " off 1920 t0 c9 k37 v95 s-1 b0, ts 1920 t0 6/8, tempo 1920 t0 1333332", parsed(new MidiPracticeTurn(third, third, 960, 0, 2, 1, TGTempoScale.percent(50)), false,
			"ts", "tempo", "on 960", "off 1440", "on 1440", "off 1920"));
	}

	// as in the song's sequence, a muted track is left out
	@Test
	public void aMutedTracksNotesAreLeftOut() {
		song.track.setMute(true);
		assertEquals("", parsed(turn(3, 3, 3840, 0), false, "on"));
	}

	@Test
	public void onlyTheFirstTurnSetsTheChannelsUp() {
		assertEquals("program 960 t1 c1 25, name 960 t1 track 1 | ",
			parsed(turn(1, 1, 960, 0), true, "program", "name") + " | " + parsed(turn(1, 1, 960, 0), false, "program", "name", "bend"));
	}

	// the beats from one to the other (from 1) of the measure (from 1) and their joint: a turn of its own from the tick,
	// after so many clicks
	private MidiPracticeTurn beats(int measure, int from, int to, long start, int clicks) throws TGPracticeRefusal {
		List<TGPracticeUnit> beats = TGPracticeGrouping.of(1, 1).units(TGPracticeUnitKind.BEAT, measure, measure, song.song, "pool");
		return new MidiPracticeTurn(TGPracticeSpan.of(song.song, beats.get(from - 1), beats.get(to - 1)), start, clicks);
	}

	// measure 1 (beats from tick 960, 1920, 2880, 3840): half notes on beat 2 (string 2, key 61) and beat 3 (string 3,
	// key 58), a quarter on beat 4 (string 4). Beat 2's turn from tick 960 plays its half note cut at the joint (beat 3),
	// the joint's cut at the joint's end; not beat 1's note, nor beat 4's
	@Test
	public void aBeatsTurnPlaysItsBeatThenTheJointEachNoteCutAtItsEnd() throws TGPracticeRefusal {
		song.note(0, 4, TGDuration.HALF, 2, 2, 0);
		song.note(0, 8, TGDuration.HALF, 3, 3, 0);
		song.note(0, 12, TGDuration.QUARTER, 4, 4, 0);
		assertEquals("on 960 t1 c1 k61, off 1920 t1 c1 k61, on 1920 t1 c1 k58, off 2880 t1 c1 k58", beatTwosNotes());
	}

	// the notes measure 1's beat 2 and its joint play, from tick 960 without a Count In: "on|off TICK tTRACK cCHANNEL kKEY"
	private String beatTwosNotes() throws TGPracticeRefusal {
		return parsed(beats(1, 2, 2, 960, 0), false, "on", "off").replaceAll(" v95 s\\d b0", "");
	}

	// on beat 2 a half note from beat 1 (string 2, voice 2) still sounds, and beat 1's quarter on string 3 goes on tied;
	// beat 2's turn plays only the eighth that starts on it (string 4, key 54)
	@Test
	public void aNoteStartingBeforeABeatsTurnIsNotPlayed() throws TGPracticeRefusal {
		song.note(0, 0, TGDuration.HALF, 2, 2, 1);
		song.note(0, 0, TGDuration.QUARTER, 3, 3, 0);
		song.note(0, 4, TGDuration.QUARTER, 3, 3, 0).setTiedNote(true);
		song.note(0, 4, TGDuration.EIGHTH, 4, 4, 0);
		assertEquals("on 960 t1 c1 k54, off 1440 t1 c1 k54", beatTwosNotes());
	}

	// measure 2 at 60 bpm: beat 4's turn from tick 960 counts in 2 clicks, to 2880, where it takes measure 1's time
	// signature and tempo; its joint (measure 2's first beat, at 3840) takes measure 2's tempo
	@Test
	public void aBeatsTurnTakesEachMeasuresTempoWhereItStartsPlayingIt() throws TGPracticeRefusal {
		song.measure(1).getHeader().getTempo().setQuarterValue(60);
		assertEquals("ts 960 t0 4/4, tempo 960 t0 500000, ts 2880 t0 4/4, tempo 2880 t0 500000, tempo 3840 t0 1000000",
			parsed(beats(1, 4, 4, 960, 2), false, "ts", "tempo"));
	}

	// with the Metronome on: beats 2 and 3 of measure 1 from tick 960, a click on each and on the joint; measure 3 from
	// 3840, a click on each of its 4 beats
	@Test
	public void theMetronomeClicksOnTheBeatsATurnPlays() throws TGPracticeRefusal {
		assertEquals("960 1920 2880 | 3840 4800 5760 6720", clicks(beats(1, 2, 3, 960, 0)) + " | " + clicks(turn(3, 3, 3840, 0)));
	}

	// the ticks of the Metronome's clicks (on the sequence's last track) in the turn
	private String clicks(MidiPracticeTurn turn) {
		String events = parsed(MidiSequenceParser.ADD_MIXER_MESSAGES | MidiSequenceParser.ADD_METRONOME, turn, false, "on");
		return Stream.of(events.split(", ")).filter(event -> event.contains(" t2 ")).map(event -> event.replaceAll("on (\\d+) .*", "$1"))
			.collect(Collectors.joining(" "));
	}

	// a slide on beat 2 (string 1) to beat 4's note: beat 2's turn plays the note, but no slide past its joint
	@Test
	public void aSlideToANoteTheTurnDoesNotPlayIsLeftOut() throws TGPracticeRefusal {
		song.note(0, 4, TGDuration.QUARTER, 1, 3, 0).getEffect().setSlide(true);
		song.note(0, 12, TGDuration.QUARTER, 1, 5, 0);
		assertEquals("on 960 t1 c1 k67 v95 s1 b1", parsed(beats(1, 2, 2, 960, 0), false, "bend", "on"));
	}
}
