package app.tuxguitar.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGSong;

/**
 * Blocks of interleaved practice (units per block): each turn plays a run of so many units of the pool in a row, and the
 * pool's blocks are every such run, sliding by one unit; beats count through the barline. A block weighs as its most
 * difficult measure, and the rules count blocks, an overlapping block being no repeat. The song has 16 measures of 4/4
 * at 120 bpm; a block reads as its first and last unit, "1-2", a beat block as "m3b4-m4b1".
 */
public class TestPracticeBlock {

	private final TGSong song = PracticeSongs.song(16);
	private final TGPracticeSettings settings = new TGPracticeSettings();

	private TestPracticeBlock set(TGPracticeUnitKind kind, int from, int to, int unitsPerBlock) {
		settings.setUnit(kind);
		settings.setPool(from, to);
		settings.setUnitsPerBlock(unitsPerBlock);
		return this;
	}

	private TGPracticeSession start() throws TGPracticeRefusal {
		return TGPracticeSession.start(settings, song);
	}

	private String outcome() {
		return PracticeRefusals.outcome(this::start);
	}

	// "2", a beat "m3b4"
	private static String reading(TGPracticeUnit unit) {
		return (unit.getKind() == TGPracticeUnitKind.BEAT ? "m" + unit.getFirst() + "b" + unit.getNumber() : String.valueOf(unit.getNumber()));
	}

	// "1-2", a block of one unit as the unit
	private static String reading(TGPracticeBlock block) {
		String from = reading(block.getFrom());
		String to = reading(block.getTo());
		return (from.equals(to) ? from : from + "-" + to);
	}

	private String pool() throws TGPracticeRefusal {
		return start().getPool().stream().map(TestPracticeBlock::reading).collect(Collectors.joining(", "));
	}

	@Test
	public void aTurnPlaysABlockOfOneUnitUntilIChangeIt() {
		assertEquals(1, new TGPracticeSettings().getUnitsPerBlock());
	}

	@Test
	public void thePoolsBlocksAreEveryRunOfSoManyUnitsInARowSlidingByOne() throws TGPracticeRefusal {
		assertEquals("1-2, 2-3, 3-4, 4-5, 5-6 | 1-3, 2-4, 3-5, 4-6 | 1, 2, 3", set(TGPracticeUnitKind.MEASURE, 1, 6, 2).pool() + " | "
			+ set(TGPracticeUnitKind.MEASURE, 1, 6, 3).pool() + " | " + set(TGPracticeUnitKind.MEASURE, 1, 3, 1).pool());
	}

	@Test
	public void aPoolOfExactlySoManyUnitsIsOneBlock() throws TGPracticeRefusal {
		assertEquals("5-7", set(TGPracticeUnitKind.MEASURE, 5, 7, 3).pool());
	}

	// lines of 2 measures: a block of 2 lines is 4 measures; pages of 2 lines, a block of 2 pages 8
	@Test
	public void aBlockOfLinesOrPagesIsThoseFixedGroupsInARow() throws TGPracticeRefusal {
		settings.setMeasuresPerLine(2);
		settings.setLinesPerPage(2);
		TGPracticeBlock lines = set(TGPracticeUnitKind.LINE, 2, 4, 2).start().getPool().get(0);
		TGPracticeBlock pages = set(TGPracticeUnitKind.PAGE, 1, 3, 2).start().getPool().get(1);
		assertEquals("2-3: 3-6 | 2-3: 5-12", reading(lines) + ": " + lines.getFrom().getFirst() + "-" + lines.getTo().getLast() + " | " + reading(pages) + ": "
			+ pages.getFrom().getFirst() + "-" + pages.getTo().getLast());
	}

	@Test
	public void beatsCountThroughTheBarlineInScoreOrder() throws TGPracticeRefusal {
		PracticeSongs.signature(song, 3, 3, 4);
		assertEquals("m3b1-m3b2, m3b2-m3b3, m3b3-m4b1, m4b1-m4b2, m4b2-m4b3, m4b3-m4b4 | m3b1-m3b3, m3b2-m4b1, m3b3-m4b2, m4b1-m4b3, m4b2-m4b4",
			set(TGPracticeUnitKind.BEAT, 3, 4, 2).pool() + " | " + set(TGPracticeUnitKind.BEAT, 3, 4, 3).pool());
	}

