package app.tuxguitar.player.base;

import java.util.List;

/**
 * A synthesizer that sends MIDI out, as a MIDI port, Java Sound or FluidSynth does: no sample clock, no drums of its
 * own. Each channel it opens writes its opening, the programs, controls and notes it is sent, and its closing, one line
 * each, the channel's id first ("-3 opened", "-3 program 0", "-3 cc 7 127", "-3 on 37 95", "-3 off 37 95", "-3 closed").
 */
class PortSynthesizer implements MidiSynthesizer {

	final List<String> received;

	PortSynthesizer(List<String> received) {
		this.received = received;
	}

	private final class Channel implements MidiChannel {
		private final int id;

		Channel(int id) {
			this.id = id;
		}

		private void log(String line) { received.add(this.id + " " + line); }

		public void sendControlChange(int controller, int value) { log("cc " + controller + " " + value); }
		public void sendProgramChange(int value) { log("program " + value); }
		public void sendNoteOn(int key, int velocity, int voice, boolean bendMode) { log("on " + key + " " + velocity); }
		public void sendNoteOff(int key, int velocity, int voice, boolean bendMode) { log("off " + key + " " + velocity); }
		public void sendPitchBend(int value, int voice, boolean bendMode) { }
		public void sendParameter(String key, String value) { }
		public void sendAllNotesOff() { }
	}

	public MidiChannel openChannel(int channelId) {
		this.received.add(channelId + " opened");
		return new Channel(channelId);
	}
	public void closeChannel(MidiChannel midiChannel) { this.received.add(((Channel) midiChannel).id + " closed"); }
	public boolean isChannelOpen(MidiChannel midiChannel) { return true; }
	public boolean isBusy() { return false; }
}
