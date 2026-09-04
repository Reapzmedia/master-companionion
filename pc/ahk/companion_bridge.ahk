#Requires AutoHotkey v2.0

; ═══════════════════════════════════════════════════════════════════
; Master Companion — PC Macro Bridge (AutoHotkey v2)
; Intercepts physical macro keypad keystrokes and sends silent HTTP
; commands to the Master Companion Android application.
; ═══════════════════════════════════════════════════════════════════

; ═══════════════════════════════════════════════════════════════════
; CONFIGURATION — Edit these values to match your setup
; ═══════════════════════════════════════════════════════════════════

; Android device IP (displayed on app System page or Onboarding)
; When connected via USB with "adb reverse", use "127.0.0.1"
global ANDROID_IP := "192.168.1.42"

; Command Bridge HTTP port (default: 8420, must match app Settings)
global ANDROID_PORT := 8420

; Auth token (copy from app: Settings > Network > Auth Token > Copy)
global AUTH_TOKEN := "master-companion-default-token"

; ═══════════════════════════════════════════════════════════════════
; KEY BINDINGS — Map your physical macro keypad keys to actions
; Common macro keys: F13-F24, Numpad keys, or custom scan codes.
; Use AHK's Key History (Window Menu > View > Key History) to find codes.
; ═══════════════════════════════════════════════════════════════════

; Primary Macro Keys (F13 - F18)
F13::SendCommand("toggle_charge_limit", '{"enabled": true}')
F14::SendCommand("navigate", '{"page": "audio"}')
F15::SendCommand("navigate", '{"page": "home"}')
F16::SendCommand("wol", '{}')
F17::SendCommand("audio_toggle", '{}')
F18::SendCommand("navigate", '{"page": "system"}')

; Secondary Macro Keys (F19 - F24)
F19::SendCommand("spotify_play_pause", '{}')
F20::SendCommand("spotify_next", '{}')
F21::SendCommand("spotify_prev", '{}')
F22::SendCommand("set_brightness", '{"value": 128}')
F23::SendCommand("set_brightness", '{"value": 255}')
F24::SendCommand("navigate", '{"page": "settings"}')

; Rotary Volume Knob Bindings (Optional: adjust hotkeys to match your hardware rotary encoder)
; Example for macro pad rotary knobs sending custom keys or scan codes:
; ^!WheelUp::SendCommand("volume_up", '{"step": 5}')
; ^!WheelDown::SendCommand("volume_down", '{"step": 5}')
; NumpadAdd::SendCommand("volume_up", '{"step": 5}')
; NumpadSub::SendCommand("volume_down", '{"step": 5}')

; ═══════════════════════════════════════════════════════════════════
; CORE FUNCTIONALITY — HTTP Dispatch & Status Feedback
; ═══════════════════════════════════════════════════════════════════

SendCommand(action, paramsJson := "{}") {
    url := "http://" ANDROID_IP ":" ANDROID_PORT "/command"
    body := '{"action":"' action '","params":' paramsJson '}'

    try {
        http := ComObject("WinHttp.WinHttpRequest.5.1")
        http.Open("POST", url, true) ; Asynchronous request for zero UI lag
        http.SetRequestHeader("Content-Type", "application/json")
        http.SetRequestHeader("X-Auth-Token", AUTH_TOKEN)
        http.SetTimeouts(2000, 2000, 2000, 2000) ; 2s timeout
        http.Send(body)
        http.WaitForResponse(2)

        if (http.Status == 200) {
            ; Success - silent or subtle tooltip
        } else if (http.Status == 401) {
            ToolTip("⚠ Auth failed — check token in AHK script", , , 2)
            SetTimer(() => ToolTip(, , , 2), -3000)
        } else {
            ToolTip("⚠ Command failed: " http.Status, , , 2)
            SetTimer(() => ToolTip(, , , 2), -2000)
        }
    } catch as e {
        ToolTip("❌ Connection error: " e.Message "`nTarget: " ANDROID_IP ":" ANDROID_PORT, , , 2)
        SetTimer(() => ToolTip(, , , 2), -3000)
    }
}

CheckConnection() {
    url := "http://" ANDROID_IP ":" ANDROID_PORT "/ping"
    try {
        http := ComObject("WinHttp.WinHttpRequest.5.1")
        http.Open("GET", url, true)
        http.SetTimeouts(2000, 2000, 2000, 2000)
        http.Send()
        http.WaitForResponse(2)
        if (http.Status == 200) {
            ToolTip("✅ Connected to Master Companion (" ANDROID_IP ")", , , 1)
        } else {
            ToolTip("⚠ Master Companion responded with status: " http.Status, , , 1)
        }
    } catch {
        ToolTip("❌ Master Companion offline at " ANDROID_IP ":" ANDROID_PORT, , , 1)
    }
    SetTimer(() => ToolTip(, , , 1), -4000)
}

; Run health check on script startup
CheckConnection()
