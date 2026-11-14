#include "testplugin.h"

#include "pluginterfaces/base/ibstream.h"
#include "public.sdk/source/common/pluginview.h"
#include "public.sdk/source/main/pluginfactory.h"
#include "pluginterfaces/gui/iplugview.h"
#include "pluginterfaces/vst/ivstparameterchanges.h"

#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#ifdef _WIN32
#define NOMINMAX
#include <windows.h>
#endif

namespace TuxGuitar {

namespace {

#ifdef _WIN32
// a loop of the editor's own, as a file dialog runs one, or Windows while the user drags a window: it dispatches the
// thread's messages until its time is up. Or, given a window of another thread, a wait for that window to answer a
// message, as Windows waits on the window owning an editor (TuxGuitar's). It starts at the first timer tick, so once its
// host waits for messages.
DWORD modalMillis = 0;
HWND waitOn = nullptr;
bool inModal = false;

void loopFor(DWORD millis) {
	DWORD end = GetTickCount() + millis;
	MSG message;
	for (DWORD now = GetTickCount(); now < end; now = GetTickCount()) {
		if (!PeekMessageW(&message, nullptr, 0, 0, PM_REMOVE)) {
			MsgWaitForMultipleObjects(0, nullptr, FALSE, end - now, QS_ALLINPUT);
		} else if (message.message == WM_QUIT) {
			PostQuitMessage((int) message.wParam);
			break;
		} else {
			TranslateMessage(&message);
			DispatchMessageW(&message);
		}
	}
}

void CALLBACK runModal(HWND, UINT, UINT_PTR timer, DWORD) {
	KillTimer(nullptr, timer);
	inModal = true;
	if (waitOn) {
		SendMessageW(waitOn, WM_NULL, 0, 0);
	} else {
		loopFor(modalMillis);
	}
	inModal = false;
}

void startModal(int64 millis, int64 window) {
	modalMillis = (DWORD) millis;
	waitOn = (HWND) (intptr_t) window;
	SetTimer(nullptr, 0, 1, runModal);
}

// as the user clicks the close box of the window the editor is in
void closeWindow(void* window) { PostMessageW((HWND) window, WM_CLOSE, 0, 0); }
#else
const bool inModal = false;

void startModal(int64, int64) {}

void closeWindow(void*) {}
#endif

// an editor with nothing on it: enough for a host to open it in a window of its own; while it is in one, the plugin
// knows that window
class TestView : public CPluginView {
public:
	explicit TestView(void** window) : CPluginView(nullptr), window(window) {
		ViewRect size(0, 0, 200, 100);
		setRect(size);
	}

	tresult PLUGIN_API attached(void* parent, FIDString type) SMTG_OVERRIDE {
		*window = parent;
		return CPluginView::attached(parent, type);
	}

	tresult PLUGIN_API removed() SMTG_OVERRIDE {
		*window = nullptr;
		return CPluginView::removed();
	}

