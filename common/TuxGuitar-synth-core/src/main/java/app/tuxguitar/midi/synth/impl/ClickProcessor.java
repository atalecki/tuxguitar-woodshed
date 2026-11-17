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

/**
 * The built-in click: the one sound of Count In and the Metronome on the TuxGuitar Synthesizer, live (its own drums,
 * see TGSynthModel) and offline (the drum tap), made in code. A 1500 Hz sine from phase 0, its level rising in a
 * straight line over 0.2 ms, then falling by e every 5 ms, silent 30 ms after it starts; at 0.5 times the note's
 * velocity / 127, the same left and right, whatever the note (the drum note is for other MIDI Ports) but
 * MidiClick.ACCENTED: beat 1's click, accented, a 2000 Hz sine 3 dB louder (0.5 * 10^(3/20), about 0.71, times the
 * velocity / 127) with the same rise and fall. A note clicks from its frame, with no latency; a click sounds out
 * whatever follows it (note offs, bends and controls change nothing).
 *
 * Notes come from the audio thread before a block (timed) and from other threads (Count In's timer, untimed): they wait
 * in a queue that only the thread filling the blocks drains, so the clicks sounding are that thread's alone.
 */
public class ClickProcessor implements TGMidiProcessor {

	private static final double FREQUENCY = 1500;
	private static final double ACCENTED_FREQUENCY = 2000;
	// an accented click's level, times a plain one's: 3 dB louder
	private static final double ACCENT = Math.pow(10, 3 / 20.0);
	private static final double ATTACK = 0.0002;
	private static final double DECAY = 0.005;
	private static final double LENGTH = 0.03;
	private static final double LEVEL = 0.5;
	private static final int MAX_VELOCITY = 127;

	// a click: from where it starts, at a gain of the sound, accented or not; counted from the next block until it is
	// drained, then from the processor's first frame
	private static final class Click {
		private final long start;
		private final float gain;
		private final boolean accented;

		private Click(long start, float gain, boolean accented) {
			this.start = start;
			this.gain = gain;
			this.accented = accented;
		}
	}

	private final Queue<Click> sent = new ConcurrentLinkedQueue<Click>();
	private final List<Click> sounding = new ArrayList<Click>();
	// the plain click and the accented one at velocity 127, a sample per frame
	private float[] plain = sound(TGAudioBuffer.SAMPLE_RATE, 1f, false);
	private float[] accented = sound(TGAudioBuffer.SAMPLE_RATE, 1f, true);
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

	public void setSampleRate(float sampleRate) {
		this.plain = sound(sampleRate, 1f, false);
		this.accented = sound(sampleRate, 1f, true);
	}

	private float[] sound(Click click) {
		return (click.accented ? this.accented : this.plain);
	}

	// the clicks over the next block, left and right alike
	public void process(float[][] block) {
		this.drain();
		for (float[] side : block) {
			Arrays.fill(side, 0f);
			for (Click click : this.sounding) {
				add(this.sound(click), click, this.frames, side);
			}
		}
		this.frames += block[0].length;
		this.dropEnded();
	}

	private void drain() {
		for (Click click = this.sent.poll(); click != null; click = this.sent.poll()) {
			if (this.open) {
				this.sounding.add(new Click(this.frames + click.start, click.gain, click.accented));
			}
		}
	}

	// the part of the click that falls in the side, whose first frame is the frame from
	private static void add(float[] sound, Click click, long from, float[] side) {
		long offset = click.start - from;
		int first = (int) within(offset, side.length);
		int last = (int) within(offset + sound.length, side.length);
		for (int frame = first; frame < last; frame++) {
			side[frame] += click.gain * sound[(int) (frame - offset)];
		}
	}

	private static long within(long frame, int length) {
		return Math.max(0, Math.min(length, frame));
	}

	// by index from the end: the audio thread allocates no iterator
	private void dropEnded() {
		for (int index = this.sounding.size() - 1; index >= 0; index--) {
			if (this.sounding.get(index).start + this.plain.length <= this.frames) {
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

	// a note on is a click at its velocity, accented for MidiClick.ACCENTED, plain for any other note
	public void sendNoteOn(int key, int velocity, int voice, boolean bendMode, long frame) {
		this.click(velocity, frame, key == MidiClick.ACCENTED);
	}

	// a click at the velocity, accented or not, on a frame of the next fill (counted from its first frame); the drum
	// tap's clicks
	public void click(int velocity, long frame, boolean accented) {
		this.sent.add(new Click(frame, gain(velocity), accented));
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

	// it has no parameters
	public void storeParameters(Map<String, String> parameters) {
	}

	public void restoreParameters(Map<String, String> parameters) {
	}
}
