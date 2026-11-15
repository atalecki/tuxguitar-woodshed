package app.tuxguitar.editor.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import app.tuxguitar.action.TGActionException;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.action.composition.TGChangeDifficultyAction;
import app.tuxguitar.editor.action.composition.TGChangeTripletFeelAction;
import app.tuxguitar.editor.action.edit.TGRedoAction;
import app.tuxguitar.editor.action.edit.TGUndoAction;
import app.tuxguitar.editor.undo.impl.custom.TGUndoableDifficultyController;
import app.tuxguitar.editor.undo.impl.custom.TGUndoableTripletFeelController;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.practice.TGPracticeChooser;
import app.tuxguitar.practice.TGPracticeRefusal;
import app.tuxguitar.practice.TGPracticeSession;
import app.tuxguitar.practice.TGPracticeSettings;
import app.tuxguitar.practice.TGPracticeUnderWay;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.util.TGContext;

/**
 * Edits during an interleaved practice session (TGPracticeEdits), as the app runs them: a flag, and the undo or redo of
 * one, goes on when the next cycles can play its weights, else it is refused saying why and changes nothing; any other
 * edit, or its undo, ends the session. Without a session under way nothing is checked.
 *
 * Each case: a song of four measures, the edits made before, then a session of 6 turns of measures over measures 1 to
 * the last given, its chooser under way with one turn chosen, then the edits made during it. It reads "the refusal's
 * message (go on without one) | each measure's flag | whether the session has ended".
 */
public class TestPracticeEdits {

	private static final String TOO_HEAVY = "Measure 2 comes up 3 times in a cycle of 4 turns and would come twice in a row.";

	private static final Consumer<Edits> NOTHING = edits -> { };

	// a case's song, its edits and its session
	private static final class Edits {
		private final SongActionFixture fixture = SongActionFixture.onDocument();
		private TGPracticeSession session;

		Edits() {
			fixture.measureCount(4);
			TGPracticeEdits.install(fixture.context);
		}

		void underWay(int last) throws TGPracticeRefusal {
			TGPracticeSettings settings = new TGPracticeSettings();
			settings.setPool(1, last);
			settings.setTurns(6);
			session = TGPracticeSession.start(settings, fixture.song);
			TGPracticeChooser chooser = session.chooser(1);
			chooser.next();
			TGPracticeUnderWay.getInstance(fixture.context).begin(chooser);
		}

		void flag(int measure, int difficulty) {
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, fixture.song.getMeasureHeader(measure - 1));
			fixture.actionContext.setAttribute(TGChangeDifficultyAction.ATTRIBUTE_DIFFICULTY, difficulty);
			fixture.executeUndoable(new TGChangeDifficultyAction(fixture.context), new TGUndoableDifficultyController());
		}

		void undo() {
			fixture.undo();
		}

		void redo() {
			fixture.redo();
		}

		void flaggedAndUndone() {
			flag(2, 3);
			undo();
		}

		// a refused flag is no undo step: undo after it undoes nothing
		void refusedThenUndone() {
			try {
				flag(2, 3);
			} catch (TGActionException refused) {
				undo();
			}
		}

