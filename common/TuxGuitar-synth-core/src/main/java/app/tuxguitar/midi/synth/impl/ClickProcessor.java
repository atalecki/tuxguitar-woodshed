package app.tuxguitar.midi.synth.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.tuxguitar.midi.synth.TGAudioBuffer;
import app.tuxguitar.midi.synth.TGMidiProcessor;

/**
 * The built-in click: the one sound of Count In and the Metronome on the TuxGuitar Synthesizer, live (its own drums,
 * see TGSynthModel) and offline (the drum tap), made in code. A 1500 Hz sine from phase 0, its level rising in a
 * straight line over 0.2 ms, then falling by e every 5 ms, silent 30 ms after it starts; at 0.5 times the note's
 * velocity / 127, the same left and right, whatever the note (the drum note is for other MIDI Ports). A note clicks from
 * its frame, with no latency; a click sounds out whatever follows it (note offs, bends and controls change nothing).
 *
 * Notes come from the audio thread before a block (timed) and from other threads (Count In's timer, untimed): they wait
 * in a queue that only the thread filling the blocks drains, so the clicks sounding are that thread's alone.
 */
public class ClickProcessor implements TGMidiProcessor {

	private static final double FREQUENCY = 1500;
	private static final double ATTACK = 0.0002;
	private static final double DECAY = 0.005;
	private static final double LENGTH = 0.03;
	private static final double LEVEL = 0.5;
	private static final int MAX_VELOCITY = 127;

	// a click: from where it starts, at a gain of the sound; counted from the next block until it is drained, then from
	// the processor's first frame
	private static final class Click {
		private final long start;
		private final float gain;

		private Click(long start, float gain) {
			this.start = start;
			this.gain = gain;
		}
	}

	private final Queue<Click> sent = new ConcurrentLinkedQueue<Click>();
	private final List<Click> sounding = new ArrayList<Click>();
	// the click at velocity 127, a sample per frame
	private float[] sound = sound(TGAudioBuffer.SAMPLE_RATE);
	// the frames filled since it opened
	private long frames;
	private volatile boolean open = true;

	// the click at the rate and velocity, a sample per frame: what a note at that velocity sounds from its frame
	public static float[] sound(float sampleRate, int velocity) {
		float[] sound = sound(sampleRate);
		float gain = gain(velocity);
		for (int frame = 0; frame < sound.length; frame++) {
			sound[frame] *= gain;
		}
		return sound;
	}

	private static float[] sound(float sampleRate) {
		float[] sound = new float[(int) Math.round(LENGTH * sampleRate)];
		for (int frame = 0; frame < sound.length; frame++) {
			double time = frame / (double) sampleRate;
			sound[frame] = (float) (LEVEL * Math.min(1, time / ATTACK) * Math.exp(-time / DECAY) * Math.sin(2 * Math.PI * FREQUENCY * time));
		}
		return sound;
	}

	private static float gain(int velocity) {
		return velocity / (float) MAX_VELOCITY;
	}

	public void setSampleRate(float sampleRate) {
		this.sound = sound(sampleRate);
	}

	// the clicks over the next block, left and right alike
	public void process(float[][] block) {
		this.drain();
		for (float[] side : block) {
			Arrays.fill(side, 0f);
			for (Click click : this.sounding) {
				add(this.sound, click, this.frames, side);
			}
		}
		this.frames += block[0].length;
		this.dropEnded();
	}

	private void drain() {
		for (Click click = this.sent.poll(); click != null; click = this.sent.poll()) {
			if (this.open) {
				this.sounding.add(new Click(this.frames + click.start, click.gain));
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
			if (this.sounding.get(index).start + this.sound.length <= this.frames) {
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

	public void sendNoteOn(int key, int velocity, int voice, boolean bendMode, long frame) {
		this.sent.add(new Click(frame, gain(velocity)));
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
