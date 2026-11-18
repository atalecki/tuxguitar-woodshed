#include "editor.h"

#include "plugin.h"
#include "../common/window_test_hooks.h"
#include "text.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <string>

#ifdef _WIN32
#include <windows.h>
#include <commctrl.h>
#include <commdlg.h>
#endif

namespace Woodshed {

using namespace Steinberg;

namespace {

const int kWidth = 460;
const int kHeight = 230;

// what the editor says of the profile and of the last refusal
std::string profileText(const ProfileStatus& status) {
	return (status.name.empty() ? say("profile.none") : say("profile.name", {status.name}));
}

// a capture being loaded is told in place of the last refusal
std::string refusalText(const ProfileStatus& status) {
	if (!status.loading.empty()) {
		return say("profile.loading", {status.loading});
	}
	return (status.refusal.empty() ? std::string() : say("profile.refused", {status.refusal}));
}

std::string decibels(double value) {
	char text[32];
	std::snprintf(text, sizeof(text), "%.1f dB", value);
	return text;
}

}  // namespace

#ifdef _WIN32

namespace {

const wchar_t kClass[] = L"WoodshedGuitarEditor";
const int kLoad = 1;
const int kUpdate = 2;
const int kQualityBox = 13;
// a gain slider's position is tenths of a dB
const double kStep = 10.0;

std::wstring wide(const std::string& utf8) {
	int size = MultiByteToWideChar(CP_UTF8, 0, utf8.data(), (int) utf8.size(), nullptr, 0);
	std::wstring text((size_t) size, L'\0');
	MultiByteToWideChar(CP_UTF8, 0, utf8.data(), (int) utf8.size(), text.data(), size);
	return text;
}

std::string utf8(const std::wstring& wide) {
	int size = WideCharToMultiByte(CP_UTF8, 0, wide.data(), (int) wide.size(), nullptr, 0, nullptr, nullptr);
	std::string text((size_t) size, '\0');
	WideCharToMultiByte(CP_UTF8, 0, wide.data(), (int) wide.size(), text.data(), size, nullptr, nullptr);
	return text;
}

std::string textOf(void* control) {
	wchar_t line[2048];
	GetWindowTextW((HWND) control, line, 2048);
	return utf8(line);
}

GuitarView* viewOf(HWND window) {
	return reinterpret_cast<GuitarView*>(GetWindowLongPtrW(window, GWLP_USERDATA));
}

// the button, Quality chosen; true when handled
bool command(GuitarView* view, WPARAM wParam) {
	if (LOWORD(wParam) == kLoad && HIWORD(wParam) == BN_CLICKED) {
		view->chooseProfile();
		return true;
	}
	if (LOWORD(wParam) == kUpdate && HIWORD(wParam) == BN_CLICKED) {
		view->updateFromFile();
		return true;
	}
	if (LOWORD(wParam) == kQualityBox && HIWORD(wParam) == CBN_SELCHANGE) {
		view->chose();
		return true;
	}
	return false;
}

// a command, or a slider moved (a gesture until its end); true when handled
bool handled(GuitarView* view, UINT message, WPARAM wParam, LPARAM lParam) {
	if (message == WM_COMMAND) {
		return command(view, wParam);
	}
	if (message == WM_HSCROLL && lParam != 0) {
		view->slid((void*) lParam, LOWORD(wParam) == TB_ENDTRACK);
		return true;
	}
	return false;
}

LRESULT CALLBACK editorProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	GuitarView* view = viewOf(window);
	if (view && handled(view, message, wParam, lParam)) {
		return 0;
	}
	if (message == WM_CTLCOLORSTATIC) {
		SetBkMode((HDC) wParam, TRANSPARENT);
		return (LRESULT) GetSysColorBrush(COLOR_BTNFACE);
	}
	return DefWindowProcW(window, message, wParam, lParam);
}

HINSTANCE module() {
	HMODULE handle = nullptr;
	GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT, (LPCWSTR) &editorProc, &handle);
	return handle;
}

