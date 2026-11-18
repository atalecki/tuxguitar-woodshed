#include "instance.h"

#include "public.sdk/source/vst/hosting/hostclasses.h"
#include "public.sdk/source/vst/utility/stringconvert.h"
#include "pluginterfaces/base/funknownimpl.h"
#include "pluginterfaces/base/ibstream.h"
#include "pluginterfaces/gui/iplugviewcontentscalesupport.h"
#include "pluginterfaces/vst/ivstattributes.h"
#include "pluginterfaces/vst/ivstmessage.h"
#include "pluginterfaces/vst/ivstnoteexpression.h"

#include <algorithm>
#include <cstring>
#include <functional>
#include <map>

#ifdef _WIN32
#include <windows.h>
#endif

namespace TuxGuitar {

using namespace Steinberg::Vst;

namespace {

// what the controller tells the host goes to the instance: its edits, restarts, that its state changed outside an
// edit (dirty), and the work it does in the background (IProgress: a state restored on a thread of its own, say); it has
// no editor TuxGuitar opens on request, and group edits are heard as their edits
class Handler : public U::Implements<U::Directly<IComponentHandler, IComponentHandler2, IProgress>> {
public:
	explicit Handler(Instance* instance) : instance(instance) {}

	tresult PLUGIN_API beginEdit(ParamID id) override { return tell(Notification::kBeginEdit, id, 0.0); }
	tresult PLUGIN_API performEdit(ParamID id, ParamValue normalized) override { return tell(Notification::kPerformEdit, id, normalized); }
	tresult PLUGIN_API endEdit(ParamID id) override { return tell(Notification::kEndEdit, id, 0.0); }
	tresult PLUGIN_API restartComponent(int32 flags) override { return tell(Notification::kRestart, flags, 0.0); }

	tresult PLUGIN_API setDirty(TBool state) override { return (state ? tell(Notification::kDirty, 0, 0.0) : kResultOk); }
	tresult PLUGIN_API requestOpenEditor(FIDString) override { return tell(Notification::kEditorRequested, 0, 0.0); }
	tresult PLUGIN_API startGroupEdit() override { return kResultOk; }
	tresult PLUGIN_API finishGroupEdit() override { return kResultOk; }

	tresult PLUGIN_API start(ProgressType type, const tchar* optionalDescription, ID& outID) override {
		outID = instance->beginProgress(type == AsyncStateRestoration, (optionalDescription ? StringConvert::convert(optionalDescription) : std::string()));
		return kResultOk;
	}
	tresult PLUGIN_API update(ID, ParamValue) override { return kResultOk; }
	tresult PLUGIN_API finish(ID id) override {
		instance->endProgress(id);
		return kResultOk;
	}

private:
	tresult tell(int32 kind, int32 id, double normalized) {
		instance->notify(kind, id, normalized);
		return kResultOk;
	}

	Instance* instance;
};

// what an editor's view asks of the window it is in: another size
class PlugFrame : public U::Implements<U::Directly<IPlugFrame>> {
public:
	explicit PlugFrame(Instance* instance) : instance(instance) {}

	tresult PLUGIN_API resizeView(IPlugView*, ViewRect* newSize) override {
		return (newSize && instance->resizeView(newSize->getWidth(), newSize->getHeight()) ? kResultTrue : kResultFalse);
	}

private:
	Instance* instance;
};

// a stream over bytes in memory
// the attributes of a state's stream: the host says it keeps parts of a state apart (the integer kApart, 1), and the
// plugin sets each part it keeps apart as a binary attribute it names; given back with the state, they are there again.
// Integers and binaries only
class StateAttributes : public U::Implements<U::Directly<IAttributeList>> {
public:
	tresult PLUGIN_API setInt(AttrID id, int64 value) override {
		integers[id] = value;
		return kResultOk;
	}
	tresult PLUGIN_API getInt(AttrID id, int64& value) override {
		auto found = integers.find(id);
		value = (found != integers.end() ? found->second : value);
		return (found != integers.end() ? kResultOk : kResultFalse);
	}
	tresult PLUGIN_API setFloat(AttrID, double) override { return kResultFalse; }
	tresult PLUGIN_API getFloat(AttrID, double&) override { return kResultFalse; }
	tresult PLUGIN_API setString(AttrID, const TChar*) override { return kResultFalse; }
	tresult PLUGIN_API getString(AttrID, TChar*, uint32) override { return kResultFalse; }
	tresult PLUGIN_API setBinary(AttrID id, const void* data, uint32 size) override {
		const char* bytes = static_cast<const char*>(data);
		binaries[id] = std::vector<char>(bytes, bytes + size);
		return kResultOk;
	}
	tresult PLUGIN_API getBinary(AttrID id, const void*& data, uint32& size) override {
		auto found = binaries.find(id);
		if (found == binaries.end()) {
			return kResultFalse;
		}
		data = found->second.data();
		size = (uint32) found->second.size();
		return kResultOk;
	}

	std::map<std::string, std::vector<char>> binaries;

private:
	std::map<std::string, int64> integers;
};

// the stream of a state: its bytes, and, when it has them, attributes the plugin reads and sets (IStreamAttributes)
class ByteStream : public U::Implements<U::Directly<IBStream, IStreamAttributes>> {
public:
	ByteStream() {}
	explicit ByteStream(const std::vector<char>& bytes) : bytes(bytes) {}
	ByteStream(const std::vector<char>& bytes, IPtr<StateAttributes> attributes) : bytes(bytes), attributes(attributes) {}

