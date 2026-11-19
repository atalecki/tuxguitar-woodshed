package app.tuxguitar.midi.synth;

public class TGAudioBufferProcessor {

	private TGSynthModel synthesizer;
	private TGAudioBuffer buffer;
	// the mix as floats, for the master's tap
	private final float[][] mix = new float[TGAudioBuffer.CHANNELS][TGAudioBuffer.BUFFER_SIZE / 2];

	public TGAudioBufferProcessor(TGSynthModel synthesizer) {
		this.synthesizer = synthesizer;
		this.buffer = new TGAudioBuffer();
	}

	// the next block: the synthesizer's listener sends its events first; each channel is delayed to the slowest chain.
	// While the synthesizer's levels are on, the mix (as it is heard, cut at full scale) is measured at the master
	public void process() {
		this.synthesizer.beforeBlock(TGAudioBuffer.BUFFER_SIZE / 2);
		this.buffer.clear();
		int latency = this.synthesizer.getLatency();
		for( int i = 0; i < this.synthesizer.countChannels(); i ++ ){
			TGSynthChannel channel = this.synthesizer.getChannel( i );
			if( channel != null ){
				channel.fillBuffer(this.buffer, latency - channel.getLatency());
			}
		}
		this.buffer.clip();
		TGLevels levels = this.synthesizer.getLevels();
		if( levels.isOn() ) {
			this.buffer.read(this.mix);
			levels.master().measure(this.mix);
		}
	}

	public TGAudioBuffer getBuffer() {
		return this.buffer;
	}
}
