package app.tuxguitar.midi.synth;

public interface TGAbstractProcessorFactory<T extends TGAudioProcessor> {

	String getType();

	T createProcessor();

	// whether an element of this type alone is a processor, so the type is offered to be chosen (the Channel Settings
	// window); false when the element must name more (a VST3 element names its plugin)
	default boolean isChosenByType() {
		return true;
	}
}
