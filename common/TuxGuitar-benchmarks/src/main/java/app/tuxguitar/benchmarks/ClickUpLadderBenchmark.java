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

import app.tuxguitar.practice.TGClickUpLadder;
import app.tuxguitar.practice.TGClickUpSettings;
import app.tuxguitar.practice.TGPracticeCourse;
import app.tuxguitar.practice.TGPracticePlan;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;

/**
 * Starting an interleaved click-up (TGClickUpLadder.of: the passage's sections, the rungs, every step), and walking
 * the whole of it step by step as the player plans it (its course). A passage of so many measures, each a section, the
 * ladder 60% to 100% by the increment: by 5 the default (9 rungs), by 1 the finest (41).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ClickUpLadderBenchmark {

	@Param({ "4", "16", "64" })
	public int sections;

	@Param({ "5", "1" })
	public int increment;

	private TGSong song;
	private TGClickUpSettings settings;
	private TGClickUpLadder ladder;

	@Setup
	public void setUp() throws TGPracticeRefusal {
		this.song = BenchSongs.song(new TGSongManager(), this.sections);
		this.settings = new TGClickUpSettings();
		this.settings.setUnit(TGPracticeUnitKind.MEASURE);
		this.settings.setPassageFrom(1);
		this.settings.setPassageTo(this.sections);
		this.settings.setIncrement(this.increment);
		this.ladder = TGClickUpLadder.of(this.settings, this.song);
	}

	@Benchmark
	public TGClickUpLadder ladder() throws TGPracticeRefusal {
		return TGClickUpLadder.of(this.settings, this.song);
	}

	// every step's plan, first to last, as the player plans one ahead and starts it; each plan used, as the player keeps
	// it (unused, the JIT drops the plans and times only the loop)
	@Benchmark
	public void course(Blackhole plans) {
		TGPracticeCourse course = this.ladder.course();
		TGPracticePlan plan;
		do {
			plan = course.ahead();
			course.started();
			plans.consume(plan);
		} while (!plan.isLast());
	}
}
