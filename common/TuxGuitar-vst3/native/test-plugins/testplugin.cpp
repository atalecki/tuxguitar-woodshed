#include "testplugin.h"

#include "../plugins/common/window_test_hooks.h"

#include "pluginterfaces/base/ibstream.h"
#include "public.sdk/source/common/pluginview.h"
#include "public.sdk/source/main/pluginfactory.h"
#include "public.sdk/source/vst/utility/stringconvert.h"
#include "pluginterfaces/gui/iplugview.h"
#include "pluginterfaces/gui/iplugviewcontentscalesupport.h"
#include "pluginterfaces/vst/ivstparameterchanges.h"

#include <algorithm>
#include <cctype>
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

// a view whose code hangs (a deadlock in its UI): once its host waits for messages, the thread sleeps that long in a
// window procedure, answering nothing
DWORD hangMillis = 0;

void CALLBACK hangView(HWND, UINT, UINT_PTR timer, DWORD) {
	KillTimer(nullptr, timer);
	Sleep(hangMillis);
}

void startHang(int64 millis) {
	hangMillis = (DWORD) millis;
	SetTimer(nullptr, 0, 1, hangView);
}

// as the user clicks the close box of the window the editor is in
void closeWindow(void* window) { PostMessageW((HWND) window, WM_CLOSE, 0, 0); }

// a window standing in for TuxGuitar's main window in the tests ("TG.window"), on its host's thread: plain, made with
// a DPI awareness (0 unaware, 1 system aware, 3 per monitor version 2), its inside of a size, shown without being
// activated
DPI_AWARENESS_CONTEXT awarenessContext(int64 awareness) {
	static const DPI_AWARENESS_CONTEXT contexts[] = {DPI_AWARENESS_CONTEXT_UNAWARE, DPI_AWARENESS_CONTEXT_SYSTEM_AWARE,
	                                                 DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE, DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2};
	return contexts[std::max<int64>(0, std::min<int64>(3, awareness))];
}

// hears the keys that come to it (see heardKeys)
LRESULT CALLBACK standInProcedure(HWND window, UINT message, WPARAM wParam, LPARAM lParam);

const wchar_t* standInClass() {
	static const wchar_t* name = [] {
		WNDCLASSW type = {};
		type.lpfnWndProc = standInProcedure;
		type.hInstance = GetModuleHandleW(nullptr);
		type.hbrBackground = (HBRUSH) (COLOR_WINDOW + 1);
		type.lpszClassName = L"TuxGuitarTestStandIn";
		RegisterClassW(&type);
		return type.lpszClassName;
	}();
	return name;
}

// its frame resized so that its inside is that size
void sizeInside(HWND window, int64 width, int64 height) {
	RECT frame = {0, 0, (LONG) width, (LONG) height};
	AdjustWindowRectEx(&frame, (DWORD) GetWindowLongPtrW(window, GWL_STYLE), FALSE, 0);
	SetWindowPos(window, nullptr, 0, 0, frame.right - frame.left, frame.bottom - frame.top, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE);
}

int64 standIn(int64 awareness, int64 width, int64 height) {
	DPI_AWARENESS_CONTEXT before = SetThreadDpiAwarenessContext(awarenessContext(awareness));
	HWND window = CreateWindowExW(0, standInClass(), L"TuxGuitar stand-in", WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN, 40, 40, 100, 100, nullptr, nullptr,
	                              GetModuleHandleW(nullptr), nullptr);
	sizeInside(window, width, height);
	SetThreadDpiAwarenessContext(before);
	ShowWindow(window, SW_SHOWNOACTIVATE);
	return (int64) (intptr_t) window;
}

void resizeStandIn(int64 window, int64 width, int64 height) { sizeInside((HWND) (intptr_t) window, width, height); }

char isWindow(int64 window) { return (IsWindow((HWND) (intptr_t) window) ? 1 : 0); }

