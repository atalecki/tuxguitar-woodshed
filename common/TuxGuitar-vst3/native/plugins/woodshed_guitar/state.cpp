#include "state.h"

#include <cstdint>
#include <cstring>

namespace Woodshed {

namespace {

const char kMagic[4] = {'W', 'S', 'G', '1'};

void putNumber(std::string& bytes, uint64_t value) {
	for (int i = 0; i < 8; i++) {
		bytes.push_back((char) ((value >> (8 * i)) & 0xFF));
	}
}

void putDouble(std::string& bytes, double value) {
	uint64_t bits = 0;
	std::memcpy(&bits, &value, sizeof(bits));
	putNumber(bytes, bits);
}

void putText(std::string& bytes, const std::string& text) {
	putNumber(bytes, text.size());
	bytes.append(text);
}

// reads the bytes from the position on, each read false once they run out
class Reader {
public:
	explicit Reader(const std::string& bytes) : bytes(bytes) {}

	bool number(uint64_t& value) {
		if (bytes.size() - at < 8) {
			return false;
		}
		value = 0;
		for (int i = 0; i < 8; i++) {
			value |= (uint64_t) (unsigned char) bytes[at + i] << (8 * i);
		}
		at += 8;
		return true;
	}

	bool real(double& value) {
		uint64_t bits = 0;
		bool read = number(bits);
		std::memcpy(&value, &bits, sizeof(value));
		return read;
	}

	bool text(std::string& value) {
		uint64_t size = 0;
		if (!number(size) || bytes.size() - at < size) {
			return false;
		}
		value = bytes.substr(at, (size_t) size);
		at += (size_t) size;
		return true;
	}

	bool magic() {
		bool same = (bytes.size() >= sizeof(kMagic) && std::memcmp(bytes.data(), kMagic, sizeof(kMagic)) == 0);
		at = sizeof(kMagic);
		return same;
	}

private:
	const std::string& bytes;
	size_t at = 0;
};

}  // namespace

std::string writeState(const Saved& saved) {
	std::string bytes(kMagic, sizeof(kMagic));
	putDouble(bytes, saved.inputGain);
	putDouble(bytes, saved.outputLevel);
	putDouble(bytes, saved.quality);
	putText(bytes, saved.path);
	putText(bytes, saved.contents);
	return bytes;
}

bool readState(const std::string& bytes, Saved& saved) {
	Reader reader(bytes);
	Saved read;
	bool whole = reader.magic() && reader.real(read.inputGain) && reader.real(read.outputLevel) && reader.real(read.quality) &&
	             reader.text(read.path) && reader.text(read.contents);
	if (whole) {
		saved = read;
	}
	return whole;
}

}  // namespace Woodshed
