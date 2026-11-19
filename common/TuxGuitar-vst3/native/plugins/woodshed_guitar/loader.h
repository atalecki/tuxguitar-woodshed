// Woodshed Guitar's profiles, loaded on a thread of their own, so that playing never waits for one: a job reads and
// checks a capture (the file at a path, or the contents a state holds), builds its model and makes it ready for the
// host's rate, then hands it to the stage, which plays it from its next block; what the job found is kept for the
// plugin, which takes it on its own thread. A job waiting is replaced by a newer one (a check never replaces a load),
// and a job made stale by a newer load hands nothing over and is not taken.
#pragma once

#include "stage.h"

#include <condition_variable>
#include <cstdint>
#include <deque>
#include <functional>
#include <mutex>
#include <optional>
#include <string>
#include <thread>

namespace Woodshed {

struct Job {
	// the file at the path, as Load profile... gives it; the contents a state holds; or only whether the file at the
	// path still holds the contents
	enum Kind { kLoad, kRestore, kCheck };
	Kind kind = kCheck;
	std::string path;
	std::string contents;
	uint64_t number = 0;
};

// what a job found: the profile's path and contents (a load's: the file's), why the capture was refused (the profile
// kept, or, restored, none), and whether the file at the path is there and holds other bytes than the contents
struct Loaded {
	Job job;
	std::string refusal;
	bool changed = false;
};

class Loader {
public:
	// finished is told, on the loader's thread, each time a job's result is kept
	Loader(Stage& stage, std::function<void()> finished);
	~Loader();

	// the host's rate and largest block, which the models are made ready for
	void prepare(double hostRate, int maxBlock);
	// the job's number
	uint64_t post(Job job);
	// waits until no job is waiting or under way (offline, a block may)
	void waitIdle();
	// the next result kept that no newer load made stale; false when there is none
	bool take(Loaded& loaded);

private:
	void run();
	bool next(Job& job);
	Loaded work(const Job& job, std::unique_ptr<Amp>& amp, bool& handOver);
	void finish(const Loaded& loaded, std::unique_ptr<Amp> amp, bool handOver);
	bool stale(const Job& job) const;

	Stage& stage;
	std::function<void()> finished;
	std::mutex lock;
	std::condition_variable wake;
	std::condition_variable idle;
	std::optional<Job> waiting;
	bool working = false;
	bool stopping = false;
	uint64_t posted = 0;
	// the number of the newest load or restore posted
	uint64_t newestLoad = 0;
	std::deque<Loaded> results;
	double hostRate = 44100.0;
	int maxBlock = 512;
	std::thread thread;
};

}  // namespace Woodshed
