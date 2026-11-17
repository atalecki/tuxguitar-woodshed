// Woodshed Guitar's state, as a song keeps it: its parameters, and its profile's path and contents (so a song opened on
// another computer, or after the file moved, still plays it)
#pragma once

#include <string>

namespace Woodshed {

struct Saved {
	double inputGain = 0.0;
	double outputLevel = 0.0;
	double quality = 0.0;
	// the profile: none when the path is empty
	std::string path;
	std::string contents;
};

// "WSG1", the parameters (doubles), then the path and the contents, each its length (8 bytes) then its bytes; numbers
// little-endian
std::string writeState(const Saved& saved);

// false when the bytes are no state of Woodshed Guitar (saved left as it was)
bool readState(const std::string& bytes, Saved& saved);

}  // namespace Woodshed
