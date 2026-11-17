// tuxguitar-vst3-host: runs VST3 plugins apart from TuxGuitar, so a plugin that crashes takes only this process down.
// TuxGuitar starts it and talks to it over its standard input and output (no network port, no named pipe): one request
// at a time, each answered by one reply. Audio goes through a file both map (see SharedBlocks).
//
// A request is an operation code and its fields; a reply is a status (0 done, 1 failed, then the reason), the
// operation's fields, then what the plugins' controllers told the host since the last reply (edits, restarts).
// The operations and their fields are in TGVst3Protocol.java, the other side of this file.
#include "instance.h"
#include "sockets.h"
#include "wire.h"

#include "public.sdk/source/vst/hosting/hostclasses.h"

#include <cstdio>
#include <map>
#include <set>

#ifdef _WIN32
#include <windows.h>
#define tg_pid() ((int64_t) GetCurrentProcessId())
#else
#include <unistd.h>
#define tg_pid() ((int64_t) getpid())
#endif

namespace TuxGuitar {

const int32_t kProtocolVersion = 5;

enum Operation {
	kHello = 1,
	kScan = 2,
	kOpen = 3,
	kClose = 4,
	kProcess = 5,
	kSetParameter = 6,
	kGetParameter = 7,
	kGetState = 8,
	kSetState = 9,
	kReset = 10,
	kListeners = 11,
	kOpenEditor = 12,
	kCloseEditor = 13,
	kMessage = 14,
	kEditorWindow = 15,
};

struct Failure {
	std::string reason;
};

#ifdef _WIN32
const UINT kHeard = WM_APP + 1;

LRESULT CALLBACK heardProcedure(HWND window, UINT message, WPARAM wParam, LPARAM lParam);
#endif

class Host {
public:
	explicit Host(Wire wire) : wire(wire), application(owned(new Vst::HostApplication())) {}

	// answers each request as it comes, until the input ends. On Windows the process's thread waits for its windows'
	// messages and dispatches them (plugin editors paint and react), and is told when a request comes
	int run() {
#ifdef _WIN32
		HWND heard = listenInWindow();
		MSG message;
		while (GetMessageW(&message, nullptr, 0, 0) > 0) {
			TranslateMessage(&message);
			busy++;
			DispatchMessageW(&message);
			busy--;
			finishClosing();
		}
		DestroyWindow(heard);
#else
		while (serveOne()) {
		}
#endif
		instances.clear();
		return 0;
	}

#ifdef _WIN32
	// answers the requests that came; at the end of the input, the message loop ends. TuxGuitar sends a request once the
	// last one is answered: one that comes while the thread dispatches messages inside a request (a plugin's loop) finds
	// no input, so a reply being written is never mixed with another. Served inside something the thread dispatched or
	// waits in (a plugin's loop of its own, a wait on another thread's window), the requests are answered at once, but
	// what they close goes once that has returned (finishClosing)
	void serve() {
		busy++;
		bool open = true;
		while (open && wire.hasInput()) {
			open = serveOne();
			doEditorWindowWork();
		}
		busy--;
		if (!open) {
			PostQuitMessage(0);
		}
		finishClosing();
	}
#endif

private:
#ifdef _WIN32
	// a message-only window, told with a sent message each time the input's listening thread reads bytes: a thread
	// handles sent messages whenever it waits for messages, in its own loop, in a loop a plugin runs (a file dialog, a
	// window dragged), and while it waits on a window of another thread
	HWND listenInWindow() {
		WNDCLASSW type = {};
		type.lpfnWndProc = heardProcedure;
		type.hInstance = GetModuleHandleW(nullptr);
		type.lpszClassName = L"TuxGuitarVst3Requests";
		RegisterClassW(&type);
		HWND window = CreateWindowExW(0, type.lpszClassName, L"", 0, 0, 0, 0, 0, HWND_MESSAGE, nullptr, type.hInstance, nullptr);
		SetWindowLongPtrW(window, GWLP_USERDATA, (LONG_PTR) this);
		wire.listen([window] { SendNotifyMessageW(window, kHeard, 0, 0); });
		return window;
	}
#endif

