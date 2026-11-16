package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import app.tuxguitar.player.base.MidiPlayerMode;
import app.tuxguitar.player.base.MidiRepeatController;
import app.tuxguitar.practice.TGPracticeSpan;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * Song time: the measures as the song plays them, following repeats and alternate endings, as the MIDI sequence
 * parser does. With Play looped on, a pass is one time through the Loop Range.
 */
public final class TGSongTime {

	private TGSongTime() {
	}

	// the measures played from start until stop is reached in the last pass, or until that pass ends
	public static List<TGPlayedMeasure> play(TGSong song, MidiPlayerMode mode, long start, long stop, int passes) {
		List<TGMeasureHeader> pass = pass(song, mode);
		List<TGMeasureHeader> sequence = Collections.nCopies(passes, pass).stream().flatMap(List::stream).collect(Collectors.toList());
		int first = indexAt(sequence, start, 0);
		if (first < 0) {
			return new ArrayList<TGPlayedMeasure>();
		}
		int stopIndex = indexAt(sequence, stop, Math.max(first, sequence.size() - pass.size()));
		int last = (stopIndex < 0 ? sequence.size() - 1 : stopIndex);
		List<TGPlayedMeasure> played = new ArrayList<TGPlayedMeasure>();
		TGTempoScale[] scales = scales(mode, passes);
		for (int i = first; i <= last; i++) {
			TGPlayedMeasure measure = played(sequence, i, first, last, start, stop, scales[i / pass.size()], pass.size());
			if (measure.getTo() > measure.getFrom()) {
				played.add(measure);
			}
		}
		return played;
	}

	// the measure of the sequence at i: the first from start on, the last until stop when it holds it; a measure that
	// does not follow the one before it starts a stretch
	private static TGPlayedMeasure played(List<TGMeasureHeader> sequence, int i, int first, int last, long start, long stop, TGTempoScale scale, int passLength) {
		TGMeasureHeader header = sequence.get(i);
		long from = (i == first ? start - header.getStart() : 0);
		long to = (i == last ? Math.min(header.getLength(), stop - header.getStart()) : header.getLength());
		boolean jump = (i > first && header.getNumber() != sequence.get(i - 1).getNumber() + 1);
		return new TGPlayedMeasure(header, from, to, jump, i / passLength, scale, i % passLength);
	}

	// a unit of interleaved practice in its turn (from 0, its pass): what its span plays of each of its measures (whole
	// measures; a beat and its joint, parts of one or two), each once and in score order, whatever repeats and alternate
	// endings say, at the tempo scale; each where the song as written first plays it (its step), the first one starting a
	// stretch in a turn after the first. A measure the song never plays (an alternate ending no pass reaches) plays too,
	// at TGPlayedMeasure.NO_STEP (List.indexOf's -1)
	public static List<TGPlayedMeasure> unit(TGSong song, TGPracticeSpan span, int turn, TGTempoScale scale) {
		List<TGMeasureHeader> written = pass(song, new MidiPlayerMode());
		List<TGPlayedMeasure> unit = new ArrayList<TGPlayedMeasure>();
		for (TGMeasureHeader header : span.headers()) {
			long from = span.startIn(header) - header.getStart();
			unit.add(new TGPlayedMeasure(header, from, from + span.ticksIn(header), turn > 0 && unit.isEmpty(), turn, scale, written.indexOf(header)));
		}
		return unit;
	}

	// the Percentage of Tempo of each pass: Training Mode raises it after each pass, as the player does
	private static TGTempoScale[] scales(MidiPlayerMode mode, int passes) {
		MidiPlayerMode copy = new MidiPlayerMode();
		copy.copyFrom(mode);
		TGTempoScale[] scales = new TGTempoScale[passes];
		for (int pass = 0; pass < passes; pass++) {
			scales[pass] = TGTempoScale.percent(copy.getCurrentPercent());
			copy.notifyLoop();
		}
		return scales;
	}

	// one pass, in song order: the whole song, or the Loop Range when Play looped is on
	private static List<TGMeasureHeader> pass(TGSong song, MidiPlayerMode mode) {
		List<TGMeasureHeader> headers = new ArrayList<TGMeasureHeader>();
		walk(song, mode, (index, move) -> headers.add(song.getMeasureHeader(index)));
		return headers;
	}

	// for each step of a pass, how far the MIDI sequence parser moves the measure's ticks (repeats are unrolled)
	public static List<Long> moves(TGSong song, MidiPlayerMode mode) {
		List<Long> moves = new ArrayList<Long>();
		walk(song, mode, (index, move) -> moves.add(move));
		return moves;
	}

	// the measures of a pass as the MIDI sequence parser walks them
	private static void walk(TGSong song, MidiPlayerMode mode, java.util.function.BiConsumer<Integer, Long> played) {
		MidiRepeatController controller = (mode.isLoop() ? new MidiRepeatController(song, mode.getLoopSHeader(), mode.getLoopEHeader()) : new MidiRepeatController(song, -1, -1));
		while (!controller.finished()) {
			int index = controller.getIndex();
			long move = controller.getRepeatMove();
			controller.process();
			if (controller.shouldPlay()) {
				played.accept(index, move);
			}
		}
	}

	// the first measure of the sequence, from the given index on, that holds the tick; -1 when none does
	private static int indexAt(List<TGMeasureHeader> sequence, long tick, int fromIndex) {
		for (int i = fromIndex; i < sequence.size(); i++) {
			TGMeasureHeader header = sequence.get(i);
			if (tick >= header.getStart() && tick < header.getStart() + header.getLength()) {
				return i;
			}
		}
		return -1;
	}
}
