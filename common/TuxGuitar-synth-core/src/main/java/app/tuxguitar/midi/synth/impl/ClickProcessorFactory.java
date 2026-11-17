package app.tuxguitar.midi.synth.impl;

import app.tuxguitar.midi.synth.TGMidiProcessor;
import app.tuxguitar.midi.synth.TGMidiProcessorFactory;

/**
 * The built-in click's source (see ClickProcessor): built into the synthesizer (TGSynthManager finds it with no plugin
 * installed), never offered as a source a track can choose.
 */
public class ClickProcessorFactory implements TGMidiProcessorFactory {

	public static final String TYPE = "click";

	public String getType() {
		return TYPE;
	}

	public TGMidiProcessor createProcessor() {
		return new ClickProcessor();
	}
}