// the window the editor is in, on a screen (1) or off every screen (0)
char onScreen(void* window) { return (MonitorFromWindow((HWND) window, MONITOR_DEFAULTTONULL) ? 1 : 0); }

// as the user drags the window the editor is in to that place on the screen
void moveTo(void* window, int64 x, int64 y) { SetWindowPos((HWND) window, nullptr, (int) x, (int) y, 0, 0, SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE); }

// the keys the windows of the tests heard (WM_KEYDOWN, WM_SYSKEYDOWN), as text, the stand-in's (kStandInHeard) and
// the views' (kViewHeard): each key ("Z", "F8", "Space", else its virtual-key number) with the modifiers its window's
// thread saw held then ("+ctrl", "+alt", "+shift"), as TuxGuitar's toolkit reads them
const int kStandInHeard = 0;
const int kViewHeard = 1;
std::string heardKeys[2];

bool isFunctionKey(WPARAM key) { return (key >= VK_F1 && key <= VK_F24); }

std::string keyName(WPARAM key) {
	if (isFunctionKey(key)) {
		return "F" + std::to_string(key - VK_F1 + 1);
	}
	if (key == VK_SPACE) {
		return "Space";
	}
	return (std::isalnum((int) key) ? std::string(1, (char) key) : "#" + std::to_string(key));
}

std::string held() {
	std::string text;
	text += (GetKeyState(VK_CONTROL) < 0 ? "+ctrl" : "");
	text += (GetKeyState(VK_MENU) < 0 ? "+alt" : "");
	text += (GetKeyState(VK_SHIFT) < 0 ? "+shift" : "");
	return text;
}

void heard(int window, UINT message, WPARAM key) {
	if (message == WM_KEYDOWN || message == WM_SYSKEYDOWN) {
		heardKeys[window] += (heardKeys[window].empty() ? "" : " ") + keyName(key) + held();
	}
}

LRESULT CALLBACK standInProcedure(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	heard(kStandInHeard, message, wParam);
	return DefWindowProcW(window, message, wParam, lParam);
}

LRESULT CALLBACK viewProcedure(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	heard(kViewHeard, message, wParam);
	return DefWindowProcW(window, message, wParam, lParam);
}

// what both heard, the stand-in's then the views' ("<stand-in> | <views>"), forgotten once told
std::string takeHeardKeys() {
	std::string told = heardKeys[kStandInHeard] + " | " + heardKeys[kViewHeard];
	heardKeys[kStandInHeard].clear();
	heardKeys[kViewHeard].clear();
	return told;
}

const wchar_t* viewClass() {
	static const wchar_t* name = [] {
		WNDCLASSW type = {};
		type.lpfnWndProc = viewProcedure;
		type.hInstance = GetModuleHandleW(nullptr);
		type.lpszClassName = L"TuxGuitarTestView";
		RegisterClassW(&type);
		return type.lpszClassName;
	}();
	return name;
}

// the view's own window inside the window it is in, as a plugin's editor has, that size
void* viewWindow(void* parent, int32 width, int32 height) {
	return CreateWindowExW(0, viewClass(), L"", WS_CHILD | WS_VISIBLE, 0, 0, width, height, (HWND) parent, nullptr, GetModuleHandleW(nullptr), nullptr);
}

void sizeViewWindow(void* own, int32 width, int32 height) { SetWindowPos((HWND) own, nullptr, 0, 0, width, height, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE); }

// gone with the view (it may have gone with the window it was in already)
void endViewWindow(void* own) {
	if (IsWindow((HWND) own)) {
		DestroyWindow((HWND) own);
	}
}

BYTE down(int64 held) { return (BYTE) (held ? 0x80 : 0); }

