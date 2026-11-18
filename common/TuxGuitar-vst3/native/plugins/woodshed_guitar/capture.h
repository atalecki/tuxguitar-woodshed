// A NAM capture as Woodshed Guitar plays it: read from a .nam file's contents by NeuralAmpModelerCore, or refused with
// the reason the user reads; and the amp stage that plays the voice through it.
#pragma once

#include "resampler.h"

#include <memory>
#include <string>
#include <vector>

namespace nam {
class DSP;
}

namespace Woodshed {

// the model of a capture and the sample rate it was made at; or, when it cannot be played, why
struct Capture {
	std::shared_ptr<nam::DSP> model;
	double rate = 0.0;
	std::string refusal;
};

// the file name of a path (UTF-8), as the user reads it
std::string fileName(const std::string& path);

// the bytes of the file at the path (UTF-8); false when it cannot be read
bool readFile(const std::string& path, std::string& contents);

// whether the file at the path is there and holds other bytes than the contents
bool differs(const std::string& path, const std::string& contents);

// the capture a .nam file holds (named in the refusal); a capture made at no known rate was made at 48000 Hz, as NAM
// captures are
Capture readCapture(const std::string& name, const std::string& contents);

// the capture at the path: refused when it is no .nam file or cannot be read, else as its contents say
Capture loadCapture(const std::string& path, std::string& contents);

// the loudness every capture that stores its own is evened out to, in dB: NAM's own plugin's target
// (github.com/sdatkinson/NeuralAmpModelerPlugin at 7704e68e166d307fb660a46746934d3c8cd5d9ba,
// NeuralAmpModeler/NeuralAmpModeler.cpp, _SetOutputGain(), lines 707-708: in its Normalized output mode, the default,
// the output level's dB plus targetLoudness - the model's loudness, targetLoudness = -18.0)
const double kTargetLoudness = -18.0;

// the voice played through a capture's model at the host's rate: in place, a block at a time; at the model's own rate
// (resampled into it and back) when the host's differs; evened out to kTargetLoudness when the capture stores its
// loudness (as NAM's trainer measures it, the level it plays a standard input at), else at its own level
class Amp : private ModelBlock {
public:
	Amp(std::shared_ptr<nam::DSP> model, double modelRate);
	~Amp();

	// for the host's rate and its largest block (before playing, and when they change)
	void prepare(double hostRate, int maxBlock);
	// the delay it adds, in samples at the host's rate
	int latency() const;
	// a slimmable capture (an A2's container) plays its full model, or, light, its smallest
	void slim(bool light);
	void process(float* signal, int frames);

private:
	void run(const double* in, double* out, int frames) override;

	std::shared_ptr<nam::DSP> model;
	double modelRate;
	// what the model's output is multiplied by to even it out (1: its own level)
	double level;
	std::unique_ptr<Resampler> resampler;
	std::vector<double> in;
	std::vector<double> out;
};

}  // namespace Woodshed
