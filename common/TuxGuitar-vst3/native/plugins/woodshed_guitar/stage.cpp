#include "stage.h"

#include <algorithm>

namespace Woodshed {

Stage::Stage() : playing(new Profile()) {}

Stage::~Stage() {
	delete playing;
	delete fading;
	delete pending.exchange(nullptr);
	delete released.exchange(nullptr);
}

// a crossfade under way ends now: nothing processes meanwhile
void Stage::prepare(double hostRate, int maxBlock) {
	delete fading;
	fading = nullptr;
	fadeLength = std::max(1, (int) (kFade * hostRate));
	old.assign((size_t) maxBlock, 0.0f);
	for (Profile* held : {playing, pending.load()}) {
		if (held && held->amp) {
			held->amp->prepare(hostRate, maxBlock);
		}
	}
}

void Stage::handOver(std::unique_ptr<Amp> amp) {
	reported.store(amp ? amp->latency() : 0);
	Profile* next = new Profile();
	next->amp = std::move(amp);
	delete pending.exchange(next);
	collect();
}

void Stage::collect() {
	delete released.exchange(nullptr);
}

int Stage::latency() const {
	return reported.load();
}

void Stage::process(float* signal, int frames, int quality, bool sounding) {
	if (!fading) {
		take(sounding);
	}
	if (fading) {
		fade(signal, frames, quality);
	} else {
		run(*playing, signal, frames, quality);
	}
}

// the profile handed over, once the last one let go of has been freed: crossfaded in while the plugin sounds
void Stage::take(bool sounding) {
	if (released.load() != nullptr) {
		return;
	}
	Profile* next = pending.exchange(nullptr);
	if (!next) {
		return;
	}
	if (sounding) {
		fading = playing;
		faded = 0;
	} else {
		released.store(playing);
	}
	playing = next;
}

// each sample the profile playing weighs one sample's worth more and the one before one less, until it alone plays
void Stage::fade(float* signal, int frames, int quality) {
	std::copy(signal, signal + frames, old.begin());
	run(*fading, old.data(), frames, quality);
	run(*playing, signal, frames, quality);
	for (int i = 0; i < frames; i++) {
		float weight = std::min(1.0f, (float) (faded + i + 1) / (float) fadeLength);
		signal[i] = old[i] + weight * (signal[i] - old[i]);
	}
	faded += frames;
	if (faded >= fadeLength) {
		released.store(fading);
		fading = nullptr;
	}
}

// a slimmable capture is told the Quality when it changes
void Stage::run(Profile& profile, float* signal, int frames, int quality) {
	if (!profile.amp) {
		return;
	}
	if (quality != profile.slimmed) {
		profile.amp->slim(quality == 1);
		profile.slimmed = quality;
	}
	profile.amp->process(signal, frames);
}

}  // namespace Woodshed
