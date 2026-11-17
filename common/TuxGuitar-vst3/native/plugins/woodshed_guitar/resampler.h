// Plays a model made at one sample rate at another: the host's signal resampled into the model's rate, the model run
// on it, and its output resampled back, delayed by a whole number of host samples (the latency).
//
// Model sample m stands at host time m / ratio - kHalfWidth (ratio: model samples per host sample), so it is made from
// host samples already given; host output n is the model's output at host time n - latency. Both resamplings are
// windowed sincs (Kaiser), symmetric, so they add no delay of their own: the latency is exact, and the least that
// leaves every model sample a host output reads already made, kHalfWidth + (kHalfWidth + 1) / ratio, rounded up.
#pragma once

#include <cstdint>
#include <vector>

namespace Woodshed {

// what runs at the model's rate, a block at a time
class ModelBlock {
public:
	virtual ~ModelBlock() = default;
	virtual void run(const double* in, double* out, int frames) = 0;
};

class Resampler {
public:
	// for blocks of up to maxBlock host samples
	Resampler(double hostRate, double modelRate, int maxBlock);

	int latency() const { return delay; }
	// the largest block the model is given
	int maxModelBlock() const { return modelBlock; }
	// in to out (which may be the same), frames host samples, the model run on what they make at its rate
	void process(const float* in, float* out, int frames, ModelBlock& model);

private:
	// a lowpass for resampling to the rate (cycles per sample at the rate it is applied at), over kHalfWidth samples
	// each side, tabulated
	struct Kernel {
		explicit Kernel(double cutoff);
		double at(double x) const;
		std::vector<double> table;
	};

	bool ready(int64_t m) const;
	double up(int64_t m) const;
	double down(int64_t n) const;

	double ratio;
	int delay;
	int modelBlock;
	Kernel toModel;
	Kernel toHost;
	// the host's input and the model's output, each a ring indexed by its sample's number
	std::vector<double> input;
	std::vector<double> output;
	int64_t given = 0;
	int64_t made = 0;
	int64_t played = 0;
	std::vector<double> blockIn;
	std::vector<double> blockOut;
};

}  // namespace Woodshed