	tresult PLUGIN_API getFileName(String128) override { return kResultFalse; }
	IAttributeList* PLUGIN_API getAttributes() override { return attributes; }

	tresult PLUGIN_API read(void* buffer, int32 count, int32* read) override {
		int32 available = (int32) std::max<int64>(0, (int64) bytes.size() - position);
		int32 done = std::min(count, available);
		if (done > 0) {
			std::memcpy(buffer, bytes.data() + position, (size_t) done);
		}
		position += done;
		if (read) {
			*read = done;
		}
		return kResultOk;
	}

	tresult PLUGIN_API write(void* buffer, int32 count, int32* written) override {
		if (position + count > (int64) bytes.size()) {
			bytes.resize((size_t) (position + count));
		}
		std::memcpy(bytes.data() + position, buffer, (size_t) count);
		position += count;
		if (written) {
			*written = count;
		}
		return kResultOk;
	}

	tresult PLUGIN_API seek(int64 pos, int32 mode, int64* result) override {
		int64 base = (mode == kIBSeekSet ? 0 : (mode == kIBSeekCur ? position : (int64) bytes.size()));
		position = std::max<int64>(0, base + pos);
		if (result) {
			*result = position;
		}
		return kResultOk;
	}

	tresult PLUGIN_API tell(int64* pos) override {
		if (pos) {
			*pos = position;
		}
		return kResultOk;
	}