// as the user types a key in the view, the modifiers asked held ("ctrl", "alt", "shift" 1): its thread's keyboard state
// says they are held, as it does while the user holds them; then the key comes to the view's window, a system key with
// Alt, as Windows sends it
void typeKey(void* own, int64 key, int64 ctrl, int64 alt, int64 shift) {
	BYTE state[256] = {};
	GetKeyboardState(state);
	state[VK_CONTROL] = down(ctrl);
	state[VK_MENU] = down(alt);
	state[VK_SHIFT] = down(shift);
	SetKeyboardState(state);
	LPARAM details = 1 | ((LPARAM) MapVirtualKeyW((UINT) key, MAPVK_VK_TO_VSC) << 16) | (alt ? (LPARAM) 1 << 29 : 0);
	PostMessageW((HWND) own, (alt ? WM_SYSKEYDOWN : WM_KEYDOWN), (WPARAM) key, details);
}

// as a plugin's code looks at its thread's next message without taking it (PeekMessage, PM_NOREMOVE), when asked
void peekQueue(int64 peek) {
	MSG next;
	if (peek) {
		PeekMessageW(&next, nullptr, 0, 0, PM_NOREMOVE);
	}
}

#else
const bool inModal = false;

void startModal(int64, int64) {}

void startHang(int64) {}

void closeWindow(void*) {}

int64 standIn(int64, int64, int64) { return 0; }

void resizeStandIn(int64, int64, int64) {}

char isWindow(int64) { return 0; }

char onScreen(void*) { return 0; }

void moveTo(void*, int64, int64) {}

std::string takeHeardKeys() { return " | "; }

void* viewWindow(void*, int32, int32) { return nullptr; }

void sizeViewWindow(void*, int32, int32) {}

void endViewWindow(void*) {}

void typeKey(void*, int64, int64, int64, int64) {}

void peekQueue(int64) {}

#endif

tresult reportDoubles(IAttributeList* attributes, const std::vector<double>& values) {
	return attributes->setBinary("report", values.data(), (uint32) (values.size() * sizeof(double)));
}

// the test plugins of this module alive, and whether one, or its editor's view, went while the editor's loop ran (its
// code still running: a host must not take a plugin away under it)
int32 livePlugins = 0;
bool goneInLoop = false;

void going() {
	goneInLoop = (goneInLoop || inModal);
}

// an editor with nothing on it: enough for a host to open it in a window of its own. The plugin knows the window it is
// in and the content scale and size its host gave it (ViewState); it can ask its host for another size, and, made
// resizable, the user can resize it down to kSmallestView
class TestView : public CPluginView, public IPlugViewContentScaleSupport {
public:
	explicit TestView(ViewState* state) : CPluginView(nullptr), state(state), resizable(state->resizable) {
		ViewRect size(0, 0, 200, 100);
		setRect(size);
	}

	tresult PLUGIN_API attached(void* parent, FIDString type) SMTG_OVERRIDE {
		state->view = this;
		state->window = parent;
		state->own = viewWindow(parent, getRect().getWidth(), getRect().getHeight());
		sized(getRect());
		return CPluginView::attached(parent, type);
	}

	// as an editor that asks its host for another size (its own layout changed, or the user chose a larger one)
	void askSize(int64 width, int64 height) {
		ViewRect size(0, 0, (int32) width, (int32) height);
		if (plugFrame) {
			plugFrame->resizeView(this, &size);
		}
	}

	tresult PLUGIN_API canResize() SMTG_OVERRIDE { return (resizable ? kResultTrue : kResultFalse); }

	tresult PLUGIN_API checkSizeConstraint(ViewRect* rect) SMTG_OVERRIDE {
		rect->right = rect->left + std::max(rect->getWidth(), kSmallestViewWidth);
		rect->bottom = rect->top + std::max(rect->getHeight(), kSmallestViewHeight);
		return kResultTrue;
	}

	tresult PLUGIN_API onSize(ViewRect* newSize) SMTG_OVERRIDE {
		sized(*newSize);
		sizeViewWindow(state->own, newSize->getWidth(), newSize->getHeight());
		return CPluginView::onSize(newSize);
	}

	tresult PLUGIN_API removed() SMTG_OVERRIDE {
		endViewWindow(state->own);
		state->own = nullptr;
		state->view = nullptr;
		state->window = nullptr;
		going();
		return CPluginView::removed();
	}

