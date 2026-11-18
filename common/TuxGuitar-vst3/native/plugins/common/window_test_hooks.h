// What a test does to the window a plugin's editor is in, as Windows and the user do it, shared by the test plugins
// (test-plugins/testplugin.cpp) and Woodshed Guitar (its editor's test messages): the window goes to a screen of
// another DPI, the user drags its border. Windows only; elsewhere nothing happens.
#pragma once

#include <vector>

#ifdef _WIN32
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#endif

namespace TuxGuitarTest {

#ifdef _WIN32
// as Windows tells the window the editor is in that it went to a screen of that DPI: the frame it suggests is the
// window's scaled from its DPI now, at the same top left corner (left, top, right, bottom)
inline std::vector<double> changeDpi(void* parent, long long dpi) {
	HWND window = (HWND) parent;
	RECT frame = {};
	GetWindowRect(window, &frame);
	int now = (int) GetDpiForWindow(window);
	RECT suggested = {frame.left, frame.top, frame.left + MulDiv(frame.right - frame.left, (int) dpi, now), frame.top + MulDiv(frame.bottom - frame.top, (int) dpi, now)};
	SendMessageW(window, WM_DPICHANGED, MAKEWPARAM(dpi, dpi), (LPARAM) &suggested);
	return std::vector<double>{(double) suggested.left, (double) suggested.top, (double) suggested.right, (double) suggested.bottom};
}

// as the user drags the border of the window the editor is in until the inside is that size
inline void dragTo(void* parent, long long width, long long height) {
	HWND window = (HWND) parent;
	RECT frame = {};
	RECT inside = {};
	GetWindowRect(window, &frame);
	GetClientRect(window, &inside);
	int extraWidth = (frame.right - frame.left) - inside.right;
	int extraHeight = (frame.bottom - frame.top) - inside.bottom;
	SetWindowPos(window, nullptr, 0, 0, (int) width + extraWidth, (int) height + extraHeight, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE);
}
#else
inline std::vector<double> changeDpi(void*, long long) { return std::vector<double>(); }

inline void dragTo(void*, long long, long long) {}
#endif

}  // namespace TuxGuitarTest
