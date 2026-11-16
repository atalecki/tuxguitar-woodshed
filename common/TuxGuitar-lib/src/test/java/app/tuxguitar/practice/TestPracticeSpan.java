package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTimeSignature;

/**
 * What a turn plays of the song (TGPracticeSpan), in the song's ticks: beats from the first's start to the last's end
 * (the joint), then the joint beat, the next one in score order, as long as its own measure's beat; whole measures, and
 * a song's last beat, have none. The songs have measures of 4/4 at 120 bpm (3840 ticks), the first from tick 960.
 */
public class TestPracticeSpan {

	private TGSong song = PracticeSongs.song(4);

	private TGMeasureHeader header(int number) {
		return song.getMeasureHeader(number - 1);
	}

	private TGPracticeUnit beat(int measure, int beat) {
		return TGPracticeUnit.beat(header(measure), beat);
	}

	private TGPracticeUnit measure(int number) throws TGPracticeRefusal {
		return TGPracticeGrouping.of(2, 1).unit(TGPracticeUnitKind.MEASURE, number, song.countMeasureHeaders());
	}

	private TGPracticeSpan beats(int measure, int beat) {
		return TGPracticeSpan.of(song, beat(measure, beat), beat(measure, beat));
	}

	// "first-last start joint end [headers]", the headers by number
	private static String span(TGPracticeSpan span) {
		return span.getFirst().getNumber() + "-" + span.getLast().getNumber() + " " + span.getStart() + " " + span.getJoint() + " " + span.getEnd() + " "
			+ span.headers().stream().map(TGMeasureHeader::getNumber).collect(Collectors.toList());
	}

	// whether the span plays each tick, written T or F
	private static String plays(TGPracticeSpan span, long... ticks) {
		return LongStream.of(ticks).mapToObj(tick -> span.plays(tick) ? "T" : "F").collect(Collectors.joining());
	}

	// gives the measure that time signature, the measures after it moved to follow it
	private void signature(int measure, int numerator, int denominator) {
		TGSongManager manager = new TGSongManager();
		TGTimeSignature signature = manager.getFactory().newTimeSignature();
		signature.setNumerator(numerator);
		signature.getDenominator().setValue(denominator);
		manager.changeTimeSignature(song, header(measure), signature, false);
	}

	@Test
	public void aBeatOf4Over4IsAQuarterAndItsJointTheNextQuarter() {
		assertEquals("1-1 1920 2880 3840 [1] | 1-2 2880 6720 7680 [1, 2] | 1-1 3840 4800 5760 [1, 2]",
			span(beats(1, 2)) + " | " + span(TGPracticeSpan.of(song, beat(1, 3), beat(2, 2))) + " | " + span(beats(1, 4)));
	}

	// measure 2 in 6/8 (2880 ticks), measure 3 back in 4/4 from tick 7680
	@Test
	public void theJointIsTheBeatOfItsOwnMeasuresTimeSignature() {
		signature(2, 6, TGDuration.EIGHTH);
		signature(3, 4, TGDuration.QUARTER);
		TGPracticeSpan intoEighths = beats(1, 4);
		assertEquals("1-1 3840 4800 5280 [1, 2] 960 480 0 3840 4800 | 2-2 7200 7680 8640 [2, 3]",
			span(intoEighths) + " " + intoEighths.ticksIn(header(1)) + " " + intoEighths.ticksIn(header(2)) + " " + intoEighths.ticksIn(header(3)) + " "
				+ intoEighths.startIn(header(1)) + " " + intoEighths.startIn(header(2)) + " | " + span(beats(2, 6)));
	}

	// measure 2 opens a repeat, 3 is its first ending, 4 its second: after the first ending comes the second in the score
	@Test
	public void theJointAfterAFirstEndingIsTheSecondEndingsFirstBeat() {
		header(2).setRepeatOpen(true);
		header(3).setRepeatAlternative(1);
		header(3).setRepeatClose(1);
		header(4).setRepeatAlternative(2);
		assertEquals("3-3 11520 12480 13440 [3, 4]", span(beats(3, 4)));
	}

	@Test
	public void theSongsLastBeatHasNoJoint() {
		song = PracticeSongs.song(2);
		TGPracticeSpan last = beats(2, 4);
		assertEquals("2-2 7680 8640 8640 [2] FTTF 960", span(last) + " " + plays(last, 7679, 7680, 8639, 8640) + " " + last.cut(7680, 1920));
	}

	@Test
	public void aBeatPlaysItsTicksAndTheJointsFirst() {
		assertEquals("FTTTFF", plays(beats(1, 2), 1919, 1920, 2879, 2880, 2881, 3840));
	}

	// a note sounds to the joint, a note on the joint to the joint's end, a shorter one its own length
	@Test
	public void aBeatsNotesAreCutAtTheJointAndTheJointsAtItsEnd() {
		TGPracticeSpan two = beats(1, 2);
		assertEquals("240 960 480 960 480", two.cut(1920, 240) + " " + two.cut(1920, 3840) + " " + two.cut(2400, 960) + " " + two.cut(2880, 1920) + " " + two.cut(2880, 480));
	}

	@Test
	public void wholeMeasuresPlayToTheirLastsEndWithoutAJoint() throws TGPracticeRefusal {
		TGPracticeSpan measures = TGPracticeSpan.of(song, measure(2), measure(3));
		assertEquals("2-3 4800 12480 12480 [2, 3] FTTF 960 3840 8640 0", span(measures) + " " + plays(measures, 4799, 4800, 12479, 12480) + " " + measures.cut(11520, 1920)
			+ " " + measures.ticksIn(header(2)) + " " + measures.startIn(header(3)) + " " + measures.ticksIn(header(4)));
	}

	// a line of 2 measures is its measures
	@Test
	public void aLineIsTheSpanOfItsMeasures() throws TGPracticeRefusal {
		TGPracticeUnit line = TGPracticeGrouping.of(2, 1).unit(TGPracticeUnitKind.LINE, 2, 4);
		assertEquals("3-4 8640 16320 16320 [3, 4] | 3-4 8640 16320 16320 [3, 4]",
			span(TGPracticeSpan.of(song, line, line)) + " | " + span(TGPracticeSpan.measures(header(3), header(4))));
	}
}
