#include "plugin.h"

#include "public.sdk/source/main/pluginfactory.h"
#include "pluginterfaces/vst/ivstnoteexpression.h"

#include <algorithm>
#include <cstring>

namespace Woodshed {

namespace {

// the voices: as many notes as ring together (six strings, let ring, chords in a row)
const int kVoices = 32;

// the note's id, its pitch when the host gives none
int32 noteId(int32 id, int16 pitch) {
	return (id >= 0 ? id : pitch);
}

}  // namespace

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

tresult PLUGIN_API GuitarPlugin::setupProcessing(ProcessSetup& setup) {
	tresult result = SingleComponentEffect::setupProcessing(setup);
	strings = std::make_unique<Strings>(kVoices, setup.sampleRate, pluckedString);
	voice.assign((size_t) setup.maxSamplesPerBlock, 0.0f);
	return result;
}

tresult PLUGIN_API GuitarPlugin::setActive(TBool state) {
	if (state && strings) {
		strings->silence();
	}
	return kResultOk;
}

tresult PLUGIN_API GuitarPlugin::canProcessSampleSize(int32 size) {
	return (size == kSample32 ? kResultTrue : kResultFalse);
}

tresult PLUGIN_API GuitarPlugin::setBusArrangements(SpeakerArrangement*, int32, SpeakerArrangement* outputs, int32 numOuts) {
	return (numOuts == 1 && outputs[0] == SpeakerArr::kStereo ? kResultTrue : kResultFalse);
}

// sample for sample: each event applies from its offset; the strings' sum on both sides
tresult PLUGIN_API GuitarPlugin::process(ProcessData& data) {
	if (data.numSamples <= 0 || data.numOutputs == 0 || !strings) {
		return kResultOk;
	}
	int32 from = 0;
	int32 count = (data.inputEvents ? data.inputEvents->getEventCount() : 0);
	for (int32 i = 0; i < count; i++) {
		Event event;
		if (data.inputEvents->getEvent(i, event) == kResultOk) {
			int32 to = std::max(from, std::min(data.numSamples, event.sampleOffset));
			sound(from, to);
			play(event);
			from = to;
		}
	}
	sound(from, data.numSamples);
	for (int32 side = 0; side < 2; side++) {
		std::memcpy(data.outputs[0].channelBuffers32[side], voice.data(), data.numSamples * sizeof(float));
	}
	return kResultOk;
}

void GuitarPlugin::sound(int32 from, int32 to) {
	strings->render(voice.data() + from, to - from);
}

// a note on plucks, a note off damps, a tuning (in its normalized form: 240 semitones across) retunes
void GuitarPlugin::play(const Event& event) {
	if (event.type == Event::kNoteOnEvent) {
		strings->noteOn(noteId(event.noteOn.noteId, event.noteOn.pitch), event.noteOn.pitch, event.noteOn.velocity);
	} else if (event.type == Event::kNoteOffEvent) {
		strings->noteOff(noteId(event.noteOff.noteId, event.noteOff.pitch));
	} else if (event.type == Event::kNoteExpressionValueEvent && event.noteExpressionValue.typeId == kTuningTypeID) {
		strings->tune(event.noteExpressionValue.noteId, 240.0 * (event.noteExpressionValue.value - 0.5));
	}
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
