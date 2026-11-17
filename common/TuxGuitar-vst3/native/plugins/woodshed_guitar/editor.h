// Woodshed Guitar's editor: Load profile... (a file dialog for .nam files), the profile's name, and why the last file
// was refused. A plain child window of whatever window its host attaches it to (its own window, or a docked panel's).
// Windows only; elsewhere the plugin has no editor and its host shows its parameters.
#pragma once

#include "public.sdk/source/common/pluginview.h"

#include <string>

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
	// what its two lines show, read from its window, a line break between
	std::string shown() const;

private:
	GuitarPlugin* plugin;
	void* window = nullptr;
	void* profile = nullptr;
	void* refusal = nullptr;
};

// whether this system has the editor
bool hasEditor();

}  // namespace Woodshed
