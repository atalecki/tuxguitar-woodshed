// The test plugins' common part: a VST3 effect or instrument whose audio behaviour is fixed (see the README of the audio
// features), with plain-valued parameters, presets, a state of 1 KB, an editor of its own, and test messages through
// which a scenario plays the plugin's own editor (a gesture, a preset) or reads what the plugin was told.
#pragma once

#include "public.sdk/source/vst/vstsinglecomponenteffect.h"
#include "pluginterfaces/vst/ivstmessage.h"

#include <string>
#include <vector>

namespace TuxGuitar {

using namespace Steinberg;
using namespace Steinberg::Vst;

struct ParamSpec {
	ParamID id;
	const char16_t* title;
	double min;
	double max;
	double def;
};

struct PresetSpec {
	const char16_t* name;
	// the plain value of each parameter, in the order of the plugin's parameters
	std::vector<double> values;
};

struct PluginSpec {
	bool instrument;
	int32 inputs;   // input channels of an effect (1 or 2)
	int32 outputs;  // output channels (1 or 2)
	bool editor;
	std::vector<ParamSpec> params;
	std::vector<PresetSpec> presets;
};

// what a test plugin knows of its editor's view: the view and the window it is in, while it is in one, the content
// scale and the size its host last gave it, and whether the views it makes can be resized (by the user, down to
// kSmallestView)
struct ViewState {
	void* view = nullptr;
	void* window = nullptr;
	double scale = 0.0;
	int32 width = 0;
	int32 height = 0;
	bool resizable = false;
};

const int32 kSmallestViewWidth = 150;
const int32 kSmallestViewHeight = 80;

// the parameter that selects a preset
const ParamID kPresetParam = 1000;
// the size of every test plugin's state
const int32 kStateBytes = 1024;

class TestPlugin : public SingleComponentEffect {
public:
	explicit TestPlugin(PluginSpec spec);
	~TestPlugin() override;

	tresult PLUGIN_API initialize(FUnknown* context) SMTG_OVERRIDE;
	tresult PLUGIN_API setActive(TBool state) SMTG_OVERRIDE;
	tresult PLUGIN_API setProcessing(TBool) SMTG_OVERRIDE { return kResultOk; }
	tresult PLUGIN_API process(ProcessData& data) SMTG_OVERRIDE;
	tresult PLUGIN_API setupProcessing(ProcessSetup& setup) SMTG_OVERRIDE;
	tresult PLUGIN_API canProcessSampleSize(int32 size) SMTG_OVERRIDE;
	tresult PLUGIN_API setBusArrangements(SpeakerArrangement* inputs, int32 numIns, SpeakerArrangement* outputs, int32 numOuts) SMTG_OVERRIDE;
	uint32 PLUGIN_API getLatencySamples() SMTG_OVERRIDE { return latency(); }
	uint32 PLUGIN_API getTailSamples() SMTG_OVERRIDE { return tail(); }

	tresult PLUGIN_API setState(IBStream* state) SMTG_OVERRIDE;
	tresult PLUGIN_API getState(IBStream* state) SMTG_OVERRIDE;
	tresult PLUGIN_API setComponentState(IBStream* state) SMTG_OVERRIDE { return setState(state); }
	tresult PLUGIN_API setParamNormalized(ParamID id, ParamValue value) SMTG_OVERRIDE;
	ParamValue PLUGIN_API getParamNormalized(ParamID id) SMTG_OVERRIDE;
	IPlugView* PLUGIN_API createView(FIDString name) SMTG_OVERRIDE;
	tresult PLUGIN_API notify(IMessage* message) SMTG_OVERRIDE;
	// the test messages come through IConnectionPoint, which SingleComponentEffect hides from hosts
	tresult PLUGIN_API queryInterface(const TUID iid, void** obj) SMTG_OVERRIDE;

protected:
	// the plain value of a parameter
	double value(ParamID id) const;
	double sampleRate() const { return processSetup.sampleRate; }

	// what the plugin does to one block; out has the plugin's output channels, in its input channels (none for an
	// instrument); frames up to the maximum block size
	virtual void render(float** in, float** out, int32 frames, IEventList* events, ProcessContext* context) = 0;
	// forget what sounds (delay lines, voices): playback stopped or moved
	virtual void reset() {}
	virtual uint32 latency() { return 0; }
	virtual uint32 tail() { return 0; }
	// what the plugin was told, for the scenarios; nothing by default
	virtual std::vector<char> report() { return std::vector<char>(); }

private:
	void checkLatency();
	void setValue(ParamID id, double plain);
	int32 indexOf(ParamID id) const;
	void applyPreset(int32 preset);
	void applyChanges(IParameterChanges* changes);
	void gesture(ParamID id, const std::vector<double>& values);
	void gestureNow();
	static std::vector<double> doubles(IAttributeList* attributes);
	static std::u16string whatOf(IAttributeList* attributes);
	tresult viewMessage(FIDString id, IAttributeList* attributes);
	tresult sizeMessage(FIDString id, IAttributeList* attributes);
	tresult windowMessage(FIDString id, IAttributeList* attributes, int64 width, int64 height);
	tresult placeMessage(FIDString id, IAttributeList* attributes, int64 x, int64 y);
	tresult restoringMessage(FIDString id, IAttributeList* attributes);
	tresult tellProgress(IProgress* progress, bool restoring, bool underway, const char16_t* what);
	void choosePreset(int32 preset);
	void edit(ParamID id, double plain);
	void load(int64 capture, bool dirty);

	PluginSpec spec;
	std::vector<double> values;
	int32 preset;
	// the capture its editor loaded (no parameter: a file, as a NAM capture player loads one), 0 for none
	int32 capture = 0;
	uint32 reportedLatency;
	// the gesture of a "TG.gesture-later" not done yet: its parameter and values
	ParamID laterParam = 0;
	std::vector<double> later;
	// its editor's view, as the test messages read and play it
	ViewState view;
	// the state restoration and the editor's task it told its host of (IProgress) and has not finished, 0 for none
	IProgress::ID restoration = 0;
	IProgress::ID background = 0;
};

}  // namespace TuxGuitar

// the module's factory: one plugin class, an effect ("Fx") or an instrument
#define TG_TEST_PLUGIN_FACTORY(NAME, CLASS, INSTRUMENT, L1, L2, L3, L4)                                              \
	static const Steinberg::FUID CLASS##UID(L1, L2, L3, L4);                                                           \
	static Steinberg::FUnknown* create##CLASS(void*) {                                                                 \
		return static_cast<Steinberg::Vst::IAudioProcessor*>(new CLASS());                                            \
	}                                                                                                                  \
	BEGIN_FACTORY_DEF("TuxGuitar tests", "https://www.tuxguitar.app", "")                                              \
	DEF_CLASS2(INLINE_UID_FROM_FUID(CLASS##UID), PClassInfo::kManyInstances, kVstAudioEffectClass, NAME,               \
	           Vst::kDistributable, (INSTRUMENT ? Vst::PlugType::kInstrumentSynth : Vst::PlugType::kFx), "1.0.0",      \
	           kVstVersionString, create##CLASS)                                                                       \
	END_FACTORY
