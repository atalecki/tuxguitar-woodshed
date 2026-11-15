package app.tuxguitar.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.tuxguitar.player.base.MidiTransport;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * What plays, on a sample clock that starts where playing starts: turns one after the other (see TGTurn), each its
 * Count In, if any, then its measures as they play, where each starts and how many samples it lasts at its tempo and
 * Percentage of Tempo. Playing the song is one turn. Measures are numbered from 0 across the turns. The audio device's
 * clock counts from the start of the song, so the timeline also knows the device's clock where playing starts.
 * Immutable.
 */
public final class TGTimeline {

	private final List<TGTurn> turns;
	private final List<TGPlayedMeasure> measures = new ArrayList<TGPlayedMeasure>();
	// by turn: where it starts, and its first measure (one more: the number of measures)
	private final long[] turnStarts;
	private final int[] firsts;
	// by measure: where it starts (one more: where the last turn ends) and where it ends
	private final long[] starts;
	private final long[] ends;
	private final long clock;
	private final int sampleRate;
	// samples after the last turn
	private final long runOn;

	public TGTimeline(TGSong song, List<TGPlayedMeasure> measures, int sampleRate) {
		this(song, measures, sampleRate, TGCountIn.NONE);
	}

	// the measures after the Count In: one turn
	public TGTimeline(TGSong song, List<TGPlayedMeasure> measures, int sampleRate, TGCountIn countIn) {
		this(Collections.singletonList(new TGTurn(countIn, measures)), sampleRate, song);
	}

	// the turns one after the other
	public static TGTimeline of(TGSong song, List<TGTurn> turns, int sampleRate) {
		return new TGTimeline(turns, sampleRate, song);
	}

	// the device's clock where playing starts: where the first measure played starts, less what plays before it
	private TGTimeline(List<TGTurn> turns, int sampleRate, TGSong song) {
		this(turns, sampleRate, clockAt(song, turns, sampleRate), 0);
	}

	private TGTimeline(List<TGTurn> turns, int sampleRate, long clock, long runOn) {
		this.turns = new ArrayList<TGTurn>(turns);
		this.runOn = runOn;
		this.sampleRate = sampleRate;
		this.clock = clock;
		this.turnStarts = new long[turns.size()];
		this.firsts = new int[turns.size() + 1];
		turns.forEach(turn -> this.measures.addAll(turn.getMeasures()));
		this.starts = new long[this.measures.size() + 1];
		this.ends = new long[this.measures.size()];
		long at = 0;
		for (int turn = 0; turn < turns.size(); turn++) {
			this.turnStarts[turn] = at;
			this.firsts[turn + 1] = this.firsts[turn] + turns.get(turn).getMeasures().size();
			at += turns.get(turn).getCountIn().length();
			for (int index = this.firsts[turn]; index < this.firsts[turn + 1]; index++) {
				TGPlayedMeasure measure = this.measures.get(index);
				this.starts[index] = at;
				at += samples(measure.getTo() - measure.getFrom(), measure, sampleRate);
				this.ends[index] = at;
			}
		}
		this.starts[this.measures.size()] = at;
	}

	// TuxGuitar stopped while the audio device's clock runs the samples from the clock: no measure plays
	public static TGTimeline stopped(int sampleRate, long clock, long samples) {
		return new TGTimeline(new ArrayList<TGTurn>(), sampleRate, clock, samples);
	}

	private static long clockAt(TGSong song, List<TGTurn> turns, int sampleRate) {
		long before = 0;
		for (TGTurn turn : turns) {
			before += turn.getCountIn().length();
			if (!turn.getMeasures().isEmpty()) {
				return clockAt(song, turn.getMeasures().get(0), sampleRate) - before;
			}
		}
		return 0;
	}

	// the device's clock where the played measure starts: the measures before it, whole, and its beats before where it
	// starts playing, at its tempo
	private static long clockAt(TGSong song, TGPlayedMeasure first, int sampleRate) {
		long clock = samples(first.getFrom(), first, sampleRate);
		for (int index = 0; index < first.getHeader().getNumber() - 1; index++) {
			TGMeasureHeader header = song.getMeasureHeader(index);
			clock += TGSampleTime.samples(header.getLength(), TGSampleTime.tempo(header, first.getPercent()), sampleRate);
		}
		return clock;
	}

	private static int samples(long ticks, TGPlayedMeasure measure, int sampleRate) {
		return TGSampleTime.samples(ticks, measure.getTempo(), sampleRate);
	}

	// the same, running on after its last turn for the samples (as long as an export's longest tail)
	public TGTimeline runningOn(long samples) {
		return new TGTimeline(this.turns, this.sampleRate, this.clock, samples);
	}

