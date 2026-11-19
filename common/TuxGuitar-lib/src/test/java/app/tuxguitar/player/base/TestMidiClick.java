package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGChannel;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.configuration.TGConfigManager;
import app.tuxguitar.util.properties.MapProperties;
import app.tuxguitar.util.properties.TGPropertiesManager;

/**
 * The click Count In and the Metronome play, the player's own setting: beat 1's sound (the other beats' sound played
 * accented, until another is chosen), the other beats' sound, and the click volume in dB from -30 to +12 over the
 * loudest click a General MIDI velocity gives. The code-made Click, beat 1 accented, at -3 dB (the velocity TuxGuitar
 * clicked at before, 95) until changed. On a synthesizer's own drums every click is sent at full velocity, accented or
 * plain, and the drums are told the sounds and the volume (its parameters); on any other channel a click is its sound's
 * General MIDI drum note at the velocity of the volume.
 */
public class TestMidiClick {

	private static final int PORT = 9;

	// beat 1's sound (or "same, accented"), the other beats' and the volume
	private static String describe(MidiClick click) {
		return click.getBeatOne().map(MidiClickSound::getId).orElse("accented") + " " + click.getOther() + " " + click.getVolume();
	}

	@Test
	public void isTheCodeMadeClickAccentedAtMinus3DecibelsUntilChanged() {
		MidiPlayer player = MidiPlayer.getInstance(new TGContext());

		assertEquals("accented click -3", describe(player.getClick()));
	}

	@Test
	public void isTheOneThePlayerIsGiven() {
		MidiPlayer player = MidiPlayer.getInstance(new TGContext());
		player.setClick(new MidiClick(MidiClickSound.CLAVES, MidiClickSound.WOOD_BLOCK, 6));

		assertEquals("claves wood-block 6", describe(player.getClick()));
	}

	// the song's one track, on a percussion channel, has no notes: every note of the sequence is the Metronome's
	@Test
	public void isWhatThePlayersMetronomeTicks() {
		PlayingFixture fixture = new PlayingFixture();
		TGChannel percussion = fixture.songManager.createChannel();
		percussion.setBank(TGChannel.DEFAULT_PERCUSSION_BANK);
		fixture.song.addChannel(percussion);
		fixture.song.getTrack(0).setChannelId(percussion.getChannelId());
		fixture.player.setClick(MidiClick.sameAccented(MidiClickSound.WOOD_BLOCK, 0));
		fixture.player.updateLoop(true);
		fixture.player.addSequence();

		List<String> notes = fixture.sequencer.sequence.events("on");
		assertEquals(16, notes.stream().filter(note -> note.endsWith(" c" + percussion.getChannelId() + " k76 v127 s-1 b0")).count(), notes.toString());
	}

	@Test
	public void keepsTheLowestAndHighestVolume() {
		assertEquals("accented cowbell -30, snare clap 12", describe(MidiClick.sameAccented(MidiClickSound.COWBELL, -30)) + ", "
			+ describe(new MidiClick(MidiClickSound.SNARE, MidiClickSound.CLAP, 12)));
	}

	@Test
	public void refusesAVolumeOutsideItsRangeOrNoSound() {
		assertThrows(IllegalArgumentException.class, () -> MidiClick.sameAccented(MidiClickSound.CLICK, -31));
		assertThrows(IllegalArgumentException.class, () -> MidiClick.sameAccented(MidiClickSound.CLICK, 13));
		assertThrows(IllegalArgumentException.class, () -> MidiClick.sameAccented(null, 0));
		assertThrows(IllegalArgumentException.class, () -> new MidiClick(null, MidiClickSound.CLICK, 0));
	}

	// plain, then accented
	private static String velocities(int volume) {
		MidiClick click = MidiClick.sameAccented(MidiClickSound.CLICK, volume);
		return click.getVelocity(PORT, false) + " " + click.getVelocity(MidiClickChannels.CLICKS, true);
	}

