package app.tuxguitar.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * Interleaved click-up of a song as it was started: its settings checked once, and its steps, in order. The sections
 * come into play one at a time; with N in play, step i (from 0) plays at rung i of the tempo ladder the run of sections
 * P_N[i mod length], where P_1 = 1, P_2 = 1-2, 2, and P_N = 1-N, N, (N-1)-N, N, ..., 2-N, N: the newest alone every
 * other step, between runs that reach back one section further each time. When the rungs run out before the steps with
 * N in play have ended on all N, they go on at the goal until they have (the ladder's tail).
 */
public final class TGClickUpLadder {

	private final List<TGClickUpStep> steps;
	private final int passes;
	private final boolean countInEachStep;
	private final boolean countInEveryPass;
	private final int writtenBpm;

	private TGClickUpLadder(List<TGClickUpStep> steps, TGClickUpSettings settings, int writtenBpm) {
		this.steps = Collections.unmodifiableList(steps);
		this.writtenBpm = writtenBpm;
		this.passes = settings.getPasses();
		this.countInEachStep = settings.isCountInEachStep();
		this.countInEveryPass = settings.isCountInEachStep() && settings.isCountInEveryPass();
	}

	// the click-up as set, or the reason it cannot be played
	public static TGClickUpLadder of(TGClickUpSettings settings, TGSong song) throws TGPracticeRefusal {
		TGPracticeGrouping grouping = TGPracticeGrouping.of(settings.getMeasuresPerLine(), settings.getLinesPerPage());
		List<TGPracticeUnit> sections = grouping.units(settings.getUnit(), settings.getPassageFrom(), settings.getPassageTo(), song.countMeasureHeaders(), "passage");
		int[] rungs = rungs(settings.getTempoMode(), settings.getStart(), settings.getGoal(), settings.getIncrement());
		if (settings.getPasses() < 1) {
			throw new TGPracticeRefusal("A step needs at least 1 pass.");
		}
		int writtenBpm = song.getMeasureHeader(sections.get(0).getFirst() - 1).getTempo().getQuarterValue();
		return new TGClickUpLadder(steps(sections, rungs, settings.getTempoMode(), writtenBpm), settings, writtenBpm);
	}

	// the steps, the sections coming into play one at a time, the tail of each at the goal, the last rung
	private static List<TGClickUpStep> steps(List<TGPracticeUnit> sections, int[] rungs, TGClickUpTempoMode mode, int writtenBpm) {
		List<TGClickUpStep> steps = new ArrayList<TGClickUpStep>();
		for (int inPlay = 1; inPlay <= sections.size(); inPlay++) {
			for (int i = 0; i < rungs.length || first(inPlay, i - 1) != 1; i++) {
				int rung = rungs[Math.min(i, rungs.length - 1)];
				int first = first(inPlay, i);
				steps.add(new TGClickUpStep(sections.get(first - 1), sections.get(inPlay - 1), first, inPlay, rung, mode, mode.scale(rung, writtenBpm)));
			}
		}
		return steps;
	}

	// the first section of the run step i (from 0) plays with so many sections in play: P_N[i mod length]
	private static int first(int inPlay, int i) {
		int j = i % Math.max(1, 2 * (inPlay - 1));
		if (j % 2 == 1) {
			return inPlay;
		}
		return (j == 0 ? 1 : inPlay - j / 2);
	}

	// the ladder's rungs: start, start + increment, ... not past the goal, and the goal itself where the increment does
	// not land on it; or the reason it cannot be climbed
	private static int[] rungs(TGClickUpTempoMode mode, int start, int goal, int increment) throws TGPracticeRefusal {
		if (start < 1) {
			throw new TGPracticeRefusal("The ladder starts below " + mode.reading(1) + ".");
		}
		if (goal < start) {
			throw new TGPracticeRefusal("The ladder's goal is below its start.");
		}
		if (increment < 1) {
			throw new TGPracticeRefusal("The ladder climbs by less than " + mode.reading(1) + ".");
		}
		int climbs = (goal - start) / increment;
		int[] rungs = new int[climbs + ((goal - start) % increment == 0 ? 1 : 2)];
		for (int k = 0; k <= climbs; k++) {
			rungs[k] = start + k * increment;
		}
		rungs[rungs.length - 1] = goal;
		return rungs;
	}

	// a play of it: its steps, one turn each, in order, from the first (each play its own course), each step's Count In
	// as many clicks as the function says before its first measure (the player's Count In's, whether it is on or not)
	public TGPracticeCourse course(ToIntFunction<TGMeasureHeader> clicks) {
		return new TGClickUpCourse(this, clicks);
	}

	// the written bpm of the passage's first measure, which a ladder in bpm is read against
	public int getWrittenBpm() {
		return this.writtenBpm;
	}

	public List<TGClickUpStep> getSteps() {
		return this.steps;
	}

	// how many times each step plays its sections
	public int getPasses() {
		return this.passes;
	}

	// whether each step has a Count In, whether or not the player's Count In is on
	public boolean isCountInEachStep() {
		return this.countInEachStep;
	}

	// whether a Count In comes before every pass of a step, or only its first (or none: without one each step)
	public boolean isCountInEveryPass() {
		return this.countInEveryPass;
	}
}
