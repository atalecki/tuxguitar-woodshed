package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.TGActionBase;
import app.tuxguitar.editor.action.marker.TGRemoveMarkerAction;
import app.tuxguitar.editor.action.marker.TGUpdateMarkerAction;
import app.tuxguitar.editor.undo.impl.marker.TGUndoableMarkerGenericController;
import app.tuxguitar.song.models.TGMarker;
import app.tuxguitar.song.models.TGTrack;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

/**
 * The edits a practice session goes on through, as the app makes them, one undo step each: muting and soloing a track
 * (see TrackSteps), adding, renaming and removing a marker (the update and remove marker actions, as the Marker editor
 * and list run them), and undoing and redoing; and the reading of a track's solo and mute and of the markers (README,
 * Track and markers reading).
 */
public class KeptEditSteps {

	private final AudioWorld world;
	private final TrackSteps trackSteps;
	private final SongSteps songSteps;
	// each edit's words, and what makes it from their numbers and titles
	private final Map<Pattern, Function<Matcher, Runnable>> edits = new LinkedHashMap<Pattern, Function<Matcher, Runnable>>();

	public KeptEditSteps(AudioWorld world, TrackSteps trackSteps, SongSteps songSteps) {
		this.world = world;
		this.trackSteps = trackSteps;
		this.songSteps = songSteps;
		edit("muting track (\\d+)", words -> () -> this.trackSteps.mute(number(words, 1)));
		edit("soloing track (\\d+)", words -> () -> this.trackSteps.solo(number(words, 1)));
		edit("adding the marker \"([^\"]*)\" at measure (\\d+)", words -> () -> addMarker(words.group(1), number(words, 2)));
		edit("renaming the marker of measure (\\d+) to \"([^\"]*)\"", words -> () -> addMarker(words.group(2), number(words, 1)));
		edit("removing the marker of measure (\\d+)", words -> () -> removeMarker(number(words, 1)));
		edit("undoing", words -> this.songSteps::undo);
		edit("redoing", words -> this.songSteps::redo);
	}

	private void edit(String words, Function<Matcher, Runnable> making) {
		edits.put(Pattern.compile(words), making);
	}

	private static int number(Matcher words, int group) {
		return Integer.parseInt(words.group(group));
	}

	// an edit a session goes on through (the words of one of the edits above)
	@ParameterType("muting track \\d+|soloing track \\d+|adding the marker \"[^\"]*\" at measure \\d+|renaming the marker of measure \\d+ to \"[^\"]*\""
		+ "|removing the marker of measure \\d+|undoing|redoing")
	public Runnable keptEdit(String words) {
		for (Map.Entry<Pattern, Function<Matcher, Runnable>> edit : edits.entrySet()) {
			Matcher matcher = edit.getKey().matcher(words);
			if (matcher.matches()) {
				return edit.getValue().apply(matcher);
			}
		}
		throw new IllegalArgumentException(words);
	}

	// as the Marker editor puts a marker on a measure: the update marker action adds it, or changes the one there
	@Given("I have added the marker {string} at measure {int}")
	public void addMarker(String title, int measure) {
		TGMarker marker = world.fixture().songManager.getFactory().newMarker();
		marker.setMeasure(measure);
		marker.setTitle(title);
		runOnMarker(new TGUpdateMarkerAction(world.fixture().context), marker);
	}

	// as the Marker list removes the measure's marker
	private void removeMarker(int measure) {
		runOnMarker(new TGRemoveMarkerAction(world.fixture().context), world.fixture().songManager.getMarker(world.song(), measure));
	}

	// the marker action on the marker, one undo step; its undo and redo run the other one too
	private void runOnMarker(TGActionBase action, TGMarker marker) {
		world.fixture().map(new TGUpdateMarkerAction(world.fixture().context), new TGRemoveMarkerAction(world.fixture().context));
		world.fixture().actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MARKER, marker);
		world.executeUndoable(action, new TGUndoableMarkerGenericController());
	}

	@Then("track {int} reads {string} and the markers read {string}")
	public void trackAndMarkersRead(int number, String track, String markers) {
		assertEquals(track + " | " + markers, soloAndMute(world.track(number)) + " | " + markers());
	}

	// "solo", "mute", "solo mute" or "-"
	private static String soloAndMute(TGTrack track) {
		String state = ((track.isSolo() ? "solo " : "") + (track.isMute() ? "mute" : "")).trim();
		return (state.isEmpty() ? "-" : state);
	}

	// "m2 Intro, m3 Verse", in measure order
	private String markers() {
		return world.fixture().songManager.getMarkers(world.song()).stream().map(marker -> "m" + marker.getMeasure() + " " + marker.getTitle())
			.collect(Collectors.joining(", "));
	}
}