void registerClass() {
	INITCOMMONCONTROLSEX controls = {sizeof(controls), ICC_BAR_CLASSES};
	InitCommonControlsEx(&controls);
	WNDCLASSW type = {};
	if (GetClassInfoW(module(), kClass, &type)) {
		return;
	}
	type.lpfnWndProc = editorProc;
	type.hInstance = module();
	type.hCursor = LoadCursor(nullptr, IDC_ARROW);
	type.hbrBackground = GetSysColorBrush(COLOR_BTNFACE);
	type.lpszClassName = kClass;
	RegisterClassW(&type);
}

// a control, its font and place given by rescale
HWND child(HWND parent, const wchar_t* type, const wchar_t* text, DWORD style, int id) {
	return CreateWindowExW(0, type, text, WS_CHILD | WS_VISIBLE | style, 0, 0, 0, 0, parent, (HMENU) (INT_PTR) id, module(), nullptr);
}

// the font Windows uses for messages, at the DPI
HFONT messageFont(UINT dpi) {
	NONCLIENTMETRICSW metrics = {};
	metrics.cbSize = sizeof(metrics);
	SystemParametersInfoForDpi(SPI_GETNONCLIENTMETRICS, sizeof(metrics), &metrics, 0, dpi);
	return CreateFontIndirectW(&metrics.lfMessageFont);
}

BOOL CALLBACK giveFont(HWND control, LPARAM font) {
	SendMessageW(control, WM_SETFONT, (WPARAM) font, TRUE);
	return TRUE;
}

// the font's size: the height of its em, in pixels
int emHeight(void* font) {
	LOGFONTW described = {};
	GetObjectW((HGDIOBJ) font, sizeof(described), &described);
	return std::abs((int) described.lfHeight);
}

int widthOf(void* control) {
	RECT box = {};
	GetWindowRect((HWND) control, &box);
	return box.right - box.left;
}

int textWidth(HDC dc, const std::string& text) {
	std::wstring line = wide(text);
	SIZE size = {};
	GetTextExtentPoint32W(dc, line.c_str(), (int) line.size(), &size);
	return size.cx;
}

// the editor's texts in the language shown, measured with the font of its controls
Measures measure(HGDIOBJ font) {
	HDC dc = GetDC(nullptr);
	HGDIOBJ before = SelectObject(dc, font);
	TEXTMETRICW metrics = {};
	GetTextMetricsW(dc, &metrics);
	Measures measured = {textWidth(dc, say("editor.load")), textWidth(dc, say("editor.update")), textWidth(dc, say("editor.input-gain")),
		textWidth(dc, say("editor.output-level")), textWidth(dc, say("editor.quality")),
		std::max(textWidth(dc, say("editor.quality.full")), textWidth(dc, say("editor.quality.light"))), textWidth(dc, decibels(-40.0)), (int) metrics.tmHeight};
	SelectObject(dc, before);
	ReleaseDC(nullptr, dc);
	return measured;
}

void put(HDWP& moves, void* control, const Box& box) {
	moves = DeferWindowPos(moves, (HWND) control, nullptr, box.x, box.y, box.width, box.height, SWP_NOZORDER | SWP_NOACTIVATE);
}

}  // namespace

bool hasEditor() {
	return true;
}

tresult PLUGIN_API GuitarView::isPlatformTypeSupported(FIDString type) {
	return (std::strcmp(type, kPlatformTypeHWND) == 0 ? kResultTrue : kResultFalse);
}

// a row: the parameter's name (its text's key), its slider over its range (tenths of a dB), its value
void GuitarView::addSlider(int row, ParamID id, const char* name, double minimum, double maximum) {
	labels[row] = child((HWND) window, L"STATIC", wide(say(name)).c_str(), SS_LEFT, 0);
	HWND bar = child((HWND) window, TRACKBAR_CLASSW, L"", TBS_HORZ | TBS_NOTICKS | WS_TABSTOP, (int) (10 + id));
	SendMessageW(bar, TBM_SETRANGEMIN, FALSE, (LPARAM) (minimum * kStep));
	SendMessageW(bar, TBM_SETRANGEMAX, TRUE, (LPARAM) (maximum * kStep));
	SendMessageW(bar, TBM_SETPAGESIZE, 0, (LPARAM) kStep);
	sliders.push_back(Slider{id, bar, child((HWND) window, L"STATIC", L"", SS_LEFT, 0)});
}

