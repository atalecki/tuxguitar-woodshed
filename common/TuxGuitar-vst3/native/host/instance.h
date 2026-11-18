// One VST3 plugin loaded in the helper: its component, audio processor and edit controller, set up for 32-bit float
// blocks of up to the maximum block size agreed when it opens, exchanging its audio with TuxGuitar through a shared
// file mapping (SharedBlocks) and keeping what its controller tells the host (edits, restarts) for TuxGuitar.
#pragma once

#include "shared.h"

#include "public.sdk/source/vst/hosting/eventlist.h"
#include "public.sdk/source/vst/hosting/module.h"
#include "public.sdk/source/vst/hosting/parameterchanges.h"
#include "public.sdk/source/vst/hosting/processdata.h"
#include "pluginterfaces/gui/iplugview.h"
#include "pluginterfaces/vst/ivstaudioprocessor.h"
#include "pluginterfaces/vst/ivstcomponent.h"
#include "pluginterfaces/vst/ivsteditcontroller.h"
#include "pluginterfaces/vst/ivstprocesscontext.h"

#include <map>
#include <memory>
#include <set>
#include <string>
#include <vector>

namespace TuxGuitar {

using namespace Steinberg;

// what the controller told the host (dirty: its state changed outside any edit, a file loaded, say), or that the
// editor's window was hidden by its close box (the editor may have changed the plugin without saying so)
struct Notification {
	enum Kind { kBeginEdit = 1, kPerformEdit = 2, kEndEdit = 3, kRestart = 4, kDirty = 5, kEditorHidden = 6 };
	int32 kind;
	int32 id;     // the parameter, or the restart flags
	double plain; // the parameter's plain value
};

// a note event of a block, at a sample of it
struct NoteEvent {
	enum Type { kNoteOn = 1, kNoteOff = 2, kTuning = 3 };
	int32 type;
	int32 offset;
	int32 pitch;
	int32 noteId;
	float value;  // velocity 0..1, or the tuning in semitones
};

// what the song is doing at the block's first sample
struct Transport {
	bool playing;
	double tempo;
	int32 numerator;
	int32 denominator;
	double quarters;
	int64 samples;
	// rendered offline (an export): the plugin may take its time over the block
	bool offline;
};

// the DPI awareness of a window, as Windows has it (none: no window)
enum Awareness { kNoWindow = -1, kUnaware = 0, kSystemAware = 1, kPerMonitorAware = 2, kPerMonitorAwareV2 = 3 };

// the window of a plugin's own editor as the system shows it (for checks); all 0 (the awareness kNoWindow) when no
// editor is open
struct EditorWindow {
	int64 window;  // native handles
	int64 owner;
	int64 parent;  // the window it is in (inside a window of TuxGuitar), else 0
	bool embedded;
	bool shown;
	bool front;    // in front of the process's other shown windows
	int32 left, top, right, bottom;  // its frame on the screen
	int32 width, height;             // inside the frame
	std::string title;               // its caption, UTF-8
	int32 dpi;                       // of its screen
	bool resizable;                  // a border the user can drag
	int32 awareness = kNoWindow;      // its DPI awareness
	int32 ownerAwareness = kNoWindow; // of the window owning it, or that it is in
};

#ifdef _WIN32
// shows the editor windows opened since in front, once the reply is sent: showing an owned window waits on the thread
// of the window owning it, TuxGuitar's UI thread, which waits for that reply
void doEditorWindowWork();
#endif

// the integer attribute of a state's stream (IStreamAttributes) by which the host tells a plugin it keeps parts of a
// state apart (1): the plugin may then set those parts as binary attributes of the stream, each by a name of its own,
// and leave them out of its bytes; they come back the same way with the state
const char* const kApart = "TuxGuitar.apart";

// what a plugin saves: its component's state, its controller's (empty when the controller is the component), and the
// parts it kept apart, by name
struct SavedState {
	std::vector<char> component;
	std::vector<char> controller;
	std::vector<std::pair<std::string, std::vector<char>>> resources;
};

struct ParameterInfo {
	int32 id;
	std::string title;
	double plain;
	// the plain values at normalized 0 and 1, and the steps between (0: continuous)
	double minimum;
	double maximum;
	int32 steps;
	int32 flags;
};

class Instance {
public:
	static std::unique_ptr<Instance> open(VST3::Hosting::Module::Ptr module, const VST3::UID& classId, FUnknown* host, double sampleRate,
	                                      int32 maxBlock, const std::string& shared, std::string& error);
	~Instance();

