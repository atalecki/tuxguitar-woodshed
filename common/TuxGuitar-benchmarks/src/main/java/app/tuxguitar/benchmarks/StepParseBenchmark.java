package app.tuxguitar.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.player.base.MidiPracticeTurn;
import app.tuxguitar.player.base.MidiSequenceHandler;
import app.tuxguitar.player.base.MidiSequenceParser;
import app.tuxguitar.player.impl.sequencer.MidiSequencerImpl;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTimeSignature;
import app.tuxguitar.util.TGContext;

/**
 * Laying a step of interleaved click-up in the player's sequence, as it does once a step ahead and again on Repeat step
 * or Previous step: MidiSequenceParser.parseTurn of a step of so many measures (32 notes each, BenchSongs) at 65% with
 * a Count In of 4, set up as the player sets it. Into the player's own sequencer (the events stored and sorted for
 * playing), and into a handler that only takes them (the parsing alone).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class StepParseBenchmark {

	private static final int CLICKS = 4;

	@Param({ "1", "4", "16" })
	public int measures;

	private TGSong song;
	private MidiSequenceParser parser;
	private MidiPracticeTurn turn;
	private MidiSequencerImpl sequencer;
	private int tracks;

	@Setup
	public void setUp() {
		TGSongManager manager = new TGSongManager();
		this.song = BenchSongs.song(manager, this.measures);
		this.parser = new MidiSequenceParser(this.song, manager, MidiSequenceParser.DEFAULT_PLAY_FLAGS);
		this.parser.setMetronomeChannelId(9);
		this.turn = new MidiPracticeTurn(this.song.getMeasureHeader(0), this.song.getMeasureHeader(this.measures - 1), 0, 0, CLICKS, 1, TGTempoScale.percent(65));
		this.sequencer = new MidiSequencerImpl(new TGContext());
		this.tracks = this.song.countTracks() + 2;
	}

	// a new sequence each time, as a session's start does: the sequencer's events cleared, then the step laid in it
	@Benchmark
	public MidiSequencerImpl intoSequencer() throws MidiPlayerException {
		this.parser.parseTurn(this.sequencer.createSequence(this.tracks), this.turn, true);
		return this.sequencer;
	}

	@Benchmark
	public void parseOnly(Blackhole events) {
		this.parser.parseTurn(new Taker(this.tracks, events), this.turn, true);
	}

	// takes every event, keeping none
	private static final class Taker extends MidiSequenceHandler {

		private final Blackhole events;

		Taker(int tracks, Blackhole events) {
			super(tracks);
			this.events = events;
		}

		public void addNoteOn(long tick, int track, int channel, int note, int velocity, int voice, boolean bendMode) {
			this.events.consume(tick + note);
		}

		public void addNoteOff(long tick, int track, int channel, int note, int velocity, int voice, boolean bendMode) {
			this.events.consume(tick + note);
		}

		public void addPitchBend(long tick, int track, int channel, int value, int voice, boolean bendMode) {
			this.events.consume(tick + value);
		}

		public void addControlChange(long tick, int track, int channel, int controller, int value) {
			this.events.consume(tick + value);
		}

		public void addProgramChange(long tick, int track, int channel, int instrument) {
			this.events.consume(tick + instrument);
		}

		public void addTempoInUSQ(long tick, int track, int usq) {
			this.events.consume(tick + usq);
		}

		public void addTrackName(long tick, int track, String name) {
			this.events.consume(name);
		}

		public void addTimeSignature(long tick, int track, TGTimeSignature ts) {
			this.events.consume(ts);
		}

		public void notifyFinish() {
			this.events.consume(this);
		}
	}
}
