#include "wake.h"

#ifdef _WIN32
#include <windows.h>
#endif

namespace Woodshed {

#ifdef _WIN32

namespace {

const wchar_t kClass[] = L"WoodshedGuitarWake";
const UINT kAct = WM_APP + 7;

LRESULT CALLBACK wakeProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam);

HINSTANCE module() {
	HMODULE handle = nullptr;
	GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT, (LPCWSTR) &wakeProc, &handle);
	return handle;
}

}  // namespace

struct WakeWindow {
	static void act(HWND window) {
		Wake* wake = reinterpret_cast<Wake*>(GetWindowLongPtrW(window, GWLP_USERDATA));
		if (wake) {
			wake->act();
		}
	}
};

namespace {

LRESULT CALLBACK wakeProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
	if (message == kAct) {
		WakeWindow::act(window);
		return 0;
	}
	return DefWindowProcW(window, message, wParam, lParam);
}

}  // namespace

Wake::Wake(std::function<void()> act) : act(std::move(act)) {
	WNDCLASSW type = {};
	if (!GetClassInfoW(module(), kClass, &type)) {
		type.lpfnWndProc = wakeProc;
		type.hInstance = module();
		type.lpszClassName = kClass;
		RegisterClassW(&type);
	}
	window = CreateWindowExW(0, kClass, L"", 0, 0, 0, 0, 0, HWND_MESSAGE, nullptr, module(), nullptr);
	SetWindowLongPtrW((HWND) window, GWLP_USERDATA, (LONG_PTR) this);
}

Wake::~Wake() {
	DestroyWindow((HWND) window);
}

void Wake::post() {
	PostMessageW((HWND) window, kAct, 0, 0);
}

#else

Wake::Wake(std::function<void()> act) : act(std::move(act)) {}

Wake::~Wake() = default;

void Wake::post() {}

#endif

}  // namespace Woodshed
