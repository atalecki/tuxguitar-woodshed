// Test Sine: a sine at each note's pitch, peaking at 0.5 x velocity / 127, following per-note tuning (note expression)
#include "sine.h"
#include "public.sdk/source/main/pluginfactory.h"

namespace TuxGuitar {

class TestSine : public SineInstrument {
public:
	TestSine() : SineInstrument(PluginSpec{true, 0, 2, true, {}, {{u"Default", {}}}}) {}
};

}  // namespace TuxGuitar

using namespace TuxGuitar;
TG_TEST_PLUGIN_FACTORY("Test Sine", TestSine, true, 0x7A1C0008, 0x54475349, 0x4E450000, 0x00000008)
