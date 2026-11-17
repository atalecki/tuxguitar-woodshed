#include "editor.h"

#include "plugin.h"

#include <cstring>
#include <string>

#ifdef _WIN32
#include <windows.h>
#include <commdlg.h>
#endif

namespace Woodshed {

using namespace Steinberg;

namespace {

const int kWidth = 460;
const int kHeight = 112;

// what the editor says of the profile and of the last refusal
std::string profileText(const ProfileStatus& status) {
	return (status.name.empty() ? std::string("No profile: the dry guitar is heard") : "Profile: " + status.name);
}

std::string refusalText(const ProfileStatus& status) {
	return (status.refusal.empty() ? std::string() : "Not loaded: " + status.refusal);
}

}  // namespace

#ifdef _WIN32

namespace {

const wchar_t kClass[] = L"WoodshedGuitarEditor";
const int kLoad = 1;

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

LRESULT CALLBACK editorProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	if (message == WM_COMMAND && LOWORD(wParam) == kLoad && HIWORD(wParam) == BN_CLICKED) {
		GuitarView* view = reinterpret_cast<GuitarView*>(GetWindowLongPtrW(window, GWLP_USERDATA));
		if (view) {
			view->chooseProfile();
		}
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

tresult PLUGIN_API GuitarView::attached(void* parent, FIDString type) {
	registerClass();
	HWND editor = CreateWindowExW(WS_EX_CONTROLPARENT, kClass, L"Woodshed Guitar", WS_CHILD | WS_VISIBLE | WS_CLIPCHILDREN, 0, 0, kWidth, kHeight, (HWND) parent, nullptr, module(), nullptr);
	SetWindowLongPtrW(editor, GWLP_USERDATA, (LONG_PTR) this);
	child(editor, L"BUTTON", L"Load profile...", BS_PUSHBUTTON | WS_TABSTOP, 12, 12, 120, 28, kLoad);
	profile = child(editor, L"STATIC", L"", SS_LEFT | SS_ENDELLIPSIS, 144, 18, kWidth - 156, 20, 0);
	refusal = child(editor, L"STATIC", L"", SS_LEFT, 12, 52, kWidth - 24, kHeight - 60, 0);
	window = editor;
	plugin->editorOpened(this);
	refresh();
	return CPluginView::attached(parent, type);
}

tresult PLUGIN_API GuitarView::removed() {
	plugin->editorOpened(nullptr);
	if (window) {
		DestroyWindow((HWND) window);
	}
	window = profile = refusal = nullptr;
	return CPluginView::removed();
}

void GuitarView::refresh() {
	if (!window) {
		return;
	}
	ProfileStatus status = plugin->status();
	SetWindowTextW((HWND) profile, wide(profileText(status)).c_str());
	SetWindowTextW((HWND) refusal, wide(refusalText(status)).c_str());
}

std::string GuitarView::shown() const {
	wchar_t line[2048];
	GetWindowTextW((HWND) profile, line, 2048);
	std::string text = utf8(line);
	GetWindowTextW((HWND) refusal, line, 2048);
	return text + "\n" + utf8(line);
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

void GuitarView::refresh() {}

void GuitarView::chooseProfile() {}

std::string GuitarView::shown() const {
	return std::string();
}

#endif

GuitarView::GuitarView(GuitarPlugin* plugin) : CPluginView(nullptr), plugin(plugin) {
	ViewRect size(0, 0, kWidth, kHeight);
	setRect(size);
}

GuitarView::~GuitarView() = default;

}  // namespace Woodshed
