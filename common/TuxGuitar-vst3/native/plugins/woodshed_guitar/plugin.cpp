#include "plugin.h"

#include "editor.h"
#include "state.h"

#include "public.sdk/source/main/pluginfactory.h"
#include "pluginterfaces/base/ibstream.h"
#include "pluginterfaces/vst/ivstattributes.h"
#include "pluginterfaces/vst/ivstnoteexpression.h"
#include "pluginterfaces/vst/ivstparameterchanges.h"

#include <algorithm>
#include <cmath>
#include <cstring>

namespace Woodshed {

namespace {

// the voices: as many notes as ring together (six strings, let ring, chords in a row)
const int kVoices = 32;

// the note's id, its pitch when the host gives none
int32 noteId(int32 id, int16 pitch) {
	return (id >= 0 ? id : pitch);
}

float gain(double decibels) {
	return (float) std::pow(10.0, decibels / 20.0);
}

// the integer attribute of a state's stream by which TuxGuitar says it keeps parts of a state apart (1), and the name
// of the part Woodshed Guitar keeps apart there: its profile's contents
const char kApart[] = "TuxGuitar.apart";
const char kProfile[] = "profile";

// a binary attribute's bytes (empty when there is none)
std::string binary(IAttributeList* attributes, const char* key) {
	const void* data = nullptr;
	uint32 size = 0;
	bool found = (attributes && attributes->getBinary(key, data, size) == kResultOk && data);
	return (found ? std::string(static_cast<const char*>(data), size) : std::string());
}

// the attributes of a state's stream when its host keeps parts of a state apart, else none
IAttributeList* apartIn(IBStream* stream) {
	FUnknownPtr<IStreamAttributes> attributes(stream);
	IAttributeList* list = (attributes ? attributes->getAttributes() : nullptr);
	int64 apart = 0;
	return (list && list->getInt(kApart, apart) == kResultOk && apart == 1 ? list : nullptr);
}

// all of a stream's bytes
std::string readAll(IBStream* stream) {
	std::string bytes;
	char chunk[65536];
	int32 read = 0;
	while (stream->read(chunk, sizeof(chunk), &read) == kResultOk && read > 0) {
		bytes.append(chunk, (size_t) read);
	}
	return bytes;
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
	parameters.addParameter(new RangeParameter(u"Input gain", kInputGain, u"dB", -24.0, 24.0, 0.0, 0, ParameterInfo::kCanAutomate));
	parameters.addParameter(new RangeParameter(u"Output level", kOutputLevel, u"dB", -40.0, 12.0, 0.0, 0, ParameterInfo::kCanAutomate));
	StringListParameter* quality = new StringListParameter(u"Quality", kQuality, nullptr, ParameterInfo::kCanAutomate | ParameterInfo::kIsList);
	quality->appendString(u"Full");
	quality->appendString(u"Light");
	parameters.addParameter(quality);
	return kResultOk;
}

tresult PLUGIN_API GuitarPlugin::setupProcessing(ProcessSetup& setup) {
	tresult result = SingleComponentEffect::setupProcessing(setup);
	hostRate = setup.sampleRate;
	maxBlock = setup.maxSamplesPerBlock;
	strings = std::make_unique<Strings>(kVoices, setup.sampleRate, pluckedString);
	voice.assign((size_t) setup.maxSamplesPerBlock, 0.0f);
	std::lock_guard<std::mutex> lock(ampLock);
	if (amp) {
		amp->prepare(hostRate, maxBlock);
	}
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

uint32 PLUGIN_API GuitarPlugin::getLatencySamples() {
	std::lock_guard<std::mutex> lock(ampLock);
	return (amp ? (uint32) amp->latency() : 0);
}

double GuitarPlugin::parameter(ParamID id) {
	return getParameterObject(id)->toPlain(getParamNormalized(id));
}

// the last value of each change in the block, from its start
void GuitarPlugin::applyChanges(IParameterChanges* changes) {
	int32 count = (changes ? changes->getParameterCount() : 0);
	for (int32 i = 0; i < count; i++) {
		IParamValueQueue* queue = changes->getParameterData(i);
		int32 offset = 0;
		ParamValue value = 0;
		if (queue->getPointCount() > 0 && queue->getPoint(queue->getPointCount() - 1, offset, value) == kResultOk) {
			SingleComponentEffect::setParamNormalized(queue->getParameterId(), value);
		}
	}
}

// the strings' sum at the input gain, through the profile (dry while a load holds the lock), at the output level, on
// both sides
tresult PLUGIN_API GuitarPlugin::process(ProcessData& data) {
	applyChanges(data.inputParameterChanges);
	if (data.numSamples <= 0 || data.numOutputs == 0 || !strings) {
		return kResultOk;
	}
	playEvents(data.inputEvents, data.numSamples);
	scale(gain(parameter(kInputGain)), data.numSamples);
	std::unique_lock<std::mutex> lock(ampLock, std::try_to_lock);
	if (lock && amp) {
		slimTo(*amp);
		amp->process(voice.data(), data.numSamples);
	}
	scale(gain(parameter(kOutputLevel)), data.numSamples);
	for (int32 side = 0; side < 2; side++) {
		std::memcpy(data.outputs[0].channelBuffers32[side], voice.data(), data.numSamples * sizeof(float));
	}
	return kResultOk;
}

// sample for sample: each event applies from its offset
void GuitarPlugin::playEvents(IEventList* events, int32 frames) {
	int32 from = 0;
	int32 count = (events ? events->getEventCount() : 0);
	for (int32 i = 0; i < count; i++) {
		Event event;
		if (events->getEvent(i, event) == kResultOk) {
			int32 to = std::max(from, std::min(frames, event.sampleOffset));
			sound(from, to);
			play(event);
			from = to;
		}
	}
	sound(from, frames);
}

void GuitarPlugin::scale(float by, int32 frames) {
	for (int32 i = 0; i < frames; i++) {
		voice[i] *= by;
	}
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

// the amp at the Quality chosen, told only when it changes
void GuitarPlugin::slimTo(Amp& amp) {
	int quality = (int) parameter(kQuality);
	if (quality != slimmed) {
		amp.slim(quality == 1);
		slimmed = quality;
	}
}

// the capture's amp, made ready for the host's rate, takes the old one's place; or, with no capture, none does
void GuitarPlugin::install(const std::string& path, const std::string& contents, const Capture& capture) {
	std::shared_ptr<Amp> next;
	if (capture.model) {
		next = std::make_shared<Amp>(capture.model, capture.rate);
		if (hostRate > 0.0) {
			next->prepare(hostRate, maxBlock);
		}
	}
	{
		std::lock_guard<std::mutex> lock(ampLock);
		amp.swap(next);
		slimmed = -1;
	}
	this->path = path;
	this->contents = contents;
	checkLatency();
}

bool GuitarPlugin::loadProfile(const std::string& path) {
	std::string read;
	Capture capture = loadCapture(path, read);
	refusal = capture.refusal;
	if (!refusal.empty()) {
		refreshEditor();
		return false;
	}
	install(path, read, capture);
	checkFile();
	refreshEditor();
	FUnknownPtr<IComponentHandler2> handler(componentHandler);
	if (handler) {
		handler->setDirty(true);
	}
	return true;
}

void GuitarPlugin::edit(ParamID id, double plain, Phase phase) {
	ParamValue normalized = getParameterObject(id)->toNormalized(plain);
	if (phase == kBegun) {
		beginEdit(id);
	}
	SingleComponentEffect::setParamNormalized(id, normalized);
	performEdit(id, normalized);
	if (phase == kEnded) {
		endEdit(id);
	}
}

tresult PLUGIN_API GuitarPlugin::setParamNormalized(ParamID id, ParamValue value) {
	tresult result = SingleComponentEffect::setParamNormalized(id, value);
	refreshEditor();
	return result;
}

// an editor opening tells what became of the profile's file
void GuitarPlugin::editorOpened(GuitarView* view) {
	this->view = view;
	if (view) {
		checkFile();
	}
}

void GuitarPlugin::refreshEditor() {
	if (view) {
		view->refresh();
	}
}

IPlugView* PLUGIN_API GuitarPlugin::createView(FIDString name) {
	return (hasEditor() && std::strcmp(name, ViewType::kEditor) == 0 ? new GuitarView(this) : nullptr);
}

void GuitarPlugin::updateFromFile() {
	loadProfile(path);
}

ProfileStatus GuitarPlugin::status() {
	return ProfileStatus{(path.empty() ? std::string() : fileName(path)), refusal, notice};
}

// a change of latency is told to the host, which asks for the new one
void GuitarPlugin::checkLatency() {
	uint32 latency = getLatencySamples();
	if (latency != reportedLatency && componentHandler) {
		reportedLatency = latency;
		componentHandler->restartComponent(kLatencyChanged);
	}
}

// the copy the state holds, whatever became of the file since (a song sounds the same each time it opens)
void GuitarPlugin::restoreProfile(const std::string& path, const std::string& contents) {
	if (path.empty()) {
		install(path, contents, Capture());
		return;
	}
	Capture capture = readCapture(fileName(path), contents);
	refusal = capture.refusal;
	bool kept = (capture.model != nullptr);
	install((kept ? path : std::string()), (kept ? contents : std::string()), capture);
}

// the file at the profile's path, when it is there and no longer the profile's contents, is told
void GuitarPlugin::checkFile() {
	notice = (!path.empty() && differs(path, contents) ? "The profile file has changed since this song was saved" : "");
}

tresult PLUGIN_API GuitarPlugin::getState(IBStream* state) {
	Saved saved;
	saved.inputGain = parameter(kInputGain);
	saved.outputLevel = parameter(kOutputLevel);
	saved.quality = parameter(kQuality);
	saved.path = path;
	saved.contents = contents;
	IAttributeList* apart = apartIn(state);
	saved.apart = (apart && !contents.empty());
	if (saved.apart) {
		apart->setBinary(kProfile, contents.data(), (uint32) contents.size());
	}
	std::string bytes = writeState(saved);
	int32 written = 0;
	return state->write(bytes.data(), (int32) bytes.size(), &written);
}

tresult PLUGIN_API GuitarPlugin::setState(IBStream* state) {
	Saved saved;
	if (!readState(readAll(state), saved)) {
		return kResultFalse;
	}
	if (saved.apart) {
		saved.contents = binary(apartIn(state), kProfile);
	}
	SingleComponentEffect::setParamNormalized(kInputGain, getParameterObject(kInputGain)->toNormalized(saved.inputGain));
	SingleComponentEffect::setParamNormalized(kOutputLevel, getParameterObject(kOutputLevel)->toNormalized(saved.outputLevel));
	SingleComponentEffect::setParamNormalized(kQuality, getParameterObject(kQuality)->toNormalized(saved.quality));
	refusal.clear();
	restoreProfile(saved.path, saved.contents);
	checkFile();
	refreshEditor();
	return kResultOk;
}

// the binary attribute as a reply's "report"
static tresult report(IAttributeList* attributes, const std::string& text) {
	return attributes->setBinary("report", text.data(), (uint32) text.size());
}

// "Woodshed.load" loads the profile at the "path" (UTF-8 bytes), as the editor's Load profile... does;
// "Woodshed.profile" answers the status: the profile's name, the refusal and the notice, a line break between; else
// kNotImplemented
tresult GuitarPlugin::profileMessage(FIDString id, IAttributeList* attributes) {
	if (std::strcmp(id, "Woodshed.load") == 0) {
		loadProfile(binary(attributes, "path"));
		return kResultOk;
	}
	if (std::strcmp(id, "Woodshed.profile") == 0) {
		ProfileStatus now = status();
		return report(attributes, now.name + "\n" + now.refusal + "\n" + now.notice);
	}
	return kNotImplemented;
}

// while its editor is open: "Woodshed.shown" answers what its lines show (read from its window), a line break
// between; "Woodshed.controls" what its controls show; "Woodshed.move" moves the control of the parameter "id" to the
// "value" (a double), as the user does; "Woodshed.click" clicks the button that shows the "button" text (UTF-8), as the
// user does, answering that text (nothing when no button shows it); else kNotImplemented
tresult GuitarPlugin::editorMessage(FIDString id, IAttributeList* attributes) {
	if (!view) {
		return kNotImplemented;
	}
	if (std::strcmp(id, "Woodshed.shown") == 0) {
		return report(attributes, view->shown());
	}
	if (std::strcmp(id, "Woodshed.controls") == 0) {
		return report(attributes, view->controls());
	}
	if (std::strcmp(id, "Woodshed.move") == 0) {
		int64 param = 0;
		const void* data = nullptr;
		uint32 size = 0;
		double value = 0.0;
		attributes->getInt("id", param);
		if (attributes->getBinary("value", data, size) == kResultOk && size == sizeof(double)) {
			std::memcpy(&value, data, sizeof(double));
		}
		view->move((ParamID) param, value);
		return kResultOk;
	}
	if (std::strcmp(id, "Woodshed.click") == 0) {
		return report(attributes, view->click(binary(attributes, "button")));
	}
	return kNotImplemented;
}

tresult PLUGIN_API GuitarPlugin::notify(IMessage* message) {
	FIDString id = message->getMessageID();
	tresult result = profileMessage(id, message->getAttributes());
	result = (result == kNotImplemented ? editorMessage(id, message->getAttributes()) : result);
	return (result == kNotImplemented ? SingleComponentEffect::notify(message) : result);
}

tresult PLUGIN_API GuitarPlugin::queryInterface(const TUID _iid, void** obj) {
	DEF_INTERFACE(IConnectionPoint)
	return SingleComponentEffect::queryInterface(_iid, obj);
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