	// a General MIDI port: the volume as a velocity, 127 * 10^(dB / 20), from 1 to 127 (above 0 dB, 127); beat 1 played
	// accented is 3 dB louder, beat 1 of a sound of its own is not
	@Test
	public void elsewhereTheVolumeIsAVelocity() {
		assertEquals("4 6 90 127 127 127 | 90 64", velocities(-30) + " " + velocities(-3) + " " + velocities(12)
			+ " | " + MidiClick.sameAccented(MidiClickSound.CLICK, -6).getVelocity(PORT, true)
			+ " " + new MidiClick(MidiClickSound.CLAP, MidiClickSound.CLICK, -6).getVelocity(PORT, true));
	}

	// on a synthesizer's own drums (the built-in click) an accented and a plain note at full velocity: the drums play the
	// sounds and the volume they are told
	@Test
	public void onTheDrumsAClickIsAccentedOrPlainAtFullVelocity() {
		MidiClick click = new MidiClick(MidiClickSound.CLAP, MidiClickSound.HI_HAT, -20);

		assertEquals("76 127 77 127", click.getNote(MidiSampleClock.DRUMS, true) + " " + click.getVelocity(MidiSampleClock.DRUMS, true)
			+ " " + click.getNote(MidiSampleClock.DRUMS, false) + " " + click.getVelocity(MidiSampleClock.DRUMS, false));
	}

	// elsewhere the drum note of the sound: beat 1's, the other beats' when beat 1 is played accented
	@Test
	public void elsewhereAClickIsTheDrumNoteOfItsSound() {
		MidiClick click = new MidiClick(MidiClickSound.CLAP, MidiClickSound.HI_HAT, 0);
		MidiClick accented = MidiClick.sameAccented(MidiClickSound.COWBELL, 0);

		assertEquals("39 42 56 56", click.getNote(PORT, true) + " " + click.getNote(MidiClickChannels.CLICKS, false)
			+ " " + accented.getNote(PORT, true) + " " + accented.getNote(PORT, false));
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
		String none = describe(MidiClick.read(config));
		new MidiClick(MidiClickSound.own("Mine.wav"), MidiClickSound.SNARE, 12).write(config);
		String own = describe(MidiClick.read(config));
		MidiClick.sameAccented(MidiClickSound.CLAVES, -30).write(config);

		assertEquals("accented click -3 | file:Mine.wav snare 12 | accented claves -30", none + " | " + own + " | " + describe(MidiClick.read(config)));
	}

	@Test
	public void keptUnderItsOwnKeys() {
		TGConfigManager config = config();
		new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, -12).write(config);
		MidiClick accented = MidiClick.sameAccented(MidiClickSound.SNARE, 1);

