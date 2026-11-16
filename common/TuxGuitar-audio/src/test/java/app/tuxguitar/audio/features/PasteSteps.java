package app.tuxguitar.audio.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.action.measure.TGCopyMeasureAction;
import app.tuxguitar.editor.action.measure.TGPasteMeasureAction;
import app.tuxguitar.editor.clipboard.TGClipboard;
import app.tuxguitar.editor.undo.impl.song.TGUndoableSongGenericController;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGString;
import app.tuxguitar.song.models.TGTimeSignature;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.song.models.TGVoice;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Steps that put notes on a track, and copy and paste measures of one track.
 */
public class PasteSteps {

	private final AudioWorld world;

	public PasteSteps(AudioWorld world) {
		this.world = world;
	}

	// fret 5 on string 1, a quarter note on each beat of every measure
	@Given("track {int} has a quarter note on every beat")
	public void aQuarterNoteOnEveryBeat(int number) {
		TGTrack track = world.track(number);
		for (int index = 0; index < track.countMeasures(); index++) {
			for (int quarter = 0; quarter < 4; quarter++) {
				world.fixture().note(world.fixture().beat(track.getMeasure(index), quarter), 1, 5);
			}
		}
	}

	// in each measure's own time signature, a note of the beat's duration on each beat, its MIDI note the number given
	// plus the measure's number (from 1): on the highest string tuned at or below it, and below the lowest string on one
	// more string tuned to the number given (35: a 7-string guitar's low B)
	@Given("track {int} has a note on every beat, MIDI note {int} plus the measure's number")
	public void aNoteOnEveryBeatByMeasure(int number, int base) {
		TGTrack track = world.track(number);
		if (base + 1 < lowest(track).getValue()) {
			TGString low = world.fixture().songManager.getFactory().newString();
			low.setNumber(track.stringCount() + 1);
			low.setValue(base);
			track.getStrings().add(low);
		}
		for (int index = 0; index < track.countMeasures(); index++) {
			TGMeasure measure = track.getMeasure(index);
			TGTimeSignature signature = measure.getTimeSignature();
			int value = signature.getDenominator().getValue();
			for (int beat = 0; beat < signature.getNumerator(); beat++) {
				note(measure, beat * TGDuration.QUARTER_TIME * 4 / value, value, base + measure.getNumber());
			}
			assertEquals(Collections.nCopies(signature.getNumerator(), base + measure.getNumber()), pitches(measure));
		}
	}

	// a note of the MIDI note, of that duration (a denominator: 4 a quarter) in voice 0, so many ticks after the measure
	// start, on the highest string tuned at or below it
	private void note(TGMeasure measure, long tick, int value, int pitch) {
		TGString string = measure.getTrack().getStrings().stream().filter(each -> each.getValue() <= pitch).max(Comparator.comparingInt(TGString::getValue)).get();
		world.fixture().note(beat(measure, tick, value), string.getNumber(), pitch - string.getValue());
	}

	private static final Map<String, Integer> LENGTHS = Map.of("a whole note", TGDuration.WHOLE, "a half note", TGDuration.HALF, "a quarter note", TGDuration.QUARTER,
		"an eighth note", TGDuration.EIGHTH);

	// the measures' notes replaced by the rows': at its "position" (README, Position: "m3b3.5"), its "MIDI note", its
	// "length" ("a half note")
	@Given("track {int} has these notes in measures {int} and {int} instead of one on every beat:")
	public void theseNotesInstead(int number, int first, int last, List<Map<String, String>> rows) {
		TGTrack track = world.track(number);
		IntStream.rangeClosed(first, last).forEach(measure -> track.getMeasure(measure - 1).clear());
		for (Map<String, String> row : rows) {
			SongPosition position = SongPosition.parse(row.get("position"));
			TGMeasure measure = track.getMeasure(position.measure() - 1);
			note(measure, position.tick(world.song()) - measure.getStart(), LENGTHS.get(row.get("length")), Integer.parseInt(row.get("MIDI note")));
		}
	}

	private static TGString lowest(TGTrack track) {
		return track.getStrings().stream().min(Comparator.comparingInt(TGString::getValue)).get();
	}

	// a new beat of that duration (a denominator: 4 a quarter) in voice 0, so many ticks after the measure start
	private TGBeat beat(TGMeasure measure, long tick, int value) {
		TGBeat beat = world.fixture().songManager.getFactory().newBeat();
		beat.setPreciseStart(TGDuration.toPreciseTime(measure.getStart() + tick));
		TGVoice voice = beat.getVoice(0);
		voice.setEmpty(false);
		voice.getDuration().setValue(value);
		world.fixture().songManager.getMeasureManager().addBeat(measure, beat);
		return beat;
	}

	// the MIDI notes of the measure's notes, beat by beat
	private static List<Integer> pitches(TGMeasure measure) {
		List<Integer> pitches = new ArrayList<Integer>();
		for (TGBeat beat : measure.getBeats()) {
			for (TGNote note : beat.getVoice(0).getNotes()) {
				pitches.add(measure.getTrack().getString(note.getString()).getValue() + note.getValue());
			}
		}
		return pitches;
	}

	@Given("I have copied measure {int} of track {int}")
	public void copyMeasure(int measure, int number) {
		world.fixture().actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, world.track(number));
		world.fixture().actionContext.setAttribute(TGCopyMeasureAction.ATTRIBUTE_MEASURE_NUMBER_1, measure);
		world.fixture().actionContext.setAttribute(TGCopyMeasureAction.ATTRIBUTE_MEASURE_NUMBER_2, measure);
		world.fixture().actionContext.setAttribute(TGCopyMeasureAction.ATTRIBUTE_ALL_TRACKS, Boolean.FALSE);
		world.execute(new TGCopyMeasureAction(world.fixture().context));
		assertNotNull(TGClipboard.getInstance(world.fixture().context).getSegment());
	}

	// replacing the measure, once, as desktop's TGActionConfigMap runs it: undoable as a whole song
	@When("I paste it over measure {int} of track {int}")
	public void pasteOver(int measure, int number) {
		TGTrack track = world.track(number);
		world.fixture().actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_TRACK, track);
		world.fixture().actionContext.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_MEASURE, track.getMeasure(measure - 1));
		world.fixture().actionContext.setAttribute(TGPasteMeasureAction.ATTRIBUTE_PASTE_MODE, TGPasteMeasureAction.TRANSFER_TYPE_REPLACE);
		world.fixture().actionContext.setAttribute(TGPasteMeasureAction.ATTRIBUTE_PASTE_COUNT, 1);
		world.executeUndoable(new TGPasteMeasureAction(world.fixture().context), new TGUndoableSongGenericController());
	}

	@Then("track {int} still has {int} notes in measure {int}")
	public void stillHasNotes(int number, int notes, int measure) {
		TGMeasure trackMeasure = world.track(number).getMeasure(measure - 1);
		assertEquals(notes, trackMeasure.getBeats().stream().mapToInt((TGBeat beat) -> beat.getVoice(0).countNotes()).sum());
	}

	// on the audio track (track 2 in the takes feature)
	@Given("I have copied measure {int} and pasted it over measure {int}")
	public void copyAndPaste(int from, int to) {
		copyMeasure(from, 2);
		pasteOver(to, 2);
		assertTrue(world.succeeded());
	}
}