	tresult PLUGIN_API isPlatformTypeSupported(FIDString type) SMTG_OVERRIDE {
		bool supported = (std::strcmp(type, kPlatformTypeHWND) == 0 || std::strcmp(type, kPlatformTypeX11EmbedWindowID) == 0 ||
		                  std::strcmp(type, kPlatformTypeNSView) == 0);
		return (supported ? kResultTrue : kResultFalse);
	}

	tresult PLUGIN_API setContentScaleFactor(ScaleFactor factor) SMTG_OVERRIDE {
		state->scale = factor;
		return kResultOk;
	}

	OBJ_METHODS(TestView, CPluginView)
	DEFINE_INTERFACES
		DEF_INTERFACE(IPlugViewContentScaleSupport)
	END_DEFINE_INTERFACES(CPluginView)
	REFCOUNT_METHODS(CPluginView)

private:
	void sized(const ViewRect& size) {
		state->width = size.getWidth();
		state->height = size.getHeight();
	}

	ViewState* state;
	bool resizable;
};

SpeakerArrangement arrangement(int32 channels) {
	return (channels == 2 ? SpeakerArr::kStereo : SpeakerArr::kMono);
}

}  // namespace

TestPlugin::TestPlugin(PluginSpec spec) : spec(spec), preset(0), reportedLatency(0) {
	for (const ParamSpec& param : this->spec.params) {
		this->values.push_back(param.def);
	}
	livePlugins++;
}

TestPlugin::~TestPlugin() {
	livePlugins--;
	going();
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

// the state: "TGTP", the preset, each parameter's plain value, then zeros up to 1 KB but for the capture, in the last 4
// bytes
tresult PLUGIN_API TestPlugin::getState(IBStream* state) {
	char bytes[kStateBytes] = {0};
	std::memcpy(bytes, "TGTP", 4);
	std::memcpy(bytes + 4, &preset, sizeof(int32));
	for (size_t i = 0; i < values.size(); i++) {
		std::memcpy(bytes + 8 + i * sizeof(double), &values[i], sizeof(double));
	}
	std::memcpy(bytes + kStateBytes - sizeof(int32), &capture, sizeof(int32));
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
	std::memcpy(&capture, bytes + kStateBytes - sizeof(int32), sizeof(int32));
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
		return new TestView(&view);
	}
	return nullptr;
}

// the test messages on the editor's view and window (kNotImplemented for others): "TG.close-window" clicks the close
// box of the window the editor is in; "TG.view" answers the view's content scale, width and height, and 1 while it is
// attached to a window (0 once removed) (doubles);
// "TG.dpi-changed" tells the window it went to a screen of that "dpi", as Windows does, and answers the frame suggested;
// "TG.resizable" makes the views made from now on resizable; and those on its size (sizeMessage)
tresult TestPlugin::viewMessage(FIDString id, IAttributeList* attributes) {
	if (std::strcmp(id, "TG.close-window") == 0) {
		closeWindow(view.window);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.view") == 0) {
		return reportDoubles(attributes, {view.scale, (double) view.width, (double) view.height, (view.window ? 1.0 : 0.0)});
	}
	if (std::strcmp(id, "TG.dpi-changed") == 0) {
		int64 dpi = 96;
		attributes->getInt("dpi", dpi);
		return reportDoubles(attributes, TuxGuitarTest::changeDpi(view.window, dpi));
	}
	if (std::strcmp(id, "TG.resizable") == 0) {
		view.resizable = true;
		return kResultOk;
	}
	return sizeMessage(id, attributes);
}

// "TG.resize": the view asks its host for that "width" and "height"; "TG.drag-to": the user drags the window's border
// until its inside is that "width" and "height"; else restoringMessage
tresult TestPlugin::sizeMessage(FIDString id, IAttributeList* attributes) {
	int64 width = 0;
	int64 height = 0;
	attributes->getInt("width", width);
	attributes->getInt("height", height);
	if (std::strcmp(id, "TG.resize") == 0 && view.view) {
		static_cast<TestView*>(view.view)->askSize(width, height);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.drag-to") == 0) {
		TuxGuitarTest::dragTo(view.window, width, height);
		return kResultOk;
	}
	return windowMessage(id, attributes, width, height);
}

