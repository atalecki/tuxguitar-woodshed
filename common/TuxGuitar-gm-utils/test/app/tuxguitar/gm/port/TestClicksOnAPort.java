package app.tuxguitar.gm.port;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import app.tuxguitar.document.TGDocumentManager;
import app.tuxguitar.gm.GMChannelRouter;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.player.base.MidiPlayerCountDown;
import app.tuxguitar.player.base.MidiPlayerException;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;

/**
 * The clicks of a song without a percussion channel on a port that sends General MIDI out (GMSynthesizer: Java Sound,
 * a MIDI port, FluidSynth): they sound on MIDI channel 9 (General MIDI channel 10, the drums every General MIDI
 * synthesizer has), set up as a new percussion channel (bank 128, program 0, Volume 127, Balance 64), and the song
 * keeps its one channel. The Count In plays them here, one tick; the Metronome plays them on the same channel (see
 * lib's TestMidiPlayerMetronome).
 *
 * The Count In plays its one tick at 10 times the tempo, so it lasts some 50 ms. The port writes what it sends on MIDI
 * channel 9 as "cc CONTROLLER VALUE", "program VALUE", "on KEY VELOCITY" and "off KEY VELOCITY".
 */
public class TestClicksOnAPort {

	private final TGContext context = new TGContext();
	private final MidiPlayer player = MidiPlayer.getInstance(context);
	private final TGSong song = TGDocumentManager.getInstance(context).getSong();
	private final List<String> sent = new ArrayList<String>();

	private class Port extends GMOutputPort implements GMReceiver {
		public String getKey() { return "port"; }
		public String getName() { return "port"; }
		public void open() { }
		public void close() { }
		public void check() { }
		public GMReceiver getReceiver() { return this; }

		private void send(int channel, String message) {
			if (channel == GMChannelRouter.PERCUSSION_CHANNEL) {
				sent.add(message);
			}
		}

		public void sendAllNotesOff() { }
		public void sendNoteOn(int channel, int key, int velocity) { send(channel, "on " + key + " " + velocity); }
		public void sendNoteOff(int channel, int key, int velocity) { send(channel, "off " + key + " " + velocity); }
		public void sendProgramChange(int channel, int value) { send(channel, "program " + value); }
		public void sendControlChange(int channel, int controller, int value) { send(channel, "cc " + controller + " " + value); }
		public void sendPitchBend(int channel, int value) { }
	}

	// the bank comes twice: the channel, rerouted to MIDI channel 9 by it, sends again what it was sent so far (as a song's
	// percussion channel does)
	@Test
	public void theCountInClicksOnMidiChannel9SetUpAsANewPercussionChannel() throws MidiPlayerException {
		player.getSynthesizerProxy().setMidiSynthesizer(new Port().getSynthesizer());
		player.updateChannels();
		MidiPlayerCountDown countDown = player.getCountDown();
		countDown.setEnabled(true);
		countDown.setTickCount(1);
		countDown.setTempoPercent(1000);
		player.setRunning(true);
		player.updateLoop(true);
		countDown.start();

		assertEquals("[cc 0 128, cc 0 128, program 0, cc 7 127, cc 10 64, on 37 95, off 37 95] | 1",
				settingsAndNotes() + " | " + song.countChannels());
	}

	// the bank, program, Volume and Balance it was sent, and the notes
	private List<String> settingsAndNotes() {
		return sent.stream().filter(line -> line.matches("cc (0|7|10) .*|program .*|on .*|off .*")).collect(Collectors.toList());
	}
}
