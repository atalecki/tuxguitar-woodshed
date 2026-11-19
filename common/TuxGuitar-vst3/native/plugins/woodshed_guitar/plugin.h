// Woodshed Guitar: a VST3 instrument whose notes pluck strings, played through a NAM capture (its profile)
#pragma once

#include "public.sdk/source/vst/vstsinglecomponenteffect.h"
#include "pluginterfaces/vst/ivstevents.h"
#include "pluginterfaces/vst/ivstmessage.h"
#include "loader.h"
#include "stage.h"
#include "voice.h"
#include "wake.h"

#include <memory>
#include <string>
#include <vector>

namespace Woodshed {

class GuitarView;

using namespace Steinberg;
using namespace Steinberg::Vst;

// its parameters: the gains in dB; Quality 0 Full, 1 Light
enum ParamIds : ParamID { kInputGain = 1, kOutputLevel = 2, kQuality = 3 };

// what the plugin plays its voice through, as the user reads it: the profile's file name (empty for none), why the
// last file given to it was refused (empty when it was not), what it says of the profile's file (empty: nothing), and
// the file name of the capture being loaded (empty: none)
struct ProfileStatus {
	std::string name;
	std::string refusal;
	std::string notice;
	std::string loading;
};

class GuitarPlugin : public SingleComponentEffect {
public:
	GuitarPlugin();

	tresult PLUGIN_API initialize(FUnknown* context) SMTG_OVERRIDE;
	tresult PLUGIN_API terminate() SMTG_OVERRIDE;
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
	IPlugView* PLUGIN_API createView(FIDString name) SMTG_OVERRIDE;
	tresult PLUGIN_API notify(IMessage* message) SMTG_OVERRIDE;
	// its messages come through IConnectionPoint, which SingleComponentEffect hides from hosts
	tresult PLUGIN_API queryInterface(const TUID iid, void** obj) SMTG_OVERRIDE;

	// as its editor's Load profile... does: the capture at the path (UTF-8) is loaded meanwhile, the profile playing
	// on; then it becomes the profile and the host is told the plugin changed, or it is refused, the profile kept, and
	// the status says why
	void loadProfile(const std::string& path);
	// as its editor's Update from file does: the file at the profile's path loaded as it is now
	void updateFromFile();
	ProfileStatus status();
	// its editor's view while one is open (none: nullptr)
	void editorOpened(GuitarView* view);
	Steinberg::tresult languageMessage(Steinberg::FIDString id, Steinberg::Vst::IAttributeList* attributes);
	// a parameter's plain value
	double parameter(ParamID id);
	// as its editor changes a parameter: a gesture begun, going on, or ended, each telling the host the value
	enum Phase { kBegun, kGoingOn, kEnded };
	void edit(ParamID id, double plain, Phase phase);
	// the host sets a parameter: the editor follows
	tresult PLUGIN_API setParamNormalized(ParamID id, ParamValue value) SMTG_OVERRIDE;

private:
	void play(const Event& event);
	void playEvents(IEventList* events, int32 frames);
	void sound(int32 from, int32 to);
	void scale(float by, int32 frames);
	tresult profileMessage(FIDString id, IAttributeList* attributes);
	tresult editorMessage(FIDString id, IAttributeList* attributes);
	tresult windowMessage(FIDString id, IAttributeList* attributes);
	void applyChanges(IParameterChanges* changes);
	void post(Job job);
	void beginRestoring();
	void endRestoring();
	void finish();
	void apply(const Loaded& loaded);
	void applyLoad(const Loaded& loaded);
	void restoreProfile(const std::string& path, const std::string& contents);
	void checkFile();
	void checkLatency();
	void refreshEditor();

	std::unique_ptr<Strings> strings;
	// the strings' sum, a block long
	std::vector<float> voice;
	double hostRate = 0.0;
	int32 maxBlock = 0;
	// the peak of the last block (the audio thread's)
	float heard = 0.0f;
	// what the audio thread plays the voice through, and what loads it (stopped before the wake goes)
	Stage stage;
	std::unique_ptr<Wake> wake;
	std::unique_ptr<Loader> loader;
	// the profile, as the plugin's own thread holds it: the path it was loaded from and its contents
	std::string path;
	std::string contents;
	std::string refusal;
	std::string notice;
	// the capture being loaded, and the number of its job
	std::string loadingName;
	uint64_t loadingNumber = 0;
	// while a profile loads, the host is told a state restoration is under way (IProgress), by this id: the plugin does
	// not play as its state says until it has loaded, which a host may wait for (TuxGuitar's Play does)
	bool restoring = false;
	IProgress::ID restoration = 0;
	uint32 reportedLatency = 0;
	GuitarView* view = nullptr;
};

}  // namespace Woodshed