// "TG.window": a window standing in for TuxGuitar's (standIn), its inside of that "width" and "height", of the DPI
// "awareness" asked, answering its handle (8 bytes); "TG.window-size": the "window" given gets an inside of that
// "width" and "height"; "TG.is-window": 1 byte, 1 while the "window" given is a window; else restoringMessage
tresult TestPlugin::windowMessage(FIDString id, IAttributeList* attributes, int64 width, int64 height) {
	int64 value = 0;
	if (std::strcmp(id, "TG.window") == 0) {
		attributes->getInt("awareness", value);
		int64 window = standIn(value, width, height);
		return attributes->setBinary("report", &window, sizeof(int64));
	}
	if (std::strcmp(id, "TG.window-size") == 0) {
		attributes->getInt("window", value);
		resizeStandIn(value, width, height);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.is-window") == 0) {
		attributes->getInt("window", value);
		char answer = isWindow(value);
		return attributes->setBinary("report", &answer, 1);
	}
	return placeMessage(id, attributes, width, height);
}

// "TG.on-screen": 1 byte, 1 while the window the editor is in is on a screen; "TG.move-to": that window goes to that
// "width" (x) and "height" (y) on the screen; "TG.request-editor": the plugin asks its host to show its editor; else
// keyMessage
tresult TestPlugin::placeMessage(FIDString id, IAttributeList* attributes, int64 x, int64 y) {
	if (std::strcmp(id, "TG.on-screen") == 0) {
		char answer = onScreen(view.window);
		return attributes->setBinary("report", &answer, 1);
	}
	if (std::strcmp(id, "TG.move-to") == 0) {
		moveTo(view.window, x, y);
		return kResultOk;
	}
	FUnknownPtr<IComponentHandler2> handler(componentHandler);
	if (std::strcmp(id, "TG.request-editor") == 0 && handler) {
		return handler->requestOpenEditor(ViewType::kEditor);
	}
	return keyMessage(id, attributes);
}

// "TG.type": as the user types the "key" (a virtual-key number) in the editor's view, with "ctrl", "alt" and "shift"
// held when 1, the plugin then looking at the key without taking it when "peek" is 1; "TG.keys": what the windows of this process heard since last asked, the stand-in's then the views'
// ("<stand-in> | <views>", UTF-8); else restoringMessage
tresult TestPlugin::keyMessage(FIDString id, IAttributeList* attributes) {
	if (std::strcmp(id, "TG.type") == 0) {
		int64 typed[5] = {0, 0, 0, 0, 0};
		const char* names[5] = {"key", "ctrl", "alt", "shift", "peek"};
		for (int i = 0; i < 5; i++) {
			attributes->getInt(names[i], typed[i]);
		}
		typeKey(view.own, typed[0], typed[1], typed[2], typed[3]);
		peekQueue(typed[4]);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.keys") == 0) {
		std::string told = takeHeardKeys();
		return attributes->setBinary("report", told.data(), (uint32) told.size());
	}
	return restoringMessage(id, attributes);
}

// "TG.restoring": with "underway" 1 the plugin tells its host it restores its state in the background (IProgress), as a
// plugin loading a file on a thread of its own does, saying it does its binary "what" (UTF-8) when there is one (nothing said without), and
// with 0 that it has finished; "TG.background" the same of a task its editor started that restores nothing; refused by
// a host that hears no progress (kNotImplemented for other messages)
tresult TestPlugin::restoringMessage(FIDString id, IAttributeList* attributes) {
	bool restoring = (std::strcmp(id, "TG.restoring") == 0);
	if (!restoring && std::strcmp(id, "TG.background") != 0) {
		return kNotImplemented;
	}
	int64 underway = 0;
	attributes->getInt("underway", underway);
	std::u16string what = whatOf(attributes);
	FUnknownPtr<IProgress> progress(componentHandler);
	return (progress ? tellProgress(progress, restoring, underway == 1, what.empty() ? nullptr : what.c_str()) : kResultFalse);
}

