// woodshed-guitar-layout-test: checks where Woodshed Guitar's editor puts its controls (layout.h): today's look at
// 96 DPI, every box scaled at another DPI, only the sliders and text lines stretched in a wider window, the label column
// as wide as a long translation, never narrower than its minimum, its height fixed. Prints each check that fails; exit 0
// when none does (TuxGuitar-vst3's TestWoodshedChecks runs it).
#include "layout.h"

#include <cmath>
#include <cstdio>
#include <string>

namespace {

using namespace Woodshed;

// as the editor measures its texts at 96 DPI in English: all within today's fixed columns
const Measures kEnglish = {70, 90, 55, 60, 40, 30, 48, 13};

int failures = 0;

std::string text(const Box& box) {
	return std::to_string(box.x) + "," + std::to_string(box.y) + " " + std::to_string(box.width) + "x" + std::to_string(box.height);
}

// every box, in order, then the size
std::string text(const Layout& layout) {
	std::string all = text(layout.load) + " | " + text(layout.profile) + " | " + text(layout.refusal) + " | " + text(layout.notice) + " | " + text(layout.update);
	for (const Box& label : layout.labels) {
		all += " | " + text(label);
	}
	for (int row = 0; row < 2; row++) {
		all += " | " + text(layout.sliders[row]) + " " + text(layout.values[row]);
	}
	return all + " | " + text(layout.quality) + " | " + std::to_string(layout.width) + "x" + std::to_string(layout.height);
}

void check(const std::string& what, const std::string& expected, const std::string& actual) {
	if (expected != actual) {
		std::printf("%s:\n  expected %s\n  got      %s\n", what.c_str(), expected.c_str(), actual.c_str());
		failures++;
	}
}

Box scaled(const Box& box, double scale) {
	return Box{(int) std::lround(box.x * scale), (int) std::lround(box.y * scale), (int) std::lround(box.width * scale), (int) std::lround(box.height * scale)};
}

// each box of the layout scaled, the size too
Layout scaled(Layout layout, double scale) {
	Box* boxes[] = {&layout.load, &layout.profile, &layout.refusal, &layout.notice, &layout.update, &layout.labels[0], &layout.labels[1], &layout.labels[2],
		&layout.sliders[0], &layout.sliders[1], &layout.values[0], &layout.values[1], &layout.quality};
	for (Box* box : boxes) {
		*box = scaled(*box, scale);
	}
	layout.width = (int) std::lround(layout.width * scale);
	layout.height = (int) std::lround(layout.height * scale);
	return layout;
}

Measures scaled(const Measures& measured, double scale) {
	auto at = [scale](int pixels) { return (int) std::lround(pixels * scale); };
	return Measures{at(measured.loadText), at(measured.updateText), at(measured.inputLabel), at(measured.outputLabel), at(measured.qualityLabel),
		at(measured.qualityItems), at(measured.value), at(measured.line)};
}

}  // namespace

int main() {
	const std::string today = "12,12 120x28 | 144,18 304x20 | 12,46 436x34 | 12,86 304x28 | 328,82 120x28 | 12,122 90x20 | 12,156 90x20 | 12,190 90x20"
		" | 104,118 260x28 372,122 76x20 | 104,152 260x28 372,156 76x20 | 104,186 120x80 | 460x230";
	check("today's look at 96 DPI", today, text(layOut(kEnglish, 460, 1.0)));
	check("every box 1.5 times at 144 DPI", text(scaled(layOut(kEnglish, 460, 1.0), 1.5)), text(layOut(scaled(kEnglish, 1.5), 690, 1.5)));
	check("wider: only the sliders and the text lines stretch, the right edges line up",
		"12,12 120x28 | 144,18 744x20 | 12,46 876x34 | 12,86 744x28 | 768,82 120x28 | 12,122 90x20 | 12,156 90x20 | 12,190 90x20"
		" | 104,118 700x28 812,122 76x20 | 104,152 700x28 812,156 76x20 | 104,186 120x80 | 900x230", text(layOut(kEnglish, 900, 1.0)));
	Measures german = kEnglish;
	german.inputLabel = 180;
	Layout wide = layOut(german, 600, 1.0);
	check("a long label widens the label column; the slider starts after it", "12,122 180x20 194,118 310x28 194",
		text(wide.labels[0]) + " " + text(wide.sliders[0]) + " " + std::to_string(wide.quality.x));
	check("a long button text widens the buttons", "12,12 154x28 | 178,18", text(layOut(Measures{130, 90, 55, 60, 40, 30, 48, 13}, 460, 1.0).load) + " | "
		+ text(layOut(Measures{130, 90, 55, 60, 40, 30, 48, 13}, 460, 1.0).profile).substr(0, 6));
	Layout wideValue = layOut(Measures{70, 90, 55, 60, 40, 30, 88, 13}, 460, 1.0);
	check("a wide value widens the value column; the slider gives way", "104,118 248x28 360,122 88x20", text(wideValue.sliders[0]) + " " + text(wideValue.values[0]));
	check("a long Quality item widens its list", "104,186 132x80", text(layOut(Measures{70, 90, 55, 60, 40, 100, 48, 13}, 460, 1.0).quality));
	Layout longUpdate = layOut(Measures{70, 110, 55, 60, 40, 30, 48, 13}, 460, 1.0);
	check("a long Update text widens both buttons", "12,12 134x28 314,82 134x28", text(longUpdate.load) + " " + text(longUpdate.update));
	check("the narrowest it goes", "320", std::to_string(minimumWidth(kEnglish, 1.0)));
	check("with very long button texts the buttons' row sets the narrowest: margins, the button, a profile line of 120", "492",
		std::to_string(minimumWidth(Measures{300, 90, 55, 60, 40, 30, 48, 13}, 1.0)));
	check("narrower than that: laid out at the narrowest", text(layOut(kEnglish, 320, 1.0)), text(layOut(kEnglish, 200, 1.0)));
	check("the height never changes with the width", "230 230 230", std::to_string(layOut(kEnglish, 200, 1.0).height) + " " + std::to_string(layOut(kEnglish, 900, 1.0).height)
		+ " " + std::to_string(naturalHeight(1.0)));
	check("the height at 175%", "403", std::to_string(naturalHeight(1.75)));
	check("a font taller than a text line's box: the box grows to the font", "24", std::to_string(layOut(Measures{70, 90, 55, 60, 40, 30, 48, 24}, 460, 1.0).labels[0].height));
	return (failures == 0 ? 0 : 1);
}
