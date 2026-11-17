package app.tuxguitar.vst3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import app.tuxguitar.vst3.TGVst3Notice.Kind;

/**
 * What a plugin's own editor did, as the song should take it: a gesture once its last edit ends; a plugin saying it is
 * dirty (a capture loaded), or its editor hidden by its close box, once no gesture is under way (the gesture's end
 * covers it); a restart or a lone perform, never.
 */
public class TestVst3Gestures {

	// how many changes the notices end, heard in that order
	private static int ended(Kind... kinds) {
		int[] ended = new int[1];
		TGVst3Gestures gestures = new TGVst3Gestures(() -> ended[0]++);
		Arrays.stream(kinds).forEach(kind -> gestures.notice(new TGVst3Notice(kind, 0, 0.0)));
		return ended[0];
	}

	@Test
	public void aPluginSayingItIsDirtyOrItsEditorHiddenEndsAChange() {
		assertEquals("1 1 2", ended(Kind.DIRTY) + " " + ended(Kind.EDITOR_HIDDEN) + " " + ended(Kind.DIRTY, Kind.EDITOR_HIDDEN));
	}

	@Test
	public void dirtyOrHiddenDuringAGestureEndsNothingTheGesturesEndDoes() {
		assertEquals("0 1 1", ended(Kind.BEGIN_EDIT, Kind.DIRTY, Kind.EDITOR_HIDDEN) + " " + ended(Kind.BEGIN_EDIT, Kind.DIRTY, Kind.END_EDIT)
			+ " " + ended(Kind.BEGIN_EDIT, Kind.BEGIN_EDIT, Kind.END_EDIT, Kind.END_EDIT));
	}

	// only the gesture ends a change here
	@Test
	public void aRestartOrAPerformAloneEndsNothing() {
		assertEquals(1, ended(Kind.RESTART, Kind.PERFORM_EDIT, Kind.BEGIN_EDIT, Kind.END_EDIT));
	}
}