		assertEquals("clap snare -12 | same-accented", config.getStringValue(MidiClick.BEAT_ONE) + " " + config.getStringValue(MidiClick.OTHER)
			+ " " + config.getStringValue(MidiClick.VOLUME) + " | " + accented.toParameters(null).get(MidiClick.BEAT_ONE));
	}

	// a configuration edited by hand: a sound that is none (or since removed from TuxGuitar) is the Click, beat 1's is
	// the other beats' accented; a volume that is no number the default one, outside the range the nearest end
	@Test
	public void keptThatIsNoSoundOrVolumeFallsBack() {
		TGConfigManager config = config();
		config.setValue(MidiClick.BEAT_ONE, "gong");
		config.setValue(MidiClick.OTHER, "file:../up.wav");
		config.setValue(MidiClick.VOLUME, "loud");
		String none = describe(MidiClick.read(config));
		config.setValue(MidiClick.OTHER, "same-accented");
		config.setValue(MidiClick.VOLUME, 40);
		String high = describe(MidiClick.read(config));
		config.setValue(MidiClick.VOLUME, -99);

		assertEquals("accented click -3 | accented click 12 | accented click -30", none + " | " + high + " | " + describe(MidiClick.read(config)));
	}

	private static String migrated(int note, int velocity) {
		TGConfigManager config = config();
		config.setValue(MidiClick.NOTE, note);
		config.setValue(MidiClick.VELOCITY, velocity);
		return describe(MidiClick.read(config));
	}

	// a configuration kept before: the drum note's built-in sound (else the Click), beat 1 accented, the velocity as a
	// volume, round(20 * log10(velocity / 127)) dB, from -30; the default one (Side Stick at 95) is the default click
	@Test
	public void keptBeforeAsANoteAndVelocityIsMigrated() {
		assertEquals("accented wood-block 0 | accented click -3 | accented click -30 | accented hi-hat -6",
			migrated(76, 127) + " | " + migrated(37, 95) + " | " + migrated(60, 1) + " | " + migrated(42, 64));
	}

	// kept before outside MIDI (edited by hand): the default click; the lowest and highest notes are MIDI
	@Test
	public void keptBeforeOutsideMidiIsTheDefault() {
		assertEquals("accented click -3 | accented click -3 | accented click 0 | accented click 0", migrated(128, 127) + " | " + migrated(76, 0)
			+ " | " + migrated(0, 127) + " | " + migrated(127, 127));
	}

	// written, the note and velocity kept before are gone: the click is kept once
	@Test
	public void writtenTheNoteAndVelocityKeptBeforeAreGone() {
		TGConfigManager config = config();
		config.setValue(MidiClick.NOTE, 76);
		config.setValue(MidiClick.VELOCITY, 127);
		MidiClick.DEFAULT.write(config);

		assertEquals("null null accented click -3", config.getStringValue(MidiClick.NOTE) + " " + config.getStringValue(MidiClick.VELOCITY) + " " + describe(MidiClick.read(config)));
	}

	// what the synthesizer's drums are told: the sounds and the volume, and the folder of the user's own sounds
	@Test
	public void theDrumsParametersGiveBackTheClickAndTheFolder() {
		MidiClick click = new MidiClick(MidiClickSound.own("Mine.wav"), MidiClickSound.COWBELL, -7);
		Map<String, String> parameters = click.toParameters("C:/click-sounds");

		assertEquals("file:Mine.wav cowbell -7 C:/click-sounds | false", describe(MidiClick.fromParameters(parameters)) + " " + parameters.get(MidiClick.FOLDER)
			+ " | " + click.toParameters(null).containsKey(MidiClick.FOLDER));
	}

	@Test
	public void noParametersAreTheDefaultClick() {
		assertEquals("accented click -3", describe(MidiClick.fromParameters(new HashMap<String, String>())));
	}

	// the volume's gain, 10^(dB / 20): 1 at 0 dB
	@Test
	public void theGainOfTheVolume() {
		assertEquals("1.0 3.981 0.5012", MidiClick.sameAccented(MidiClickSound.CLICK, 0).getGain() + " " + Math.round(MidiClick.sameAccented(MidiClickSound.CLICK, 12).getGain() * 1000) / 1000.0
			+ " " + Math.round(MidiClick.sameAccented(MidiClickSound.CLICK, -6).getGain() * 10000) / 10000.0);
	}

	// the click of the setting kept before, a General MIDI note at a velocity (Settings until it offers the sounds)
	@Test
	public void theClickOfANoteAndVelocity() {
		assertEquals("accented cowbell -2 | accented click 0", describe(MidiClick.ofMidi(56, 100)) + " | " + describe(MidiClick.ofMidi(60, 127)));
	}

	@Test
	public void clicksOfTheSameSoundsAndVolumeAreEqual() {
		MidiClick click = new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, 3);

		assertEquals("true true false false false false", click.equals(new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, 3)) + " "
			+ (click.hashCode() == new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, 3).hashCode()) + " " + click.equals(MidiClick.sameAccented(MidiClickSound.SNARE, 3))
			+ " " + click.equals(new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, 2)) + " " + click.equals(new MidiClick(MidiClickSound.CLAP, MidiClickSound.CLICK, 3))
			+ " " + click.equals("clap snare 3"));
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
}
