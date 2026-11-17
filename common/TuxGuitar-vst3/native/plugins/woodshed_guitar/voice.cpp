#include "voice.h"

#include <algorithm>
#include <climits>
#include <cmath>

namespace Woodshed {

namespace {

// the lowest a string is tuned (a bend down included): the delay line holds its period
const double kLowest = 20.0;
// seconds to die away by 60 dB: held, and damped once the note ends
const double kRing = 3.0;
const double kDamped = 0.08;
// below this a string is silent
const float kSilent = 1e-6f;

// the burst a pluck starts with, frames long: the shape of a string pulled aside a fifth of the way along (a triangle,
// so the fundamental is the strongest partial, as a guitar's is), roughened by a little noise (the same every time),
// without its mean, its loudest sample at 1
void makeBurst(std::vector<float>& burst, int frames) {
	burst.resize((size_t) frames);
	int apex = std::max(1, frames / 5);
	uint32_t noise = 0x2545F491u;
	double sum = 0.0;
	for (int i = 0; i < frames; i++) {
		noise ^= noise << 13;
		noise ^= noise >> 17;
		noise ^= noise << 5;
		float white = (float) ((noise >> 8) / 8388608.0 - 1.0);
		float shape = (i < apex ? (float) i / apex : (float) (frames - i) / (frames - apex));
		burst[i] = shape + 0.1f * white;
		sum += burst[i];
	}
	float mean = (float) (sum / frames);
	float loudest = 0.0f;
	for (float& sample : burst) {
		sample -= mean;
		loudest = std::max(loudest, std::fabs(sample));
	}
	for (float& sample : burst) {
		sample /= loudest;
	}
}

}  // namespace

PluckedString::PluckedString(double sampleRate) : sampleRate(sampleRate), line((size_t) std::ceil(sampleRate / kLowest) + 4, 0.0f), quiet(INT_MAX / 2) {
	burst.reserve(line.size());
}

double PluckedString::frequency() const {
	double tuned = 440.0 * std::pow(2.0, (pitch + semitones - 69.0) / 12.0);
	return std::clamp(tuned, kLowest, sampleRate / 4.0);
}

// the loop's delay is a period, the average's half sample included; its gain dies away in the time held or damped
void PluckedString::retune() {
	double hertz = frequency();
	delay = sampleRate / hertz - 0.5;
	loss = std::pow(10.0, -3.0 / (hertz * (released ? kDamped : kRing)));
}

void PluckedString::pluck(int pitch, float level) {
	this->pitch = pitch;
	semitones = 0.0;
	released = false;
	std::fill(line.begin(), line.end(), 0.0f);
	written = 0;
	previous = 0.0f;
	quiet = 0;
	retune();
	// the burst ends before the loop brings back its first sample, so no sample is louder than the burst
	makeBurst(burst, std::max(1, (int) delay - 1));
	for (float& sample : burst) {
		sample *= level;
	}
	bursting = 0;
}

void PluckedString::tune(double semitones) {
	this->semitones = semitones;
	retune();
}

void PluckedString::release() {
	released = true;
	retune();
}

// the line read back delay samples ago, between its two nearest samples
float PluckedString::next() {
	int size = (int) line.size();
	double at = written - delay;
	double whole = std::floor(at);
	float part = (float) (at - whole);
	int index = (((int) whole % size) + size) % size;
	float delayed = line[index] + part * (line[(index + 1) % size] - line[index]);
	float sample = (float) (loss * 0.5 * (delayed + previous));
	previous = delayed;
	if (bursting < (int) burst.size()) {
		sample += burst[bursting++];
	}
	line[written % size] = sample;
	written = (written + 1) % (size * 1024);
	quiet = (std::fabs(sample) < kSilent ? quiet + 1 : 0);
	return sample;
}

void PluckedString::add(float* out, int frames) {
	for (int i = 0; i < frames && !finished(); i++) {
		out[i] += next();
	}
}

bool PluckedString::finished() const {
	return quiet > (int) delay + 2;
}

void PluckedString::stop() {
	quiet = INT_MAX / 2;
}

std::unique_ptr<Voice> pluckedString(double sampleRate) {
	return std::make_unique<PluckedString>(sampleRate);
}

Strings::Strings(int count, double sampleRate, std::unique_ptr<Voice> (*make)(double sampleRate)) : slots((size_t) count) {
	for (Slot& slot : slots) {
		slot.voice = make(sampleRate);
	}
}

// the held note of the id plucked last, or none
Strings::Slot* Strings::sounding(int32_t id) {
	Slot* found = nullptr;
	for (Slot& slot : slots) {
		if (slot.held && slot.id == id && (found == nullptr || slot.plucked > found->plucked)) {
			found = &slot;
		}
	}
	return found;
}

// a silent voice, else the one plucked longest ago
Strings::Slot& Strings::free() {
	Slot* oldest = &slots.front();
	for (Slot& slot : slots) {
		if (slot.voice->finished()) {
			return slot;
		}
		oldest = (slot.plucked < oldest->plucked ? &slot : oldest);
	}
	return *oldest;
}

void Strings::noteOn(int32_t id, int pitch, float velocity) {
	Slot& slot = free();
	slot.id = id;
	slot.held = true;
	slot.plucked = ++plucks;
	slot.voice->pluck(pitch, 0.5f * velocity);
}

void Strings::noteOff(int32_t id) {
	Slot* slot = sounding(id);
	if (slot) {
		slot->held = false;
		slot->voice->release();
	}
}

void Strings::tune(int32_t id, double semitones) {
	Slot* slot = sounding(id);
	if (slot) {
		slot->voice->tune(semitones);
	}
}

void Strings::silence() {
	for (Slot& slot : slots) {
		slot.held = false;
		slot.voice->stop();
	}
}

void Strings::render(float* out, int frames) {
	std::fill(out, out + frames, 0.0f);
	for (Slot& slot : slots) {
		slot.voice->add(out, frames);
	}
}

}  // namespace Woodshed