// each control where the layout puts it, all at once, the editor's window its size
void GuitarView::place(const Layout& layout) {
	HDWP moves = BeginDeferWindowPos(16);
	put(moves, load, layout.load);
	put(moves, profile, layout.profile);
	put(moves, refusal, layout.refusal);
	put(moves, notice, layout.notice);
	put(moves, update, layout.update);
	put(moves, quality, layout.quality);
	for (int row = 0; row < 3; row++) {
		put(moves, labels[row], layout.labels[row]);
	}
	for (size_t row = 0; row < sliders.size(); row++) {
		put(moves, sliders[row].bar, layout.sliders[row]);
		put(moves, sliders[row].value, layout.values[row]);
	}
	EndDeferWindowPos(moves);
	SetWindowPos((HWND) window, nullptr, 0, 0, layout.width, layout.height, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE);
}

tresult PLUGIN_API GuitarView::attached(void* parent, FIDString type) {
	registerClass();
	HWND editor = CreateWindowExW(WS_EX_CONTROLPARENT, kClass, L"Woodshed Guitar", WS_CHILD | WS_VISIBLE | WS_CLIPCHILDREN, 0, 0, kWidth, kHeight, (HWND) parent, nullptr, module(), nullptr);
	window = editor;
	load = child(editor, L"BUTTON", wide(say("editor.load")).c_str(), BS_PUSHBUTTON | WS_TABSTOP, kLoad);
	profile = child(editor, L"STATIC", L"", SS_LEFT | SS_ENDELLIPSIS, 0);
	refusal = child(editor, L"STATIC", L"", SS_LEFT, 0);
	notice = child(editor, L"STATIC", L"", SS_LEFT, 0);
	update = child(editor, L"BUTTON", wide(say("editor.update")).c_str(), BS_PUSHBUTTON | WS_TABSTOP, kUpdate);
	addSlider(0, kInputGain, "editor.input-gain", -24.0, 24.0);
	addSlider(1, kOutputLevel, "editor.output-level", -40.0, 12.0);
	labels[2] = child(editor, L"STATIC", wide(say("editor.quality")).c_str(), SS_LEFT, 0);
	quality = child(editor, WC_COMBOBOXW, L"", CBS_DROPDOWNLIST | WS_TABSTOP, kQualityBox);
	SendMessageW((HWND) quality, CB_ADDSTRING, 0, (LPARAM) wide(say("editor.quality.full")).c_str());
	SendMessageW((HWND) quality, CB_ADDSTRING, 0, (LPARAM) wide(say("editor.quality.light")).c_str());
	scale = (scale > 0 ? scale : GetDpiForWindow((HWND) parent) / 96.0);
	rescale();
	SetWindowLongPtrW(editor, GWLP_USERDATA, (LONG_PTR) this);
	plugin->editorOpened(this);
	refresh();
	return CPluginView::attached(parent, type);
}

tresult PLUGIN_API GuitarView::removed() {
	plugin->editorOpened(nullptr);
	if (window) {
		DestroyWindow((HWND) window);
	}
	window = load = profile = refusal = notice = update = quality = nullptr;
	labels[0] = labels[1] = labels[2] = nullptr;
	sliders.clear();
	return CPluginView::removed();
}

tresult PLUGIN_API GuitarView::setContentScaleFactor(ScaleFactor factor) {
	scale = factor;
	rescale();
	ViewRect size = getRect();
	if (window && plugFrame) {
		plugFrame->resizeView(this, &size);
	}
	return kResultOk;
}

// the size at the scale: the design's width, or more when the texts need it, and the design's height
void GuitarView::rescale() {
	HFONT made = messageFont((UINT) std::lround(USER_DEFAULT_SCREEN_DPI * scale));
	measured = measure(made);
	Layout natural = layOut(measured, (int) std::lround(kWidth * scale), scale);
	ViewRect size(0, 0, natural.width, natural.height);
	setRect(size);
	if (window) {
		EnumChildWindows((HWND) window, giveFont, (LPARAM) made);
		place(natural);
	}
	dropFont();
	font = made;
}

void GuitarView::dropFont() {
	if (font) {
		DeleteObject((HGDIOBJ) font);
	}
	font = nullptr;
}

