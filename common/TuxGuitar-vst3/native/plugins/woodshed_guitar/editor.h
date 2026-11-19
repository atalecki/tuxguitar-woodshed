// Woodshed Guitar's editor: Load profile... (a file dialog for .nam files), the profile's name, why the last file was
// refused, what it says of the profile's file (with Update from file when the file has changed), and a control for each parameter (sliders for Input gain and Output level, a list for Quality), each move a
// gesture: TuxGuitar shows its own parameter window only for plugins without an editor. A plain child window of whatever window its host attaches it to (its own window, or a docked panel's).
// Windows only; elsewhere the plugin has no editor and its host shows its parameters.
#pragma once

#include "public.sdk/source/common/pluginview.h"
#include "pluginterfaces/vst/vsttypes.h"

#include <string>
#include <vector>

namespace Woodshed {

class GuitarPlugin;

class GuitarView : public Steinberg::CPluginView {
public:
	explicit GuitarView(GuitarPlugin* plugin);
	~GuitarView() override;

	Steinberg::tresult PLUGIN_API isPlatformTypeSupported(Steinberg::FIDString type) SMTG_OVERRIDE;
	Steinberg::tresult PLUGIN_API attached(void* parent, Steinberg::FIDString type) SMTG_OVERRIDE;
	Steinberg::tresult PLUGIN_API removed() SMTG_OVERRIDE;

	// shows the plugin's profile and refusal as they are now
	void refresh();
	// as the user does it: the file dialog, then the file chosen loaded
	void chooseProfile();
	// Update from file clicked: the file at the profile's path loaded
	void updateFromFile();
	// what its lines show, read from its window, a line break between: the profile, the refusal, the notice, and the
	// text of the button next to the notice (empty while it is hidden)
	std::string shown() const;
	// as the user clicks the shown button with that text; the text, or empty when no shown button has it
	std::string click(const std::string& text);
	// what its controls show: "Input gain 0.0 dB, Output level 0.0 dB, Quality Full"
	std::string controls() const;
	// as the user moves the parameter's control to the value (dB, or Quality's index)
	void move(Steinberg::Vst::ParamID id, double value);
	// the user moved the slider (its move ended or not), chose a Quality
	void slid(void* bar, bool ended);
	void chose();

private:
	struct Slider {
		Steinberg::Vst::ParamID id;
		void* bar;
		void* value;
	};
	static const Steinberg::Vst::ParamID kNoEdit = 0;

	// name: the key of its text (text.h)
	void addSlider(int row, Steinberg::Vst::ParamID id, const char* name, double minimum, double maximum);

	GuitarPlugin* plugin;
	void* window = nullptr;
	void* profile = nullptr;
	void* refusal = nullptr;
	void* notice = nullptr;
	// Update from file, shown with a notice
	void* update = nullptr;
	void* quality = nullptr;
	std::vector<Slider> sliders;
	// the parameter a slider's gesture is editing, kNoEdit between gestures
	Steinberg::Vst::ParamID editing = kNoEdit;
};

// whether this system has the editor
bool hasEditor();

}  // namespace Woodshed
