"""Makes the test captures of the audio features (README: the test captures) from NeuralAmpModelerCore's examples.

    python make_test_captures.py <NeuralAmpModelerCore directory> <output directory>

Each keeps the structure of an example (architecture, config, sample rate) drops its metadata (whose amp, how loud) and replaces every weight by one of its own:
the same numbers every run (xorshift32 from a fixed seed), evenly spread in [-SCALE, SCALE], so the captures are no one's
amp and no real capture is in the tests. The core is the one native/cmake/nam.cmake pins (v0.6.0), found in the native
build at common/TuxGuitar-vst3/native-build/nam/NeuralAmpModelerCore.
"""
import json
import os
import sys

SCALE = 0.1
CAPTURES = {
    "A1 standard test.nam": "wavenet_a1_standard.nam",
    "A2 test.nam": "A2.nam",
}


class Weights:
    def __init__(self):
        self.state = 0x2545F491

    def next(self):
        self.state ^= (self.state << 13) & 0xFFFFFFFF
        self.state ^= self.state >> 17
        self.state ^= (self.state << 5) & 0xFFFFFFFF
        return round(SCALE * (self.state / 2147483648.0 - 1.0), 5)


def replace(node, weights):
    if isinstance(node, dict):
        return {key: ([weights.next() for _ in value] if key == "weights" else replace(value, weights)) for key, value in node.items() if key != "metadata"}
    if isinstance(node, list):
        return [replace(value, weights) for value in node]
    return node


def main(core, output):
    for name, example in CAPTURES.items():
        with open(os.path.join(core, "example_models", example), encoding="utf-8") as file:
            capture = json.load(file)
        capture = replace(capture, Weights())
        with open(os.path.join(output, name), "w", encoding="utf-8", newline="\n") as file:
            json.dump(capture, file, separators=(",", ":"))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
