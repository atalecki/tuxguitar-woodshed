package app.tuxguitar.player.base;

import java.util.List;

/**
 * A synthesizer that sends MIDI out, as a MIDI port, Java Sound or FluidSynth does: no sample clock, no drums of its
 * own. Each channel it opens writes its opening, the programs, controls and notes it is sent, and its closing, one line
 * each, the channel's id first ("-3 opened", "-3 program 0", "-3 cc 7 127", "-3 on 37 95", "-3 off 37 95", "-3 closed", a note's voice and
 * bend mode after it unless no voice and not in bend mode: "-3 on 64 95 s0 bfalse"), and the
 * click's parameters ("-2 param volume -3").
 */
class PortSynthesizer implements MidiSynthesizer {

	// the click's parameters (see MidiClick.toParameters), written; the others are not
	private static final String CLICK = "midi.click.";

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
		public void sendNoteOn(int key, int velocity, int voice, boolean bendMode) { log("on " + key + " " + velocity + voice(voice, bendMode)); }
		public void sendNoteOff(int key, int velocity, int voice, boolean bendMode) { log("off " + key + " " + velocity + voice(voice, bendMode)); }
		public void sendPitchBend(int value, int voice, boolean bendMode) { }
		public void sendParameter(String key, String value) { if (key.startsWith(CLICK)) log("param " + key.substring(CLICK.length()) + " " + value); }
		public void sendAllNotesOff() { }
	}

	// a note's voice and bend mode, written when not those of a click (no voice, not in bend mode)
	private static String voice(int voice, boolean bendMode) {
		return (voice == -1 && !bendMode ? "" : " s" + voice + " b" + bendMode);
	}

	public MidiChannel openChannel(int channelId) {
		this.received.add(channelId + " opened");
		return new Channel(channelId);
	}
	public void closeChannel(MidiChannel midiChannel) { this.received.add(((Channel) midiChannel).id + " closed"); }
	public boolean isChannelOpen(MidiChannel midiChannel) { return true; }
	public boolean isBusy() { return false; }
}
