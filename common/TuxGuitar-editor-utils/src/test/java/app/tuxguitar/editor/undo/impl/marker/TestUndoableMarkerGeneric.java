package app.tuxguitar.editor.undo.impl.marker;

import static app.tuxguitar.editor.action.SongActionFixture.reads;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.SongActionFixture;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.action.marker.TGRemoveMarkerAction;
import app.tuxguitar.editor.action.marker.TGUpdateMarkerAction;
import app.tuxguitar.editor.undo.TGCannotRedoException;
import app.tuxguitar.editor.undo.TGCannotUndoException;
import app.tuxguitar.editor.undo.TGUndoableEdit;
import app.tuxguitar.song.models.TGMarker;

/**
 * Pins the marker actions and their undoable edit (TGUndoableMarkerGeneric), as the Marker editor and list run them:
 * update puts the marker on its measure (adding it, or changing the one there), remove takes the measure's marker
 * away; undo gives the measure back the marker it had (or none), redo what the action left. An action given no marker
 * changes nothing, nor do its undo and redo. An edit cannot be undone twice, nor redone before it is undone.
 *
 * The song of four measures has the marker "Intro" on measure 2; its markers read after the action, the undo and the
 * redo as "markers / markers / markers", the markers in measure order, each "m<measure> <title>", "-" for none.
 */
public class TestUndoableMarkerGeneric {

	private final SongActionFixture fixture = SongActionFixture.onDocument();

	public TestUndoableMarkerGeneric() {
		fixture.measureCount(4);
		fixture.songManager.updateMarker(fixture.song, marker(2, "Intro"));
		fixture.map(new TGUpdateMarkerAction(fixture.context), new TGRemoveMarkerAction(fixture.context));
	}

	private TGMarker marker(int measure, String title) {
		TGMarker marker = fixture.songManager.getFactory().newMarker();
		marker.setMeasure(measure);
		marker.setTitle(title);
		return marker;
	}

	private TGUndoableEdit run(TGActionBase action, TGMarker marker) {
		fixture.actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MARKER, marker);
		return fixture.executeUndoable(action, new TGUndoableMarkerGenericController());
	}

	private String runUndoRedo(TGActionBase action, TGMarker marker) {
		run(action, marker);
		String done = markers();
		fixture.undo();
		String undone = markers();
		fixture.redo();
		return done + " / " + undone + " / " + markers();
	}

	private String markers() {
		String markers = fixture.songManager.getMarkers(fixture.song).stream().map(marker -> "m" + marker.getMeasure() + " " + marker.getTitle())
			.collect(Collectors.joining(", "));
		return markers.isEmpty() ? "-" : markers;
	}

	private String update(int measure, String title) {
		return runUndoRedo(new TGUpdateMarkerAction(fixture.context), marker(measure, title));
	}

	private String remove(int measure) {
		return runUndoRedo(new TGRemoveMarkerAction(fixture.context), marker(measure, ""));
	}

	private String removeNone() {
		return runUndoRedo(new TGRemoveMarkerAction(fixture.context), null);
	}

	@TestFactory
	public List<DynamicTest> undoAndRedo() {
		return Arrays.asList(
			reads("add a marker", "m2 Intro, m3 Verse / m2 Intro / m2 Intro, m3 Verse", () -> new TestUndoableMarkerGeneric().update(3, "Verse")),
			reads("change a marker", "m2 Chorus / m2 Intro / m2 Chorus", () -> new TestUndoableMarkerGeneric().update(2, "Chorus")),
			reads("remove a marker", "- / m2 Intro / -", () -> new TestUndoableMarkerGeneric().remove(2)),
			reads("remove no marker", "m2 Intro / m2 Intro / m2 Intro", () -> new TestUndoableMarkerGeneric().remove(3)),
			reads("remove, given no marker", "m2 Intro / m2 Intro / m2 Intro", () -> new TestUndoableMarkerGeneric().removeNone()));
	}

	@Test
	public void anEditCannotBeRedoneBeforeItIsUndone() {
		TGUndoableEdit edit = run(new TGUpdateMarkerAction(fixture.context), marker(3, "Verse"));

		assertThrows(TGCannotRedoException.class, () -> edit.redo(null));
	}

	@Test
	public void anEditCannotBeUndoneTwice() {
		TGUndoableEdit edit = run(new TGUpdateMarkerAction(fixture.context), marker(3, "Verse"));
		fixture.undo();

		assertThrows(TGCannotUndoException.class, () -> edit.undo(null));
	}
}