	int32 inputs() const { return inputChannels; }
	int32 outputs() const { return outputChannels; }
	int32 latency() const;
	int32 tail() const;
	bool hasEditor();
	std::vector<ParameterInfo> parameters();

	void process(int32 frames, const Transport& transport, const std::vector<NoteEvent>& events);
	double setParameter(int32 id, double plain);
	double parameter(int32 id);
	SavedState state();
	void setState(const SavedState& saved);
	void reset();
	// the plugin's own editor shown in front, in a window the owner window owns (a native handle; 0, or a handle that is
	// no window: none), its caption the title (UTF-8); false when it has none for this platform
	bool openEditor(int64 owner, const std::string& title);
	void closeEditor();
	EditorWindow editorWindow();
	// tells the editor's view the scale of its screen (its DPI over 96), when the view takes one
	void scaleView(double factor);
	// what the view asks (IPlugFrame.resizeView): its window's inside takes that size; false when it has no window
	bool resizeView(int32 width, int32 height);
	// the window's inside took that size: the view is told
	void viewSized(int32 width, int32 height);
	// the size nearest to that one the view accepts, when it can be resized (else false, the size as it was)
	bool constrainView(int32& width, int32& height);
	// sends the controller a message with the integer and binary attributes; answers its binary attribute "report"
	std::vector<char> message(const std::string& id, const std::vector<std::pair<std::string, int64>>& integers,
	                          const std::vector<std::pair<std::string, std::vector<char>>>& binaries);
	std::vector<Notification> takeNotifications();

	// what the controller's component handler hears
	void notify(int32 kind, int32 id, double normalized);
	// work the plugin does in the background (IProgress), begun (its id) and finished: whether it restores the
	// plugin's state (a capture loaded on a thread of its own, say), and what it said it does (UTF-8, empty for nothing)
	Vst::IProgress::ID beginProgress(bool restoration, const std::string& what);
	void endProgress(Vst::IProgress::ID id);
	// whether a state restoration the plugin began is under way: it does not play as its state says yet
	bool restoring() const { return !restorations.empty(); }
	// what the first of the restorations under way said it does, empty for none (or nothing said)
	std::string restoringWhat() const { return (restorations.empty() ? std::string() : restorations.begin()->second); }

private:
	Instance(VST3::Hosting::Module::Ptr module, int32 maxBlock, const std::string& shared);
	bool load(const VST3::UID& classId, FUnknown* host, std::string& error);
	bool loadController(FUnknown* host);
	void setupBuses();
	bool start(double sampleRate, std::string& error);
	bool createEditor(int64 owner);
	void stop();
	void copyIn(int32 frames);
	void copyOut(int32 frames);
	void addEvents(const std::vector<NoteEvent>& events);
	void setContext(int32 frames, const Transport& transport);

	VST3::Hosting::Module::Ptr module;
	int32 maxBlock;
	double sampleRate = 44100.0;
	SharedBlocks shared;
	IPtr<Vst::IComponent> component;
	IPtr<Vst::IAudioProcessor> processor;
	IPtr<Vst::IEditController> controller;
	bool separateController = false;
	IPtr<Vst::IComponentHandler> handler;
	IPtr<IPlugView> view;
	// what the view asks of its window
	IPtr<IPlugFrame> frame;
	void* window = nullptr;
	int32 inputChannels = 0;
	int32 outputChannels = 0;
	Vst::HostProcessData data;
	Vst::ProcessContext context = {};
	Vst::EventList eventList;
	Vst::ParameterChanges changes;
	Vst::ParameterChanges outputChanges;
	std::vector<Notification> notifications;
	// the state restorations under way and what each said it does, by progress id (in the order begun), and the id the
	// next progress takes
	std::map<Vst::IProgress::ID, std::string> restorations;
	Vst::IProgress::ID nextProgress = 1;
	bool active = false;
};

}  // namespace TuxGuitar