bool GuitarView::labelsHold() const {
	return widthOf(labels[0]) >= measured.inputLabel && widthOf(labels[1]) >= measured.outputLabel && widthOf(labels[2]) >= measured.qualityLabel;
}

std::string GuitarView::laidOut() const {
	RECT inside = {};
	GetClientRect((HWND) window, &inside);
	return "scale=" + std::to_string(scale) + " dpi=" + std::to_string(GetDpiForWindow((HWND) window)) + " size=" + std::to_string(inside.right) + "x"
		+ std::to_string(inside.bottom) + " text=" + std::to_string(emHeight((void*) SendMessageW((HWND) labels[0], WM_GETFONT, 0, 0))) + " sliders=" + std::to_string(widthOf(sliders[0].bar)) + ","
		+ std::to_string(widthOf(sliders[1].bar)) + " labels-fit=" + (labelsHold() ? "true" : "false");
}

void GuitarView::changeDpi(int64 dpi) {
	TuxGuitarTest::changeDpi(GetAncestor((HWND) window, GA_ROOT), dpi);
}

void GuitarView::dragTo(int64 width, int64 height) {
	TuxGuitarTest::dragTo(GetAncestor((HWND) window, GA_ROOT), width, height);
}

void GuitarView::refresh() {
	if (!window) {
		return;
	}
	ProfileStatus status = plugin->status();
	SetWindowTextW((HWND) profile, wide(profileText(status)).c_str());
	SetWindowTextW((HWND) refusal, wide(refusalText(status)).c_str());
	SetWindowTextW((HWND) notice, wide(status.notice).c_str());
	ShowWindow((HWND) update, (status.notice.empty() ? SW_HIDE : SW_SHOW));
	for (const Slider& slider : sliders) {
		double value = plugin->parameter(slider.id);
		SendMessageW((HWND) slider.bar, TBM_SETPOS, TRUE, (LPARAM) (value * kStep + (value < 0 ? -0.5 : 0.5)));
		SetWindowTextW((HWND) slider.value, wide(decibels(value)).c_str());
	}
	SendMessageW((HWND) quality, CB_SETCURSEL, (WPARAM) plugin->parameter(kQuality), 0);
}

// a slider moved by the user: its value edited, the edit begun on its first move and ended with the move
void GuitarView::slid(void* bar, bool ended) {
	for (const Slider& slider : sliders) {
		if (slider.bar == bar) {
			double value = (double) SendMessageW((HWND) bar, TBM_GETPOS, 0, 0) / kStep;
			plugin->edit(slider.id, value, (editing == slider.id ? GuitarPlugin::kGoingOn : GuitarPlugin::kBegun));
			editing = slider.id;
			SetWindowTextW((HWND) slider.value, wide(decibels(value)).c_str());
			if (ended) {
				plugin->edit(slider.id, value, GuitarPlugin::kEnded);
				editing = kNoEdit;
			}
		}
	}
}

// Quality chosen: one edit
void GuitarView::chose() {
	double index = (double) SendMessageW((HWND) quality, CB_GETCURSEL, 0, 0);
	plugin->edit(kQuality, index, GuitarPlugin::kBegun);
	plugin->edit(kQuality, index, GuitarPlugin::kEnded);
}

// as the user moves the parameter's control to the value: the control set, then its move told to the window
void GuitarView::move(ParamID id, double value) {
	if (!window) {
		return;
	}
	if (id == kQuality) {
		SendMessageW((HWND) quality, CB_SETCURSEL, (WPARAM) value, 0);
		SendMessageW((HWND) window, WM_COMMAND, MAKEWPARAM(kQualityBox, CBN_SELCHANGE), (LPARAM) quality);
		return;
	}
	for (const Slider& slider : sliders) {
		if (slider.id == id) {
			LPARAM position = (LPARAM) (value * kStep + (value < 0 ? -0.5 : 0.5));
			SendMessageW((HWND) slider.bar, TBM_SETPOS, TRUE, position);
			SendMessageW((HWND) window, WM_HSCROLL, MAKEWPARAM(TB_THUMBTRACK, (WORD) position), (LPARAM) slider.bar);
			SendMessageW((HWND) window, WM_HSCROLL, MAKEWPARAM(TB_ENDTRACK, 0), (LPARAM) slider.bar);
		}
	}
}