	// whether code the thread dispatched or waits in may still be running under this request (a plugin's: its plugin or
	// view must not go now)
	bool underway() const { return busy > 1; }

	// the plugins and editors closed while code was underway go once nothing is
	void finishClosing() {
		if (busy > 0) {
			return;
		}
		std::set<int32_t> editors;
		std::vector<std::unique_ptr<Instance>> plugins;
		editors.swap(editorsClosing);
		plugins.swap(closing);
		for (int32_t id : editors) {
			auto found = instances.find(id);
			if (found != instances.end()) {
				found->second->closeEditor();
			}
		}
	}

	// reads a request and answers it; false at the end of the input
	bool serveOne() {
		try {
			int32_t operation;
			bool served = wire.readInt(operation);
			if (served) {
				answer(operation);
			}
			return served;
		} catch (const WireClosed&) {
			return false;
		}
	}

	void answer(int32_t operation) {
		try {
			wire.writeInt(0);
			dispatch(operation);
			writeNotifications();
		} catch (const Failure& failure) {
			discardAndFail(failure.reason);
		} catch (const std::exception& exception) {
			discardAndFail(exception.what());
		}
		wire.flush();
	}

	void discardAndFail(const std::string& reason) {
		wire.discard();
		wire.writeInt(1);
		wire.writeString(reason);
		writeNotifications();
	}

	void dispatch(int32_t operation) {
		switch (operation) {
			case kHello: return hello();
			case kScan: return scan();
			case kOpen: return open();
			case kClose: return close();
			case kProcess: return process();
			case kSetParameter: return setParameter();
			case kGetParameter: return getParameter();
			case kGetState: return getState();
			case kSetState: return setState();
			case kReset: return instance().reset();
			case kListeners: return wire.writeInt(networkListeners(wire.readLong()));
			case kOpenEditor: return openEditor();
			case kCloseEditor: return closeEditor();
			case kMessage: return message();
			case kEditorWindow: return editorWindow();
			default: throw Failure{"unknown operation " + std::to_string(operation)};
		}
	}

	void hello() {
		wire.writeInt(kProtocolVersion);
		wire.writeLong(tg_pid());
	}

	VST3::Hosting::Module::Ptr module(const std::string& path) {
		auto known = modules.find(path);
		if (known != modules.end()) {
			return known->second;
		}
		std::string error;
		VST3::Hosting::Module::Ptr loaded = VST3::Hosting::Module::create(path, error);
		if (!loaded) {
			throw Failure{"not a VST3 module: " + path + " (" + error + ")"};
		}
		modules[path] = loaded;
		return loaded;
	}

	// the audio processors of a module: class id, name, and whether it is an instrument
	void scan() {
		VST3::Hosting::Module::Ptr scanned = module(wire.readString());
		std::vector<VST3::Hosting::ClassInfo> processors;
		for (const VST3::Hosting::ClassInfo& info : scanned->getFactory().classInfos()) {
			if (info.category() == kVstAudioEffectClass) {
				processors.push_back(info);
			}
		}
		wire.writeInt((int32_t) processors.size());
		for (const VST3::Hosting::ClassInfo& info : processors) {
			wire.writeString(info.ID().toString());
			wire.writeString(info.name());
			wire.writeInt(info.subCategoriesString().find("Instrument") != std::string::npos ? 1 : 0);
		}
	}

