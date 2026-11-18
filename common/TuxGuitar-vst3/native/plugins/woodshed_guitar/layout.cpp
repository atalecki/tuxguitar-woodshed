#include "layout.h"

#include <algorithm>
#include <cmath>

namespace Woodshed {

namespace {

// the 96-DPI design, in pixels
const int kMargin = 12;
const int kButtonWidth = 120;
const int kButtonHeight = 28;
// a button's text has this much room on both sides together
const int kButtonPadding = 24;
const int kLabelWidth = 90;
const int kLabelGap = 2;
const int kValueGap = 8;
const int kValueWidth = 76;
const int kLine = 20;
const int kSliderWidth = 120;
const int kQualityWidth = 120;
// room for the arrow of Quality's list
const int kArrow = 32;
const int kHeight = 230;

// the design's pixels at the scale
int at(int pixels, double scale) {
	return (int) std::lround(pixels * scale);
}

// the buttons as wide as the wider of their texts, the labels as their widest, the values as the widest value; never
// narrower than the design
int buttonWidth(const Measures& measured, double scale) {
	return std::max({at(kButtonWidth, scale), measured.loadText + at(kButtonPadding, scale), measured.updateText + at(kButtonPadding, scale)});
}

int labelWidth(const Measures& measured, double scale) {
	return std::max({at(kLabelWidth, scale), measured.inputLabel, measured.outputLabel, measured.qualityLabel});
}

int valueWidth(const Measures& measured, double scale) {
	return std::max(at(kValueWidth, scale), measured.value);
}

// the columns every row shares
struct Columns {
	int margin;
	int button;
	int label;
	int value;
	int line;
	int width;
};

Columns columns(const Measures& measured, int width, double scale) {
	Columns cut;
	cut.margin = at(kMargin, scale);
	cut.button = buttonWidth(measured, scale);
	cut.label = labelWidth(measured, scale);
	cut.value = valueWidth(measured, scale);
	cut.line = std::max(at(kLine, scale), measured.line);
	cut.width = std::max(width, minimumWidth(measured, scale));
	return cut;
}

// Load profile... and the profile's name after it; the refusal under them; the notice and Update from file at the right
void topRows(Layout& layout, const Columns& cut, double scale) {
	int profileX = cut.margin + cut.button + cut.margin;
	layout.load = Box{cut.margin, cut.margin, cut.button, at(kButtonHeight, scale)};
	layout.profile = Box{profileX, at(18, scale), cut.width - profileX - cut.margin, cut.line};
	layout.refusal = Box{cut.margin, at(46, scale), cut.width - 2 * cut.margin, at(34, scale)};
	layout.update = Box{cut.width - cut.margin - cut.button, at(82, scale), cut.button, at(kButtonHeight, scale)};
	layout.notice = Box{cut.margin, at(86, scale), layout.update.x - 2 * cut.margin, at(kButtonHeight, scale)};
}

// a gain's row: its label, its slider taking the rest, its value at the right edge
void sliderRow(Layout& layout, int row, const Columns& cut, double scale) {
	int top = at(118 + row * 34, scale);
	int sliderX = cut.margin + cut.label + at(kLabelGap, scale);
	int valueX = cut.width - cut.margin - cut.value;
	layout.labels[row] = Box{cut.margin, at(122 + row * 34, scale), cut.label, cut.line};
	layout.sliders[row] = Box{sliderX, top, valueX - at(kValueGap, scale) - sliderX, at(kButtonHeight, scale)};
	layout.values[row] = Box{valueX, at(122 + row * 34, scale), cut.value, cut.line};
}

// Quality's label and its list, where the sliders start
void qualityRow(Layout& layout, const Measures& measured, const Columns& cut, double scale) {
	layout.labels[2] = Box{cut.margin, at(190, scale), cut.label, cut.line};
	layout.quality = Box{layout.sliders[0].x, at(186, scale), std::max(at(kQualityWidth, scale), measured.qualityItems + at(kArrow, scale)), at(80, scale)};
}

}  // namespace

Layout layOut(const Measures& measured, int width, double scale) {
	Columns cut = columns(measured, width, scale);
	Layout layout = {};
	topRows(layout, cut, scale);
	sliderRow(layout, 0, cut, scale);
	sliderRow(layout, 1, cut, scale);
	qualityRow(layout, measured, cut, scale);
	layout.width = cut.width;
	layout.height = naturalHeight(scale);
	return layout;
}

int minimumWidth(const Measures& measured, double scale) {
	int margin = at(kMargin, scale);
	int sliders = margin + labelWidth(measured, scale) + at(kLabelGap, scale) + at(kSliderWidth, scale) + at(kValueGap, scale) + valueWidth(measured, scale) + margin;
	return std::max(sliders, 4 * margin + buttonWidth(measured, scale) + at(kSliderWidth, scale));
}

int naturalHeight(double scale) {
	return at(kHeight, scale);
}

}  // namespace Woodshed
