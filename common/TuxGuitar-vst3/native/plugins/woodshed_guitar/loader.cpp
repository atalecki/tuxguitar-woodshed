#include "loader.h"

#include <chrono>

namespace Woodshed {

namespace {

// how often the loader frees what the audio thread let go of while no job comes
const std::chrono::milliseconds kCollect(50);

}  // namespace

Loader::Loader(Stage& stage, std::function<void()> finished) : stage(stage), finished(std::move(finished)) {
	thread = std::thread([this] { run(); });
}

Loader::~Loader() {
	{
		std::lock_guard<std::mutex> held(lock);
		stopping = true;
	}
	wake.notify_all();
	thread.join();
}

void Loader::prepare(double hostRate, int maxBlock) {
	std::lock_guard<std::mutex> held(lock);
	this->hostRate = hostRate;
	this->maxBlock = maxBlock;
	stage.prepare(hostRate, maxBlock);
}

uint64_t Loader::post(Job job) {
	std::lock_guard<std::mutex> held(lock);
	job.number = ++posted;
	newestLoad = (job.kind != Job::kCheck ? job.number : newestLoad);
	if (job.kind != Job::kCheck || !waiting || waiting->kind == Job::kCheck) {
		waiting = job;
	}
	wake.notify_all();
	return job.number;
}

void Loader::waitIdle() {
	std::unique_lock<std::mutex> held(lock);
	idle.wait(held, [this] { return !waiting && !working; });
}

bool Loader::take(Loaded& loaded) {
	std::lock_guard<std::mutex> held(lock);
	while (!results.empty() && stale(results.front().job)) {
		results.pop_front();
	}
	if (results.empty()) {
		return false;
	}
	loaded = results.front();
	results.pop_front();
	return true;
}

// a newer load makes a job stale
bool Loader::stale(const Job& job) const {
	return job.number < newestLoad;
}

void Loader::run() {
	Job job;
	while (next(job)) {
		std::unique_ptr<Amp> amp;
		bool handOver = false;
		Loaded loaded = work(job, amp, handOver);
		finish(loaded, std::move(amp), handOver);
	}
}

// the job waiting, once there is one (meanwhile what the audio thread let go of is freed); false once stopping
bool Loader::next(Job& job) {
	std::unique_lock<std::mutex> held(lock);
	working = false;
	idle.notify_all();
	while (!stopping && !waiting) {
		wake.wait_for(held, kCollect);
		stage.collect();
	}
	if (stopping) {
		return false;
	}
	job = *waiting;
	waiting.reset();
	working = true;
	return true;
}

// a load refused keeps the profile playing; a restore always hands over (none when there is no capture, or it is
// refused); a check hands nothing over
Loaded Loader::work(const Job& job, std::unique_ptr<Amp>& amp, bool& handOver) {
	Loaded loaded;
	loaded.job = job;
	if (job.kind == Job::kCheck) {
		loaded.changed = differs(job.path, job.contents);
		return loaded;
	}
	Capture capture;
	if (job.kind == Job::kLoad) {
		capture = loadCapture(job.path, loaded.job.contents);
	} else if (!job.path.empty()) {
		capture = readCapture(fileName(job.path), job.contents);
	}
	loaded.refusal = capture.refusal;
	handOver = (job.kind == Job::kRestore || capture.model != nullptr);
	if (capture.model) {
		amp = std::make_unique<Amp>(capture.model, capture.rate);
		std::unique_lock<std::mutex> held(lock);
		double rate = hostRate;
		int block = maxBlock;
		held.unlock();
		amp->prepare(rate, block);
		loaded.changed = (job.kind == Job::kRestore && differs(job.path, job.contents));
	}
	return loaded;
}

// kept, and handed over, unless a newer load made the job stale (its amp is then freed here)
void Loader::finish(const Loaded& loaded, std::unique_ptr<Amp> amp, bool handOver) {
	{
		std::lock_guard<std::mutex> held(lock);
		if (stale(loaded.job)) {
			return;
		}
		if (handOver) {
			stage.handOver(std::move(amp));
		}
		results.push_back(loaded);
	}
	finished();
}

}  // namespace Woodshed