	void open() {
		std::string path = wire.readString();
		std::string classId = wire.readString();
		double sampleRate = wire.readDouble();
		int32_t maxBlock = wire.readInt();
		std::string shared = wire.readString();
		auto uid = VST3::UID::fromString(classId);
		if (!uid) {
			throw Failure{"not a class id: " + classId};
		}
		std::string error;
		std::unique_ptr<Instance> opened = Instance::open(module(path), *uid, application, sampleRate, maxBlock, shared, error);
		if (!opened) {
			throw Failure{error};
		}
		int32_t id = next++;
		wire.writeInt(id);
		wire.writeInt(opened->inputs());
		wire.writeInt(opened->outputs());
		wire.writeInt(opened->hasEditor() ? 1 : 0);
		writeParameters(*opened);
		writeTiming(*opened);
		instances[id] = std::move(opened);
	}

	void writeParameters(Instance& opened) {
		std::vector<ParameterInfo> parameters = opened.parameters();
		wire.writeInt((int32_t) parameters.size());
		for (const ParameterInfo& parameter : parameters) {
			wire.writeInt(parameter.id);
			wire.writeString(parameter.title);
			wire.writeDouble(parameter.plain);
			wire.writeDouble(parameter.minimum);
			wire.writeDouble(parameter.maximum);
			wire.writeInt(parameter.steps);
			wire.writeInt(parameter.flags);
		}
	}

	void writeTiming(Instance& timed) {
		wire.writeInt(timed.latency());
		wire.writeInt(timed.tail());
	}

	Instance& instance() { return instance(wire.readInt()); }

	Instance& instance(int32_t id) {
		auto found = instances.find(id);
		if (found == instances.end()) {
			throw Failure{"no plugin " + std::to_string(id)};
		}
		return *found->second;
	}

	void close() {
		int32_t id = wire.readInt();
		collect(instance(id));
		if (underway()) {
			closing.push_back(std::move(instances[id]));
		}
		instances.erase(id);
	}

	void closeEditor() {
		int32_t id = wire.readInt();
		Instance& closed = instance(id);
		if (underway()) {
			editorsClosing.insert(id);
		} else {
			closed.closeEditor();
		}
	}

	void process() {
		int32_t id = wire.readInt();
		int32_t frames = wire.readInt();
		Transport transport = readTransport();
		std::vector<NoteEvent> events = readEvents();
		Instance& processed = instance(id);
		processed.process(frames, transport, events);
		writeTiming(processed);
	}

	Transport readTransport() {
		Transport transport;
		transport.playing = (wire.readInt() != 0);
		transport.tempo = wire.readDouble();
		transport.numerator = wire.readInt();
		transport.denominator = wire.readInt();
		transport.quarters = wire.readDouble();
		transport.samples = wire.readLong();
		return transport;
	}

	std::vector<NoteEvent> readEvents() {
		std::vector<NoteEvent> events((size_t) std::max(0, wire.readInt()));
		for (NoteEvent& event : events) {
			event.type = wire.readInt();
			event.offset = wire.readInt();
			event.pitch = wire.readInt();
			event.noteId = wire.readInt();
			event.value = wire.readFloat();
		}
		return events;
	}

	void setParameter() {
		Instance& changed = instance();
		int32_t id = wire.readInt();
		wire.writeDouble(changed.setParameter(id, wire.readDouble()));
		writeTiming(changed);
	}

	void getParameter() {
		Instance& read = instance();
		wire.writeDouble(read.parameter(wire.readInt()));
	}

	// the component's state, the controller's, then the parts kept apart (count, then name and bytes)
	void getState() {
		SavedState saved = instance().state();
		wire.writeBytes(saved.component.data(), saved.component.size());
		wire.writeBytes(saved.controller.data(), saved.controller.size());
		wire.writeInt((int32_t) saved.resources.size());
		for (const auto& resource : saved.resources) {
			wire.writeString(resource.first);
			wire.writeBytes(resource.second.data(), resource.second.size());
		}
	}

	void setState() {
		Instance& restored = instance();
		SavedState saved;
		saved.component = wire.readBytes();
		saved.controller = wire.readBytes();
		saved.resources.resize((size_t) std::max(0, wire.readInt()));
		for (auto& resource : saved.resources) {
			resource.first = wire.readString();
			resource.second = wire.readBytes();
		}
		restored.setState(saved);
		writeTiming(restored);
	}