std::string GuitarView::shown() const {
	std::string button = (IsWindowVisible((HWND) update) ? textOf(update) : std::string());
	return textOf(profile) + "\n" + textOf(refusal) + "\n" + textOf(notice) + "\n" + button;
}

// only a shown button is clicked, as the user can; its click told to the window
std::string GuitarView::click(const std::string& text) {
	if (!window || !IsWindowVisible((HWND) update) || textOf(update) != text) {
		return std::string();
	}
	SendMessageW((HWND) window, WM_COMMAND, MAKEWPARAM(kUpdate, BN_CLICKED), (LPARAM) update);
	return text;
}

void GuitarView::updateFromFile() {
	plugin->updateFromFile();
}

// as the controls read: "Input gain 0.0 dB, Output level 0.0 dB, Quality Full"
std::string GuitarView::controls() const {
	std::string text;
	const char* names[] = {"editor.input-gain", "editor.output-level"};
	for (size_t i = 0; i < sliders.size(); i++) {
		text += say(names[i]) + " " + decibels((double) SendMessageW((HWND) sliders[i].bar, TBM_GETPOS, 0, 0) / kStep) + ", ";
	}
	return text + say("editor.quality") + " " + say(SendMessageW((HWND) quality, CB_GETCURSEL, 0, 0) == 1 ? "editor.quality.light" : "editor.quality.full");
}

void GuitarView::chooseProfile() {
	wchar_t file[32768] = {0};
	OPENFILENAMEW dialog = {};
	dialog.lStructSize = sizeof(dialog);
	dialog.hwndOwner = GetAncestor((HWND) window, GA_ROOT);
	// pairs of a name and its pattern, each ending in a null, the last pair followed by another
	std::wstring filter = wide(say("dialog.captures")) + std::wstring(L"\0*.nam\0", 7) + wide(say("dialog.all")) + std::wstring(L"\0*.*\0\0", 6);
	std::wstring title = wide(say("dialog.title"));
	dialog.lpstrFilter = filter.c_str();
	dialog.lpstrFile = file;
	dialog.nMaxFile = (DWORD) (sizeof(file) / sizeof(file[0]));
	dialog.lpstrTitle = title.c_str();
	dialog.Flags = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST | OFN_EXPLORER;
	if (GetOpenFileNameW(&dialog)) {
		plugin->loadProfile(utf8(file));
	}
	refresh();
}

#else

bool hasEditor() {
	return false;
}

tresult PLUGIN_API GuitarView::isPlatformTypeSupported(FIDString) {
	return kResultFalse;
}

tresult PLUGIN_API GuitarView::attached(void* parent, FIDString type) {
	return CPluginView::attached(parent, type);
}

tresult PLUGIN_API GuitarView::removed() {
	return CPluginView::removed();
}

void GuitarView::addSlider(int, ParamID, const char*, double, double) {}

void GuitarView::place(const Layout&) {}

tresult PLUGIN_API GuitarView::setContentScaleFactor(ScaleFactor factor) {
	scale = factor;
	return kResultOk;
}

void GuitarView::rescale() {}

void GuitarView::dropFont() {}

bool GuitarView::labelsHold() const {
	return false;
}

std::string GuitarView::laidOut() const {
	return std::string();
}

void GuitarView::changeDpi(int64) {}

void GuitarView::dragTo(int64, int64) {}

void GuitarView::refresh() {}

void GuitarView::slid(void*, bool) {}

void GuitarView::chose() {}

void GuitarView::move(ParamID, double) {}

std::string GuitarView::shown() const {
	return std::string();
}

std::string GuitarView::click(const std::string&) {
	return std::string();
}

void GuitarView::updateFromFile() {}

std::string GuitarView::controls() const {
	return std::string();
}

void GuitarView::chooseProfile() {}

#endif

GuitarView::GuitarView(GuitarPlugin* plugin) : CPluginView(nullptr), plugin(plugin) {
	ViewRect size(0, 0, kWidth, kHeight);
	setRect(size);
}

GuitarView::~GuitarView() {
	dropFont();
}

}  // namespace Woodshed
