// woodshed-guitar-time: what playing a capture costs, as Woodshed Guitar plays it (a six-string chord plucked each
// second, the amp, resampling when the capture's rate is not the host's), per instance, on one core.
//
//   woodshed-guitar-time <capture.nam> <host rate> <block size> <seconds of audio> [light]
//
// Prints the capture, its rate, the latency, the time taken as a share of the audio's own length (100% is one core
// kept busy), and how loud it played: the peak and the RMS over every sample (to compare the levels of two captures
// from the same strings).
#include "capture.h"
#include "voice.h"

#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

using namespace Woodshed;

namespace {

// how loud a signal is, block after block: its peak and the sum of its squares over every sample
struct Level {
	double peak = 0.0;
	double squares = 0.0;
	long long samples = 0;

	void add(const float* signal, int frames) {
		for (int i = 0; i < frames; i++) {
			peak = std::max(peak, (double) std::abs(signal[i]));
			squares += (double) signal[i] * signal[i];
		}
		samples += frames;
	}

	double rms() const {
		return (samples > 0 ? std::sqrt(squares / (double) samples) : 0.0);
	}
};

}  // namespace

int main(int argc, char** argv) {
	if (argc < 5) {
		std::fprintf(stderr, "usage: woodshed-guitar-time <capture.nam> <host rate> <block size> <seconds> [light]\n");
		return 2;
	}
	std::string contents;
	Capture capture = loadCapture(argv[1], contents);
	if (!capture.model) {
		std::fprintf(stderr, "%s\n", capture.refusal.c_str());
		return 1;
	}
	double hostRate = std::atof(argv[2]);
	int block = std::atoi(argv[3]);
	double seconds = std::atof(argv[4]);
	bool light = (argc > 5 && std::strcmp(argv[5], "light") == 0);

	Amp amp(capture.model, capture.rate);
	amp.prepare(hostRate, block);
	amp.slim(light);
	Strings strings(32, hostRate, pluckedString);
	std::vector<float> signal((size_t) block);
	const int chord[6] = {40, 45, 50, 55, 59, 64};
	long long blocks = (long long) (seconds * hostRate / block);
	long long perSecond = (long long) (hostRate / block);
	int id = 0;
	Level level;
	auto start = std::chrono::steady_clock::now();
	for (long long i = 0; i < blocks; i++) {
		if (i % perSecond == 0) {
			for (int pitch : chord) {
				strings.noteOn(id++, pitch, 0.8f);
			}
		}
		strings.render(signal.data(), block);
		amp.process(signal.data(), block);
		level.add(signal.data(), block);
	}
	double taken = std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count();
	double played = blocks * (double) block / hostRate;
	std::printf("%s | made at %.0f Hz | host %.0f Hz, blocks of %d | %s | latency %d | %.3f s for %.1f s of audio: %.2f%% of one core (peak %.3f, RMS %.4f)\n",
	            argv[1], capture.rate, hostRate, block, (light ? "light" : "full"), amp.latency(), taken, played, 100.0 * taken / played, level.peak, level.rms());
	return 0;
}