		void tripletFeel() {
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_HEADER, fixture.song.getMeasureHeader(1));
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, fixture.song.getTrack(0).getMeasure(1));
			fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, fixture.song.getTrack(0));
			fixture.actionContext.setAttribute(TGChangeTripletFeelAction.ATTRIBUTE_TRIPLET_FEEL, TGMeasureHeader.TRIPLET_FEEL_EIGHTH);
			fixture.actionContext.setAttribute(TGChangeTripletFeelAction.ATTRIBUTE_APPLY_TO_END, false);
			fixture.actionContext.setAttribute(TGActionBase.ATTRIBUTE_SUCCESS, null);
			fixture.executeUndoable(new TGChangeTripletFeelAction(fixture.context), new TGUndoableTripletFeelController());
		}

		void endedThenFlagged() {
			session.end();
			flag(2, 3);
		}

		String during(Consumer<Edits> before, int last, Consumer<Edits> edit) throws TGPracticeRefusal {
			before.accept(this);
			underWay(last);
			String outcome = "go on";
			try {
				edit.accept(this);
			} catch (TGActionException refused) {
				outcome = refused.getMessage();
			}
			return outcome + " | " + fixture.measures(measure -> String.valueOf(measure.getHeader().getDifficulty())) + " | " + session.isEnded();
		}
	}

	private static DynamicTest reads(String name, String expected, Consumer<Edits> before, int last, Consumer<Edits> edit) {
		return DynamicTest.dynamicTest(name, () -> assertEquals(expected, new Edits().during(before, last, edit)));
	}

	@TestFactory
	public List<DynamicTest> editsDuringASession() {
		return Arrays.asList(
			reads("a flag the next cycles can play goes on", "go on | 1 | 3 | 1 | 1 | false", NOTHING, 4, edits -> edits.flag(2, 3)),
			reads("a flag the next cycles could not play is refused", TOO_HEAVY + " | 1 | 1 | 1 | 1 | false", NOTHING, 2, edits -> edits.flag(2, 3)),
			reads("a refused flag is no undo step", "go on | 1 | 1 | 1 | 1 | false", NOTHING, 2, Edits::refusedThenUndone),
			reads("undoing a flag the next cycles could not play is refused", TOO_HEAVY + " | 1 | 1 | 1 | 1 | false", edits -> {
				edits.flag(2, 3);
				edits.flag(2, 1);
			}, 2, Edits::undo),
			reads("undoing a flag the next cycles can play goes on", "go on | 1 | 1 | 1 | 1 | false", edits -> edits.flag(3, 2), 2, Edits::undo),
			reads("redoing a flag the next cycles could not play is refused", TOO_HEAVY + " | 1 | 1 | 1 | 1 | false", Edits::flaggedAndUndone, 2, Edits::redo),
			reads("redoing a flag the next cycles can play goes on", "go on | 1 | 3 | 1 | 1 | false", Edits::flaggedAndUndone, 4, Edits::redo),
			// the flag action's to refuse
			reads("a flag outside 1x to 5x is not checked", "go on | 1 | 1 | 1 | 1 | false", NOTHING, 2, edits -> edits.flag(2, 6)),
			reads("any other edit ends the session", "go on | 1 | 1 | 1 | 1 | true", NOTHING, 4, Edits::tripletFeel),
			reads("undoing any other edit ends the session", "go on | 1 | 1 | 1 | 1 | true", Edits::tripletFeel, 4, Edits::undo),
			reads("without a session under way a flag is not checked", "go on | 1 | 3 | 1 | 1 | true", NOTHING, 2, Edits::endedThenFlagged));
	}

	// the app's action that moves the caret left
	private static final String CARET_MOVE = "action.caret.go-left";

	// the edits made, the player running or not, a session under way or not
	private static Edits state(Consumer<Edits> before, boolean running, boolean session) throws TGPracticeRefusal {
		Edits edits = new Edits();
		before.accept(edits);
		if (session) {
			edits.underWay(4);
		}
		MidiPlayer.getInstance(edits.fixture.context).setRunning(running);
		return edits;
	}

	// which of a flag, triplet feel, undo, redo and a caret move the rule holds for (the app disables them, as while
	// playing, or greys their items out) after the edits, with the player running or not, with a session under way or
	// not: "flag triplet undo redo caret", each true or false
	private static String holds(BiPredicate<TGContext, String> rule, Consumer<Edits> before, boolean running, boolean session) throws TGPracticeRefusal {
		TGContext context = state(before, running, session).fixture.context;
		return Stream.of(TGChangeDifficultyAction.NAME, TGChangeTripletFeelAction.NAME, TGUndoAction.NAME, TGRedoAction.NAME, CARET_MOVE)
			.map(id -> String.valueOf(rule.test(context, id))).collect(Collectors.joining(" "));
	}

	private static DynamicTest disables(String name, String expected, Consumer<Edits> before, boolean running, boolean session) {
		return DynamicTest.dynamicTest(name, () -> assertEquals(expected, holds(TGPracticeEdits::disables, before, running, session)));
	}

	private static DynamicTest greys(String name, String expected, Consumer<Edits> before, boolean running, boolean session) {
		return DynamicTest.dynamicTest(name, () -> assertEquals(expected, holds(TGPracticeEdits::greys, before, running, session)));
	}

	// a flag undone: one to undo (flag 2) and one to redo (its undone flag)
	private static void flaggedTwiceAndUndone(Edits edits) {
		edits.flag(2, 2);
		edits.flaggedAndUndone();
	}

	// a triplet feel set and undone, then set again: one to undo and one to redo, neither a flag
	private static void tripletFeelTwiceAndUndone(Edits edits) {
		edits.tripletFeel();
		edits.tripletFeel();
		edits.undo();
	}

	@TestFactory
	public List<DynamicTest> whatTheAppDisables() {
		return Arrays.asList(
			disables("stopped, nothing is disabled", "false false false false false", Edits::flaggedAndUndone, false, false),
			disables("playing the song, every edit and caret move is", "true true true true true", Edits::flaggedAndUndone, true, false),
			disables("in a session, a flag, the undo and redo of a flag and a caret move are not", "false true false false false", TestPracticeEdits::flaggedTwiceAndUndone, true, true),
			disables("in a session, undo and redo of another edit are", "false true true true false", TestPracticeEdits::tripletFeelTwiceAndUndone, true, true),
			disables("in a paused session, as in a playing one", "false true false false false", TestPracticeEdits::flaggedTwiceAndUndone, false, true));
	}

	// the items of what the app disables during a session are greyed out, paused too; without a session each item's own
	// rule stands (nothing more is greyed)
	@TestFactory
	public List<DynamicTest> whatTheAppGreysOut() {
		return Arrays.asList(
			greys("stopped, nothing more", "false false false false false", Edits::flaggedAndUndone, false, false),
			greys("playing the song, nothing more", "false false false false false", Edits::flaggedAndUndone, true, false),
			greys("in a session, all but a flag, the undo and redo of a flag and a caret move", "false true false false false", TestPracticeEdits::flaggedTwiceAndUndone, true, true),
			greys("in a session, undo and redo of another edit too", "false true true true false", TestPracticeEdits::tripletFeelTwiceAndUndone, true, true),
			greys("in a paused session, as in a playing one", "false true false false false", TestPracticeEdits::flaggedTwiceAndUndone, false, true));
	}

	// whether moving the caret moves the player too: "stopped playing | in a session playing paused"; only while the
	// song plays, never during a session (moving the player would end it)
	@Test
	public void theCaretMovesThePlayerOnlyWhileTheSongPlays() throws TGPracticeRefusal {
		String song = moves(false, false) + " " + moves(true, false);
		String session = moves(true, true) + " " + moves(false, true);
		assertEquals("false true | false false", song + " | " + session);
	}

	private static boolean moves(boolean running, boolean session) throws TGPracticeRefusal {
		return TGPracticeCaret.movesPlayer(state(NOTHING, running, session).fixture.context);
	}
}
