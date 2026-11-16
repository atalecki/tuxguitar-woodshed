package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.player.base.MidiSoundingPosition;
import app.tuxguitar.song.models.TGDuration;

/**
 * Where the player reports the song is (the played-beat highlight, the caret) while a timeline plays, as it reports it
 * live: the tick the output sounds, the delay (plugin delay compensation and the audio device's output latency) behind
 * the clock. The timeline anchors the position as the live sequencer anchors its MidiSoundingPosition, in song ticks:
 * where each turn's break starts (where the turn starts, without one) it stays on the turn's first tick through its
 * break and Count In (live, a Count In is a timer before the sequencer runs; a break is the start of its turn), and from where each played measure starts (where the tempo may change and the song may jump, as a
 * loop's jump back) it moves on at its tempo. Before the first anchor the position is that anchor's run back (before
 * the output sounds where playing starts, without a Count In); with no measure played it is 0. Immutable.
 */
public final class TGReportedPositions {

	private final List<Anchor> anchors = new ArrayList<Anchor>();
	private final int delay;

	private TGReportedPositions(TGTimeline timeline, int delay) {
		this.delay = delay;
		for (int turn = 0; turn < timeline.turns(); turn++) {
			if (timeline.firstOf(turn) < timeline.firstOf(turn + 1)) {
				this.anchor(new Anchor(timeline.breakStart(turn), firstTick(timeline.get(timeline.firstOf(turn))), 0, 1));
			}
			for (int index = timeline.firstOf(turn); index < timeline.firstOf(turn + 1); index++) {
				TGPlayedMeasure measure = timeline.get(index);
				this.anchor(new Anchor(timeline.start(index), firstTick(measure), measure.getTempo() * TGDuration.QUARTER_TIME, 60.0 * timeline.getSampleRate()));
			}
		}
	}

	// where the timeline reports the song is, the delay behind its clock
	public static TGReportedPositions at(TGTimeline timeline, int delay) {
		return new TGReportedPositions(timeline, delay);
	}

	// the tick reported at the end of each block of the timeline (the last one may run past its end)
	public static long[] of(TGTimeline timeline, int delay) {
		TGReportedPositions positions = at(timeline, delay);
		long[] reported = new long[(int) ((timeline.length() + TGBlocks.FRAMES - 1) / TGBlocks.FRAMES)];
		for (int block = 0; block < reported.length; block++) {
			reported[block] = positions.tickAt((block + 1L) * TGBlocks.FRAMES);
		}
		return reported;
	}

	// the measure's first tick played, in song ticks
	private static long firstTick(TGPlayedMeasure measure) {
		return measure.getHeader().getStart() + measure.getFrom();
	}

	// an anchor on the frame of the one before replaces it, as in a MidiSoundingPosition
	private void anchor(Anchor anchor) {
		if (!this.anchors.isEmpty() && this.anchors.get(this.anchors.size() - 1).frame == anchor.frame) {
			this.anchors.remove(this.anchors.size() - 1);
		}
		this.anchors.add(anchor);
	}

	// the tick reported when the timeline's clock reaches the sample: the output sounds the frame the delay before it,
	// from the anchors of the frames the clock has played (before the sample), as live, where the sequencer anchors a
	// block as it plays it
	public long tickAt(long sample) {
		if (this.anchors.isEmpty()) {
			return 0;
		}
		long frame = sample - this.delay;
		Anchor anchor = this.anchors.get(this.lastAtOrBefore(Math.min(frame, sample - 1)));
		MidiSoundingPosition sounding = new MidiSoundingPosition();
		sounding.restart(anchor.frame, anchor.tick, anchor.ticks, anchor.frames);
		return sounding.tickAt(frame);
	}

	// the index of the last anchor on the frame or before it; the first when none is
	private int lastAtOrBefore(long frame) {
		int low = 1;
		int high = this.anchors.size();
		while (low < high) {
			int middle = (low + high) >>> 1;
			if (this.anchors.get(middle).frame <= frame) {
				low = middle + 1;
			} else {
				high = middle;
			}
		}
		return low - 1;
	}

	// from the frame on, the song is at the tick and moves on so many ticks every so many frames
	private static final class Anchor {
		private final long frame;
		private final long tick;
		private final double ticks;
		private final double frames;

		Anchor(long frame, long tick, double ticks, double frames) {
			this.frame = frame;
			this.tick = tick;
			this.ticks = ticks;
			this.frames = frames;
		}
	}
}
