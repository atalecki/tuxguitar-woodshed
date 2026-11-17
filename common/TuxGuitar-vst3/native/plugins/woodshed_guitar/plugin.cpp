#include "plugin.h"

#include "public.sdk/source/main/pluginfactory.h"

#include <cstring>

namespace Woodshed {

GuitarPlugin::GuitarPlugin() {}

tresult PLUGIN_API GuitarPlugin::initialize(FUnknown* context) {
	tresult result = SingleComponentEffect::initialize(context);
	if (result != kResultOk) {
		return result;
	}
	addEventInput(u"Notes", 16);
	addAudioOutput(u"Output", SpeakerArr::kStereo);
	return kResultOk;
}

tresult PLUGIN_API GuitarPlugin::canProcessSampleSize(int32 size) {
	return (size == kSample32 ? kResultTrue : kResultFalse);
}

tresult PLUGIN_API GuitarPlugin::setBusArrangements(SpeakerArrangement*, int32, SpeakerArrangement* outputs, int32 numOuts) {
	return (numOuts == 1 && outputs[0] == SpeakerArr::kStereo ? kResultTrue : kResultFalse);
}

tresult PLUGIN_API GuitarPlugin::process(ProcessData& data) {
	if (data.numSamples > 0 && data.numOutputs > 0) {
		for (int32 side = 0; side < 2; side++) {
			std::memset(data.outputs[0].channelBuffers32[side], 0, data.numSamples * sizeof(float));
		}
	}
	return kResultOk;
}

}  // namespace Woodshed

static const Steinberg::FUID GuitarPluginUID(0x57534847, 0x55495441, 0x52000000, 0x00000001);

static Steinberg::FUnknown* createGuitarPlugin(void*) {
	return static_cast<Steinberg::Vst::IAudioProcessor*>(new Woodshed::GuitarPlugin());
}

BEGIN_FACTORY_DEF("TuxGuitar Woodshed", "https://github.com/atalecki/tuxguitar-woodshed", "")
DEF_CLASS2(INLINE_UID_FROM_FUID(GuitarPluginUID), PClassInfo::kManyInstances, kVstAudioEffectClass, "Woodshed Guitar",
           Vst::kDistributable, Vst::PlugType::kInstrumentSynth, "1.0.0", kVstVersionString, createGuitarPlugin)
END_FACTORY
