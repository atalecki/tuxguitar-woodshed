// Woodshed Guitar's voice: each note plucks a string of its own, and the strings ring together, summed to one signal
// (a clean guitar, as a DI out gives it). A note is told by its id; its pitch can be retuned while it sounds (a bend),
// and it is damped when it ends.
#pragma once

#include <cstdint>
#include <memory>
#include <vector>

namespace Woodshed {

// one note's sound, from its pluck until it has died away: the seam a voice of recorded DI samples can replace the
// plucked string at. Made before playing (it holds what it needs for its lowest note), so nothing is allocated on the
// audio thread.
class Voice {
public:
	virtual ~Voice() = default;
	// a note of the MIDI pitch, its loudest sample at level
	virtual void pluck(int pitch, float level) = 0;
	// the note's tuning, in semitones from its pitch
	virtual void tune(double semitones) = 0;
	// the note ended: damped from now on
	virtual void release() = 0;
	// adds its next frames to out
	virtual void add(float* out, int frames) = 0;
	// silent at once (playback stopped or moved)
	virtual void stop() = 0;
	// silent from now on, until plucked again
	virtual bool finished() const = 0;
};

// Karplus-Strong: a delay line a period long, fed back through a two-point average, so a burst the length of the
// period (a pluck's shape) rings at the pitch and loses its highs as it dies away, in about 3 seconds held, 80 ms damped. The burst is
// the same for every pluck and peaks at the level, which no later sample exceeds.
class PluckedString : public Voice {
public:
	explicit PluckedString(double sampleRate);

	void pluck(int pitch, float level) override;
	void tune(double semitones) override;
	void release() override;
	void add(float* out, int frames) override;
	void stop() override;
	bool finished() const override;

private:
	double frequency() const;
	void retune();
	float next();

	double sampleRate;
	std::vector<float> line;
	int written = 0;
	int pitch = 0;
	double semitones = 0.0;
	bool released = false;
	// the delay read back (in samples, the period less the average's half sample) and the gain per pass of the loop
	double delay = 0.0;
	double loss = 0.0;
	float previous = 0.0f;
	// the burst still to play, and how long the string has been nearly silent
	std::vector<float> burst;
	int bursting = 0;
	int quiet = 0;
};

// the voices of the notes sounding, one per note id; a note on with no voice free takes the one plucked longest ago
class Strings {
public:
	// count voices made by the maker, for the sample rate
	Strings(int count, double sampleRate, std::unique_ptr<Voice> (*make)(double sampleRate));

	void noteOn(int32_t id, int pitch, float velocity);
	void noteOff(int32_t id);
	void tune(int32_t id, double semitones);
	// every voice silent at once (playback stopped or moved)
	void silence();
	// writes the next frames of the sum to out
	void render(float* out, int frames);

private:
	struct Slot {
		std::unique_ptr<Voice> voice;
		int32_t id = -1;
		bool held = false;
		uint64_t plucked = 0;
	};
	Slot* sounding(int32_t id);
	Slot& free();

	std::vector<Slot> slots;
	uint64_t plucks = 0;
};

// the plucked string the plugin plays
std::unique_ptr<Voice> pluckedString(double sampleRate);

}  // namespace Woodshed
