// Woodshed Guitar's editor: Load profile... (a file dialog for .nam files), the profile's name, why the last file was
// refused, what it says of the profile's file (with Update from file when the file has changed), and a control for each parameter (sliders for Input gain and Output level, a list for Quality), each move a
// gesture: TuxGuitar shows its own parameter window only for plugins without an editor. A plain child window of whatever window its host attaches it to (its own window, or a docked panel's).
// Windows only; elsewhere the plugin has no editor and its host shows its parameters.
#pragma once

#include "layout.h"

#include "public.sdk/source/common/pluginview.h"
#include "pluginterfaces/gui/iplugviewcontentscalesupport.h"
#include "pluginterfaces/vst/vsttypes.h"

#include <string>
#include <vector>

namespace Woodshed {

class GuitarPlugin;

// Drawn at the scale its host tells it (IPlugViewContentScaleSupport; a host that tells none: its window's DPI), in the
// system's message font at that scale, its controls where layout.h puts them for the texts measured in that font. The
// user can make it wider (its sliders and text lines stretch) but not narrower than its labels, sliders and values need;
// its height is fixed.
class GuitarView : public Steinberg::CPluginView, public Steinberg::IPlugViewContentScaleSupport {
public:
	explicit GuitarView(GuitarPlugin* plugin);
	~GuitarView() override;

	Steinberg::tresult PLUGIN_API isPlatformTypeSupported(Steinberg::FIDString type) SMTG_OVERRIDE;
	Steinberg::tresult PLUGIN_API attached(void* parent, Steinberg::FIDString type) SMTG_OVERRIDE;
	Steinberg::tresult PLUGIN_API removed() SMTG_OVERRIDE;
	// the screen's scale (1 at 96 DPI): the font, the texts' measures and the layout made again, the host asked for the
	// size at that scale
	Steinberg::tresult PLUGIN_API setContentScaleFactor(ScaleFactor factor) SMTG_OVERRIDE;
	Steinberg::tresult PLUGIN_API canResize() SMTG_OVERRIDE;
	// at least minimumWidth (layout.h) wide, exactly its height high
	Steinberg::tresult PLUGIN_API checkSizeConstraint(Steinberg::ViewRect* rect) SMTG_OVERRIDE;
	// laid out again in the width given (a host that asks no constraint: never narrower than the minimum)
	Steinberg::tresult PLUGIN_API onSize(Steinberg::ViewRect* newSize) SMTG_OVERRIDE;

	// shows the plugin's profile and refusal as they are now
	void refresh();
	// its texts said again (the language changed) and laid out for them
	void relabel();
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
	// how it is laid out: "scale=<told> dpi=<its window's> size=<w>x<h> text=<the em height of its labels' font>
	// sliders=<w>,<w> labels-fit=<whether each label holds its text>", the size the inside of the window it is in (as
	// the user sees it)
	std::string laidOut() const;
	// its profile's line as last drawn, the text of its tooltip (empty when the line is shown whole), and the line as its
	// own font cuts it in its box now, a line break between
	std::string profileLine() const;
	// the profile's line drawn in its box (a DRAWITEMSTRUCT): as much as it holds, ending in "..." when cut, the whole
	// line then its tooltip
	void drawProfile(const void* item);
	// as Windows tells the window it is in that it went to a screen of that DPI from the one it was told last; as the
	// user drags that window's border
	// until its inside is that size
	void changeDpi(Steinberg::int64 dpi);
	void dragTo(Steinberg::int64 width, Steinberg::int64 height);

	OBJ_METHODS(GuitarView, CPluginView)
	DEFINE_INTERFACES
		DEF_INTERFACE(IPlugViewContentScaleSupport)
	END_DEFINE_INTERFACES(CPluginView)
	REFCOUNT_METHODS(CPluginView)

private:
	struct Slider {
		Steinberg::Vst::ParamID id;
		void* bar;
		void* value;
	};
	static const Steinberg::Vst::ParamID kNoEdit = 0;

	// name: the key of its text (text.h)
	void addSlider(int row, Steinberg::Vst::ParamID id, const char* name, double minimum, double maximum);
	// each control where the layout (layout.h) puts it
	void place(const Layout& layout);
	// the font, the measures and the size made for the scale; the controls given them and placed when there are any
	void rescale();
	// the font given back
	void dropFont();
	// whether each label is as wide as its text
	bool labelsHold() const;
	void showParameters();

	GuitarPlugin* plugin;
	void* window = nullptr;
	void* load = nullptr;
	void* profile = nullptr;
	// the tooltip holding the profile's whole line while it is cut, and the line as last drawn
	void* tip = nullptr;
	std::wstring profileShown;
	void* refusal = nullptr;
	void* notice = nullptr;
	// Update from file, shown with a notice
	void* update = nullptr;
	void* quality = nullptr;
	// Input gain, Output level, Quality
	void* labels[3] = {nullptr, nullptr, nullptr};
	std::vector<Slider> sliders;
	// the scale told (0 until then), the font made for it, its texts' measures in it
	double scale = 0;
	void* font = nullptr;
	Measures measured = {};
	// the parameter a slider's gesture is editing, kNoEdit between gestures
	Steinberg::Vst::ParamID editing = kNoEdit;
};

// whether this system has the editor
bool hasEditor();

}  // namespace Woodshed