	// what plays before the sample: the turns that start before it, each its Count In up to it and its measures that
	// start before it, each up to the first of its ticks that sounds at the sample or after (its end, for a measure that
	// ends before the sample)
	public TGTimeline until(long sample) {
		List<TGTurn> kept = new ArrayList<TGTurn>();
		for (int turn = 0; turn < this.turns() && this.turnStart(turn) < sample; turn++) {
			List<TGPlayedMeasure> measures = new ArrayList<TGPlayedMeasure>();
			for (int index = this.firstOf(turn); index < this.firstOf(turn + 1) && this.start(index) < sample; index++) {
				measures.add(this.get(index).until(this.tickAt(index, sample)));
			}
			kept.add(new TGTurn(this.turns.get(turn).getCountIn().until(sample - this.turnStart(turn)), measures));
		}
		return new TGTimeline(kept, this.sampleRate, this.clock, 0);
	}

	// the first tick of the played measure that sounds at the sample or after
	private long tickAt(int index, long sample) {
		long low = this.get(index).getFrom();
		long high = this.get(index).getTo();
		while (low < high) {
			long middle = (low + high) / 2;
			if (this.sampleAt(index, middle) < sample) {
				low = middle + 1;
			} else {
				high = middle;
			}
		}
		return low;
	}

	public int getSampleRate() {
		return this.sampleRate;
	}

	// the audio device's clock where playing starts
	public long getClock() {
		return this.clock;
	}

	// the Count In's clicks, every turn's
	public TGClicks getCountIn() {
		List<TGClicks> clicks = new ArrayList<TGClicks>();
		for (int turn = 0; turn < this.turns(); turn++) {
			clicks.add(this.turns.get(turn).getCountIn().clicks(this.turnStart(turn)));
		}
		return TGClicks.join(clicks);
	}

	public int turns() {
		return this.turns.size();
	}

	// where the turn starts: its Count In, or its first measure without one
	public long turnStart(int turn) {
		return this.turnStarts[turn];
	}

	// where the turn ends: where the next one starts, or the last one ends
	public long turnEnd(int turn) {
		return (turn + 1 < this.turns() ? this.turnStart(turn + 1) : this.start(this.count()));
	}

	// the index of the turn's first measure; of the turn after the last, the number of measures
	public int firstOf(int turn) {
		return this.firsts[turn];
	}

	public int count() {
		return this.measures.size();
	}

	public TGPlayedMeasure get(int index) {
		return this.measures.get(index);
	}

	// where the played measure starts; of the one after the last, where the last turn ends
	public long start(int index) {
		return this.starts[index];
	}

	public long end(int index) {
		return this.ends[index];
	}

	public long length() {
		return this.start(this.count()) + this.runOn;
	}

	// what the song does over the block of frames from the sample, as plugins are told: a span from its first frame and
	// one from where each played measure is told from in it; a turn's first measure is told from where the turn starts:
	// in its Count In its state runs back; after the measures the last one's runs on; without measures the song is
	// stopped. A timeline is an offline clock: it waits for each block
	public void tell(MidiTransport transport, long start, int frames) {
		transport.setOffline(true);
		if (this.count() == 0) {
			transport.stop();
			return;
		}
		transport.clear();
		int index = 0;
		while (index + 1 < this.count() && this.toldFrom(index + 1) <= start) {
			index++;
		}
		this.tell(transport, index, start, start);
		for (index++; index < this.count() && this.toldFrom(index) < start + frames; index++) {
			this.tell(transport, index, start, this.toldFrom(index));
		}
	}

	// where the played measure is told from: where its turn starts, for the turn's first
	private long toldFrom(int index) {
		int turn = this.turnOf(index);
		return (index == this.firstOf(turn) ? this.turnStart(turn) : this.start(index));
	}

	// the turn the played measure is in
	private int turnOf(int index) {
		int turn = 0;
		while (this.firstOf(turn + 1) <= index) {
			turn++;
		}
		return turn;
	}

	// the span of the played measure from the sample
	private void tell(MidiTransport transport, int index, long block, long sample) {
		TGPlayedMeasure measure = this.get(index);
		TGMeasureHeader header = measure.getHeader();
		long into = sample - this.start(index);
		double ticks = header.getStart() - TGDuration.QUARTER_TIME + measure.getFrom() + into * measure.getTempo() * TGDuration.QUARTER_TIME / (60.0 * this.sampleRate);
		long samples = clockAt(header.getSong(), measure, this.sampleRate) + into;
		transport.add((int) (sample - block), true, measure.getTempo(), header.getTimeSignature().getNumerator(), header.getTimeSignature().getDenominator().getValue(),
			ticks / TGDuration.QUARTER_TIME, samples);
	}

	// where a tick of the played measure sounds, counted from where the measure starts playing
	public long sampleAt(int index, long tick) {
		return this.starts[index] + samples(tick - this.measures.get(index).getFrom(), this.measures.get(index), this.sampleRate);
	}
}
