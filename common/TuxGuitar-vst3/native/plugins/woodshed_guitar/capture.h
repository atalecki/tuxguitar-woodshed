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

// the capture a .nam file holds (named in the refusal); a capture made at no known rate was made at 48000 Hz, as NAM
// captures are
Capture readCapture(const std::string& name, const std::string& contents);

// the capture at the path: refused when it is no .nam file or cannot be read, else as its contents say
Capture loadCapture(const std::string& path, std::string& contents);

// the voice played through a capture's model at the host's rate: in place, a block at a time; at the model's own rate
// (resampled into it and back) when the host's differs
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
	std::unique_ptr<Resampler> resampler;
	std::vector<double> in;
	std::vector<double> out;
};

}  // namespace Woodshed
