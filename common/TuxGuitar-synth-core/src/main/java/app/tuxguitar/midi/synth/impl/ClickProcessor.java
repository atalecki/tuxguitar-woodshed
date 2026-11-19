package app.tuxguitar.midi.synth.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.tuxguitar.midi.synth.TGAudioBuffer;
import app.tuxguitar.midi.synth.TGMidiProcessor;
import app.tuxguitar.player.base.MidiClick;
import app.tuxguitar.player.base.MidiClickSound;

/**
 * The built-in click: the sound of Count In and the Metronome on the TuxGuitar Synthesizer, live (its own drums, see
 * TGSynthModel) and offline (the drum tap). It plays the click it is told (restoreParameters, MidiClick.toParameters):
 * beat 1's sound for MidiClick.ACCENTED, the other beats' sound for any other note, at the click volume (10^(dB/20))
 * times the note's velocity / 127, the same left and right. Until told, and told the default sounds at 0 dB, it is the
 * code-made Click, bit for bit as always: a 1500 Hz sine from phase 0, its level rising in a straight line over 0.2 ms,
 * then falling by e every 5 ms, silent 30 ms after it starts, at 0.5; accented, a 2000 Hz sine 3 dB louder with the
 * same rise and fall. A recording (TGClickRecording) plays at the synthesizer's rate; beat 1 played as the other
 * beats' recording accented, 2 semitones higher and 3 dB louder. A note clicks from its frame, with no latency; a click
 * sounds out whatever follows it (note offs, bends and controls change nothing), each as long as its sound.
 *
 * Told a click, it reads and evens its recordings then (not on the audio thread) and swaps the sounds at once (Voices,
 * immutable, through a volatile field); a note takes the sounds there when it comes, so a click sounds what it was sent
 * with to its end. Notes come from the audio thread before a block (timed) and from other threads (Count In's timer,
 * untimed): they wait in a queue that only the thread filling the blocks drains, so the clicks sounding are that
 * thread's alone.
 */
public class ClickProcessor implements TGMidiProcessor {

	private static final double FREQUENCY = 1500;
	private static final double ACCENTED_FREQUENCY = 2000;
	// an accented click's level, times a plain one's: 3 dB louder
	private static final double ACCENT = Math.pow(10, MidiClick.ACCENT_DECIBELS / 20.0);
	private static final double ATTACK = 0.0002;
	private static final double DECAY = 0.005;
	private static final double LENGTH = 0.03;
	private static final double LEVEL = 0.5;
	private static final int MAX_VELOCITY = 127;

	// a click: from where it starts, its sound at a gain; counted from the next block until it is drained, then from the
	// processor's first frame
	private static final class Click {
		private final long start;
		private final float gain;
		private final float[] sound;

		private Click(long start, float gain, float[] sound) {
			this.start = start;
			this.gain = gain;
			this.sound = sound;
		}
	}

	// the sounds of the click told, at the rate, a sample per frame at velocity 127 and 0 dB, and the volume's gain
	private static final class Voices {
		private final float[] beatOne;
		private final float[] other;
		private final float gain;

		private Voices(float[] beatOne, float[] other, float gain) {
			this.beatOne = beatOne;
			this.other = other;
			this.gain = gain;
		}
	}

	private final Queue<Click> sent = new ConcurrentLinkedQueue<Click>();
	private final List<Click> sounding = new ArrayList<Click>();
	// the click told (none: the code-made Click accented, at 0 dB), and the folder of the user's own sounds
	private MidiClick click;
	private String folder;
	private float sampleRate = TGAudioBuffer.SAMPLE_RATE;
	private volatile Voices voices = voices(null, null, TGAudioBuffer.SAMPLE_RATE);
	// the frames filled since it opened
	private long frames;
	private volatile boolean open = true;

	// the click at the rate and velocity, a sample per frame: what a note at that velocity sounds from its frame
	public static float[] sound(float sampleRate, int velocity) {
		return sound(sampleRate, velocity, false);
	}

	// accented or not
	public static float[] sound(float sampleRate, int velocity, boolean accented) {
		return sound(sampleRate, gain(velocity), accented);
	}

	// at the gain, as a note adds it to a block: the first frame is silent (phase 0, no level yet)
	private static float[] sound(float sampleRate, float gain, boolean accented) {
		double level = (accented ? LEVEL * ACCENT : LEVEL);
		double frequency = (accented ? ACCENTED_FREQUENCY : FREQUENCY);
		float[] sound = new float[(int) Math.round(LENGTH * sampleRate)];
		for (int frame = 1; frame < sound.length; frame++) {
			double time = frame / (double) sampleRate;
			sound[frame] = gain * (float) (level * Math.min(1, time / ATTACK) * Math.exp(-time / DECAY) * Math.sin(2 * Math.PI * frequency * time));
		}
		return sound;
	}

	private static float gain(int velocity) {
		return velocity / (float) MAX_VELOCITY;
	}