	// the editor: none of its own (0), opened in a window of its own (1), or in a window the owner window owns (2)
	void openEditor() {
		int32_t id = wire.readInt();
		Instance& edited = instance(id);
		int64_t owner = wire.readLong();
		std::string title = wire.readString();
		editorsClosing.erase(id);
		bool opened = edited.openEditor(owner, title);
		bool owned = (opened && owner != 0 && edited.editorWindow().owner == owner);
		wire.writeInt(opened ? (owned ? 2 : 1) : 0);
	}

	void editorWindow() {
		EditorWindow facts = instance().editorWindow();
		wire.writeLong(facts.window);
		wire.writeLong(facts.owner);
		wire.writeInt(facts.shown ? 1 : 0);
		wire.writeInt(facts.front ? 1 : 0);
		for (int32_t value : {facts.left, facts.top, facts.right, facts.bottom, facts.width, facts.height}) {
			wire.writeInt(value);
		}
		wire.writeString(facts.title);
		wire.writeInt(facts.dpi);
		wire.writeInt(facts.perMonitorDpiAware ? 1 : 0);
		wire.writeInt(facts.resizable ? 1 : 0);
	}

	// a message to the plugin's controller, with integer and binary attributes; the reply holds its report
	void message() {
		Instance& told = instance();
		std::string id = wire.readString();
		std::vector<std::pair<std::string, int64>> integers((size_t) std::max(0, wire.readInt()));
		for (auto& entry : integers) {
			entry.first = wire.readString();
			entry.second = wire.readLong();
		}
		std::vector<std::pair<std::string, std::vector<char>>> binaries((size_t) std::max(0, wire.readInt()));
		for (auto& entry : binaries) {
			entry.first = wire.readString();
			entry.second = wire.readBytes();
		}
		std::vector<char> report = told.message(id, integers, binaries);
		wire.writeBytes(report.data(), report.size());
		writeTiming(told);
	}

	void collect(Instance& told) {
		for (const Notification& notification : told.takeNotifications()) {
			pending.push_back(std::make_pair(idOf(told), notification));
		}
	}

	int32_t idOf(const Instance& told) const {
		for (const auto& entry : instances) {
			if (entry.second.get() == &told) {
				return entry.first;
			}
		}
		return 0;
	}

	void writeNotifications() {
		for (auto& entry : instances) {
			collect(*entry.second);
		}
		wire.writeInt((int32_t) pending.size());
		for (const auto& entry : pending) {
			wire.writeInt(entry.first);
			wire.writeInt(entry.second.kind);
			wire.writeInt(entry.second.id);
			wire.writeDouble(entry.second.plain);
		}
		pending.clear();
	}

	Wire wire;
	IPtr<Vst::HostApplication> application;
	std::map<std::string, VST3::Hosting::Module::Ptr> modules;
	std::map<int32_t, std::unique_ptr<Instance>> instances;
	std::vector<std::pair<int32_t, Notification>> pending;
	int32_t next = 1;
	// how deep the thread is in what it dispatches and serves (0: waiting in its own loop)
	int busy = 0;
	// closed while code was underway: the plugins, and the instances whose editors close (finishClosing)
	std::vector<std::unique_ptr<Instance>> closing;
	std::set<int32_t> editorsClosing;
};

#ifdef _WIN32
LRESULT CALLBACK heardProcedure(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	if (message == kHeard) {
		reinterpret_cast<Host*>(GetWindowLongPtrW(window, GWLP_USERDATA))->serve();
		return 0;
	}
	return DefWindowProcW(window, message, wParam, lParam);
}
#endif

}  // namespace TuxGuitar

int main() {
#ifdef _WIN32
	// before any window: each window is drawn at its own screen's DPI (plugin views are told its scale), not scaled up
	SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2);
#endif
	TuxGuitar::Host host(TuxGuitar::Wire::claimStandardStreams());
	return host.run();
}
