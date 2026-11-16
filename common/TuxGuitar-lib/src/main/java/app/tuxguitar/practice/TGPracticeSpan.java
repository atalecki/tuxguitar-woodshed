package app.tuxguitar.practice;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * What a turn plays of the song, in the song's ticks, whatever repeats and alternate endings say: its units, from the
 * start of the first (start) to the end of the last (joint), then the joint, the beat after them in score order (to the
 * end), as long as a beat of its own measure, whose notes starting on it sound to its end. Whole measures (a measure, a
 * line, a page) have no joint, nor does the song's last beat: the joint is then the end. The one place that decides
 * where a beat and its joint are. Immutable.
 */
public final class TGPracticeSpan {

	private final TGMeasureHeader first;
	private final long start;
	private final long joint;
	private final long end;

	private TGPracticeSpan(TGMeasureHeader first, long start, long joint, long end) {
		this.first = first;
		this.start = start;
		this.joint = joint;
		this.end = end;
	}

	// the units from one to the other (the same kind, from in score order before to): beats, then their joint; others,
	// their whole measures
	public static TGPracticeSpan of(TGSong song, TGPracticeUnit from, TGPracticeUnit to) {
		TGMeasureHeader first = header(song, from.getFirst());
		if (from.getKind() != TGPracticeUnitKind.BEAT) {
			return measures(first, header(song, to.getLast()));
		}
		TGMeasureHeader last = header(song, to.getFirst());
		long joint = beatStart(last, to.getNumber() + 1);
		return new TGPracticeSpan(first, beatStart(first, from.getNumber()), joint, joint + jointLength(song, last, to.getNumber()));
	}

	// the measures from the first to the last, whole, without a joint
	public static TGPracticeSpan measures(TGMeasureHeader first, TGMeasureHeader last) {
		long end = last.getStart() + last.getLength();
		return new TGPracticeSpan(first, first.getStart(), end, end);
	}

	private static TGMeasureHeader header(TGSong song, int number) {
		return song.getMeasureHeader(number - 1);
	}

	// where the beat (from 1; one past the last: where the measure ends) of the measure starts
	private static long beatStart(TGMeasureHeader header, int beat) {
		return header.getStart() + (beat - 1) * beatLength(header);
	}

	private static long beatLength(TGMeasureHeader header) {
		return header.getTimeSignature().getDenominator().getTime();
	}

	// the length of the beat after the measure's beat, in score order: one of the same measure, or of the next's first;
	// none after the song's last
	private static long jointLength(TGSong song, TGMeasureHeader last, int beat) {
		if (beat < last.getTimeSignature().getNumerator()) {
			return beatLength(last);
		}
		if (last.getNumber() < song.countMeasureHeaders()) {
			return beatLength(header(song, last.getNumber() + 1));
		}
		return 0;
	}

	// the measure its first unit starts in
	public TGMeasureHeader getFirst() {
		return this.first;
	}

	// the measure its last unit ends in (the joint's aside)
	public TGMeasureHeader getLast() {
		return this.headers().stream().filter(header -> header.getStart() < this.joint).reduce((before, after) -> after).get();
	}

	// where its first unit starts
	public long getStart() {
		return this.start;
	}

	// where its last unit ends and its joint starts
	public long getJoint() {
		return this.joint;
	}

	// where its joint ends (its last unit, without one)
	public long getEnd() {
		return this.end;
	}

	// whether it plays what starts on the tick: its units' ticks, and the joint's first
	public boolean plays(long tick) {
		return (tick >= this.start && tick < this.joint) || (tick == this.joint && this.joint < this.end);
	}

	// the length a note starting on the tick sounds, at most the one given: to the joint at most, a note on the joint to
	// its end at most
	public long cut(long noteStart, long duration) {
		return Math.min(duration, (noteStart < this.joint ? this.joint : this.end) - noteStart);
	}

	// where it starts playing the measure: its start or, in its first measure, its own
	public long startIn(TGMeasureHeader header) {
		return Math.max(header.getStart(), this.start);
	}

	// the ticks of the measure it plays
	public long ticksIn(TGMeasureHeader header) {
		return Math.max(0, Math.min(this.end, header.getStart() + header.getLength()) - this.startIn(header));
	}

	// the measures it plays, in score order: its first to the one its joint (or last unit) ends in
	public List<TGMeasureHeader> headers() {
		TGSong song = this.first.getSong();
		List<TGMeasureHeader> headers = new ArrayList<TGMeasureHeader>();
		for (int number = this.first.getNumber(); number <= song.countMeasureHeaders() && header(song, number).getStart() < this.end; number++) {
			headers.add(header(song, number));
		}
		return headers;
	}
}