	// the sounds of the click at the rate, its recordings read from the folder; none, the code-made Click accented
	private static Voices voices(MidiClick click, String folder, float sampleRate) {
		MidiClick told = (click != null ? click : MidiClick.sameAccented(MidiClickSound.CLICK, 0));
		float[] other = plain(told.getOther(), folder, sampleRate);
		float[] beatOne = told.getBeatOne().map(sound -> plain(sound, folder, sampleRate)).orElseGet(() -> accented(told.getOther(), folder, sampleRate));
		return new Voices(beatOne, other, (float) told.getGain());
	}

	// the sound as it is: its recording, else the code-made Click
	private static float[] plain(MidiClickSound sound, String folder, float sampleRate) {
		TGClickRecording recording = TGClickSounds.recording(sound, folder);
		return (recording != null ? recording.at((int) sampleRate, 0) : sound(sampleRate, 1f, false));
	}

	// the sound accented: its recording 2 semitones higher and 3 dB louder, else the code-made Click's accent
	private static float[] accented(MidiClickSound sound, String folder, float sampleRate) {
		TGClickRecording recording = TGClickSounds.recording(sound, folder);
		if (recording == null) {
			return sound(sampleRate, 1f, true);
		}
		float[] higher = recording.at((int) sampleRate, MidiClick.ACCENT_SEMITONES);
		for (int i = 0; i < higher.length; i++) {
			higher[i] *= (float) ACCENT;
		}
		return higher;
	}

	public synchronized void setSampleRate(float sampleRate) {
		this.sampleRate = sampleRate;
		this.voices = voices(this.click, this.folder, sampleRate);
	}

	// the clicks over the next block, left and right alike
	public void process(float[][] block) {
		this.drain();
		for (float[] side : block) {
			Arrays.fill(side, 0f);
			for (Click click : this.sounding) {
				add(click, this.frames, side);
			}
		}
		this.frames += block[0].length;
		this.dropEnded();
	}

	private void drain() {
		for (Click click = this.sent.poll(); click != null; click = this.sent.poll()) {
			if (this.open) {
				this.sounding.add(new Click(this.frames + click.start, click.gain, click.sound));
			}
		}
	}

	// the part of the click that falls in the side, whose first frame is the frame from
	private static void add(Click click, long from, float[] side) {
		long offset = click.start - from;
		int first = (int) within(offset, side.length);
		int last = (int) within(offset + click.sound.length, side.length);
		for (int frame = first; frame < last; frame++) {
			side[frame] += click.gain * click.sound[(int) (frame - offset)];
		}
	}

	private static long within(long frame, int length) {
		return Math.max(0, Math.min(length, frame));
	}

	// by index from the end: the audio thread allocates no iterator
	private void dropEnded() {
		for (int index = this.sounding.size() - 1; index >= 0; index--) {
			Click click = this.sounding.get(index);
			if (click.start + click.sound.length <= this.frames) {
				this.sounding.remove(index);
			}
		}
	}

	public void fillBuffer(TGAudioBuffer buffer) {
		float[][] block = new float[TGAudioBuffer.CHANNELS][TGAudioBuffer.BUFFER_SIZE / 2];
		this.process(block);
		buffer.write(block);
	}

	public void sendNoteOn(int key, int velocity, int voice, boolean bendMode) {
		this.sendNoteOn(key, velocity, voice, bendMode, 0);
	}

	// a note on is a click at its velocity, beat 1's for MidiClick.ACCENTED, the other beats' for any other note
	public void sendNoteOn(int key, int velocity, int voice, boolean bendMode, long frame) {
		this.click(velocity, frame, key == MidiClick.ACCENTED);
	}

	// a click at the velocity, beat 1's or the other beats', on a frame of the next fill (counted from its first frame),
	// with the sounds told now; the drum tap's clicks
	public void click(int velocity, long frame, boolean accented) {
		Voices voices = this.voices;
		this.sent.add(new Click(frame, gain(velocity) * voices.gain, (accented ? voices.beatOne : voices.other)));
	}

	public void sendNoteOff(int key, int velocity, int voice, boolean bendMode) {
	}

	public void sendPitchBend(int value, int voice, boolean bendMode) {
	}

	public void sendControlChange(int controller, int value) {
	}

	// silent once closed: what is sounding stops, and notes sent later are dropped
	public void close() {
		this.open = false;
		this.sent.clear();
		this.sounding.clear();
	}

	// nothing to free
	public void finalize() {
	}

	public boolean isOpen() {
		return this.open;
	}

	public boolean isBusy() {
		return false;
	}

	// the click is the player's, not kept with a song: it stores nothing
	public void storeParameters(Map<String, String> parameters) {
	}

	// told a click (MidiClick.toParameters), it plays its sounds from now on; parameters that tell none change nothing
	public synchronized void restoreParameters(Map<String, String> parameters) {
		if (parameters.containsKey(MidiClick.OTHER)) {
			this.click = MidiClick.fromParameters(parameters);
			this.folder = parameters.get(MidiClick.FOLDER);
			this.voices = voices(this.click, this.folder, this.sampleRate);
		}
	}
}
