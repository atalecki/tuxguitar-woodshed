package app.tuxguitar.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.practice.TGTempoScale;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;

/**
 * The turns of interleaved practice (TGTurn): a unit's turn, and the passes a practice turn is planned as. The song has
 * 4 measures of 4/4 at 120 bpm (a measure 2 seconds, 88200 samples at 44100 Hz).
 */
public class TestTurn {

	private final TGSongManager songManager = new TGSongManager();
	private final TGSong song = songManager.newSong();

	public TestTurn() {
		for (int m = 1; m < 4; m++) {
			songManager.addNewMeasureBeforeEnd(song);
		}
	}

	// "<rest> <opens|passes> m<number>:<pass>[ jump]" for the turn's first measure
	private static String read(TGTurn turn) {
		TGPlayedMeasure first = turn.getMeasures().get(0);
		return turn.getRest() + (turn.isOpening() ? " opens" : " passes") + " m" + first.getHeader().getNumber() + ":" + first.getPass() + (first.isJump() ? " jump" : "");
	}

	// a unit's turn of its own opens a practice turn, with no break; it knows its span
	@Test
	public void aUnitsTurnOpensAPracticeTurnWithNoBreak() throws TGPracticeRefusal {
		TGTurn turn = TGTurn.unit(song, TestSongTime.beat(song, 2, 3), 0, TGTempoScale.percent(100), header -> 0, 44100);

		assertEquals("0 opens m2:0 true", read(turn) + " " + turn.getSpan().isPresent());
	}

	// a timed turn of measure 2 for a minute (Count In off: 30 passes of 2 seconds), planned as the timeline's turn of the
	// index after a break of a second: its first pass opens it after the break, the others continue it; each pass is
	// the timeline's turn after the one before, a stretch of its own (but the timeline's very first)
	private List<String> planned(int index) throws TGPracticeRefusal {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setUnit(TGPracticeUnitKind.MEASURE);
		settings.setPool(2, 2);
		settings.setIntervalClock(true);
		settings.setTurnMinutes(1);
		settings.setBreakSeconds(10);
		settings.setSessionMinutes(1);
		TGPracticeSession session = TGPracticeSession.start(settings, song);
		List<TGTurn> passes = TGTurn.planned(song, session.course(1, header -> 0, TGTempoScale.percent(100)).next(), index, 44100, header -> 0, 44100);
		return passes.subList(0, 3).stream().map(TestTurn::read).collect(Collectors.toList());
	}

	@Test
	public void aPlannedTurnsPassesFollowItsFirstEachAStretchOfItsOwn() throws TGPracticeRefusal {
		assertEquals("[44100 opens m2:0, 0 passes m2:1 jump, 0 passes m2:2 jump] [44100 opens m2:5 jump, 0 passes m2:6 jump, 0 passes m2:7 jump]",
			planned(0) + " " + planned(5));
	}
}
