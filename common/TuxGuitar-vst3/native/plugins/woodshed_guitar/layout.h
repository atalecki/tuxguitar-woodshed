// Where each control of Woodshed Guitar's editor goes (editor.cpp), apart from Windows so it can be checked
// (layout_test.cpp): the 96-DPI design (a 12 px margin, 28 px buttons, a 34 px row pitch) times the screen's scale, the
// label column as wide as its widest translated label, the buttons as wide as their text (at least 120 px at 96 DPI),
// the value column as wide as its widest value; the sliders and the profile and refusal lines take the rest of the
// width. Only the width stretches: the height is the design's, scaled.
#pragma once

#include <string>
#include <vector>

namespace Woodshed {

// what the editor measured with the font it uses, in pixels: each button's text, each label's, Quality's widest item,
// the widest value ("-40.0 dB"), a line of text's height
struct Measures {
	int loadText;
	int updateText;
	int inputLabel;
	int outputLabel;
	int qualityLabel;
	int qualityItems;
	int value;
	int line;
};

struct Box {
	int x;
	int y;
	int width;
	int height;
};

// the boxes of the controls: Load profile..., the profile's line, the refusal's, the notice's, Update from file, the
// labels (Input gain, Output level, Quality), the two sliders and their values, Quality's list; and the editor's size
struct Layout {
	Box load;
	Box profile;
	Box refusal;
	Box notice;
	Box update;
	Box labels[3];
	Box sliders[2];
	Box values[2];
	Box quality;
	int width;
	int height;
};

// laid out in the width given (at least minimumWidth) at the scale (1 at 96 DPI)
Layout layOut(const Measures& measured, int width, double scale);
// the narrowest the editor goes: its widest label, a slider of 120 px at 96 DPI, its value, margins; and its buttons'
// row
int minimumWidth(const Measures& measured, double scale);
// the editor's height at the scale: the only height it has
int naturalHeight(double scale);

// a line of text as shown in the width: whole when it fits, else as much of it as fits followed by "\u2026" (spaces
// before it dropped, a character of two UTF-16 units never cut in half; too narrow for any, the "\u2026" alone). edges:
// each character's right edge as the font measures the line (GetTextExtentExPoint), ellipsis: the width of "\u2026"
std::wstring ellipsized(const std::wstring& text, const std::vector<int>& edges, int ellipsis, int width);

}  // namespace Woodshed
