package app.tuxguitar.benchmarks;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import app.tuxguitar.midi.synth.TGAudioBuffer;
import app.tuxguitar.midi.synth.TGAudioBufferProcessor;
import app.tuxguitar.midi.synth.TGAudioProcessor;
import app.tuxguitar.midi.synth.TGAudioProcessorFactory;
import app.tuxguitar.midi.synth.TGLevelTap;
import app.tuxguitar.midi.synth.TGMidiProcessor;
import app.tuxguitar.midi.synth.TGMidiProcessorFactory;
import app.tuxguitar.midi.synth.TGProgram;
import app.tuxguitar.midi.synth.TGProgramElement;
import app.tuxguitar.midi.synth.TGSynthChannel;
import app.tuxguitar.midi.synth.TGSynthManager;
import app.tuxguitar.midi.synth.TGSynthModel;
import app.tuxguitar.util.TGContext;

/**
 * The level meters on the audio thread (TGLevelTap, measured by the synthesizer's channels): one tap measuring a block,
 * and a whole block of 16 channels, each a float source and three float effects, with the levels on (81 taps) and off.
 * The audio thread must allocate nothing per block: gc.alloc.rate.norm should be about 0 B/op for all three.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class LevelTapBenchmark {

	private static final int FRAMES = TGAudioBuffer.BUFFER_SIZE / 2;
	private static final int CHANNELS = 16;

	@Param({"true", "false"})
	public boolean metered;

	private TGLevelTap tap;
	private float[][] block;
	private TGAudioBufferProcessor blocks;

	@Setup
	public void setUp() throws Exception {
		this.tap = new TGLevelTap();
		this.block = new float[2][FRAMES];
		for (int i = 0; i < FRAMES; i++) {
			this.block[0][i] = (float) Math.sin(i * 0.0627);
			this.block[1][i] = (float) Math.cos(i * 0.0627) * 0.5f;
		}
		TGContext context = new TGContext();
		TGSynthManager manager = TGSynthManager.getInstance(context);
		manager.addExtension(TGMidiProcessorFactory.class, new SourceFactory());
		manager.addExtension(TGAudioProcessorFactory.class, new GainFactory());
		TGSynthModel model = new TGSynthModel(context);
		model.getProgram(0, 0).copyFrom(chain());
		for (int c = 0; c < CHANNELS; c++) {
			((TGSynthChannel) model.openChannel(c)).sendProgramChange(0);
		}
		if (this.metered) {
			model.getLevels().watch();
		}
		this.blocks = new TGAudioBufferProcessor(model);
	}

	// a source, then three effects
	private static TGProgram chain() {
		TGProgram program = new TGProgram();
		for (int e = 0; e <= 3; e++) {
			TGProgramElement element = new TGProgramElement();
			element.setId("element" + e);
			element.setType(e == 0 ? SourceFactory.TYPE : GainFactory.TYPE);
			if (e == 0) {
				program.setReceiver(element);
			} else {
				program.addOutput(element);
			}
		}
		return program;
	}

	@Benchmark
	public TGLevelTap tapMeasuresABlock() {
		this.tap.measure(this.block);
		return this.tap;
	}

	@Benchmark
	public TGAudioBuffer aBlockOfSixteenChannels() {
		this.blocks.process();
		return this.blocks.getBuffer();
	}

	// a processor working on float blocks, as a plugin does
	private abstract static class FloatProcessor implements TGAudioProcessor {
		public void close() {
		}

		public void finalize() {
		}

		public boolean isOpen() {
			return true;
		}

		public boolean isBusy() {
			return false;
		}

		public void fillBuffer(TGAudioBuffer buffer) {
		}

		public void storeParameters(Map<String, String> parameters) {
		}

		public void restoreParameters(Map<String, String> parameters) {
		}
	}

	// a sine at -6 dB on both sides
	private static final class Source extends FloatProcessor implements TGMidiProcessor {
		private long frame;

		public void process(float[][] block) {
			for (int i = 0; i < block[0].length; i++) {
				float sample = 0.5f * (float) Math.sin(this.frame++ * 0.0627);
				block[0][i] = sample;
				block[1][i] = sample;
			}
		}

		public void sendNoteOn(int key, int velocity, int voice, boolean bendMode) {
		}

		public void sendNoteOff(int key, int velocity, int voice, boolean bendMode) {
		}

		public void sendPitchBend(int value, int voice, boolean bendMode) {
		}

		public void sendControlChange(int controller, int value) {
		}
	}

	private static final class Gain extends FloatProcessor {
		public void process(float[][] block) {
			for (float[] side : block) {
				for (int i = 0; i < side.length; i++) {
					side[i] *= 0.9f;
				}
			}
		}
	}

	private static final class SourceFactory implements TGMidiProcessorFactory {
		static final String TYPE = "bench-source";

		public String getType() {
			return TYPE;
		}

		public TGMidiProcessor createProcessor() {
			return new Source();
		}
	}

	private static final class GainFactory implements TGAudioProcessorFactory {
		static final String TYPE = "bench-gain";

		public String getType() {
			return TYPE;
		}

		public TGAudioProcessor createProcessor() {
			return new Gain();
		}
	}
}
