// woodshed-guitar-level-test: checks how loud Woodshed Guitar plays a capture (capture.h): one that stores its loudness
// evened out to NAM's target, one that does not at its own level, at the host's rate and resampled. The captures are
// Linear ones made up here, multiplying the signal by 3. Prints each check that fails; exit 0 when none does
// (TuxGuitar-vst3's TestWoodshedChecks runs it).
#include "capture.h"

#include <cmath>
#include <cstdio>
#include <string>
#include <vector>

namespace {

using namespace Woodshed;

int failures = 0;

// a Linear capture multiplying the signal by 3, made at 48 kHz, with the metadata given ("" for none)
std::string gain3(const std::string& metadata) {
	return "{\"version\": \"0.7.0\", \"architecture\": \"Linear\", \"config\": {\"receptive_field\": 1, \"bias\": false, "
	       "\"in_channels\": 1, \"out_channels\": 1}, \"weights\": [3.0], \"sample_rate\": 48000"
	       + (metadata.empty() ? std::string() : ", \"metadata\": " + metadata) + "}";
}

// what the amp plays of a steady 0.1 at the host's rate, once the resampling (if any) has settled: its last block
// (none when the capture is refused)
std::vector<float> played(const std::string& contents, double hostRate) {
	Capture capture = readCapture("test.nam", contents);
	if (!capture.model) {
		return {};
	}
	Amp amp(capture.model, capture.rate);
	amp.prepare(hostRate, 256);
	std::vector<float> signal(256);
	for (int block = 0; block < 40; block++) {
		signal.assign(signal.size(), 0.1f);
		amp.process(signal.data(), (int) signal.size());
	}
	return signal;
}

// the first sample of the block that is not the one expected, within 0.1%; the block's size when every one is
size_t firstOff(double expected, const std::vector<float>& block) {
	size_t i = 0;
	while (i < block.size() && std::fabs(block[i] - expected) <= 0.001 * std::fabs(expected)) {
		i++;
	}
	return i;
}

// a block was played and every sample of it is the one expected
void check(const std::string& what, double expected, const std::vector<float>& block) {
	size_t off = firstOff(expected, block);
	if (block.empty() || off < block.size()) {
		std::printf("%s: expected %.6f, got %.6f at sample %zu of %zu\n", what.c_str(), expected,
		            (off < block.size() ? block[off] : 0.0), off, block.size());
		failures++;
	}
}

}  // namespace

int main() {
	// -18 dB, the level NAM's plugin evens out to: 6 dB below it doubles the signal (10^(6/20)), 6 dB above halves it
	check("no loudness stored: its own level", 0.3, played(gain3(""), 48000.0));
	check("metadata without a loudness: its own level", 0.3, played(gain3("{\"name\": \"Gain 3\"}"), 48000.0));
	check("at the target already", 0.3, played(gain3("{\"loudness\": -18.0}"), 48000.0));
	check("6 dB quieter than the target", 0.3 * std::pow(10.0, 6.0 / 20.0), played(gain3("{\"loudness\": -24.0}"), 48000.0));
	check("6 dB louder than the target", 0.3 * std::pow(10.0, -6.0 / 20.0), played(gain3("{\"loudness\": -12.0}"), 48000.0));
	check("resampled, 6 dB quieter than the target", 0.3 * std::pow(10.0, 6.0 / 20.0), played(gain3("{\"loudness\": -24.0}"), 44100.0));
	return (failures == 0 ? 0 : 1);
}
