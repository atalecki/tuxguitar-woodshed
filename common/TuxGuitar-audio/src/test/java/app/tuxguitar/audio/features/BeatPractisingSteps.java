package app.tuxguitar.audio.features;

import static app.tuxguitar.audio.features.PractisingSteps.first;
import static app.tuxguitar.audio.features.PractisingSteps.last;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import app.tuxguitar.audio.Meters;
import app.tuxguitar.audio.TGPlayedMeasure;
import app.tuxguitar.audio.TGTimeline;
import app.tuxguitar.audio.features.HeardNotes.Note;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Then;

/**
 * Steps that measure the turn of one beat of interleaved practice played offline (README, A beat; The joint): the turn
 * whose first beat is the one named ("m3b2"), as PractisingSteps measures turns (its notes on track 1, beat by beat),
 * or, where the notes are not one per beat, the notes it sounds from one position to another: each a sine at its pitch
 * from its first sample (Test Sine) up to the next one's, where the turn plays those positions.
 */
public class BeatPractisingSteps {

	// -60 dBFS
	private static final double SILENT = 0.001;
	// a note's sine is fitted from so many samples after its start, over at most so many
	private static final int EDGE = 64;
	private static final int FIT = 2048;
	private static final Pattern SOUNDED = Pattern.compile("(?:MIDI note )?(\\d+)(?: from (m\\d+b[\\d.]+))? to (m\\d+b[\\d.]+)");

	private final AudioWorld world;
	private final PractisingSteps practising;

	public BeatPractisingSteps(AudioWorld world, PractisingSteps practising) {
		this.world = world;
		this.practising = practising;
	}

	// "MIDI note 39, then MIDI note 40" or "MIDI note 99 alone": the notes, one per beat
	@ParameterType("MIDI note \\d+(?: alone|(?:, then MIDI note \\d+)*)")
	public List<Integer> beatNotes(String text) {
		return Arrays.stream(text.replace(" alone", "").split(", then ")).map(note -> Integer.valueOf(note.replace("MIDI note ", ""))).collect(Collectors.toList());
	}

	/** A note sounded from one position to another. */
	static final class Sounded {
		final int note;
		final SongPosition from;
		final SongPosition to;

		Sounded(int note, SongPosition from, SongPosition to) {
			this.note = note;
			this.from = from;
			this.to = to;
		}
	}

	// "MIDI note 62 from m3b3 to m3b3.5, 64 to m3b4, then 65 to m4b1": each note from where the one before ends
	@ParameterType("MIDI note \\d+ from m\\d+b[\\d.]+ to m\\d+b[\\d.]+(?:(?:, then |, )(?:MIDI note )?\\d+ to m\\d+b[\\d.]+)*")
	public List<Sounded> soundedNotes(String text) {
		List<Sounded> notes = new ArrayList<Sounded>();
		Matcher note = SOUNDED.matcher(text);
		while (note.find()) {
			SongPosition from = (note.group(2) != null ? SongPosition.parse(note.group(2)) : notes.get(notes.size() - 1).to);
			notes.add(new Sounded(Integer.parseInt(note.group(1)), from, SongPosition.parse(note.group(3))));
		}
		return notes;
	}

	// the turn (of the timeline, from 0) whose first beat is the measure's beat: the first such
	private int turnOf(int measure, int beat) {
		long tick = SongPosition.parse("m" + measure + "b" + beat).tick(world.song());
		List<List<Note>> turns = practising.turns();
		int turn = IntStream.range(0, turns.size()).filter(each -> first(turns.get(each)).beat.tick == tick).findFirst().orElse(-1);
		assertTrue(turn >= 0, "no turn of m" + measure + "b" + beat);
		return turn;
	}

	@Then("the turn of m{int}b{int} sounds {beatNotes}, each within 1 sample of its beat")
	public void theTurnSoundsItsBeats(int measure, int beat, List<Integer> notes) {
		List<Note> turn = practising.turns().get(turnOf(measure, beat));
		String name = "the turn of m" + measure + "b" + beat;
		assertEquals(notes, PractisingSteps.notes(turn), name);
		PractisingSteps.assertOnTheBeat(turn, practising.percent(), name);
	}

	// from where its first beat sounds to where its last one ends
	@Then("the turn of m{int}b{int} lasts {int} samples, within 1 sample")
	public void theTurnLasts(int measure, int beat, int samples) {
		List<Note> turn = practising.turns().get(turnOf(measure, beat));
		assertEquals(samples, last(turn).beat.sample + last(turn).beat.length - first(turn).beat.sample, 1, "the turn of m" + measure + "b" + beat);
	}

	@Then("the turn of m{int}b{int} sounds {soundedNotes}, each within 1 sample")
	public void theTurnSoundsTheNotes(int measure, int beat, List<Sounded> notes) {
		assertSounds(turnOf(measure, beat), notes);
	}

	// and, before, between and after them in the turn, the track sounds nothing above -60 dBFS
	@Then("the turn of m{int}b{int} sounds {soundedNotes}, each within 1 sample, and nothing else above -60 dBFS")
	public void theTurnSoundsTheNotesAlone(int measure, int beat, List<Sounded> notes) {
		int turn = turnOf(measure, beat);
		int[][] sounding = assertSounds(turn, notes);
		float[] tap = tap();
		List<Note> beats = practising.turns().get(turn);
		int at = first(beats).beat.sample;
		for (int[] note : sounding) {
			assertTrue(Meters.peak(tap, at, note[0]) < SILENT, "the turn sounds before " + note[0] + ", from " + at);
			at = note[1];
		}
		int end = last(beats).beat.sample + last(beats).beat.length;
		assertTrue(Meters.peak(tap, at, end) < SILENT, "the turn sounds after " + at + ", to " + end);
	}

	private float[] tap() {
		return practising.heard().tap(world.track(1));
	}

	// each note sounds from where the turn plays its first position to where it plays its second: a sine at its pitch
	// from its first sample (within 1 sample), its pitch the one heard there: {start, end} of each
	private int[][] assertSounds(int turn, List<Sounded> notes) {
		float[] tap = tap();
		int[][] sounding = new int[notes.size()][];
		for (int i = 0; i < notes.size(); i++) {
			Sounded note = notes.get(i);
			int start = sampleIn(turn, note.from);
			int end = sampleIn(turn, note.to);
			String name = "MIDI note " + note.note + " from " + start + " to " + end;
			assertEquals(note.note, HeardNotes.nearest(Meters.frequency(tap, start + EDGE, end - EDGE, TestInterface.SAMPLE_RATE)), name);
			assertEquals(start, HeardNotes.sineStart(tap, note.note, start + EDGE, Math.min(start + EDGE + FIT, end - EDGE), start), 1, name);
			sounding[i] = new int[] {start, end};
		}
		return sounding;
	}

	// where the turn (of the timeline, from 0) plays the position: in the measure that plays it, or where the measure
	// that stops there ends
	private int sampleIn(int turn, SongPosition position) {
		TGTimeline timeline = practising.heard().timeline();
		long tick = position.tick(world.song());
		for (int index = timeline.firstOf(turn); index < timeline.firstOf(turn + 1); index++) {
			TGPlayedMeasure measure = timeline.get(index);
			long into = tick - measure.getHeader().getStart();
			if (into >= measure.getFrom() && into <= measure.getTo()) {
				return (int) (into < measure.getTo() ? timeline.sampleAt(index, into) : timeline.end(index));
			}
		}
		throw new IllegalArgumentException("turn " + (turn + 1) + " does not play " + tick);
	}
}
