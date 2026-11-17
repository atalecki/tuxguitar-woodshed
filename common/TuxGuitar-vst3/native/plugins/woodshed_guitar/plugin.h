// Woodshed Guitar: a VST3 instrument whose notes pluck strings, played through a NAM capture (its profile)
#pragma once

#include "public.sdk/source/vst/vstsinglecomponenteffect.h"

namespace Woodshed {

using namespace Steinberg;
using namespace Steinberg::Vst;

class GuitarPlugin : public SingleComponentEffect {
public:
	GuitarPlugin();

	tresult PLUGIN_API initialize(FUnknown* context) SMTG_OVERRIDE;
	tresult PLUGIN_API setProcessing(TBool) SMTG_OVERRIDE { return kResultOk; }
	tresult PLUGIN_API process(ProcessData& data) SMTG_OVERRIDE;
	tresult PLUGIN_API canProcessSampleSize(int32 size) SMTG_OVERRIDE;
	tresult PLUGIN_API setBusArrangements(SpeakerArrangement* inputs, int32 numIns, SpeakerArrangement* outputs, int32 numOuts) SMTG_OVERRIDE;
};

}  // namespace Woodshed
