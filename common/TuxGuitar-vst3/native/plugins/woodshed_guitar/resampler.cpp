#include "resampler.h"

#include <algorithm>
#include <cmath>

namespace Woodshed {

namespace {

const int kHalfWidth = 24;
// table points per sample of the kernel
const int kSteps = 256;
const double kBeta = 8.0;
// the passband's edge, a fraction of the lower rate
const double kPassband = 0.45;
const double kPi = 3.14159265358979323846;

// the modified Bessel function of order 0 (Kaiser's window), by its series
double bessel0(double x) {
	double sum = 1.0;
	double term = 1.0;
	for (int k = 1; k < 50 && term > 1e-12 * sum; k++) {
		term *= (x / (2.0 * k)) * (x / (2.0 * k));
		sum += term;
	}
	return sum;
}

size_t ringFor(double samples) {
	size_t size = 1;
	while (size < samples) {
		size <<= 1;
	}
	return size;
}

double sample(const std::vector<double>& ring, int64_t index) {
	return (index < 0 ? 0.0 : ring[(size_t) index & (ring.size() - 1)]);
}

}  // namespace

Resampler::Kernel::Kernel(double cutoff) : table((size_t) (2 * kHalfWidth * kSteps + 2), 0.0) {
	for (size_t i = 0; i + 1 < table.size(); i++) {
		double x = (double) i / kSteps - kHalfWidth;
		double sinc = (x == 0.0 ? 1.0 : std::sin(2.0 * kPi * cutoff * x) / (2.0 * kPi * cutoff * x));
		double edge = x / kHalfWidth;
		double window = bessel0(kBeta * std::sqrt(std::max(0.0, 1.0 - edge * edge))) / bessel0(kBeta);
		table[i] = 2.0 * cutoff * sinc * window;
	}
}

// between the two nearest points of the table; nothing beyond kHalfWidth
double Resampler::Kernel::at(double x) const {
	double position = (x + kHalfWidth) * kSteps;
	if (position < 0.0 || position >= (double) (table.size() - 1)) {
		return 0.0;
	}
	size_t index = (size_t) position;
	double part = position - index;
	return table[index] + part * (table[index + 1] - table[index]);
}

Resampler::Resampler(double hostRate, double modelRate, int maxBlock)
    : ratio(modelRate / hostRate),
      delay((int) std::ceil(kHalfWidth + (kHalfWidth + 1) / (modelRate / hostRate))),
      modelBlock((int) std::ceil(maxBlock * modelRate / hostRate) + 4),
      toModel(kPassband * std::min(hostRate, modelRate) / hostRate),
      toHost(kPassband * std::min(hostRate, modelRate) / modelRate),
      input(ringFor(maxBlock + 4.0 * kHalfWidth + 16)),
      output(ringFor(ratio * (maxBlock + delay + 4.0 * kHalfWidth) + 4.0 * kHalfWidth + 64)),
      blockIn((size_t) modelBlock),
      blockOut((size_t) modelBlock) {}

// model sample m is made once every host sample it reads is given
bool Resampler::ready(int64_t m) const {
	return (int64_t) std::floor(m / ratio) <= given - 1;
}

double Resampler::up(int64_t m) const {
	double t = m / ratio - kHalfWidth;
	double sum = 0.0;
	for (int64_t k = (int64_t) std::floor(t - kHalfWidth) + 1; k <= (int64_t) std::floor(t + kHalfWidth); k++) {
		sum += sample(input, k) * toModel.at(t - k);
	}
	return sum;
}

double Resampler::down(int64_t n) const {
	double mu = ratio * (double) (n - delay + kHalfWidth);
	double sum = 0.0;
	for (int64_t j = (int64_t) std::floor(mu - kHalfWidth) + 1; j <= (int64_t) std::floor(mu + kHalfWidth); j++) {
		sum += (j < made ? sample(output, j) : 0.0) * toHost.at(mu - j);
	}
	return sum;
}

// the block given, then every model sample it lets be made (run a model block at a time), then the block played
void Resampler::process(const float* in, float* out, int frames, ModelBlock& model) {
	for (int i = 0; i < frames; i++) {
		input[(size_t) (given + i) & (input.size() - 1)] = in[i];
	}
	given += frames;
	while (ready(made)) {
		int count = 0;
		for (; count < modelBlock && ready(made + count); count++) {
			blockIn[count] = up(made + count);
		}
		model.run(blockIn.data(), blockOut.data(), count);
		for (int i = 0; i < count; i++) {
			output[(size_t) (made + i) & (output.size() - 1)] = blockOut[i];
		}
		made += count;
	}
	for (int i = 0; i < frames; i++) {
		out[i] = (float) down(played + i);
	}
	played += frames;
}

}  // namespace Woodshed
