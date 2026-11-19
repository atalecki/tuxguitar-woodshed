package app.tuxguitar.midi.synth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

import app.tuxguitar.player.base.MidiTransport;
import app.tuxguitar.util.TGContext;

public class TGSynthChannelProcessor {

	private TGAudioBuffer buffer;
	// the chain's block: 32-bit float from the source to the channel's 16-bit write, so it has room above full scale
	private float[][] block;
	private TGDelayLine delayLine;
	private TGMidiProcessor processor;
	private List<TGAudioProcessor> outputs;
	private Map<TGProgramElement, TGAudioProcessor> processorMap;
	// the element of each mapped processor, looked up without allocating (the audio thread asks every block)
	private Map<TGAudioProcessor, TGProgramElement> elements;
	private Object lock;

	private float sampleRate;
	private MidiTransport transport;
	// the synthesizer's level taps, and the channel this processor sounds, its points' taps keyed by it
	private TGLevels levels;
	private int channel;
	// the tap after each mapped processor, looked up without allocating; and the tap at the output
	private Map<TGAudioProcessor, TGLevelTap> taps;
	private TGLevelTap out;
	// the left and right gains of the block being filled, kept so that no block allocates them
	private final float[] gains = new float[TGLevelTap.SIDES];

	// its processors render at the sample rate and are told the song's state over each block in the transport; the sound
	// after each of its elements and at its output is measured into the levels' taps of the channel
	public TGSynthChannelProcessor(float sampleRate, MidiTransport transport, TGLevels levels, int channel) {
		this.sampleRate = sampleRate;
		this.transport = transport;
		this.levels = levels;
		this.channel = channel;
		this.taps = new IdentityHashMap<TGAudioProcessor, TGLevelTap>();
		this.out = levels.open(TGLevels.Key.out(channel));
		this.lock = new Object();
		this.buffer = new TGAudioBuffer();
		this.block = new float[TGAudioBuffer.CHANNELS][TGAudioBuffer.BUFFER_SIZE / 2];
		this.delayLine = new TGDelayLine();
		this.outputs = new ArrayList<TGAudioProcessor>();
		this.processorMap = new HashMap<TGProgramElement, TGAudioProcessor>();
		this.elements = new IdentityHashMap<TGAudioProcessor, TGProgramElement>();
	}

	public void open(TGContext context, TGProgram program) {
		synchronized (this.lock) {
			this.processor = null;
			this.outputs.clear();
			this.closeOrphanProcessors(program);
			this.levels.keep(this.channel, effects(program));
			this.openReceiver(context, program.getReceiver());
			this.openOutputs(context, program);
		}
	}

	private static List<String> effects(TGProgram program) {
		List<String> effects = new ArrayList<String>();
		for( int i = 0 ; i < program.countOutputs() ; i ++ ){
			effects.add(program.getOutput(i).getId());
		}
		return effects;
	}

	// the processor of the element (mapped) is the other element's from now on, when both are of the same type: opened
	// for it next (addProcessor), it is given its parameters
	public void handOver(TGProgramElement from, TGProgramElement to) {
		synchronized (this.lock) {
			if( from.getType().equals(to.getType()) ) {
				this.processorMap.put(to, this.processorMap.remove(from));
			}
		}
	}

	public void openReceiver(TGContext context, TGProgramElement receiver) {
		synchronized (this.lock) {
			TGMidiProcessor processor = (TGMidiProcessor) this.getAudioProcessor(receiver);
			if( processor == null || !processor.isOpen()) {
				TGSynthManager synthManager = TGSynthManager.getInstance(context);
				TGMidiProcessorFactory factory = synthManager.findMidiProcessorFactory(receiver.getType());
				if( factory != null ) {
					processor = factory.createProcessor();
					processor.setSampleRate(this.sampleRate);
					processor.setTransport(this.transport);
				}
			}
			if( processor != null ) {
				this.setMidiProcessor(receiver, processor);
			}
		}
	}

	public void openOutputs(TGContext context, TGProgram program) {
		synchronized (this.lock) {
			for( int i = 0 ; i < program.countOutputs() ;i ++){
				this.openOutput(context, program.getOutput(i));
			}
		}
	}