	@Test
	public void aBlockOfFewerThanOneUnitIsRefused() {
		assertEquals("A block needs at least 1 measure. | A block needs at least 1 beat.", set(TGPracticeUnitKind.MEASURE, 1, 4, 0).outcome() + " | "
			+ set(TGPracticeUnitKind.BEAT, 1, 1, -1).outcome());
	}

	@Test
	public void aBlockOfMoreUnitsThanThePoolHoldsIsRefused() {
		assertEquals("A block of 5 measures does not fit in the pool's 4 measures. | A block of 5 beats does not fit in the pool's 4 beats. | accepted",
			set(TGPracticeUnitKind.MEASURE, 1, 4, 5).outcome() + " | " + set(TGPracticeUnitKind.BEAT, 3, 3, 5).outcome() + " | "
				+ set(TGPracticeUnitKind.BEAT, 3, 3, 4).outcome());
	}

	// a flag weighs every block that holds its measure
	@Test
	public void aBlockWeighsAsItsMostDifficultMeasure() throws TGPracticeRefusal {
		song.getMeasureHeader(3).setDifficulty(3);
		int[] measures = set(TGPracticeUnitKind.MEASURE, 2, 6, 2).start().weights();
		int[] beats = set(TGPracticeUnitKind.BEAT, 3, 4, 2).start().weights();
		assertEquals("[1, 3, 3, 1] [1, 1, 1, 3, 3, 3, 3] 3", Arrays.toString(measures) + " " + Arrays.toString(beats) + " " + start().getPool().get(3).weight(song));
	}

	// measures 1 to 4 in blocks of 2, measure 1 flagged 3x: 3x, 1x and 1x, a cycle of 5 turns
	@Test
	public void theRulesCountBlocksAndNameTheBlock() {
		String one = set(TGPracticeUnitKind.MEASURE, 1, 4, 4).outcome();
		settings.setTurns(2);
		String twice = outcome();
		song.getMeasureHeader(0).setDifficulty(3);
		settings.setUnitsPerBlock(2);
		settings.setTurns(5);
		String five = outcome();
		settings.setTurns(6);
		assertEquals("accepted | The block of measures 1-4 would come twice in a row. | accepted | "
			+ "The block of measures 1-2 comes up 3 times in a cycle of 5 turns and would come twice in a row.", one + " | " + twice + " | " + five + " | " + outcome());
	}

	@Test
	public void aBlockOfBeatsIsNamedByItsFirstAndLastBeat() throws TGPracticeRefusal {
		set(TGPracticeUnitKind.BEAT, 3, 4, 2);
		assertEquals("The block of measure 3 beat 4 to measure 4 beat 1 | Measure 3 beat 2", start().getPool().get(3) + " | "
			+ set(TGPracticeUnitKind.BEAT, 3, 4, 1).start().getPool().get(1));
	}

	// blocks 1-2, 2-3 and 3-4: every cycle chooses each once, never the same twice in a row, but 2-3 may follow 1-2
	@Test
	public void onlyTheSameBlockIsARepeatNotOneThatOverlaps() throws TGPracticeRefusal {
		set(TGPracticeUnitKind.MEASURE, 1, 4, 2);
		settings.setTurns(9);
		TGPracticeSession session = start();
		boolean overlaps = false;
		for (int seed = 1; seed <= 200; seed++) {
			List<String> turns = choose(session.chooser(seed), 9);
			for (int cycle = 0; cycle < 9; cycle += 3) {
				assertEquals("[1-2, 2-3, 3-4]", turns.subList(cycle, cycle + 3).stream().sorted().collect(Collectors.toList()).toString(), turns.toString());
			}
			assertTrue(IntStream.range(1, 9).noneMatch(turn -> turns.get(turn).equals(turns.get(turn - 1))), turns.toString());
			overlaps |= IntStream.range(1, 9).anyMatch(turn -> Math.abs(turns.get(turn).charAt(0) - turns.get(turn - 1).charAt(0)) == 1);
		}
		assertTrue(overlaps);
	}

	private static List<String> choose(TGPracticeChooser chooser, int turns) {
		List<String> chosen = new ArrayList<String>();
		for (int turn = 0; turn < turns; turn++) {
			chosen.add(reading(chooser.next()));
		}
		return chosen;
	}
}
