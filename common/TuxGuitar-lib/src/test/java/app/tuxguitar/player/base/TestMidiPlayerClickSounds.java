package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * The click sounds the player plays: a synthesizer with drums of its own (the TuxGuitar Synthesizer's built-in click) is
 * told the click's sounds and volume, and the folder of the user's own sounds, as parameters of its drums channel, when
 * the channel opens and whenever they change; a port is told nothing (its click is a drum note at a velocity). Settings'
 * Try plays beat 1, then an other beat, of a click it is given, on the channel Count In clicks on, leaving the player's
 * click as it was.
 *
 * What a channel is sent is written as PortSynthesizer writes it, a click's parameter as "param" and its key after
 * "midi.click." ("-2 param volume -3").
 */
public class TestMidiPlayerClickSounds {

	private final PlayingFixture fixture = new PlayingFixture();
	private final MidiPlayer player = fixture.player;
	// what the synthesizer's channels were sent
	private final List<String> received = new ArrayList<String>();

	private void open(MidiSynthesizer synthesizer) throws MidiPlayerException {
		player.getSynthesizerProxy().setMidiSynthesizer(synthesizer);
		player.updateChannels();
	}

	private void openWithDrums() throws MidiPlayerException {
		open(new ClockedSynthesizer(received, true));
	}

	// what the channel with the id was sent since the last time, notes and click parameters
	private String sent(int id) {
		String sent = received.stream().filter(line -> line.startsWith(id + " ") && !line.contains(" cc ") && !line.contains(" opened"))
			.map(line -> line.substring(line.indexOf(' ') + 1)).collect(Collectors.joining(", "));
		received.clear();
		return sent;
	}

	@Test
	public void theDrumsAreToldTheClickWhenTheyOpen() throws MidiPlayerException {
		openWithDrums();

		assertEquals("param beat-one same-accented, param other click, param volume -3", sent(MidiSampleClock.DRUMS));
	}

	@Test
	public void theDrumsAreToldAClickSetWhileTheyAreOpen() throws MidiPlayerException {
		openWithDrums();
		sent(MidiSampleClock.DRUMS);
		player.setClick(new MidiClick(MidiClickSound.CLAP, MidiClickSound.COWBELL, 6));

		assertEquals("param beat-one clap, param other cowbell, param volume 6", sent(MidiSampleClock.DRUMS));
	}

	@Test
	public void theDrumsAreToldTheFolderOfTheUsersOwnSounds() throws MidiPlayerException {
		openWithDrums();
		sent(MidiSampleClock.DRUMS);
		player.setClickSoundsFolder("C:/Users/me/click-sounds");
		String told = sent(MidiSampleClock.DRUMS);
		player.updateChannels();

		assertEquals("C:/Users/me/click-sounds | param beat-one same-accented, param folder C:/Users/me/click-sounds, param other click, param volume -3 | "
			+ "param beat-one same-accented, param folder C:/Users/me/click-sounds, param other click, param volume -3",
			player.getClickSoundsFolder() + " | " + told + " | " + sent(MidiSampleClock.DRUMS));
	}

	// a click set before any synthesizer is open is told the drums once they open
	@Test
	public void aClickSetBeforeTheDrumsOpenIsToldThemWhenTheyOpen() throws MidiPlayerException {
		player.setClick(MidiClick.sameAccented(MidiClickSound.HI_HAT, -12));
		openWithDrums();

		assertEquals("param beat-one same-accented, param other hi-hat, param volume -12", sent(MidiSampleClock.DRUMS));
	}

	// on the drums: told the click tried, beat 1 accented, an other beat plain (each at full velocity, the drums play the
	// volume), then told the player's click again; about 0.4 s apart
	@Test
	public void tryPlaysTheClickGivenOnTheDrumsAndLeavesThePlayersClick() throws MidiPlayerException {
		openWithDrums();
		sent(MidiSampleClock.DRUMS);
		player.tryClick(new MidiClick(MidiClickSound.CLAP, MidiClickSound.SNARE, 9));
		long start = System.nanoTime();
		fixture.threads.runStarted();
		long millis = (System.nanoTime() - start) / 1000000;

		assertEquals("param beat-one clap, param other snare, param volume 9, on 76 127, off 76 127, on 77 127, off 77 127, "
			+ "param beat-one same-accented, param other click, param volume -3", sent(MidiSampleClock.DRUMS));
		assertEquals(MidiClick.DEFAULT, player.getClick());
		assertTrue(millis >= 400 && millis < 1000, millis + " ms");
	}

	// on a port, without a percussion channel: the drum notes of the click tried at its velocities, on the player's own
	// percussion channel, as Count In; a port is told no parameters
	@Test
	public void tryPlaysTheClickGivenOnAPortAsCountIn() throws MidiPlayerException {
		open(new PortSynthesizer(received));
		received.clear();
		player.tryClick(MidiClick.sameAccented(MidiClickSound.COWBELL, -6));
		fixture.threads.runStarted();

		assertEquals("on 56 90, off 56 90, on 56 64, off 56 64", sent(MidiClickChannels.CLICKS));
	}
}