	public void openOutput(TGContext context, TGProgramElement output) {
		synchronized (this.lock) {
			TGAudioProcessor processor = this.getAudioProcessor(output);
			if( processor == null || !processor.isOpen()) {
				TGSynthManager synthManager = TGSynthManager.getInstance(context);
				TGAudioProcessorFactory factory = synthManager.findAudioProcessorFactory(output.getType());
				if( factory != null ) {
					processor = factory.createProcessor();
					processor.setSampleRate(this.sampleRate);
					processor.setTransport(this.transport);
				}
			}
			if( processor != null ) {
				this.addOutputProcessor(output, processor);
			}
		}
	}

	public void setMidiProcessor(TGProgramElement element, TGMidiProcessor processor) {
		this.processor = processor;
		this.addProcessor(element, processor);
		this.taps.put(processor, this.levels.open(TGLevels.Key.source(this.channel)));
	}

	public void addOutputProcessor(TGProgramElement element, TGAudioProcessor processor) {
		this.addOutput(processor);
		this.addProcessor(element, processor);
		this.taps.put(processor, this.levels.open(TGLevels.Key.effect(this.channel, element.getId())));
	}

	public void addProcessor(TGProgramElement element, TGAudioProcessor processor) {
		this.unmap(element);
		this.processorMap.put(element, processor);
		this.elements.put(processor, element);
		this.restoreParameters(element, processor);
	}

	public void restoreParameters(TGProgramElement element, TGAudioProcessor processor) {
		if( element.getParameters() != null ) {
			processor.restoreParameters(element.getParameters());
		}
	}

	public void close(){
		synchronized (this.lock) {
			this.closeOrphanProcessors(null);
		}
	}

	public void closeOrphanProcessors(TGProgram program) {
		synchronized (this.lock) {
			Map<TGProgramElement, TGAudioProcessor> mappedProcessors = new HashMap<TGProgramElement, TGAudioProcessor>(this.processorMap);
			for(TGProgramElement element : mappedProcessors.keySet()) {
				if( shouldCloseProcessor(program, element)) {
					closeProcessor(element);
				}
			}
		}
	}

	public void closeProcessor(TGProgramElement element) {
		synchronized (this.lock) {
			TGAudioProcessor mappedProcessor = this.getAudioProcessor(element);
			if( mappedProcessor != null ) {
				mappedProcessor.close();
				mappedProcessor.finalize();

				this.unmap(element);
			}
		}
	}

	// the element no longer has a processor, nor its processor (if any) an element or a tap
	private void unmap(TGProgramElement element) {
		TGAudioProcessor processor = this.processorMap.remove(element);
		this.elements.remove(processor);
		this.taps.remove(processor);
	}

	public boolean shouldCloseProcessor(TGProgram program, TGProgramElement element) {
		synchronized (this.lock) {
			if( program != null ) {
				if( program.getReceiver() != null && program.getReceiver().equals(element)) {
					return false;
				}
				for( int i = 0 ; i < program.countOutputs() ;i ++){
					if( program.getOutput(i).equals(element)) {
						return false;
					}
				}
			}
			return true;
		}
	}

	public TGProgramElement getElement(TGAudioProcessor processor) {
		synchronized (this.lock) {
			return this.elements.get(processor);
		}
	}

	public TGAudioProcessor getAudioProcessor(TGProgramElement element) {
		synchronized (this.lock) {
			if( this.processorMap.containsKey(element)) {
				return this.processorMap.get(element);
			}
			return null;
		}
	}

	public TGMidiProcessor getProcessor(){
		synchronized (this.lock) {
			return this.processor;
		}
	}

	public void addOutput(TGAudioProcessor effect){
		synchronized (this.lock) {
			TGSynthLists.addPresent(this.outputs, effect);
		}
	}

	public TGAudioProcessor getOutput( int index ){
		synchronized (this.lock) {
			return TGSynthLists.get(this.outputs, index);
		}
	}

	public int countOutputs(){
		synchronized (this.lock) {
			return this.outputs.size();
		}
	}

