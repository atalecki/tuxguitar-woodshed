// Has the thread that made it (a plugin's own thread, its UI thread) act soon, when any other thread asks: on Windows
// through a message-only window of its own, whose message that thread's loop dispatches. Elsewhere it does nothing, and
// the plugin acts the next time that thread calls it.
#pragma once

#include <functional>

namespace Woodshed {

class Wake {
public:
	explicit Wake(std::function<void()> act);
	~Wake();
	// any thread
	void post();

private:
	std::function<void()> act;
	void* window = nullptr;

	friend struct WakeWindow;
};

}  // namespace Woodshed
