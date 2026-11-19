// Test Tone: Test Sine's tones at its parameter Level (1.0 by default), with no editor of its own (an instrument whose
// Edit opens TuxGuitar's Parameters window)
#include "sine.h"
#include "public.sdk/source/main/pluginfactory.h"

namespace TuxGuitar {

class TestTone : public SineInstrument {
public:
	TestTone() : SineInstrument(PluginSpec{true, 0, 2, false, {{1, u"Level", 0.0, 1.0, 1.0}}, {{u"Default", {1.0}}}}) {}

protected:
	float level() override { return (float) value(1); }
};

}  // namespace TuxGuitar

using namespace TuxGuitar;
TG_TEST_PLUGIN_FACTORY("Test Tone", TestTone, true, 0x7A1C000B, 0x5447544F, 0x4E450000, 0x0000000B)
