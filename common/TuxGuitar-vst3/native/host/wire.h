// The helper's side of the pipes to TuxGuitar: requests come in on what was its standard input, replies go out on what
// was its standard output, both big-endian (Java's DataInputStream and DataOutputStream). Nothing else can open these
// pipes: they have no name and no network port.
#pragma once

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>

namespace TuxGuitar {

struct Inbox;

class Wire {
public:
	// takes the standard input and output for the protocol, so a plugin printing to them cannot break it: from now on
	// the process's own standard output goes to its standard error, and its standard input reads nothing
	static Wire claimStandardStreams();

	// from now on a thread of its own reads the input, and calls heard (on that thread) each time bytes come or the input
	// ends, so the process's thread can wait for its windows' messages instead of for the input
	void listen(std::function<void()> heard);
	// whether a read finds bytes, or the end of the input, at once (always, until it listens)
	bool hasInput();

	// false at the end of the input (TuxGuitar closed the pipe or is gone)
	bool readInt(int32_t& value);
	int32_t readInt();
	int64_t readLong();
	float readFloat();
	double readDouble();
	std::string readString();
	std::vector<char> readBytes();

	void writeInt(int32_t value);
	void writeLong(int64_t value);
	void writeFloat(float value);
	void writeDouble(double value);
	void writeString(const std::string& value);
	void writeBytes(const void* data, size_t size);
	// sends what was written as one reply
	void flush();
	// forgets what was written since the last flush
	void discard() { pending.clear(); }

private:
	Wire(int input, int output) : input(input), output(output) {}
	// at least one byte, and at most size; none at the end of the input
	size_t readSome(void* data, size_t size);
	void readFully(void* data, size_t size);
	void put(uint64_t bits, int bytes);
	uint64_t get(int bytes);

	int input;
	int output;
	std::vector<unsigned char> pending;
	// what the listening thread read and the process's thread has not yet (none until it listens)
	std::shared_ptr<Inbox> inbox;
};

// thrown when the input ends inside a request
struct WireClosed {};

}  // namespace TuxGuitar