	// the chain delayed by the frames (unless its source delays itself), at the volume and balance, written in 16 bits
	// (clipped there) and mixed into the buffer. While the levels are on, the sound after each element and at the output
	// (as written: held at full scale) is measured. The audio thread calls it every block: it allocates nothing
	public void fillBuffer(TGAudioBuffer buffer, int volume, int balance, int delay) {
		synchronized (this.lock) {
			if( this.isOpen() ){
				boolean measured = this.levels.isOn();
				boolean compensated = this.processor.compensate(delay);
				this.fillChain(this.block, measured);
				this.delayLine.delay(this.block, (compensated ? 0 : delay));
				gains(volume, balance, this.gains);
				amplify(this.block, this.gains);
				measure(this.out, this.block, measured, 1f);
				this.buffer.write(this.block);
				buffer.mix(this.buffer);
			}
		}
	}

	private static void amplify(float[][] block, float[] gains) {
		for( int side = 0 ; side < block.length ; side ++ ) {
			for( int i = 0 ; i < block[side].length ; i ++ ) {
				block[side][i] *= gains[side];
			}
		}
	}

	// the receiver, then each output in processing order, from silence, in 32-bit float; silence when the receiver is
	// not open. While the levels are on, the sound after each enabled element is measured
	public void fillChain(float[][] block) {
		synchronized (this.lock) {
			this.fillChain(block, this.levels.isOn());
		}
	}

	private void fillChain(float[][] block, boolean measured) {
		for( float[] side : block ) {
			java.util.Arrays.fill(side, 0f);
		}
		if( this.isOpen() ){
			this.process(block, this.processor, measured);
			// by index: no iterator is allocated
			for( int i = 0; i < this.outputs.size(); i ++ ){
				this.process(block, this.outputs.get(i), measured);
			}
		}
	}

	// the block as it is here, into the tap (if any) when measured, each side held at the ceiling
	private static void measure(TGLevelTap tap, float[][] block, boolean measured, float ceiling) {
		if( measured && tap != null ) {
			tap.measure(block, ceiling);
		}
	}

	// the latencies of the enabled processors along the chain; none when the receiver is not open
	public int getLatency() {
		return this.along(TGAudioProcessor::getLatency);
	}

	// the tails of the enabled processors along the chain; none when the receiver is not open
	public int getTail() {
		return this.along(TGAudioProcessor::getTail);
	}

	private int along(ToIntFunction<TGAudioProcessor> frames) {
		synchronized (this.lock) {
			int sum = 0;
			if( this.isOpen() ){
				sum = TGAudioProcessor.along(sum, this.of(this.processor, frames));
				// by index: the audio thread reads it every block, without allocating an iterator
				for( int i = 0; i < this.outputs.size(); i ++ ){
					sum = TGAudioProcessor.along(sum, this.of(this.outputs.get(i), frames));
				}
			}
			return sum;
		}
	}

	private int of(TGAudioProcessor processor, ToIntFunction<TGAudioProcessor> frames) {
		return (this.isEnabled(processor) ? frames.applyAsInt(processor) : 0);
	}

	private boolean isOpen() {
		return (this.processor != null && this.processor.isOpen());
	}

	private void process(float[][] block, TGAudioProcessor processor, boolean measured) {
		if( this.isEnabled(processor) ) {
			processor.process(block);
			measure(this.taps.get(processor), block, measured, TGLevelTap.UNLIMITED);
		}
	}

	// the left and right gains of a Volume and Balance (0 to 127). Constant-power panning law: use sine/cosine so that
	// the total power (left² + right²) remains constant across all pan positions. At center (balance=63.5) both
	// channels get cos(π/4) ≈ 0.707, whereas a linear law would give 0.5 — a 3 dB centre-hole.
	public static float[] gains(int volume, int balance) {
		float[] gains = new float[TGLevelTap.SIDES];
		gains(volume, balance, gains);
		return gains;
	}

	// into the two gains, so that the audio thread allocates none
	private static void gains(int volume, int balance, float[] gains) {
		float pan = (balance / 127f) * ((float) Math.PI / 2f);
		float gain = volume / 127f;
		gains[0] = gain * (float) Math.cos(pan);
		gains[1] = gain * (float) Math.sin(pan);
	}

	public boolean isBusy() {
		synchronized (this.lock) {
			if( this.processor != null && this.processor.isBusy()) {
				return true;
			}
			for(TGAudioProcessor processor : this.outputs) {
				if( processor.isBusy() ) {
					return true;
				}
			}
			return false;
		}
	}

	public boolean isEnabled(TGAudioProcessor processor) {
		synchronized (this.lock) {
			TGProgramElement element = this.getElement(processor);

			return (element != null && element.isEnabled());
		}
	}
}
