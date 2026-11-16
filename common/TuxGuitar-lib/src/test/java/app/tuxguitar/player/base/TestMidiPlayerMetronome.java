package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.song.models.TGChannel;

/**
 * The Metronome while the song plays live: the player makes its clicks a track of the sequence it plays (the last one,
 * muted while the Metronome is off), the click note held a beat on every beat of every measure played.
 *
 * The song has 4 measures of 4/4 and one track; it plays through a FakeSequencer (see PlayingFixture), which keeps
 * the sequence. A click is written "on TICK cCHANNEL kKEY vVELOCITY".
 */
public class TestMidiPlayerMetronome {

	private final PlayingFixture fixture = new PlayingFixture();
	private final MidiPlayer player = fixture.player;

	// a percussion channel as a song file may have it: with id 0
	private void withPercussion() {
		TGChannel channel = fixture.songManager.createChannel();
		channel.setChannelId(0);
		channel.setBank(TGChannel.DEFAULT_PERCUSSION_BANK);
		fixture.song.addChannel(channel);
	}

	// plays: how many clicks the sequence has | its first | its last
	private String clicks() throws MidiPlayerException {
		player.play();
		String track = " t" + (fixture.song.countTracks() + 1) + " ";
		List<String> clicks = fixture.sequencer.sequence.events("on").stream()
				.filter(event -> event.contains(track))
				.map(event -> event.replace(track, " ").replaceAll(" s-?\\d+ b\\d$", ""))
				.collect(Collectors.toList());
		return clicks.size() + (clicks.isEmpty() ? "" : " | " + clicks.get(0) + " | " + clicks.get(clicks.size() - 1));
	}

	// what the synthesizer's channels were sent
	private final List<String> received = new ArrayList<String>();

	// plays on a synthesizer with a sample clock, with General MIDI drums of its own (the TuxGuitar Synthesizer) or
	// without: the clicks, as clicks() writes them
	private String clicksOnASampleClock(boolean drums) throws MidiPlayerException {
		player.getSynthesizerProxy().setMidiSynthesizer(new ClockedSynthesizer(received, drums));
		return clicks();
	}

	// plays on a port (a synthesizer without a sample clock or drums of its own: a MIDI port, Java Sound, FluidSynth):
	// the clicks, as clicks() writes them
	private String clicksOnAPort() throws MidiPlayerException {
		player.getSynthesizerProxy().setMidiSynthesizer(new PortSynthesizer(received));
		return clicks();
	}

	// what the synthesizer's channel with the id was sent
	private List<String> channel(int id) {
		return received.stream().filter(line -> line.startsWith(id + " ")).collect(Collectors.toList());
	}

	// what the clock's drums channel was sent
	private List<String> drums() {
		return channel(MidiSampleClock.DRUMS);
	}

	// a new percussion channel's settings, as the player sends every channel's (scaled by its volume, 10 the whole)
	private static String newPercussionChannel(int id, int volume) {
		return "[" + id + " opened, " + id + " cc 0 128, " + id + " program 0, " + id + " cc 7 " + volume + ", " + id + " cc 10 64, "
				+ id + " cc 93 0, " + id + " cc 91 0, " + id + " cc 95 0, " + id + " cc 92 0, " + id + " cc 11 127, "
				+ id + " cc 101 0, " + id + " cc 100 0, " + id + " cc 6 12, " + id + " cc 38 0]";
	}

	// a MIDI Port without a sample clock (one that sends MIDI out): on the song's percussion channel
	@Test
	public void onAPortWithoutASampleClockTheMetronomeClicksOnThePercussionChannel() throws MidiPlayerException {
		withPercussion();

		assertEquals("16 | on 960 c0 k37 v95 | on 15360 c0 k37 v95", clicks());
	}

	// without one, on a percussion channel of the player's own (CLICKS), not the song's: opened as playing starts with
	// a new percussion channel's settings, so a port gives it General MIDI channel 10 (see TestClicksOnAPort in
	// gm-utils); the song keeps its one channel
	@Test
	public void onAPortWithoutAPercussionChannelTheMetronomeClicksOnAPercussionChannelOfThePlayersOwn() throws MidiPlayerException {
		assertEquals("16 | on 960 c-3 k37 v95 | on 15360 c-3 k37 v95 | " + newPercussionChannel(-3, 127) + " | 1",
				clicksOnAPort() + " | " + channel(MidiClickChannels.CLICKS) + " | " + fixture.song.countChannels());
	}