	tresult PLUGIN_API isPlatformTypeSupported(FIDString type) SMTG_OVERRIDE {
		bool supported = (std::strcmp(type, kPlatformTypeHWND) == 0 || std::strcmp(type, kPlatformTypeX11EmbedWindowID) == 0 ||
		                  std::strcmp(type, kPlatformTypeNSView) == 0);
		return (supported ? kResultTrue : kResultFalse);
	}

private:
	void** window;
};

SpeakerArrangement arrangement(int32 channels) {
	return (channels == 2 ? SpeakerArr::kStereo : SpeakerArr::kMono);
}

}  // namespace

TestPlugin::TestPlugin(PluginSpec spec) : spec(spec), preset(0), reportedLatency(0) {
	for (const ParamSpec& param : this->spec.params) {
		this->values.push_back(param.def);
	}
}

tresult PLUGIN_API TestPlugin::initialize(FUnknown* context) {
	tresult result = SingleComponentEffect::initialize(context);
	if (result != kResultOk) {
		return result;
	}
	if (spec.instrument) {
		addEventInput(u"Events", 16);
	} else {
		addAudioInput(u"Input", arrangement(spec.inputs));
	}
	addAudioOutput(u"Output", arrangement(spec.outputs));
	for (const ParamSpec& param : spec.params) {
		parameters.addParameter(new RangeParameter(param.title, param.id, nullptr, param.min, param.max, param.def, 0, ParameterInfo::kCanAutomate));
	}
	StringListParameter* presets = new StringListParameter(u"Preset", kPresetParam, nullptr, ParameterInfo::kCanAutomate | ParameterInfo::kIsList | ParameterInfo::kIsProgramChange);
	for (const PresetSpec& entry : spec.presets) {
		presets->appendString(entry.name);
	}
	parameters.addParameter(presets);
	reportedLatency = latency();
	return kResultOk;
}

tresult PLUGIN_API TestPlugin::setActive(TBool state) {
	if (state) {
		reset();
	}
	return kResultOk;
}

tresult PLUGIN_API TestPlugin::setupProcessing(ProcessSetup& setup) {
	tresult result = SingleComponentEffect::setupProcessing(setup);
	reset();
	return result;
}

tresult PLUGIN_API TestPlugin::canProcessSampleSize(int32 size) {
	return (size == kSample32 ? kResultTrue : kResultFalse);
}

tresult PLUGIN_API TestPlugin::setBusArrangements(SpeakerArrangement* inputs, int32 numIns, SpeakerArrangement* outputs, int32 numOuts) {
	bool same = (numOuts == 1 && outputs[0] == arrangement(spec.outputs));
	if (!spec.instrument) {
		same = same && (numIns == 1 && inputs[0] == arrangement(spec.inputs));
	}
	return (same ? kResultTrue : kResultFalse);
}

tresult PLUGIN_API TestPlugin::process(ProcessData& data) {
	gestureNow();
	applyChanges(data.inputParameterChanges);
	if (data.numSamples > 0 && data.numOutputs > 0) {
		float** in = (spec.instrument || data.numInputs == 0 ? nullptr : data.inputs[0].channelBuffers32);
		render(in, data.outputs[0].channelBuffers32, data.numSamples, data.inputEvents, data.processContext);
		data.outputs[0].silenceFlags = 0;
	}
	checkLatency();
	return kResultOk;
}

// a change of latency is told to the host, which asks for the new one
void TestPlugin::checkLatency() {
	if (latency() != reportedLatency && componentHandler) {
		reportedLatency = latency();
		componentHandler->restartComponent(kLatencyChanged);
	}
}

// the last value of each change in the block, from its start
void TestPlugin::applyChanges(IParameterChanges* changes) {
	int32 count = (changes ? changes->getParameterCount() : 0);
	for (int32 i = 0; i < count; i++) {
		IParamValueQueue* queue = changes->getParameterData(i);
		ParamValue value;
		int32 offset;
		if (queue && queue->getPoint(queue->getPointCount() - 1, offset, value) == kResultTrue) {
			setParamNormalized(queue->getParameterId(), value);
		}
	}
}

tresult PLUGIN_API TestPlugin::setParamNormalized(ParamID id, ParamValue value) {
	tresult result = SingleComponentEffect::setParamNormalized(id, value);
	Parameter* parameter = getParameterObject(id);
	if (id == kPresetParam && parameter) {
		applyPreset((int32) parameter->toPlain(value));
	} else if (parameter) {
		setValue(id, parameter->toPlain(value));
	}
	checkLatency();
	return result;
}

void TestPlugin::applyPreset(int32 index) {
	if (index < 0 || index >= (int32) spec.presets.size()) {
		return;
	}
	preset = index;
	const std::vector<double>& presetValues = spec.presets[index].values;
	for (size_t i = 0; i < spec.params.size() && i < presetValues.size(); i++) {
		ParamID id = spec.params[i].id;
		SingleComponentEffect::setParamNormalized(id, getParameterObject(id)->toNormalized(presetValues[i]));
		values[i] = presetValues[i];
	}
}

void TestPlugin::setValue(ParamID id, double plain) {
	int32 index = indexOf(id);
	if (index >= 0) {
		values[index] = plain;
	}
}

int32 TestPlugin::indexOf(ParamID id) const {
	for (size_t i = 0; i < spec.params.size(); i++) {
		if (spec.params[i].id == id) {
			return (int32) i;
		}
	}
	return -1;
}

double TestPlugin::value(ParamID id) const {
	int32 index = indexOf(id);
	return (index >= 0 ? values[index] : 0.0);
}

// the state: "TGTP", the preset, each parameter's plain value, then zeros up to 1 KB
tresult PLUGIN_API TestPlugin::getState(IBStream* state) {
	char bytes[kStateBytes] = {0};
	std::memcpy(bytes, "TGTP", 4);
	std::memcpy(bytes + 4, &preset, sizeof(int32));
	for (size_t i = 0; i < values.size(); i++) {
		std::memcpy(bytes + 8 + i * sizeof(double), &values[i], sizeof(double));
	}
	int32 written = 0;
	return state->write(bytes, kStateBytes, &written);
}

tresult PLUGIN_API TestPlugin::setState(IBStream* state) {
	char bytes[kStateBytes] = {0};
	int32 read = 0;
	if (state->read(bytes, kStateBytes, &read) != kResultOk || read != kStateBytes || std::memcmp(bytes, "TGTP", 4) != 0) {
		return kResultFalse;
	}
	std::memcpy(&preset, bytes + 4, sizeof(int32));
	for (size_t i = 0; i < values.size(); i++) {
		std::memcpy(&values[i], bytes + 8 + i * sizeof(double), sizeof(double));
		ParamID id = spec.params[i].id;
		SingleComponentEffect::setParamNormalized(id, getParameterObject(id)->toNormalized(values[i]));
	}
	Parameter* presets = getParameterObject(kPresetParam);
	SingleComponentEffect::setParamNormalized(kPresetParam, presets->toNormalized(preset));
	return kResultOk;
}

IPlugView* PLUGIN_API TestPlugin::createView(FIDString name) {
	if (spec.editor && name && std::strcmp(name, ViewType::kEditor) == 0) {
		return new TestView(&editorWindow);
	}
	return nullptr;
}

// the test messages play the plugin's own editor: "TG.gesture" moves a parameter through values in one gesture,
// "TG.gesture-later" does it at the plugin's next call from its host (a process, or a parameter read), as an editor
// does on its own window's thread between the host's calls; "TG.preset" chooses a preset; "TG.report" answers with what
// the plugin was told; "TG.crash" takes its process down, as a plugin that crashes does, saying so on its standard error;
// "TG.modal" makes the editor run a loop of its own for "millis" milliseconds once its host waits, or wait for the
// "window" of another thread to answer a message, and "TG.in-modal" answers 1 while it does (0 else); "TG.close-window"
// clicks the close box of the window the editor is in
tresult PLUGIN_API TestPlugin::notify(IMessage* message) {
	FIDString id = message->getMessageID();
	IAttributeList* attributes = message->getAttributes();
	if (std::strcmp(id, "TG.close-window") == 0) {
		closeWindow(editorWindow);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.modal") == 0) {
		int64 millis = 0;
		int64 window = 0;
		attributes->getInt("millis", millis);
		attributes->getInt("window", window);
		startModal(millis, window);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.in-modal") == 0) {
		char running = (inModal ? 1 : 0);
		return attributes->setBinary("report", &running, 1);
	}
	if (std::strcmp(id, "TG.gesture") == 0) {
		int64 param = 0;
		attributes->getInt("id", param);
		gesture((ParamID) param, doubles(attributes));
		return kResultOk;
	}
	if (std::strcmp(id, "TG.gesture-later") == 0) {
		int64 param = 0;
		attributes->getInt("id", param);
		laterParam = (ParamID) param;
		later = doubles(attributes);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.preset") == 0) {
		int64 index = 0;
		attributes->getInt("index", index);
		choosePreset((int32) index);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.crash") == 0) {
		std::fputs("Test plugin: crashing as asked\n", stderr);
		std::fflush(stderr);
		std::_Exit(70);
	}
	if (std::strcmp(id, "TG.report") == 0) {
		std::vector<char> bytes = report();
		return attributes->setBinary("report", bytes.data(), (uint32) bytes.size());
	}
	return SingleComponentEffect::notify(message);
}

tresult PLUGIN_API TestPlugin::queryInterface(const TUID _iid, void** obj) {
	DEF_INTERFACE(IConnectionPoint)
	return SingleComponentEffect::queryInterface(_iid, obj);
}

// the message's "values": plain values one after the other
std::vector<double> TestPlugin::doubles(IAttributeList* attributes) {
	const void* data = nullptr;
	uint32 size = 0;
	attributes->getBinary("values", data, size);
	std::vector<double> plain(size / sizeof(double));
	if (size > 0) {
		std::memcpy(plain.data(), data, plain.size() * sizeof(double));
	}
	return plain;
}

// the gesture of a "TG.gesture-later", once
void TestPlugin::gestureNow() {
	if (!later.empty()) {
		std::vector<double> values;
		values.swap(later);
		gesture(laterParam, values);
	}
}

ParamValue PLUGIN_API TestPlugin::getParamNormalized(ParamID id) {
	gestureNow();
	return SingleComponentEffect::getParamNormalized(id);
}

void TestPlugin::gesture(ParamID id, const std::vector<double>& plain) {
	beginEdit(id);
	for (double value : plain) {
		edit(id, value);
	}
	endEdit(id);
}

// as the editor's preset menu does it: the preset and every parameter it sets, in one gesture
void TestPlugin::choosePreset(int32 index) {
	beginEdit(kPresetParam);
	for (const ParamSpec& param : spec.params) {
		beginEdit(param.id);
	}
	edit(kPresetParam, index);
	for (const ParamSpec& param : spec.params) {
		performEdit(param.id, getParamNormalized(param.id));
	}
	for (const ParamSpec& param : spec.params) {
		endEdit(param.id);
	}
	endEdit(kPresetParam);
}

void TestPlugin::edit(ParamID id, double plain) {
	ParamValue normalized = getParameterObject(id)->toNormalized(plain);
	setParamNormalized(id, normalized);
	performEdit(id, normalized);
}

}  // namespace TuxGuitar
