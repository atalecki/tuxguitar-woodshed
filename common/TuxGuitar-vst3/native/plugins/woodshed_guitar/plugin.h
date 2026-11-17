// Woodshed Guitar: a VST3 instrument whose notes pluck strings, played through a NAM capture (its profile)
#pragma once

#include "public.sdk/source/vst/vstsinglecomponenteffect.h"
#include "pluginterfaces/vst/ivstevents.h"
#include "pluginterfaces/vst/ivstmessage.h"
#include "capture.h"
#include "voice.h"

#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace Woodshed {

using namespace Steinberg;
using namespace Steinberg::Vst;

// its parameters, plain values in dB
enum ParamIds : ParamID { kInputGain = 1, kOutputLevel = 2 };

// what the plugin plays its voice through, as the user reads it: the profile's file name (empty for none) and why the
// last file given to it was refused (empty when it was not)
struct ProfileStatus {
	std::string name;
	std::string refusal;
};

class GuitarPlugin : public SingleComponentEffect {
public:
	GuitarPlugin();

	tresult PLUGIN_API initialize(FUnknown* context) SMTG_OVERRIDE;
	tresult PLUGIN_API setActive(TBool state) SMTG_OVERRIDE;
	tresult PLUGIN_API setupProcessing(ProcessSetup& setup) SMTG_OVERRIDE;
	tresult PLUGIN_API setProcessing(TBool) SMTG_OVERRIDE { return kResultOk; }
	tresult PLUGIN_API process(ProcessData& data) SMTG_OVERRIDE;
	tresult PLUGIN_API canProcessSampleSize(int32 size) SMTG_OVERRIDE;
	tresult PLUGIN_API setBusArrangements(SpeakerArrangement* inputs, int32 numIns, SpeakerArrangement* outputs, int32 numOuts) SMTG_OVERRIDE;
	uint32 PLUGIN_API getLatencySamples() SMTG_OVERRIDE;

	tresult PLUGIN_API setState(IBStream* state) SMTG_OVERRIDE;
	tresult PLUGIN_API getState(IBStream* state) SMTG_OVERRIDE;
	tresult PLUGIN_API setComponentState(IBStream* state) SMTG_OVERRIDE { return setState(state); }
	tresult PLUGIN_API notify(IMessage* message) SMTG_OVERRIDE;
	// its messages come through IConnectionPoint, which SingleComponentEffect hides from hosts
	tresult PLUGIN_API queryInterface(const TUID iid, void** obj) SMTG_OVERRIDE;

	// as its editor's Load profile... does: the capture at the path (UTF-8) becomes the profile and the host is told
	// the plugin changed; or it is refused, the profile kept, and the status says why
	bool loadProfile(const std::string& path);
	ProfileStatus status();

private:
	void play(const Event& event);
	void sound(int32 from, int32 to);
	void applyChanges(IParameterChanges* changes);
	double plain(ParamID id);
	void install(const std::string& path, const std::string& contents, const Capture& capture);
	void restoreProfile(const std::string& path, const std::string& contents);
	void checkLatency();

	std::unique_ptr<Strings> strings;
	// the strings' sum, a block long
	std::vector<float> voice;
	double hostRate = 0.0;
	int32 maxBlock = 0;
	// the profile: its amp (swapped under the lock; the audio thread skips it while the lock is taken), the path it
	// was loaded from and its contents
	std::mutex ampLock;
	std::shared_ptr<Amp> amp;
	std::string path;
	std::string contents;
	std::string refusal;
	uint32 reportedLatency = 0;
};

}  // namespace Woodshed
