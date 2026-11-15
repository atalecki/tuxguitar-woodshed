package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The player's ticks and the song's (MidiSongTicks), as the desktop's caret and transport map them: 4 measures of 4/4
 * (3840 ticks each, the first from tick 960), measures 2 and 3 repeated once, so the player plays measures 1, 2, 3, 2,
 * 3, 4, the n-th from tick 960 + (n - 1) * 3840.
 */
public class TestMidiSongTicks {

	private static final long MEASURE = 3840;

	private final PlayerSongFixture fixture = new PlayerSongFixture();

	public TestMidiSongTicks() {
		repeat(fixture);
	}

	// measures 2 and 3 repeated once
	static void repeat(PlayerSongFixture fixture) {
		fixture.song.getMeasureHeader(1).setRepeatOpen(true);
		fixture.song.getMeasureHeader(2).setRepeatClose(1);
	}

	// the player's tick of the n-th measure played, plus the ticks
	private static long played(int n, long plus) {
		return 960 + (n - 1) * MEASURE + plus;
	}

	private long start(long tick) {
		return MidiSongTicks.getStart(fixture.player, fixture.song, tick);
	}

	private long tick(long start) {
		return MidiSongTicks.getTick(fixture.player, fixture.song, start);
	}

	@Test
	public void aTickOfThePlayerIsTheSongsTickInTheMeasureThatPassPlays() {
		assertEquals((fixture.start(1) + 5) + " " + (fixture.start(3) + 7) + " " + (fixture.start(2) + 9) + " " + (fixture.start(4) + MEASURE - 1),
			start(played(1, 5)) + " " + start(played(3, 7)) + " " + start(played(4, 9)) + " " + start(played(6, MEASURE - 1)));
	}

	// before the loop start: the loop start; past the end: the player's tick of the last measure played
	@Test
	public void aTickBeforeTheStartOrPastTheEnd() {
		assertEquals("960 " + played(6, 0), start(100) + " " + start(played(7, 1)));
	}

	// where the first pass that plays its measure starts it
	@Test
	public void aTickOfTheSongIsThePlayersTickOfTheMeasureOnTheFirstPassThatPlaysIt() {
		assertEquals(played(1, 0) + " " + played(2, 0) + " " + played(3, 0) + " " + played(6, 0),
			tick(fixture.start(1) + 5) + " " + tick(fixture.start(2)) + " " + tick(fixture.start(3) + MEASURE - 1) + " " + tick(fixture.start(4) + 2));
	}

	@Test
	public void aTickOfTheSongBeforeTheStartOrPastTheEnd() {
		assertEquals("960 " + played(6, 0), tick(100) + " " + tick(played(9, 0)));
	}

	// with a loop from measure 3, the player starts there: its first tick plays measure 3
	@Test
	public void theTicksCountFromTheLoopStart() {
		fixture.loopMode(3, 4);
		assertEquals(fixture.start(3) + " " + fixture.start(3), start(fixture.start(3)) + " " + tick(fixture.start(3)));
	}
}
