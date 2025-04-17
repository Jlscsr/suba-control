#include <napi.h>
#include <windows.h>
#include <thread>

Napi::ThreadSafeFunction tsfn;

// Mouse hook procedure: sends an event for mouse moves and left-button clicks.
LRESULT CALLBACK MouseHookProc(int nCode, WPARAM wParam, LPARAM lParam)
{
    if (nCode == HC_ACTION)
    {
        MSLLHOOKSTRUCT *pMouseStruct = reinterpret_cast<MSLLHOOKSTRUCT *>(lParam);
        if (pMouseStruct)
        {
            // Create a callback that sends a JavaScript object
            if (wParam == WM_MOUSEMOVE)
            {
                tsfn.BlockingCall([=](Napi::Env env, Napi::Function jsCallback)
                                  {
          Napi::Object event = Napi::Object::New(env);
          event.Set("type", "move");
          event.Set("x", Napi::Number::New(env, pMouseStruct->pt.x));
          event.Set("y", Napi::Number::New(env, pMouseStruct->pt.y));
          jsCallback.Call({ event }); });
            }
            else if (wParam == WM_LBUTTONDOWN)
            {
                tsfn.BlockingCall([=](Napi::Env env, Napi::Function jsCallback)
                                  {
          Napi::Object event = Napi::Object::New(env);
          event.Set("type", "click");
          event.Set("x", Napi::Number::New(env, pMouseStruct->pt.x));
          event.Set("y", Napi::Number::New(env, pMouseStruct->pt.y));
          jsCallback.Call({ event }); });
            }
        }
    }
    return CallNextHookEx(NULL, nCode, wParam, lParam);
}

HHOOK hook;

// This thread installs the low-level mouse hook and enters a message loop.
DWORD WINAPI HookThread(LPVOID lpParam)
{
    HINSTANCE hInstance = GetModuleHandle(NULL);
    hook = SetWindowsHookEx(WH_MOUSE_LL, MouseHookProc, hInstance, 0);
    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0))
    {
        // Process messages if needed (empty loop works fine for the hook)
    }
    return 0;
}

// JavaScript binding: startHook(callback)
// Expects a function which receives an event object from the native hook.
Napi::Value StartHook(const Napi::CallbackInfo &info)
{
    Napi::Env env = info.Env();
    if (!info[0].IsFunction())
    {
        Napi::TypeError::New(env, "Function expected as first argument").ThrowAsJavaScriptException();
        return env.Null();
    }
    Napi::Function callback = info[0].As<Napi::Function>();

    tsfn = Napi::ThreadSafeFunction::New(
        env,
        callback,
        "MouseHookCallback",
        0,
        1);

    std::thread([]()
                { HookThread(NULL); })
        .detach();

    return env.Undefined();
}

// Initialize and export the native module.
Napi::Object Init(Napi::Env env, Napi::Object exports)
{
    exports.Set("startHook", Napi::Function::New(env, StartHook));
    return exports;
}

NODE_API_MODULE(mousehook, Init)
