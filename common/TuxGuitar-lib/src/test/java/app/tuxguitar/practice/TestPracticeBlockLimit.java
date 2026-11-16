package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * The most units per block a form still being filled in allows (TGPracticeSession.blockLimit): as many as its pool
 * holds, one fewer while "No consecutive repeats" would refuse a pool of one block (on, the clock off, more than 1
 * turn), and at least 1. Start accepts that many and refuses one more. The song has 64 measures of 4/4; a line holds 4
 * measures, a page 4 lines.
 */
public class TestPracticeBlockLimit {

	private final TGSong song = PracticeSongs.song(64);
	private final TGPracticeSettings form = new TGPracticeSettings();

	// the limit for each pool of the kind (from, to, from, to...), the rest of the form as set
	private String limits(TGPracticeUnitKind kind, int... pools) {
		form.setUnit(kind);
		return IntStream.range(0, pools.length / 2).mapToObj(pool -> {
			form.setPool(pools[2 * pool], pools[2 * pool + 1]);
			return String.valueOf(TGPracticeSession.blockLimit(form, song));
		}).collect(Collectors.joining(" "));
	}

	// measures 2-5, lines 2-3, pages 1-4, with 1 turn: a pool of one block plays
	@Test
	public void aBlockHoldsNoMoreUnitsThanThePool() {
		assertEquals("4 2 4", limits(TGPracticeUnitKind.MEASURE, 2, 5) + " " + limits(TGPracticeUnitKind.LINE, 2, 3) + " " + limits(TGPracticeUnitKind.PAGE, 1, 4));
	}

	// a pool of beats holds the beats of its measures: 4 of 4/4 and 6 of 6/8
	@Test
	public void aBlockOfBeatsHoldsNoMoreThanThePoolsMeasuresBeats() {
		PracticeSongs.signature(song, 4, 6, 8);
		assertEquals("10", limits(TGPracticeUnitKind.BEAT, 3, 4));
	}

	// a pool refused when the session starts bounds a block by the part of it in the song (from before measure 1, to
	// past the song's end), or as 1 when none is (an empty pool)
	@Test
	public void aPoolStillBeingFilledInBoundsABlockByItsPartInTheSongAtLeastOne() {
		assertEquals("3 5 1", limits(TGPracticeUnitKind.MEASURE, 0, 3, 60, 70, 5, 4));
	}

	// a group size of 0 bounds a block as 1 would: a line of one measure, a page of one line
	@Test
	public void aGroupSizeOfNoneBoundsABlockAsOneWould() {
		form.setMeasuresPerLine(0);
		String lines = limits(TGPracticeUnitKind.LINE, 1, 99);
		form.setMeasuresPerLine(1);
		form.setLinesPerPage(0);
		assertEquals("64 64", lines + " " + limits(TGPracticeUnitKind.PAGE, 1, 99));
	}

	// measures 2-5 and 5-5 with 2 turns, then the rule off, the clock on, 1 turn: one block is left out only while the
	// rule would refuse it; a pool of one unit keeps 1, the fewest a block holds (Start refuses that pool anyway)
	@Test
	public void aPoolOfOneBlockIsLeftOutWhileNoConsecutiveRepeatsWouldRefuseIt() {
		form.setTurns(2);
		String refused = limits(TGPracticeUnitKind.MEASURE, 2, 5, 5, 5);
		form.setNoConsecutiveRepeats(false);
		String off = limits(TGPracticeUnitKind.MEASURE, 2, 5);
		form.setNoConsecutiveRepeats(true);
		form.setIntervalClock(true);
		String timed = limits(TGPracticeUnitKind.MEASURE, 2, 5);
		form.setIntervalClock(false);
		form.setTurns(1);
		assertEquals("3 1 | 4 | 4 | 4", refused + " | " + off + " | " + timed + " | " + limits(TGPracticeUnitKind.MEASURE, 2, 5));
	}

	private String start(int unitsPerBlock) {
		form.setUnitsPerBlock(unitsPerBlock);
		return PracticeRefusals.outcome(() -> TGPracticeSession.start(form, song));
	}

	// Start's outcomes with the limit and with one more, as "accepted | refused"
	private String atAndPastTheLimit() {
		int limit = TGPracticeSession.blockLimit(form, song);
		return start(limit) + " | " + (start(limit + 1).equals("accepted") ? "accepted" : "refused");
	}

	// the form with the rule (flag 1), the complete cycle (2) and the clock (4) on as the flags say, for that many turns:
	// "MEASURE 5 2: accepted | refused"
	private String outcome(int flags, int turns) {
		form.setNoConsecutiveRepeats((flags & 1) != 0);
		form.setCompleteCycle((flags & 2) != 0);
		form.setIntervalClock((flags & 4) != 0);
		form.setTurns(turns);
		return form.getUnit() + " " + flags + " " + turns + ": " + atAndPastTheLimit();
	}

	// the outcomes of the pool of the kind from and to, with every setting of the flags, for 1, 2 and 12 turns
	private List<String> outcomes(TGPracticeUnitKind kind, int from, int to) {
		form.setUnit(kind);
		form.setPool(from, to);
		return IntStream.range(0, 8).boxed().flatMap(flags -> IntStream.of(1, 2, 12).mapToObj(turns -> outcome(flags, turns))).collect(Collectors.toList());
	}

	// every form of the grid (measures 2-5, lines 1-3 and beats over measures 3-4; the rule, the complete cycle and the
	// clock each on and off; 1, 2 and 12 turns; no flags, whose refusals stay Start's): Start accepts the limit and
	// refuses one more, so the field and Start keep one rule. Pools of 2 units or more: Start refuses a pool of one unit
	// with any block while the rule applies.
	@Test
	public void startAcceptsTheLimitAndRefusesOneMore() {
		List<String> outcomes = new ArrayList<String>(outcomes(TGPracticeUnitKind.MEASURE, 2, 5));
		outcomes.addAll(outcomes(TGPracticeUnitKind.LINE, 1, 3));
		outcomes.addAll(outcomes(TGPracticeUnitKind.BEAT, 3, 4));
		assertEquals("72 []", outcomes.size() + " " + outcomes.stream().filter(outcome -> !outcome.endsWith(": accepted | refused")).collect(Collectors.toList()));
	}
}
