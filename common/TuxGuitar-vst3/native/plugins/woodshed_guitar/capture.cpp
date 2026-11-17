#include "capture.h"

#include "NAM/get_dsp.h"
#include "NAM/nam_file.h"

#include <algorithm>
#include <cctype>
#include <filesystem>
#include <fstream>
#include <iterator>

namespace Woodshed {

namespace {

// NAM captures made before their rate was written down were made at 48 kHz
const double kUnknownRate = 48000.0;

std::filesystem::path toPath(const std::string& utf8) {
	return std::filesystem::path(std::u8string(utf8.begin(), utf8.end()));
}

std::string lower(std::string text) {
	std::transform(text.begin(), text.end(), text.begin(), [](unsigned char c) { return (char) std::tolower(c); });
	return text;
}

Capture refused(const std::string& refusal) {
	Capture capture;
	capture.refusal = refusal;
	return capture;
}

// the model the capture's JSON describes, built by NeuralAmpModelerCore; one input and at least one output
Capture fromJson(const std::string& name, const nlohmann::json& json) {
	try {
		nam::dspData data;
		nam::DspLoadOptions options;
		options.prewarm = false;
		Capture capture;
		capture.model = nam::get_dsp(json, data, options);
		capture.rate = (data.expected_sample_rate > 0.0 ? data.expected_sample_rate : kUnknownRate);
		if (capture.model->NumInputChannels() != 1) {
			return refused(name + " could not be loaded: it has " + std::to_string(capture.model->NumInputChannels()) + " inputs, and Woodshed Guitar plays captures of one");
		}
		return capture;
	} catch (const std::exception& e) {
		return refused(name + " could not be loaded: " + e.what());
	}
}

}  // namespace

std::string fileName(const std::string& path) {
	std::u8string name = toPath(path).filename().u8string();
	return std::string(name.begin(), name.end());
}

bool readFile(const std::string& path, std::string& contents) {
	std::ifstream file(toPath(path), std::ios::binary);
	if (!file) {
		return false;
	}
	contents.assign(std::istreambuf_iterator<char>(file), std::istreambuf_iterator<char>());
	return !file.bad();
}

Capture readCapture(const std::string& name, const std::string& contents) {
	nlohmann::json json = nlohmann::json::parse(contents, nullptr, false);
	if (json.is_discarded()) {
		return refused(name + " could not be loaded: it is not the JSON of a .nam file");
	}
	return fromJson(name, json);
}

// the core's own validation of the file first, so its reason is the one the user reads
Capture loadCapture(const std::string& path, std::string& contents) {
	std::string name = fileName(path);
	if (lower(toPath(path).extension().string()) != ".nam") {
		return refused(name + " is not a NAM capture: Woodshed Guitar plays .nam files");
	}
	if (!readFile(path, contents)) {
		return refused(name + " could not be read");
	}
	try {
		return fromJson(name, nam::validate_nam_file(toPath(path)));
	} catch (const std::exception& e) {
		return refused(name + " could not be loaded: " + e.what());
	}
}

Amp::Amp(std::shared_ptr<nam::DSP> model, double modelRate) : model(std::move(model)), modelRate(modelRate) {}

Amp::~Amp() = default;

void Amp::prepare(double, int maxBlock) {
	in.assign((size_t) maxBlock, 0.0);
	out.assign((size_t) maxBlock, 0.0);
	model->ResetAndPrewarm(modelRate, maxBlock);
}

int Amp::latency() const {
	return 0;
}

void Amp::run(const double* input, double* output, int frames) {
	double* inputs[1] = {const_cast<double*>(input)};
	double* outputs[1] = {output};
	model->process(inputs, outputs, frames);
}

void Amp::process(float* signal, int frames) {
	std::copy(signal, signal + frames, in.begin());
	run(in.data(), out.data(), frames);
	std::copy(out.begin(), out.begin() + frames, signal);
}

}  // namespace Woodshed
