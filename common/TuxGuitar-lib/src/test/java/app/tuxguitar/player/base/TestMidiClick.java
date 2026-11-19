package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.song.models.TGVelocities;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.util.properties.MapProperties;
import app.tuxguitar.util.properties.TGPropertiesManager;

/**
 * The click sound Count In and the Metronome play: a drum note from 0 to 127 at a velocity from 1 to 127 (0 would be
 * no note at all), Side Stick at the default velocity until changed, the player's own setting.
 */
public class TestMidiClick {

	private static String read(MidiClick click) {
		return click.getNote() + " at " + click.getVelocity();
	}

	@Test
	public void isSideStickAtTheDefaultVelocityUntilChanged() {
		MidiPlayer player = MidiPlayer.getInstance(new TGContext());

		assertEquals("37 at " + TGVelocities.DEFAULT, read(player.getClick()));
	}

	@Test
	public void isTheOneThePlayerIsGiven() {
		MidiPlayer player = MidiPlayer.getInstance(new TGContext());
		player.setClick(new MidiClick(76, 127));

		assertEquals("76 at 127", read(player.getClick()));
	}

	// the song's one track, on a percussion channel, has no notes: every note of the sequence is the Metronome's
	@Test
	public void isWhatThePlayersMetronomeTicks() {
		PlayingFixture fixture = new PlayingFixture();
		TGChannel percussion = fixture.songManager.createChannel();
		percussion.setBank(TGChannel.DEFAULT_PERCUSSION_BANK);
		fixture.song.addChannel(percussion);
		fixture.song.getTrack(0).setChannelId(percussion.getChannelId());
		fixture.player.setClick(new MidiClick(76, 127));
		fixture.player.updateLoop(true);
		fixture.player.addSequence();

		List<String> notes = fixture.sequencer.sequence.events("on");
		assertEquals(16, notes.stream().filter(note -> note.endsWith(" c" + percussion.getChannelId() + " k76 v127 s-1 b0")).count(), notes.toString());
	}

	@Test
	public void keepsTheLowestAndHighestNoteAndVelocity() {
		assertEquals("0 at 1, 127 at 127", read(new MidiClick(0, 1)) + ", " + read(new MidiClick(127, 127)));
	}

	// a TuxGuitar setting: kept in a configuration of the app's, which a test keeps in memory
	private static TGConfigManager config() {
		TGContext context = new TGContext();
		TGPropertiesManager.getInstance(context).setPropertiesFactory(MapProperties::new);
		return new TGConfigManager(context, "tuxguitar");
	}

	@Test
	public void keptIsReadBackAsWrittenAndNothingKeptIsTheDefault() {
		TGConfigManager config = config();
		String none = read(MidiClick.read(config));
		new MidiClick(76, 127).write(config);
		assertEquals("37 at " + TGVelocities.DEFAULT + " | 76 at 127", none + " | " + read(MidiClick.read(config)));
	}

	// a configuration edited by hand: what is not a MIDI note and velocity is the default click
	@Test
	public void keptOutsideMidiIsTheDefault() {
		TGConfigManager config = config();
		config.setValue(MidiClick.NOTE, 128);
		config.setValue(MidiClick.VELOCITY, 127);
		String note = read(MidiClick.read(config));
		config.setValue(MidiClick.NOTE, 76);
		config.setValue(MidiClick.VELOCITY, 0);
		assertEquals("37 at " + TGVelocities.DEFAULT + " | 37 at " + TGVelocities.DEFAULT, note + " | " + read(MidiClick.read(config)));
	}

	@Test
	public void refusesANoteOrVelocityOutsideMidi() {
		assertThrows(IllegalArgumentException.class, () -> new MidiClick(-1, 95));
		assertThrows(IllegalArgumentException.class, () -> new MidiClick(128, 95));
		assertThrows(IllegalArgumentException.class, () -> new MidiClick(37, 0));
		assertThrows(IllegalArgumentException.class, () -> new MidiClick(37, 128));
	}

	// beat 1 of each measure: the first of each measure's worth of clicks, counted from 0
	@Test
	public void theFirstClickOfEachMeasureIsAccented() {
		assertEquals("[true, false, false, true, false, false, true, false]",
			java.util.stream.IntStream.range(0, 8).mapToObj(click -> MidiClick.isAccented(click, 3)).collect(java.util.stream.Collectors.toList()).toString());
	}

	// the accents of so many Count In clicks before a start that many ticks into a measure of numerator/denominator, "x"
	// for an accented click, "." for a plain one
	private static String countIn(int clicks, int numerator, int denominator, long from) {
		app.tuxguitar.song.models.TGTimeSignature signature = new app.tuxguitar.song.factory.TGFactory().newTimeSignature();
		signature.setNumerator(numerator);
		signature.getDenominator().setValue(denominator);
		return java.util.stream.IntStream.range(0, clicks).mapToObj(click -> MidiClick.isCountInAccented(click, clicks, signature, from) ? "x" : ".")
			.collect(java.util.stream.Collectors.joining());
	}

	// counted backwards from the start, in its measure's time signature: the clicks on beat 1 are accented. 4/4: 4
	// clicks "1 2 3 4", 6 "3 4 | 1 2 3 4", 9 "4 | 1 2 3 4 | 1 2 3 4"; 3/4: 2 "2 3"; 6/8 (6 eighths): 8 "5 6 | 1 2 3 4 5 6"
	@Test
	public void countInAccentsTheBarLinesBeforeTheDownbeat() {
		assertEquals("x... ..x... .x...x... .. ..x.....", countIn(4, 4, 4, 0) + " " + countIn(6, 4, 4, 0) + " " + countIn(9, 4, 4, 0)
			+ " " + countIn(2, 3, 4, 0) + " " + countIn(8, 6, 8, 0));
	}

	// before beat 3 of 4/4 (1920 ticks in): 4 clicks "3 4 | 1 2", 2 "1 2", 3 "4 | 1 2"; before beat 2 of 6/8 (480 ticks
	// in): 3 "5 6 1"
	@Test
	public void countInBeforeAStartOnABeatLeadsIntoThatBeat() {
		assertEquals("..x. x. .x. ..x", countIn(4, 4, 4, 1920) + " " + countIn(2, 4, 4, 1920) + " " + countIn(3, 4, 4, 1920)
			+ " " + countIn(3, 6, 8, 480));
	}

	// before a start between two beats no click falls on a beat of the song: the first is accented, then the first of
	// each measure's worth after it, as before (half way through beat 2 of 4/4: 960 + 480 ticks in; of 3/4)
	@Test
	public void countInBeforeAStartBetweenTwoBeatsAccentsItsFirstClick() {
		assertEquals("x...x. x..x", countIn(6, 4, 4, 1440) + " " + countIn(4, 3, 4, 1440));
	}

	// on a synthesizer's own drums (the built-in click) an accented and a plain note; elsewhere the drum note either way
	@Test
	public void aClickOnTheDrumsIsAccentedOrPlainAndElsewhereTheDrumNote() {
		MidiClick click = new MidiClick(42, 100);

		assertEquals("76 77 42 42", click.getNote(MidiSampleClock.DRUMS, true) + " " + click.getNote(MidiSampleClock.DRUMS, false)
			+ " " + click.getNote(9, true) + " " + click.getNote(MidiClickChannels.CLICKS, false));
	}
}
