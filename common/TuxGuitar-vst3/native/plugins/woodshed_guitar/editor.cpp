#include "editor.h"

#include "plugin.h"

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
	return (status.name.empty() ? std::string("No profile: the dry guitar is heard") : "Profile: " + status.name);
}

// a capture being loaded is told in place of the last refusal
std::string refusalText(const ProfileStatus& status) {
	if (!status.loading.empty()) {
		return "Loading " + status.loading + "...";
	}
	return (status.refusal.empty() ? std::string() : "Not loaded: " + status.refusal);
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

HWND child(HWND parent, const wchar_t* type, const wchar_t* text, DWORD style, int x, int y, int width, int height, int id) {
	HWND control = CreateWindowExW(0, type, text, WS_CHILD | WS_VISIBLE | style, x, y, width, height, parent, (HMENU) (INT_PTR) id, module(), nullptr);
	SendMessageW(control, WM_SETFONT, (WPARAM) GetStockObject(DEFAULT_GUI_FONT), TRUE);
	return control;
}

}  // namespace

bool hasEditor() {
	return true;
}

tresult PLUGIN_API GuitarView::isPlatformTypeSupported(FIDString type) {
	return (std::strcmp(type, kPlatformTypeHWND) == 0 ? kResultTrue : kResultFalse);
}

// a row: the parameter's name, its slider over its range (tenths of a dB), its value
void GuitarView::addSlider(int row, ParamID id, const wchar_t* name, double minimum, double maximum) {
	int y = 118 + row * 34;
	child((HWND) window, L"STATIC", name, SS_LEFT, 12, y + 4, 90, 20, 0);
	HWND bar = child((HWND) window, TRACKBAR_CLASSW, L"", TBS_HORZ | TBS_NOTICKS | WS_TABSTOP, 104, y, 260, 28, (int) (10 + id));
	SendMessageW(bar, TBM_SETRANGEMIN, FALSE, (LPARAM) (minimum * kStep));
	SendMessageW(bar, TBM_SETRANGEMAX, TRUE, (LPARAM) (maximum * kStep));
	SendMessageW(bar, TBM_SETPAGESIZE, 0, (LPARAM) kStep);
	sliders.push_back(Slider{id, bar, child((HWND) window, L"STATIC", L"", SS_LEFT, 372, y + 4, 76, 20, 0)});
}

tresult PLUGIN_API GuitarView::attached(void* parent, FIDString type) {
	registerClass();
	HWND editor = CreateWindowExW(WS_EX_CONTROLPARENT, kClass, L"Woodshed Guitar", WS_CHILD | WS_VISIBLE | WS_CLIPCHILDREN, 0, 0, kWidth, kHeight, (HWND) parent, nullptr, module(), nullptr);
	window = editor;
	child(editor, L"BUTTON", L"Load profile...", BS_PUSHBUTTON | WS_TABSTOP, 12, 12, 120, 28, kLoad);
	profile = child(editor, L"STATIC", L"", SS_LEFT | SS_ENDELLIPSIS, 144, 18, kWidth - 156, 20, 0);
	refusal = child(editor, L"STATIC", L"", SS_LEFT, 12, 46, kWidth - 24, 34, 0);
	notice = child(editor, L"STATIC", L"", SS_LEFT, 12, 86, kWidth - 156, 28, 0);
	update = child(editor, L"BUTTON", L"Update from file", BS_PUSHBUTTON | WS_TABSTOP, kWidth - 132, 82, 120, 28, kUpdate);
	addSlider(0, kInputGain, L"Input gain", -24.0, 24.0);
	addSlider(1, kOutputLevel, L"Output level", -40.0, 12.0);
	child(editor, L"STATIC", L"Quality", SS_LEFT, 12, 190, 90, 20, 0);
	quality = child(editor, WC_COMBOBOXW, L"", CBS_DROPDOWNLIST | WS_TABSTOP, 104, 186, 120, 80, kQualityBox);
	SendMessageW((HWND) quality, CB_ADDSTRING, 0, (LPARAM) L"Full");
	SendMessageW((HWND) quality, CB_ADDSTRING, 0, (LPARAM) L"Light");
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
	window = profile = refusal = notice = update = quality = nullptr;
	sliders.clear();
	return CPluginView::removed();
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
	const char* names[] = {"Input gain ", "Output level "};
	for (size_t i = 0; i < sliders.size(); i++) {
		text += names[i] + decibels((double) SendMessageW((HWND) sliders[i].bar, TBM_GETPOS, 0, 0) / kStep) + ", ";
	}
	return text + "Quality " + (SendMessageW((HWND) quality, CB_GETCURSEL, 0, 0) == 1 ? "Light" : "Full");
}

void GuitarView::chooseProfile() {
	wchar_t file[32768] = {0};
	OPENFILENAMEW dialog = {};
	dialog.lStructSize = sizeof(dialog);
	dialog.hwndOwner = GetAncestor((HWND) window, GA_ROOT);
	dialog.lpstrFilter = L"NAM captures (*.nam)\0*.nam\0All files (*.*)\0*.*\0";
	dialog.lpstrFile = file;
	dialog.nMaxFile = (DWORD) (sizeof(file) / sizeof(file[0]));
	dialog.lpstrTitle = L"Load profile";
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

void GuitarView::addSlider(int, ParamID, const wchar_t*, double, double) {}

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

GuitarView::~GuitarView() = default;

}  // namespace Woodshed