// the message's binary "what" (UTF-8), empty when there is none
std::u16string TestPlugin::whatOf(IAttributeList* attributes) {
	const void* data = nullptr;
	uint32 size = 0;
	if (attributes->getBinary("what", data, size) != kResultTrue) {
		return std::u16string();
	}
	return StringConvert::convert(std::string(static_cast<const char*>(data), size));
}

// the restoration or the editor's task begun, saying what it does (nullptr for nothing), or finished, once
tresult TestPlugin::tellProgress(IProgress* progress, bool restoring, bool underway, const char16_t* what) {
	IProgress::ID& task = (restoring ? restoration : background);
	if (underway && task == 0) {
		progress->start(restoring ? IProgress::AsyncStateRestoration : IProgress::UIBackgroundTask, reinterpret_cast<const tchar*>(what), task);
	} else if (!underway && task != 0) {
		progress->finish(task);
		task = 0;
	}
	return kResultOk;
}

// the test messages play the plugin's own editor: "TG.gesture" moves a parameter through values in one gesture,
// "TG.gesture-later" does it at the plugin's next call from its host (a process, or a parameter read), as an editor
// does on its own window's thread between the host's calls; "TG.preset" chooses a preset; "TG.load" loads the
// "capture" (an integer kept in the state, no parameter, no edit begun or ended), saying the plugin is dirty when "dirty"
// is 1, as a capture player loading a file may or may not; "TG.capture" answers the capture (4 bytes); "TG.report"
// answers with what the plugin was told; "TG.crash" takes its process down, as a plugin that crashes does, saying so on its standard error;
// "TG.hang-view" makes the editor's code hang for "millis" milliseconds once its host waits (no answer meanwhile);
// "TG.modal" makes the editor run a loop of its own for "millis" milliseconds once its host waits, or wait for the
// "window" of another thread to answer a message, and "TG.in-modal" answers three bytes: 1 while it does (0 else), 1 once
// a plugin of the module or an editor's view went while it did, and how many plugins of the module are alive; and
// those on the editor's view (viewMessage)
tresult PLUGIN_API TestPlugin::notify(IMessage* message) {
	FIDString id = message->getMessageID();
	IAttributeList* attributes = message->getAttributes();
	tresult viewed = viewMessage(id, attributes);
	if (viewed != kNotImplemented) {
		return viewed;
	}
	if (std::strcmp(id, "TG.modal") == 0) {
		int64 millis = 0;
		int64 window = 0;
		attributes->getInt("millis", millis);
		attributes->getInt("window", window);
		startModal(millis, window);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.hang-view") == 0) {
		int64 millis = 0;
		attributes->getInt("millis", millis);
		startHang(millis);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.in-modal") == 0) {
		char state[3] = {(char) (inModal ? 1 : 0), (char) (goneInLoop ? 1 : 0), (char) livePlugins};
		return attributes->setBinary("report", state, 3);
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
	if (std::strcmp(id, "TG.load") == 0) {
		int64 loaded = 0;
		int64 dirty = 0;
		attributes->getInt("capture", loaded);
		attributes->getInt("dirty", dirty);
		load(loaded, dirty == 1);
		return kResultOk;
	}
	if (std::strcmp(id, "TG.capture") == 0) {
		return attributes->setBinary("report", &capture, sizeof(int32));
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

// as a capture player's editor loads a file: the state changes, no parameter does; dirty, the host is told so
void TestPlugin::load(int64 loaded, bool dirty) {
	capture = (int32) loaded;
	FUnknownPtr<IComponentHandler2> handler(componentHandler);
	if (dirty && handler) {
		handler->setDirty(true);
	}
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