	// at the player's volume, as every channel
	@Test
	public void thePlayersPercussionChannelPlaysAtThePlayersVolume() throws MidiPlayerException {
		player.setVolume(5);
		clicksOnAPort();

		assertEquals(newPercussionChannel(-3, 63), channel(MidiClickChannels.CLICKS).toString());
	}

	// with a percussion channel, on it: the player opens no channel of its own, and closes the one it had once the song
	// has one
	@Test
	public void onAPortWithAPercussionChannelTheMetronomeClicksOnItAndThePlayerHasNoChannelOfItsOwn() throws MidiPlayerException {
		String without = clicksOnAPort();
		withPercussion();
		received.clear();

		assertEquals("16 | on 960 c-3 k37 v95 | on 15360 c-3 k37 v95 / 16 | on 960 c0 k37 v95 | on 15360 c0 k37 v95 | [-3 closed]",
				without + " / " + clicks() + " | " + channel(MidiClickChannels.CLICKS));
	}

	// on a sample clock without drums of its own, as on a port without one (the TuxGuitar Synthesizer's before it had
	// drums): on the song's percussion channel (its sound chain); on the TuxGuitar Synthesizer's clock, on its own drums,
	// outside every sound chain
	@Test
	public void onASampleClockTheMetronomeClicksOnItsDrumsWhenItHasThem() throws MidiPlayerException {
		withPercussion();

		assertEquals("16 | on 960 c0 k37 v95 | on 15360 c0 k37 v95 / 16 | on 960 c-2 k37 v95 | on 15360 c-2 k37 v95",
				clicksOnASampleClock(false) + " / " + clicksOnASampleClock(true));
	}

	@Test
	public void onASampleClockWithoutDrumsTheMetronomeClicksOnThePlayersPercussionChannelWithoutOne() throws MidiPlayerException {
		assertEquals("16 | on 960 c-3 k37 v95 | on 15360 c-3 k37 v95 | []", clicksOnASampleClock(false) + " | " + drums());
	}

	// with drums, percussion channel or not: the player opens no percussion channel of its own
	@Test
	public void onASampleClockWithDrumsTheMetronomeClicksWithoutAPercussionChannel() throws MidiPlayerException {
		assertEquals("16 | on 960 c-2 k37 v95 | on 15360 c-2 k37 v95 | []", clicksOnASampleClock(true) + " | " + channel(MidiClickChannels.CLICKS));
	}

	// the drums open when playing starts, at the percussion channel's Volume and Balance (scaled by the player's volume,
	// as every channel's), and close with the channels
	@Test
	public void theDrumsPlayAtThePercussionChannelsVolumeAndBalance() throws MidiPlayerException {
		withPercussion();
		fixture.song.getChannel(fixture.song.countChannels() - 1).setVolume((short) 100);
		fixture.song.getChannel(fixture.song.countChannels() - 1).setBalance((short) 20);
		player.setVolume(5);
		clicksOnASampleClock(true);
		player.closeChannels();

		assertEquals("[-2 opened, -2 cc 7 50, -2 cc 10 20, -2 closed]", drums().toString());
	}

	// a new channel's, 127 and 64, without a percussion channel; playing again keeps the drums open
	@Test
	public void withoutAPercussionChannelTheDrumsPlayAtFullVolumeInTheCentre() throws MidiPlayerException {
		clicksOnASampleClock(true);
		player.play();

		assertEquals("[-2 opened, -2 cc 7 127, -2 cc 10 64, -2 cc 7 127, -2 cc 10 64]", drums().toString());
	}

	@Test
	public void theMetronomeClicksTheClickSound() throws MidiPlayerException {
		withPercussion();
		player.setClick(new MidiClick(76, 127));

		assertEquals("16 | on 960 c0 k76 v127 | on 15360 c0 k76 v127", clicks());
	}
}