	const std::vector<char>& data() const { return bytes; }

private:
	std::vector<char> bytes;
	int64 position = 0;
	IPtr<StateAttributes> attributes;
};

// the attributes of a state's stream, the host keeping its parts apart, with those parts
IPtr<StateAttributes> apart(const std::vector<std::pair<std::string, std::vector<char>>>& resources) {
	IPtr<StateAttributes> attributes = owned(new StateAttributes());
	attributes->setInt(kApart, 1);
	for (const auto& resource : resources) {
		attributes->binaries[resource.first] = resource.second;
	}
	return attributes;
}

int32 mainChannels(IComponent* component, BusDirection direction) {
	BusInfo info = {};
	if (component->getBusCount(kAudio, direction) > 0 && component->getBusInfo(kAudio, direction, 0, info) == kResultOk) {
		return info.channelCount;
	}
	return 0;
}

}  // namespace

Instance::Instance(VST3::Hosting::Module::Ptr module, int32 maxBlock, const std::string& sharedPath)
    : module(module), maxBlock(maxBlock), shared(sharedPath, maxBlock), eventList(1024), changes(256), outputChanges(256) {}

std::unique_ptr<Instance> Instance::open(VST3::Hosting::Module::Ptr module, const VST3::UID& classId, FUnknown* host, double sampleRate,
                                         int32 maxBlock, const std::string& sharedPath, std::string& error) {
	std::unique_ptr<Instance> instance(new Instance(module, maxBlock, sharedPath));
	if (!instance->shared.isOpen()) {
		error = "cannot map the shared blocks " + sharedPath;
		return nullptr;
	}
	if (!instance->load(classId, host, error)) {
		return nullptr;
	}
	instance->setupBuses();
	if (!instance->start(sampleRate, error)) {
		return nullptr;
	}
	return instance;
}

bool Instance::load(const VST3::UID& classId, FUnknown* host, std::string& error) {
	component = module->getFactory().createInstance<IComponent>(classId);
	if (!component || component->initialize(host) != kResultOk) {
		error = "the plugin's component did not start";
		component = nullptr;
		return false;
	}
	processor = U::cast<IAudioProcessor>(component);
	controller = U::cast<IEditController>(component);
	if (!processor || (!controller && !loadController(host))) {
		error = "the plugin has no audio processor or no edit controller";
		return false;
	}
	handler = owned(new Handler(this));
	controller->setComponentHandler(handler);
	return true;
}

// a controller apart from the component: created, connected to it, and given its state
bool Instance::loadController(FUnknown* host) {
	TUID cid;
	if (component->getControllerClassId(cid) != kResultTrue) {
		return false;
	}
	controller = module->getFactory().createInstance<IEditController>(VST3::UID::fromTUID(cid));
	if (!controller || controller->initialize(host) != kResultOk) {
		controller = nullptr;
		return false;
	}
	separateController = true;
	IPtr<IConnectionPoint> fromComponent = U::cast<IConnectionPoint>(component);
	IPtr<IConnectionPoint> fromController = U::cast<IConnectionPoint>(controller);
	if (fromComponent && fromController) {
		fromComponent->connect(fromController);
		fromController->connect(fromComponent);
	}
	ByteStream state;
	component->getState(&state);
	ByteStream copy(state.data());
	controller->setComponentState(&copy);
	return true;
}

// the main audio buses and the main event input are on; side chains are off
void Instance::setupBuses() {
	for (BusDirection direction : {kInput, kOutput}) {
		int32 count = component->getBusCount(kAudio, direction);
		for (int32 i = 0; i < count; i++) {
			component->activateBus(kAudio, direction, i, i == 0);
		}
	}
	if (component->getBusCount(kEvent, kInput) > 0) {
		component->activateBus(kEvent, kInput, 0, true);
	}
	inputChannels = mainChannels(component, kInput);
	outputChannels = mainChannels(component, kOutput);
}

bool Instance::start(double rate, std::string& error) {
	sampleRate = rate;
	ProcessSetup setup = {kRealtime, kSample32, maxBlock, rate};
	if (processor->setupProcessing(setup) != kResultOk || !data.prepare(*component, maxBlock, kSample32)) {
		error = "the plugin refused to process 32-bit float blocks of " + std::to_string(maxBlock) + " samples";
		return false;
	}
	data.inputEvents = &eventList;
	data.inputParameterChanges = &changes;
	data.outputParameterChanges = &outputChanges;
	data.processContext = &context;
	component->setActive(true);
	processor->setProcessing(true);
	active = true;
	return true;
}

void Instance::stop() {
	if (active) {
		processor->setProcessing(false);
		component->setActive(false);
		active = false;
	}
}

Instance::~Instance() {
	closeEditor();
	if (component) {
		stop();
	}
	if (separateController) {
		IPtr<IConnectionPoint> fromComponent = U::cast<IConnectionPoint>(component);
		IPtr<IConnectionPoint> fromController = U::cast<IConnectionPoint>(controller);
		if (fromComponent && fromController) {
			fromComponent->disconnect(fromController);
			fromController->disconnect(fromComponent);
		}
	}
	if (controller) {
		controller->setComponentHandler(nullptr);
		if (separateController) {
			controller->terminate();
		}
	}
	if (component) {
		component->terminate();
	}
}

int32 Instance::latency() const { return (int32) processor->getLatencySamples(); }

// kInfiniteTail comes out as -1
int32 Instance::tail() const { return (int32) processor->getTailSamples(); }

void Instance::process(int32 frames, const Transport& transport, const std::vector<NoteEvent>& events) {
	frames = std::max(0, std::min(frames, maxBlock));
	copyIn(frames);
	addEvents(events);
	setContext(frames, transport);
	data.numSamples = frames;
	data.processMode = (transport.offline ? kOffline : kRealtime);
	processor->process(data);
	copyOut(frames);
	eventList.clear();
	changes.clearQueue();
	outputChanges.clearQueue();
}

// a mono input takes the mean of the two sides; a stereo one each side; any other channel silence
void Instance::copyIn(int32 frames) {
	if (data.numInputs == 0) {
		return;
	}
	AudioBusBuffers& bus = data.inputs[0];
	for (int32 c = 0; c < bus.numChannels; c++) {
		float* to = bus.channelBuffers32[c];
		for (int32 i = 0; i < frames; i++) {
			float left = shared.input(0)[i];
			float right = shared.input(1)[i];
			to[i] = (bus.numChannels == 1 ? 0.5f * (left + right) : (c < 2 ? (c == 0 ? left : right) : 0.f));
		}
	}
}

// a mono output sounds on both sides
void Instance::copyOut(int32 frames) {
	int32 channels = (data.numOutputs > 0 ? data.outputs[0].numChannels : 0);
	for (int32 c = 0; c < 2; c++) {
		float* to = shared.output(c);
		if (channels == 0) {
			std::fill(to, to + frames, 0.f);
		} else {
			std::memcpy(to, data.outputs[0].channelBuffers32[std::min(c, channels - 1)], sizeof(float) * (size_t) frames);
		}
	}
}

void Instance::addEvents(const std::vector<NoteEvent>& events) {
	for (const NoteEvent& note : events) {
		Event event = {};
		event.sampleOffset = note.offset;
		event.flags = Event::kIsLive;
		if (note.type == NoteEvent::kNoteOn) {
			event.type = Event::kNoteOnEvent;
			event.noteOn = {0, (int16) note.pitch, 0.f, note.value, 0, note.noteId};
		} else if (note.type == NoteEvent::kNoteOff) {
			event.type = Event::kNoteOffEvent;
			event.noteOff = {0, (int16) note.pitch, note.value, note.noteId, 0.f};
		} else {
			event.type = Event::kNoteExpressionValueEvent;
			event.noteExpressionValue = {kTuningTypeID, note.noteId, 0.5 + note.value / 240.0};
		}
		eventList.addEvent(event);
	}
}

void Instance::setContext(int32 frames, const Transport& transport) {
	context.state = ProcessContext::kTempoValid | ProcessContext::kTimeSigValid | ProcessContext::kProjectTimeMusicValid |
	                ProcessContext::kContTimeValid | (transport.playing ? ProcessContext::kPlaying : 0);
	context.sampleRate = sampleRate;
	context.projectTimeSamples = transport.samples;
	context.tempo = transport.tempo;
	context.timeSigNumerator = transport.numerator;
	context.timeSigDenominator = transport.denominator;
	context.projectTimeMusic = transport.quarters;
	context.continousTimeSamples += frames;  // (sic: the SDK spells it so)
}

std::vector<ParameterInfo> Instance::parameters() {
	std::vector<ParameterInfo> infos;
	int32 count = controller->getParameterCount();
	for (int32 i = 0; i < count; i++) {
		Vst::ParameterInfo info = {};
		if (controller->getParameterInfo(i, info) == kResultOk) {
			std::string title;
			for (int k = 0; k < 128 && info.title[k] != 0; k++) {
				title.push_back(info.title[k] < 128 ? (char) info.title[k] : '?');
			}
			double minimum = controller->normalizedParamToPlain(info.id, 0.0);
			double maximum = controller->normalizedParamToPlain(info.id, 1.0);
			infos.push_back(ParameterInfo{(int32) info.id, title, parameter((int32) info.id), minimum, maximum, info.stepCount, info.flags});
		}
	}
	return infos;
}

double Instance::parameter(int32 id) {
	return controller->normalizedParamToPlain((ParamID) id, controller->getParamNormalized((ParamID) id));
}

// on the controller now, and on the processor from the start of the next block
double Instance::setParameter(int32 id, double plain) {
	ParamValue normalized = controller->plainParamToNormalized((ParamID) id, plain);
	controller->setParamNormalized((ParamID) id, normalized);
	int32 index = 0;
	IParamValueQueue* queue = changes.addParameterData((ParamID) id, index);
	if (queue) {
		queue->addPoint(0, normalized, index);
	}
	return parameter(id);
}

SavedState Instance::state() {
	SavedState saved;
	IPtr<StateAttributes> attributes = apart({});
	ByteStream state(std::vector<char>(), attributes);
	component->getState(&state);
	saved.component = state.data();
	for (const auto& resource : attributes->binaries) {
		saved.resources.push_back(resource);
	}
	ByteStream own;
	if (separateController) {
		controller->getState(&own);
	}
	saved.controller = own.data();
	return saved;
}

void Instance::setState(const SavedState& saved) {
	IPtr<StateAttributes> attributes = apart(saved.resources);
	ByteStream state(saved.component, attributes);
	component->setState(&state);
	if (separateController) {
		ByteStream copy(saved.component, attributes);
		controller->setComponentState(&copy);
		if (!saved.controller.empty()) {
			ByteStream own(saved.controller);
			controller->setState(&own);
		}
	}
}

// what still sounds (tails, voices) is dropped: the plugin starts again
void Instance::reset() {
	stop();
	component->setActive(true);
	processor->setProcessing(true);
	active = true;
}

bool Instance::hasEditor() {
	IPtr<IPlugView> editor = owned(controller->createView(ViewType::kEditor));
	return editor != nullptr;
}

#ifdef _WIN32
namespace {

// the shown top-level window of this process in front of its others, or none
HWND frontWindow() {
	for (HWND window = GetTopWindow(nullptr); window; window = GetWindow(window, GW_HWNDNEXT)) {
		DWORD process = 0;
		GetWindowThreadProcessId(window, &process);
		if (process == GetCurrentProcessId() && IsWindowVisible(window)) {
			return window;
		}
	}
	return nullptr;
}

// a caption with a close box; a border to drag only when the view can be resized
const DWORD kEditorStyle = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU;

// the instance whose editor is in the window (none while the window is being made)
Instance* editing(HWND window) { return reinterpret_cast<Instance*>(GetWindowLongPtrW(window, GWLP_USERDATA)); }

// the frame of a window of that style whose inside is that size, at that DPI
SIZE frameFor(DWORD style, LONG width, LONG height, UINT dpi) {
	RECT frame = {0, 0, width, height};
	AdjustWindowRectExForDpi(&frame, style, FALSE, 0, dpi);
	return SIZE{frame.right - frame.left, frame.bottom - frame.top};
}

DWORD styleOf(HWND window) { return (DWORD) GetWindowLongPtrW(window, GWL_STYLE); }

// a window inside another: the window it is in; else its owner
HWND outer(HWND window) { return ((styleOf(window) & WS_CHILD) != 0 ? GetAncestor(window, GA_PARENT) : GetWindow(window, GW_OWNER)); }

// the width of a window's inside, and its height
int32 insideWidth(HWND window) {
	RECT inside = {};
	GetClientRect(window, &inside);
	return inside.right;
}

int32 insideHeight(HWND window) {
	RECT inside = {};
	GetClientRect(window, &inside);
	return inside.bottom;
}

// the thread takes the DPI awareness of a window while this lives (the windows it makes meanwhile have it), then the
// one it had
struct ThreadAwareness {
	explicit ThreadAwareness(HWND window) : before(SetThreadDpiAwarenessContext(GetWindowDpiAwarenessContext(window))) {}
	~ThreadAwareness() { SetThreadDpiAwarenessContext(before); }
	DPI_AWARENESS_CONTEXT before;
};

// the DPI awareness of a window (of any process); kNoWindow (DPI_AWARENESS_INVALID) for none
int32 awarenessOf(HWND window) {
	DPI_AWARENESS_CONTEXT context = GetWindowDpiAwarenessContext(window);
	bool version2 = (AreDpiAwarenessContextsEqual(context, DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2) != FALSE);
	return (version2 ? kPerMonitorAwareV2 : (int32) GetAwarenessFromDpiAwarenessContext(context));
}

// where the window is: owned by a window, or inside one of TuxGuitar (its parent), and that window's DPI awareness
void where(EditorWindow& facts, HWND shown) {
	facts.owner = (int64) (intptr_t) GetWindow(shown, GW_OWNER);
	facts.embedded = ((styleOf(shown) & WS_CHILD) != 0);
	facts.parent = (int64) (intptr_t) (facts.embedded ? outer(shown) : nullptr);
	facts.ownerAwareness = awarenessOf(outer(shown));
}

// a new size of the window, as the user drags its border: the inside is the size nearest to it the view accepts
void constrain(HWND window, WINDOWPOS* position) {
	SIZE border = frameFor(styleOf(window), 0, 0, GetDpiForWindow(window));
	int32 width = position->cx - border.cx;
	int32 height = position->cy - border.cy;
	if (!(position->flags & SWP_NOSIZE) && editing(window) && editing(window)->constrainView(width, height)) {
		position->cx = width + border.cx;
		position->cy = height + border.cy;
	}
}

// closing an editor's window (its close box, Alt+F4) hides it, the plugin's view still in it: Edit shows it again; the
// instance tells TuxGuitar, as what the editor did may not have been told. The window is destroyed when TuxGuitar
// closes the editor. Gone to a screen of another DPI, it tells the view that screen's scale and takes the frame Windows
// suggests. Resized, it tells the view, within the sizes the view accepts
LRESULT CALLBACK editorProcedure(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	if (message == WM_CLOSE) {
		ShowWindow(window, SW_HIDE);
		if (editing(window)) {
			editing(window)->notify(Notification::kEditorHidden, 0, 0.0);
		}
		return 0;
	}
	if (message == WM_DPICHANGED) {
		const RECT* frame = reinterpret_cast<const RECT*>(lParam);
		editing(window)->scaleView(HIWORD(wParam) / (double) USER_DEFAULT_SCREEN_DPI);
		SetWindowPos(window, nullptr, frame->left, frame->top, frame->right - frame->left, frame->bottom - frame->top, SWP_NOZORDER | SWP_NOACTIVATE);
		return 0;
	}
	if (message == WM_WINDOWPOSCHANGING) {
		constrain(window, reinterpret_cast<WINDOWPOS*>(lParam));
	}
	if (message == WM_SIZE && editing(window)) {
		editing(window)->viewSized(LOWORD(lParam), HIWORD(lParam));
	}
	return DefWindowProcW(window, message, wParam, lParam);
}

// what the frame of an editor inside a window of TuxGuitar tells its instance: its size (the view is told), the
// window it is in gone to a screen of another DPI (the view is told the scale), the frame destroyed with that window
void frameMessage(Instance* instance, HWND window, UINT message, LPARAM lParam) {
	switch (message) {
	case WM_SIZE:
		instance->viewSized(LOWORD(lParam), HIWORD(lParam));
		break;
	case WM_DPICHANGED_AFTERPARENT:
		instance->scaleView(GetDpiForWindow(window) / (double) USER_DEFAULT_SCREEN_DPI);
		break;
	case WM_NCDESTROY:
		instance->frameGone(window);
		break;
	}
}

LRESULT CALLBACK embeddedProcedure(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	if (editing(window)) {
		frameMessage(editing(window), window, message, lParam);
	}
	return DefWindowProcW(window, message, wParam, lParam);
}

// a class of the host's windows, registered once
const wchar_t* registered(const wchar_t* name, WNDPROC procedure) {
	WNDCLASSW type = {};
	type.lpfnWndProc = procedure;
	type.hInstance = GetModuleHandleW(nullptr);
	type.hCursor = LoadCursorW(nullptr, IDC_ARROW);
	type.hbrBackground = (HBRUSH) (COLOR_WINDOW + 1);
	type.lpszClassName = name;
	RegisterClassW(&type);
	return name;
}

// the class of editor windows of their own
const wchar_t* editorClass() {
	static const wchar_t* name = registered(L"TuxGuitarVst3Editor", editorProcedure);
	return name;
}

// the class of the frames of editors inside a window of TuxGuitar
const wchar_t* embeddedClass() {
	static const wchar_t* name = registered(L"TuxGuitarVst3Embedded", embeddedProcedure);
	return name;
}

// where a window of that size opens: centred on the owner window, within the work area of the screen that window is
// on; without an owner, where the system places a new window
POINT editorPosition(HWND owner, LONG width, LONG height) {
	RECT area = {};
	MONITORINFO screen = {sizeof(MONITORINFO)};
	if (!owner || !GetWindowRect(owner, &area) || !GetMonitorInfoW(MonitorFromWindow(owner, MONITOR_DEFAULTTONEAREST), &screen)) {
		return POINT{CW_USEDEFAULT, CW_USEDEFAULT};
	}
	const RECT& work = screen.rcWork;
	LONG x = area.left + (area.right - area.left - width) / 2;
	LONG y = area.top + (area.bottom - area.top - height) / 2;
	return POINT{std::max(work.left, std::min(x, work.right - width)), std::max(work.top, std::min(y, work.bottom - height))};
}

// the frame moved onto the work area of the screen nearest to it, its size kept as far as it fits there
RECT onScreen(const RECT& frame) {
	MONITORINFO screen = {sizeof(MONITORINFO)};
	GetMonitorInfoW(MonitorFromRect(&frame, MONITOR_DEFAULTTONEAREST), &screen);
	const RECT& work = screen.rcWork;
	LONG width = std::min(frame.right - frame.left, work.right - work.left);
	LONG height = std::min(frame.bottom - frame.top, work.bottom - work.top);
	LONG x = std::max(work.left, std::min(frame.left, work.right - width));
	LONG y = std::max(work.top, std::min(frame.top, work.bottom - height));
	return RECT{x, y, x + width, y + height};
}

// what is done to the editors' windows once the reply is sent (doEditorWindowWork), as it waits on the thread of a
// window of TuxGuitar (TuxGuitar's UI thread, which waits for the reply meanwhile): an owned window shown, a frame put
// inside a window of TuxGuitar, or destroyed there
std::vector<std::function<void()>> afterReply;

// the keys each frame of an editor inside a window of TuxGuitar hands to that window (virtual key | modifiers << 8),
// while the frame is there
struct PassedKeys {
	HWND parent;
	std::set<int32> keys;
};
std::map<HWND, PassedKeys> passing;

// the modifiers the thread sees held: 1 Shift, 2 Ctrl, 4 Alt
int32 modifiersHeld() {
	int32 shift = (GetKeyState(VK_SHIFT) < 0 ? 1 : 0);
	int32 control = (GetKeyState(VK_CONTROL) < 0 ? 2 : 0);
	return shift | control | (GetKeyState(VK_MENU) < 0 ? 4 : 0);
}

// the keys of the frame the window is, or is in; null in no frame passing keys
const PassedKeys* passingFor(HWND window) {
	for (const auto& entry : passing) {
		if (entry.first == window || IsChild(entry.first, window)) {
			return &entry.second;
		}
	}
	return nullptr;
}

// a key typed in an editor inside a window of TuxGuitar that its frame passes goes to that window instead, as if typed
// there, posted (nothing waits on TuxGuitar's thread); the view gets nothing
void passOn(MSG* message) {
	const PassedKeys* passed = passingFor(message->hwnd);
	if (passed && passed->keys.count((int32) message->wParam | modifiersHeld() << 8)) {
		PostMessageW(passed->parent, message->message, message->wParam, message->lParam);
		message->message = WM_NULL;
	}
}

bool isKeyDown(UINT kind) { return (kind == WM_KEYDOWN || kind == WM_SYSKEYDOWN); }

// sees each message the thread takes from its queue before it is dispatched
LRESULT CALLBACK keysHook(int code, WPARAM removal, LPARAM message) {
	MSG* taken = reinterpret_cast<MSG*>(message);
	if (code == HC_ACTION && removal == PM_REMOVE && isKeyDown(taken->message)) {
		passOn(taken);
	}
	return CallNextHookEx(nullptr, code, removal, message);
}

// the frame inside the parent hands it those keys; the thread's hook set once (on its own thread only, no global hook)
void passKeys(HWND frame, HWND parent, const std::set<int32>& keys) {
	static HHOOK hook = SetWindowsHookExW(WH_GETMESSAGE, keysHook, nullptr, GetCurrentThreadId());
	(void) hook;
	passing[frame] = PassedKeys{parent, keys};
}

// the frame goes inside the window: a child of it at its top left, shown, never activated (the user gives it the focus
// by clicking in it)
void putInside(HWND frame, HWND parent, int32 width, int32 height) {
	SetWindowLongPtrW(frame, GWL_STYLE, WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS);
	SetParent(frame, parent);
	SetWindowPos(frame, nullptr, 0, 0, width, height, SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED | SWP_SHOWWINDOW);
}

// shown, in front of the other windows, and the active window when Windows lets this process make it so
void showInFront(HWND window) {
	ShowWindow(window, SW_SHOWNORMAL);
	SetWindowPos(window, HWND_TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE);
	SetForegroundWindow(window);
}

// UTF-8 as Windows' UTF-16, and back
std::wstring wide(const std::string& text) {
	int count = MultiByteToWideChar(CP_UTF8, 0, text.data(), (int) text.size(), nullptr, 0);
	std::wstring converted((size_t) count, L'\0');
	MultiByteToWideChar(CP_UTF8, 0, text.data(), (int) text.size(), &converted[0], count);
	return converted;
}

std::string narrow(const std::wstring& text) {
	int count = WideCharToMultiByte(CP_UTF8, 0, text.data(), (int) text.size(), nullptr, 0, nullptr, nullptr);
	std::string converted((size_t) count, '\0');
	WideCharToMultiByte(CP_UTF8, 0, text.data(), (int) text.size(), &converted[0], count, nullptr, nullptr);
	return converted;
}

std::string caption(HWND window) {
	std::wstring text((size_t) GetWindowTextLengthW(window) + 1, L'\0');
	text.resize((size_t) GetWindowTextW(window, &text[0], (int) text.size()));
	return narrow(text);
}

}  // namespace

// the list is taken first: a request answered while this work waits on another thread does its own work (a window
// closed meanwhile is no window: Windows refuses to show it)
void doEditorWindowWork() {
	std::vector<std::function<void()>> work;
	work.swap(afterReply);
	for (const std::function<void()>& done : work) {
		done();
	}
}

// an editor already open in a window of its own is shown and brought to the front as it is (its owner stays the one it
// opened with), titled anew; one inside a window of TuxGuitar closes first
bool Instance::openEditor(int64 owner, const std::string& title) {
	if (embeddedIn) {
		closeEditor();
	}
	if (!window && !createEditor(owner)) {
		return false;
	}
	HWND shown = (HWND) window;
	SetWindowTextW(shown, wide(title).c_str());
	afterReply.push_back([shown] { showInFront(shown); });
	return true;
}

ViewFit Instance::embedEditor(int64 parentHandle, double share, const std::string& title, const std::set<int32>& keys) {
	HWND parent = (HWND) (intptr_t) parentHandle;
	passed = keys;
	if (window && embeddedIn == parent) {
		passKeys((HWND) window, parent, keys);
		return fitEditor();
	}
	closeEditor();
	ThreadAwareness taken(parent);
	int32 maxHeight = (int32) std::lround(insideHeight(GetAncestor(parent, GA_ROOT)) * share);
	return (makeView(GetDpiForWindow(parent)) ? embedView(parent, maxHeight, title) : ViewFit{kNoEditor, 0, 0, false, 0});
}

// the view made goes inside the parent, unless it is too large for it: then it goes, the size it would take told
ViewFit Instance::embedView(void* parent, int32 maxHeight, const std::string& title) {
	ViewFit fit = fitTo(parent);
	if (fit.height > maxHeight || fit.width > insideWidth((HWND) parent)) {
		closeEditor();
		fit.kind = kTooLarge;
		return fit;
	}
	fit.kind = (frameIn(parent, fit, title) ? kEmbedded : kNoEditor);
	return fit;
}

// a frame for the view, made hidden and top level, of the thread's DPI awareness (the parent's), the view attached to
// it and told its size; it goes inside the parent once the reply is sent. False, the view gone, when it does not attach
bool Instance::frameIn(void* parent, const ViewFit& fit, const std::string& title) {
	HWND made = CreateWindowExW(WS_EX_NOPARENTNOTIFY, embeddedClass(), wide(title).c_str(), WS_POPUP | WS_CLIPCHILDREN, 0, 0, fit.width, fit.height, nullptr, nullptr,
	                            GetModuleHandleW(nullptr), nullptr);
	embeddedIn = parent;
	if (!attach(made)) {
		return false;
	}
	passKeys(made, (HWND) parent, passed);
	viewSized(fit.width, fit.height);
	afterReply.push_back([made, parent, fit] { putInside(made, (HWND) parent, fit.width, fit.height); });
	return true;
}

// in the window: the view's own size, or, when it can be resized, the size it accepts nearest to the width of the
// window's inside at its height
ViewFit Instance::fitTo(void* parent) {
	ViewRect size = {0, 0, 400, 300};
	view->getSize(&size);
	int32 width = insideWidth((HWND) parent);
	int32 fitWidth = width;
	int32 fitHeight = size.getHeight();
	bool resizable = constrainView(fitWidth, fitHeight);
	return (resizable ? ViewFit{kEmbedded, fitWidth, fitHeight, true, width} : ViewFit{kEmbedded, size.getWidth(), size.getHeight(), false, width});
}

ViewFit Instance::fitEditor() {
	if (!embeddedIn) {
		return ViewFit{kNoEditor, 0, 0, false, 0};
	}
	ViewFit fit = fitTo(embeddedIn);
	SetWindowPos((HWND) window, nullptr, 0, 0, fit.width, fit.height, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE);
	return fit;
}

void Instance::frameGone(void* gone) {
	passing.erase((HWND) gone);
	if (gone == window) {
		removeView();
		window = nullptr;
		embeddedIn = nullptr;
	}
}

// a hidden window whose inside is the size of the view, attached to it; an owner that is no window (0, or a window
// closed since) owns nothing: the editor opens in a window of its own. The view is told the scale of the screen it opens
// on (the owner's; else the main screen's) before it says its size
bool Instance::createEditor(int64 owner) {
	HWND ownerWindow = (IsWindow((HWND) (intptr_t) owner) ? (HWND) (intptr_t) owner : nullptr);
	UINT dpi = (ownerWindow ? GetDpiForWindow(ownerWindow) : GetDpiForSystem());
	return (makeView(dpi) && windowOwnedBy(ownerWindow, dpi));
}

// the window of the view made, at that DPI, owned by the window given (none: nullptr); false, the view gone, when it
// does not attach
bool Instance::windowOwnedBy(void* ownerWindow, uint32 dpi) {
	ViewRect size = {0, 0, 400, 300};
	view->getSize(&size);
	DWORD style = kEditorStyle | (view->canResize() == kResultTrue ? WS_THICKFRAME : 0);
	SIZE outside = frameFor(style, size.getWidth(), size.getHeight(), dpi);
	POINT at = editorPosition((HWND) ownerWindow, outside.cx, outside.cy);
	HWND made = CreateWindowExW(0, editorClass(), L"", style, at.x, at.y, outside.cx, outside.cy, (HWND) ownerWindow, nullptr, GetModuleHandleW(nullptr), nullptr);
	bool attached = attach(made);
	place(made);
	return attached;
}

void Instance::placeEditor(const std::vector<int32>& frame) {
	placed = frame;
}

// the window made goes where it was asked to open, onto a visible screen: its size too when its view can be resized
// (within what the view accepts)
void Instance::place(void* made) {
	RECT wanted = {placed[0], placed[1], placed[2], placed[3]};
	if (wanted.right > wanted.left && window == made) {
		RECT at = onScreen(wanted);
		UINT keep = ((styleOf((HWND) made) & WS_THICKFRAME) != 0 ? 0 : SWP_NOSIZE);
		SetWindowPos((HWND) made, nullptr, at.left, at.top, at.right - at.left, at.bottom - at.top, SWP_NOZORDER | SWP_NOACTIVATE | keep);
	}
}

// the view attached to the window made, which tells the instance from now on; false, the view gone, when there is no
// window or the view does not attach
bool Instance::attach(void* made) {
	window = made;
	SetWindowLongPtrW((HWND) made, GWLP_USERDATA, (LONG_PTR) this);
	if (!made || view->attached(made, kPlatformTypeHWND) != kResultOk) {
		closeEditor();
		return false;
	}
	return true;
}

// the view of the plugin's editor for a window, told the scale of that DPI and given its frame; false (no view kept)
// when it has none for windows
bool Instance::makeView(uint32 dpi) {
	view = owned(controller->createView(ViewType::kEditor));
	if (!view || view->isPlatformTypeSupported(kPlatformTypeHWND) != kResultTrue) {
		view = nullptr;
		return false;
	}
	scaleView(dpi / (double) USER_DEFAULT_SCREEN_DPI);
	frame = owned(new PlugFrame(this));
	view->setFrame(frame);
	return true;
}

// the window takes the size the view asks, and TuxGuitar is told it (an editor inside its window: the panel follows)
bool Instance::resizeView(int32 width, int32 height) {
	HWND sized = (HWND) window;
	if (!sized) {
		return false;
	}
	notifications.push_back(Notification{Notification::kEditorResized, width, (double) height});
	SIZE outside = frameFor(styleOf(sized), width, height, GetDpiForWindow(sized));
	return (SetWindowPos(sized, nullptr, 0, 0, outside.cx, outside.cy, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE) != FALSE);
}

void Instance::closeEditor() {
	removeView();
	releaseWindow();
}

void Instance::removeView() {
	if (view && window) {
		view->removed();
	}
	if (view) {
		view->setFrame(nullptr);
	}
	view = nullptr;
	frame = nullptr;
}

// its window goes: a window of its own at once; a frame inside a window of TuxGuitar once the reply is sent (destroying
// a child waits on its parent's thread), no longer telling the instance
void Instance::releaseWindow() {
	HWND released = (HWND) window;
	bool inside = (embeddedIn != nullptr);
	window = nullptr;
	embeddedIn = nullptr;
	if (inside) {
		SetWindowLongPtrW(released, GWLP_USERDATA, 0);
		afterReply.push_back([released] { DestroyWindow(released); });
	} else if (released) {
		DestroyWindow(released);
	}
}

EditorWindow Instance::editorWindow() {
	EditorWindow facts = {};
	HWND shown = (HWND) window;
	RECT frame = {};
	RECT inside = {};
	if (shown && GetWindowRect(shown, &frame) && GetClientRect(shown, &inside)) {
		facts.window = (int64) (intptr_t) shown;
		where(facts, shown);
		facts.shown = (IsWindowVisible(shown) != FALSE);
		facts.front = (frontWindow() == shown);
		facts.left = frame.left;
		facts.top = frame.top;
		facts.right = frame.right;
		facts.bottom = frame.bottom;
		facts.width = inside.right;
		facts.height = inside.bottom;
		facts.title = caption(shown);
		facts.dpi = (int32) GetDpiForWindow(shown);
		facts.resizable = ((GetWindowLongPtrW(shown, GWL_STYLE) & WS_THICKFRAME) != 0);
		facts.awareness = awarenessOf(shown);
	}
	return facts;
}
#else
// editor windows need the platform's window system: only Windows so far
bool Instance::openEditor(int64, const std::string&) { return false; }

void Instance::placeEditor(const std::vector<int32>& frame) { placed = frame; }

ViewFit Instance::embedEditor(int64, double, const std::string&, const std::set<int32>&) { return ViewFit{kNoEditor, 0, 0, false, 0}; }

ViewFit Instance::fitEditor() { return ViewFit{kNoEditor, 0, 0, false, 0}; }

void Instance::frameGone(void*) {}

void Instance::closeEditor() { view = nullptr; }

EditorWindow Instance::editorWindow() { return EditorWindow{}; }

bool Instance::resizeView(int32, int32) { return false; }
#endif

void Instance::viewSized(int32 width, int32 height) {
	if (view) {
		ViewRect size(0, 0, width, height);
		view->onSize(&size);
	}
}

bool Instance::constrainView(int32& width, int32& height) {
	if (!view || view->canResize() != kResultTrue) {
		return false;
	}
	ViewRect size(0, 0, width, height);
	view->checkSizeConstraint(&size);
	width = size.getWidth();
	height = size.getHeight();
	return true;
}

void Instance::scaleView(double factor) {
	FUnknownPtr<IPlugViewContentScaleSupport> scaled(view);
	if (scaled) {
		scaled->setContentScaleFactor((IPlugViewContentScaleSupport::ScaleFactor) factor);
	}
}

std::vector<char> Instance::message(const std::string& id, const std::vector<std::pair<std::string, int64>>& integers,
                                    const std::vector<std::pair<std::string, std::vector<char>>>& binaries) {
	IPtr<IConnectionPoint> connection = U::cast<IConnectionPoint>(controller);
	if (!connection) {
		return std::vector<char>();
	}
	IPtr<IMessage> message = owned(new HostMessage());
	message->setMessageID(id.c_str());
	IAttributeList* attributes = message->getAttributes();
	for (const auto& entry : integers) {
		attributes->setInt(entry.first.c_str(), entry.second);
	}
	for (const auto& entry : binaries) {
		attributes->setBinary(entry.first.c_str(), entry.second.data(), (uint32) entry.second.size());
	}
	connection->notify(message);
	const void* report = nullptr;
	uint32 size = 0;
	if (attributes->getBinary("report", report, size) == kResultOk && report) {
		const char* bytes = static_cast<const char*>(report);
		return std::vector<char>(bytes, bytes + size);
	}
	return std::vector<char>();
}

void Instance::notify(int32 kind, int32 id, double normalized) {
	double plain = (kind == Notification::kPerformEdit ? controller->normalizedParamToPlain((ParamID) id, normalized) : 0.0);
	notifications.push_back(Notification{kind, id, plain});
	if (kind == Notification::kPerformEdit && separateController) {
		int32 index = 0;
		IParamValueQueue* queue = changes.addParameterData((ParamID) id, index);
		if (queue) {
			queue->addPoint(0, normalized, index);
		}
	}
}

IProgress::ID Instance::beginProgress(bool restoration, const std::string& what) {
	IProgress::ID id = nextProgress++;
	if (restoration) {
		restorations[id] = what;
	}
	return id;
}

void Instance::endProgress(IProgress::ID id) {
	restorations.erase(id);
}

std::vector<Notification> Instance::takeNotifications() {
	std::vector<Notification> taken;
	taken.swap(notifications);
	return taken;
}

}  // namespace TuxGuitar
