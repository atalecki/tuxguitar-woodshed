// The profile Woodshed Guitar's voice plays through, as the audio thread plays it: the amp playing (none: the dry
// voice). A new one is handed over from another thread and taken at the start of a block: while the plugin sounds, the
// one it replaces fades out as it fades in, over kFade seconds (linear), else it plays at once. The audio thread
// neither allocates, frees nor waits: hand-overs pass through atomic pointers, and what it lets go of is freed on
// another thread.
#pragma once

#include "capture.h"

#include <atomic>
#include <memory>
#include <vector>

namespace Woodshed {

// how long a profile taking over while the plugin sounds crossfades with the one before, in seconds
const double kFade = 0.03;

class Stage {
public:
	Stage();
	~Stage();

	// before playing, not while processing: the host's rate and largest block, the amps held made ready for them
	void prepare(double hostRate, int maxBlock);
	// one thread at a time, never the audio thread: the amp to play from the next block (none: the dry voice), in place
	// of one handed over and not taken yet; its delay is the one reported from now on
	void handOver(std::unique_ptr<Amp> amp);
	// never the audio thread: what the audio thread let go of is freed
	void collect();
	// the delay of the amp handed over last, in samples at the host's rate
	int latency() const;
	// the audio thread: the signal in place through the profile, at the Quality (1: light); sounding: the plugin
	// sounded before the block, so a profile taking over now crossfades
	void process(float* signal, int frames, int quality, bool sounding);

private:
	struct Profile {
		std::unique_ptr<Amp> amp;
		// the Quality it was told (-1: none yet)
		int slimmed = -1;
	};

	void take(bool sounding);
	void fade(float* signal, int frames, int quality);
	static void run(Profile& profile, float* signal, int frames, int quality);

	std::atomic<Profile*> pending{nullptr};
	std::atomic<Profile*> released{nullptr};
	std::atomic<int> reported{0};
	// the audio thread's: the profile playing, the one fading out (none but in a crossfade), how far the crossfade is
	Profile* playing;
	Profile* fading = nullptr;
	int faded = 0;
	int fadeLength = 0;
	// the signal through the profile fading out, a block long
	std::vector<float> old;
};

}  // namespace Woodshed
