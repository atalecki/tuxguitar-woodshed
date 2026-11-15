package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnitKind;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;

/**
 * An interleaved practice session as the player lays it out in its sequence (MidiPractice): from where it starts, each
 * turn its Count In then its unit's measures, the next one right after it; a turn is added to the sequence once the
 * turn before has started (the first one when the session starts), up to the session's turns.
 *
 * The song has 8 measures of 4/4 (3840 ticks each, the first from tick 960); lines of 2 measures. The turns are those
 * the chooser chooses from the same seed; the Count In has a click per beat of the turn's first measure (4).
 */
public class TestMidiPractice {

	private static final long SEED = 7;
	private static final long MEASURE = 3840;

	private final TGSong song = new TGSongManager().newSong();
	private final List<String> added = new ArrayList<String>();
	private TGPracticeSession session;

	public TestMidiPractice() {
		while (song.countMeasureHeaders() < 8) {
			new TGSongManager().addNewMeasureBeforeEnd(song);
		}
	}

	// lines 1 to 4 for so many turns, started from tick 960, each turn added written "first-last@start setup"
	private MidiPractice practice(int turns) throws TGPracticeRefusal {
		TGPracticeSettings settings = new TGPracticeSettings();
		settings.setUnit(TGPracticeUnitKind.LINE);
		settings.setPool(1, 4);
		settings.setMeasuresPerLine(2);
		settings.setTurns(turns);
		session = TGPracticeSession.start(settings, song);
		MidiPractice practice = new MidiPractice(session.chooser(SEED), song, header -> header.getTimeSignature().getNumerator());
		practice.start(960, (turn, setup) -> added.add(turn.getFirst().getNumber() + "-" + turn.getLast().getNumber() + "@" + turn.getStart() + (setup ? " setup" : "")));
		return practice;
	}

	// the first measures of the turns the seed chooses, in order
	private List<Integer> chosen(int turns) {
		TGPracticeChooser chooser = session.chooser(SEED);
		return IntStream.range(0, turns).mapToObj(turn -> chooser.next().getFirst()).collect(Collectors.toList());
	}

	private static String turn(int first, long start) {
		return first + "-" + (first + 1) + "@" + start;
	}

	@Test
	public void eachTurnFollowsTheOneBeforeOnceItHasStarted() throws TGPracticeRefusal {
		MidiPractice practice = practice(3);
		String atTheStart = String.join(", ", added);
		long second = 960 + MEASURE + 2 * MEASURE;
		practice.follow(second - 1);
		practice.follow(second);
		List<Integer> first = chosen(3);
		assertEquals(turn(first.get(0), 960) + " setup | " + turn(first.get(0), 960) + " setup, " + turn(first.get(1), second) + ", "
			+ turn(first.get(2), second + 3 * MEASURE), atTheStart + " | " + String.join(", ", added));
	}

	@Test
	public void noTurnIsAddedAfterTheLast() throws TGPracticeRefusal {
		MidiPractice practice = practice(2);
		practice.follow(960);
		practice.follow(960 + 6 * MEASURE);
		practice.follow(960 + 100 * MEASURE);
		assertEquals(2, added.size(), added.toString());
	}

	// during a turn's Count In the song plays its first measure's first tick; then its measures' ticks
	@Test
	public void theSongTickAndTheTurnOfATickOfTheSequence() throws TGPracticeRefusal {
		MidiPractice practice = practice(3);
		practice.follow(960 + 3 * MEASURE);
		List<Integer> first = chosen(2);
		long secondStart = 960 + 3 * MEASURE;
		TGMeasureHeader secondFirst = song.getMeasureHeader(first.get(1) - 1);
		assertEquals(secondFirst.getStart() + " " + (secondFirst.getStart() + 10) + " " + song.getMeasureHeader(first.get(0) - 1).getStart() + " 1 2 2",
			practice.songTick(secondStart + MEASURE - 1) + " " + practice.songTick(secondStart + MEASURE + 10) + " " + practice.songTick(960)
				+ " " + practice.turnAt(secondStart - 1) + " " + practice.turnAt(secondStart) + " " + practice.turnAt(secondStart + 3 * MEASURE));
	}
}
