package app.tuxguitar.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.models.TGTempo;

/**
 * The exact tempo scale a step plays at (TGTempoScale): making one (reduced by the greatest common divisor), and the
 * conversions the player and the audio make with it, per tempo event and per measure. 65 bpm against a written 120.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class TempoScaleBenchmark {

	// fields, not constants, so the JIT cannot fold the work away
	public int bpm = 65;
	public int written = 120;
	public long ticks = 3840;

	private TGTempoScale scale;
	private TGTempo tempo;

	@Setup
	public void setUp() {
		this.scale = TGTempoScale.bpm(this.bpm, this.written);
		this.tempo = new TGFactory().newTempo();
		this.tempo.setQuarterValue(this.written);
	}

	@Benchmark
	public TGTempoScale make() {
		return TGTempoScale.bpm(this.bpm, this.written);
	}

	@Benchmark
	public int usq() {
		return this.scale.usq(this.tempo);
	}

	@Benchmark
	public long micros() {
		return this.scale.micros(this.ticks, this.tempo);
	}
}
