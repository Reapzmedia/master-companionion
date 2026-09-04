# Software Requirements Specification (SRS)
## Master Companion — Standby Dashboard & PC Bridge
**Version:** 1.1  
**Date:** 2026-09-04  
**Status:** Draft  

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-09-04 | Initial draft |
| 1.1 | 2026-09-04 | Added Android 9 / API 28 compatibility, Huawei EMUI handling, non-root fallbacks, error handling specification, expanded data models, logging architecture |

---

## 1. System Overview

Master Companion is a two-node distributed system:
- **Android Application** (the Dashboard) — runs on the phone.
- **PC-Side Utilities** — AutoHotkey scripts for macro bridge, Python audio streamer.

All communication is local-network-only (no cloud services beyond Spotify Web API for metadata).

### 1.1 Minimum Platform Requirements

| Platform | Minimum | Recommended |
|----------|---------|-------------|
| Android API Level | 28 (Android 9 Pie) | 34 (Android 14) |
| Kotlin | 1.9.x | 1.9.x |
| Jetpack Compose Compiler | Compatible with Kotlin 1.9.x | Latest stable |
| JVM Target | 1.8 (for API 28 compat) | 17 |
| Gradle | 8.4+ | 8.6+ |
| AGP | 8.2+ | 8.4+ |

---

## 2. System Architecture

### 2.1 High-Level Component Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                        LOCAL NETWORK (Wi-Fi / USB)                   │
│                                                                      │
│  ┌─────────────────────┐              ┌────────────────────────────┐ │
│  │     DESKTOP PC      │              │   ANDROID DEVICE           │ │
│  │                     │              │   (Pixel 7 Pro / P20 Lite) │ │
│  │  ┌───────────────┐  │  HTTP POST   │  ┌──────────────────────┐  │ │
│  │  │  AutoHotkey   │──┼──────────────┼──│  Ktor HTTP Server    │  │ │
│  │  │  Macro Bridge │  │  /command    │  │  (Port 8420)         │  │ │
│  │  └───────────────┘  │              │  └──────────┬───────────┘  │ │
│  │                     │              │             │              │ │
│  │  ┌───────────────┐  │  UDP Audio   │  ┌──────────▼───────────┐  │ │
│  │  │  Python Audio │──┼──────────────┼──│  Audio Receiver      │  │ │
│  │  │  Streamer     │  │  (Port 8421) │  │  (AudioTrack)        │  │ │
│  │  └───────────────┘  │              │  └──────────────────────┘  │ │
│  │                     │              │                            │ │
│  │  ┌───────────────┐  │  UDP WOL     │  ┌──────────────────────┐  │ │
│  │  │  NIC (WOL     │◄─┼──────────────┼──│  WOL Sender          │  │ │
│  │  │  Enabled)     │  │  Magic Pkt   │  └──────────────────────┘  │ │
│  │  └───────────────┘  │              │                            │ │
│  │                     │              │  ┌──────────────────────┐  │ │
│  │                     │              │  │  Spotify API Client  │──┼─┼── api.spotify.com
│  │  ┌───────────────┐  │              │  │  (OAuth PKCE)        │  │ │
│  │  │  USB Macro    │  │              │  └──────────────────────┘  │ │
│  │  │  Keypad       │  │              │                            │ │
│  │  └───────┬───────┘  │              │  ┌──────────────────────┐  │ │
│  │          │ HID      │              │  │  Battery Manager     │  │ │
│  │          ▼          │              │  │  (sysfs / API)       │  │ │
│  │  (AHK intercepts)  │              │  └──────────────────────┘  │ │
│  └─────────────────────┘              └────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 Android Application Architecture (MVVM + Clean Architecture)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │  Home Page    │  │  Audio Page  │  │  System Page │  │  Settings  │  │
│  │  Composables  │  │  Composables │  │  Composables │  │  Page      │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬──────┘  │
│         └──────────────────┴────────────────┬┴────────────────┘         │
│                                             │                           │
│  ┌──────────────────────────────────────────▼────────────────────────┐  │
│  │                    ViewModels (Hilt-injected)                     │  │
│  │  HomeVM  │  AudioVM  │  SystemVM  │  SettingsVM  │  OnboardingVM │  │
│  └──────────────────────────────────────────┬────────────────────────┘  │
├─────────────────────────────────────────────┼───────────────────────────┤
│                   Domain Layer              │                           │
│  ┌──────────────────────────────────────────▼────────────────────────┐  │
│  │                       Use Cases                                   │  │
│  │  GetCurrentTrack    │  ToggleChargeLimit  │  ExecuteCommand       │  │
│  │  SendWolPacket      │  ReceiveAudio       │  CheckRootAccess      │  │
│  │  GetBatteryInfo     │  RefreshSpotifyToken │  DetectDeviceType    │  │
│  └──────────────────────────────────────────┬────────────────────────┘  │
├─────────────────────────────────────────────┼───────────────────────────┤
│                    Data Layer               │                           │
│  ┌──────────────┐ ┌───────────────┐ ┌───────▼──────┐ ┌──────────────┐  │
│  │ SpotifyRepo  │ │ BatteryRepo   │ │ CommandRepo  │ │ AudioRepo    │  │
│  │ (Retrofit)   │ │ (sysfs + API) │ │ (JSON file)  │ │ (UDP socket) │  │
│  └──────────────┘ └───────────────┘ └──────────────┘ └──────────────┘  │
│  ┌──────────────┐ ┌───────────────┐ ┌──────────────┐                   │
│  │ NetworkRepo  │ │ PrefsRepo     │ │ DeviceRepo   │                   │
│  │ (WOL/UDP)    │ │ (DataStore)   │ │ (Build info) │                   │
│  └──────────────┘ └───────────────┘ └──────────────┘                   │
├─────────────────────────────────────────────────────────────────────────┤
│                      Infrastructure                                     │
│  ┌──────────────┐  ┌──────────────────┐  ┌───────────────────────────┐ │
│  │ Ktor Server  │  │ Foreground Svcs   │  │ Root Shell Executor      │ │
│  │ (HTTP input) │  │ (Battery, Bridge, │  │ (with fallback to        │ │
│  │              │  │  Audio)           │  │  non-root BatteryManager)│ │
│  └──────────────┘  └──────────────────┘  └───────────────────────────┘ │
│  ┌──────────────┐  ┌──────────────────┐                                │
│  │ EMUI Compat  │  │ Device Detector  │                                │
│  │ Layer        │  │ (OEM, root, API) │                                │
│  └──────────────┘  └──────────────────┘                                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. API 28 (Android 9) Compatibility Notes

### 3.1 Foreground Services

**API 28 (Android 9):** Foreground services use `startForeground(id, notification)` without a `foregroundServiceType` parameter. The `FOREGROUND_SERVICE_TYPE_*` constants were added in API 29.

```kotlin
// Compatibility pattern
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
} else {
    startForeground(NOTIFICATION_ID, notification)
}
```

**Manifest:** On API 28, `android:foregroundServiceType` attribute is ignored. Include it for API 29+ but wrap the service declaration properly:

```xml
<service
    android:name=".service.BatteryGuardService"
    android:foregroundServiceType="specialUse"
    android:exported="false" />
<!-- foregroundServiceType is ignored on API < 29 but doesn't cause errors -->
```

### 3.2 Immersive Mode / System UI

**API 28:** Must use deprecated `systemUiVisibility` flags. `WindowInsetsController` (API 30+) is unavailable.

```kotlin
fun Activity.enterImmersiveMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }
}
```

### 3.3 Display Cutout (Notch)

**API 28:** Added `DisplayCutout` API and `layoutInDisplayCutoutMode`. The Huawei P20 Lite has a notch.

```kotlin
// In Activity.onCreate or theme
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
}
```

In Compose, use `WindowInsets.displayCutout` to get the cutout bounds and add padding.

### 3.4 Notification Channels

**API 26+:** Notification channels are required. This is fully supported on API 28.

```kotlin
val channels = listOf(
    NotificationChannelConfig("battery_guard", "Battery Guard", NotificationManager.IMPORTANCE_LOW),
    NotificationChannelConfig("command_bridge", "Command Bridge", NotificationManager.IMPORTANCE_LOW),
    NotificationChannelConfig("audio_stream", "Audio Stream", NotificationManager.IMPORTANCE_LOW)
)
```

### 3.5 EncryptedSharedPreferences

`androidx.security:security-crypto:1.1.0-alpha06` supports API 23+. No compatibility issues on API 28.

### 3.6 Jetpack Compose on API 28

Jetpack Compose supports API 21+. No issues on API 28, but:
- `basicMarquee()` modifier requires Compose Foundation 1.6.0+.
- Some Material 3 components may have rendering differences on non-AMOLED displays (P20 Lite is IPS LCD — pure black `#000000` has no power benefit but is still visually consistent).

### 3.7 Network Security Config

On API 28+, cleartext HTTP is blocked by default. The embedded server communicates over localhost and LAN, so cleartext must be explicitly allowed:

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">192.168.0.0/16</domain>
        <domain includeSubdomains="true">10.0.0.0/8</domain>
        <domain includeSubdomains="true">172.16.0.0/12</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

---

## 4. Component Specifications

### 4.1 Spotify API Client

#### Authentication

| Property | Value |
|----------|-------|
| Flow | OAuth 2.0 Authorization Code with PKCE (RFC 7636) |
| Authorize URL | `https://accounts.spotify.com/authorize` |
| Token URL | `https://accounts.spotify.com/api/token` |
| Redirect URI | `mastercompanion://spotify/callback` (deep link) |
| Code Verifier | 43–128 char random string (A-Z, a-z, 0-9, `-._~`) |
| Code Challenge | Base64URL-encoded SHA-256 of code verifier |
| Scopes | `user-read-currently-playing`, `user-read-recently-played`, `user-read-playback-state`, `user-modify-playback-state` |
| Token Storage | Access token: in-memory `MutableStateFlow`. Refresh token: `EncryptedSharedPreferences`. |
| Auto-Refresh | Trigger when `System.currentTimeMillis() >= tokenExpiryTime - 60_000` |

#### Fallback Strategy for Custom Tabs (Huawei / older devices)

```kotlin
fun openSpotifyAuth(context: Context, authUrl: String) {
    try {
        // Attempt Custom Tabs first
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
    } catch (e: Exception) {
        // Fallback to standard browser intent
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        if (browserIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(browserIntent)
        } else {
            // Last resort: embedded WebView (least preferred)
            // Navigate to WebView-based auth screen
        }
    }
}
```

#### Polling Endpoints

| Endpoint | Method | Interval | Fallback | Notes |
|----------|--------|----------|----------|-------|
| `/v1/me/player/currently-playing` | GET | 3s (Pixel) / 5s (P20 Lite) | → `recently-played` | Returns 204 when nothing playing |
| `/v1/me/player/recently-played?limit=1` | GET | On 204 | → Idle screen | — |
| `/v1/me/player/play` | PUT | On user action | — | Requires Premium |
| `/v1/me/player/pause` | PUT | On user action | — | Requires Premium |
| `/v1/me/player/next` | POST | On user action | — | Requires Premium |
| `/v1/me/player/previous` | POST | On user action | — | Requires Premium |

#### Rate Limiting

Spotify enforces rate limits (varies, typically ~180 requests/minute). The app must:
1. Track request timestamps.
2. On `429 Too Many Requests`, read `Retry-After` header and back off.
3. Log rate limit events.

#### Data Models

```kotlin
@Serializable
data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<String>,
    val albumName: String,
    val albumArtUrl: String,
    val durationMs: Long,
    val progressMs: Long,
    val isPlaying: Boolean,
    val isRecentlyPlayed: Boolean = false
)

@Serializable
data class CurrentlyPlayingResponse(
    val is_playing: Boolean,
    val item: TrackItem?,
    val progress_ms: Long?
)

@Serializable
data class TrackItem(
    val id: String,
    val name: String,
    val artists: List<ArtistItem>,
    val album: AlbumItem,
    val duration_ms: Long
)

@Serializable
data class ArtistItem(val name: String)

@Serializable
data class AlbumItem(
    val name: String,
    val images: List<ImageItem>
)

@Serializable
data class ImageItem(
    val url: String,
    val width: Int?,
    val height: Int?
)

@Serializable
data class RecentlyPlayedResponse(
    val items: List<PlayHistoryItem>
)

@Serializable
data class PlayHistoryItem(
    val track: TrackItem,
    val played_at: String
)
```

---

### 4.2 Battery Manager

#### Dual-Mode Architecture

The battery module operates in two modes, selected at runtime:

```
┌─────────────────────────────────────┐
│         BatteryRepository            │
│                                      │
│  ┌───────────────────────────────┐  │
│  │     DeviceCapabilityChecker   │  │
│  │     - hasRoot()               │  │
│  │     - hasSysfsAccess()        │  │
│  │     - getChargingControlPath()│  │
│  └──────────┬────────────────────┘  │
│             │                        │
│     ┌───────┴────────┐              │
│     ▼                ▼              │
│  ┌─────────┐   ┌──────────────┐    │
│  │ RootMode│   │ StandardMode │    │
│  │ (sysfs) │   │(BatteryMgr)  │    │
│  └─────────┘   └──────────────┘    │
└─────────────────────────────────────┘
```

#### Root Mode — sysfs File Paths

##### Pixel 7 Pro (Tensor G2)

| Parameter | sysfs Path | Read/Write | Unit |
|-----------|-----------|------------|------|
| Battery Level | `/sys/class/power_supply/battery/capacity` | R | % (0–100) |
| Voltage | `/sys/class/power_supply/battery/voltage_now` | R | µV (÷ 1,000,000 = V) |
| Current | `/sys/class/power_supply/battery/current_now` | R | µA (÷ 1,000,000 = A) |
| Status | `/sys/class/power_supply/battery/status` | R | String |
| Charging Enable | `/sys/class/power_supply/battery/charging_enabled` | W (root) | 0/1 |
| Temperature | `/sys/class/power_supply/battery/temp` | R | Tenths °C (÷ 10) |
| Health | `/sys/class/power_supply/battery/health` | R | String |
| Charge Type | `/sys/class/power_supply/battery/charge_type` | R | String |

##### Huawei P20 Lite (Kirin 659)

| Parameter | sysfs Path | Read/Write | Unit | Notes |
|-----------|-----------|------------|------|-------|
| Battery Level | `/sys/class/power_supply/battery/capacity` | R | % | Standard |
| Voltage | `/sys/class/power_supply/battery/voltage_now` | R | µV | Standard |
| Current | `/sys/class/power_supply/battery/current_now` | R | µA | May be negative when discharging |
| Status | `/sys/class/power_supply/battery/status` | R | String | Standard |
| Charging Enable | `/sys/class/power_supply/battery/charging_enabled` | W (root) | 0/1 | **May not exist.** Fallback: `/sys/class/power_supply/usb/online` (read-only) |
| Temperature | `/sys/class/power_supply/battery/temp` | R | Tenths °C | Standard |
| Charge Control | `/sys/devices/platform/charger/charge_control_limit` | W (root) | — | Huawei-specific, may vary |

**Auto-detection order:**
```kotlin
val CHARGE_CONTROL_CANDIDATES = listOf(
    "/sys/class/power_supply/battery/charging_enabled",
    "/sys/class/power_supply/battery/charge_control_limit",
    "/sys/class/power_supply/battery/input_suspend",
    "/sys/class/power_supply/maxfg/charging_enabled",
    "/sys/devices/platform/charger/charge_control_limit"
)

suspend fun detectChargingControlPath(): String? = withContext(Dispatchers.IO) {
    CHARGE_CONTROL_CANDIDATES.firstOrNull { path ->
        RootShell.exec("test -w $path").exitCode == 0
    }
}
```

#### Standard Mode — BatteryManager API (Non-Root Fallback)

```kotlin
class StandardBatteryDataSource(private val context: Context) : BatteryDataSource {
    
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    override fun getBatteryData(): BatteryData {
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        
        // Register for ACTION_BATTERY_CHANGED for voltage, temp, status
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)?.div(1000f) ?: 0f // mV → V
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10f) ?: 0f // tenths °C → °C
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val levelPct = intent?.let {
            val lvl = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (lvl >= 0 && scale > 0) (lvl * 100) / scale else 0
        } ?: 0
        
        return BatteryData(
            level = levelPct,
            voltageV = voltage,
            currentMa = level.toFloat() / 1000f,  // µA → mA (may be 0 on some devices)
            wattageW = null, // Cannot reliably calculate without root on some devices
            temperatureC = temperature,
            status = mapBatteryStatus(status),
            chargeLimitSupported = false,
            chargeLimitEnabled = false
        )
    }
}
```

#### Charge Limiter State Machine

```
                        ┌────────────────────────┐
                        │     USER DISABLED       │
                        │  (charge limit off)     │
                        └───────────┬────────────┘
                                    │ User enables
                                    ▼
                        ┌────────────────────────┐
              ┌────────│      MONITORING         │────────┐
              │         │   (charging allowed)    │        │
              │         └────────────────────────┘        │
              │ capacity ≤ 75%                capacity ≥ 80%
              │ (resume threshold)              (limit threshold)
              │                                           │
              ▼                                           ▼
   ┌────────────────────────┐              ┌────────────────────────┐
   │      CHARGING          │              │       LIMITED          │
   │  (sysfs: enabled=1)   │──────────── │  (sysfs: enabled=0)   │
   │  UI: "Charging"       │  cap ≥ 80%  │  UI: "Limited at 80%" │
   └────────────────────────┘              └────────────────────────┘
              ▲                                           │
              └───────────────────────────────────────────┘
                            cap ≤ 75%
```

**Hysteresis band:** 75%–80% prevents rapid toggling near the threshold.

#### Root Shell Executor

```kotlin
object RootShell {
    private val TAG = "RootShell"
    
    data class ShellResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
        val durationMs: Long
    )
    
    suspend fun exec(command: String, timeoutMs: Long = 5000L): ShellResult = withContext(Dispatchers.IO) {
        val start = SystemClock.elapsedRealtime()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            
            if (!completed) {
                process.destroyForcibly()
                return@withContext ShellResult(-1, "", "Command timed out after ${timeoutMs}ms", timeoutMs)
            }
            
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val duration = SystemClock.elapsedRealtime() - start
            
            ShellResult(process.exitValue(), stdout, stderr, duration).also {
                Timber.d("RootShell: [$command] → exit=${it.exitCode}, ${it.durationMs}ms")
            }
        } catch (e: IOException) {
            val duration = SystemClock.elapsedRealtime() - start
            Timber.e(e, "RootShell: [$command] failed")
            ShellResult(-1, "", e.message ?: "IOException", duration)
        }
    }
    
    suspend fun isRootAvailable(): Boolean {
        return exec("id").stdout.contains("uid=0")
    }
    
    suspend fun readSysfs(path: String): String? {
        val result = exec("cat $path")
        return if (result.exitCode == 0) result.stdout else null
    }
    
    suspend fun writeSysfs(path: String, value: String): Boolean {
        return exec("echo $value > $path").exitCode == 0
    }
}
```

---

### 4.3 EMUI Compatibility Layer

Huawei's EMUI aggressively kills background services. The app must detect Huawei devices and guide the user through whitelist settings.

#### Detection

```kotlin
object DeviceCompat {
    
    val isHuawei: Boolean
        get() = Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
                Build.BRAND.equals("HUAWEI", ignoreCase = true) ||
                Build.BRAND.equals("HONOR", ignoreCase = true)
    
    val isEMUI: Boolean
        get() = isHuawei && try {
            val clazz = Class.forName("com.huawei.android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            val version = method.invoke(null, "ro.build.version.emui", "") as String
            version.isNotBlank()
        } catch (e: Exception) { false }
    
    val oemBatteryOptimizationIntent: Intent?
        get() = when {
            isHuawei -> Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            // Add Samsung, Xiaomi, etc. for future device support
            else -> null
        }
    
    val protectedAppsIntent: Intent?
        get() = if (isHuawei) {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            }
        } else null
}
```

#### User Prompt Flow

```
App Start
  │
  ├── Detect EMUI
  │     │
  │     ├── Check if battery optimization is disabled for this app
  │     │     │
  │     │     ├── YES → Continue normally
  │     │     └── NO → Show dialog:
  │     │           "Your Huawei device may kill this app in the background.
  │     │            Please follow these steps:"
  │     │           [1] Disable battery optimization → (deep link to Settings)
  │     │           [2] Enable auto-launch → (deep link to Startup Manager)
  │     │           [3] Add to Protected Apps → (deep link)
  │     │           [Don't show again] → save preference
  │     │
  │     └── Log EMUI version for debugging
  │
  └── Continue to dashboard
```

---

### 4.4 Embedded HTTP Server (Command Bridge)

#### Server Configuration

| Property | Value |
|----------|-------|
| Engine | Ktor `CIO` (Coroutine I/O) — compatible with API 28+ |
| Bind Address | `0.0.0.0` (all interfaces) |
| Default Port | `8420` (configurable) |
| Content-Type | `application/json` |
| Max Request Size | 64 KB |
| Request Timeout | 10s |
| Concurrent Connections | 10 max |

#### Endpoint Reference

##### `GET /ping`

Health check.

**Response `200 OK`:**
```json
{
  "status": "ok",
  "version": "1.0.0",
  "uptime_seconds": 3600,
  "device": "Pixel 7 Pro",
  "android_version": 14,
  "battery_level": 78,
  "charging": false,
  "root_available": true
}
```

##### `POST /command`

Execute a registered command.

**Request Headers:**
```
X-Auth-Token: <pre-shared-key>
Content-Type: application/json
```

**Request Body:**
```json
{
  "action": "toggle_charge_limit",
  "params": {
    "enabled": true
  }
}
```

**Responses:**

| Code | Body | Condition |
|------|------|-----------|
| `200` | `{"status":"ok","action":"...","result":"...","duration_ms":12}` | Success |
| `400` | `{"status":"error","message":"Invalid request body"}` | Malformed JSON or missing `action` |
| `401` | `{"status":"error","message":"Invalid or missing auth token"}` | Bad/missing `X-Auth-Token` |
| `403` | `{"status":"error","message":"Action requires root access"}` | Shell action on non-rooted device |
| `404` | `{"status":"error","message":"Unknown action: xyz"}` | Action not in registry |
| `429` | `{"status":"error","message":"Rate limited","retry_after_ms":1000}` | > 30 requests/minute |
| `500` | `{"status":"error","message":"Internal error","detail":"..."}` | Unhandled exception |

##### `GET /status`

Full system snapshot.

**Response `200 OK`:**
```json
{
  "spotify": {
    "authenticated": true,
    "is_playing": true,
    "track": "Song Name",
    "artist": "Artist Name",
    "album": "Album Name",
    "progress_ms": 45000,
    "duration_ms": 210000
  },
  "battery": {
    "level": 78,
    "voltage_v": 4.12,
    "current_ma": -450,
    "wattage_w": 1.85,
    "temperature_c": 28.5,
    "charging_enabled": false,
    "status": "Not charging",
    "charge_limit_active": true,
    "health": "Good",
    "data_source": "sysfs"
  },
  "audio_stream": {
    "active": true,
    "connected": true,
    "latency_ms": 85,
    "codec": "opus",
    "packets_received": 12345,
    "packets_lost": 2
  },
  "server": {
    "uptime_seconds": 3600,
    "commands_processed": 42,
    "port": 8420,
    "auth_enabled": true
  },
  "device": {
    "model": "Pixel 7 Pro",
    "manufacturer": "Google",
    "android_version": 14,
    "api_level": 34,
    "root_available": true,
    "ip_address": "192.168.1.42"
  }
}
```

##### `GET /commands`

List all registered commands (for discovery by PC tools).

**Response `200 OK`:**
```json
{
  "commands": [
    {
      "action": "toggle_charge_limit",
      "type": "toggle",
      "description": "Toggle the 80% battery charge limit",
      "requires_root": true,
      "params_schema": { "enabled": "boolean" }
    },
    {
      "action": "navigate",
      "type": "ui",
      "description": "Navigate to a dashboard page",
      "requires_root": false,
      "params_schema": { "page": "string (home|audio|system|settings)" }
    }
  ]
}
```

#### Command Registry Schema (`commands.json`)

```json
{
  "version": 1,
  "commands": [
    {
      "action": "toggle_charge_limit",
      "type": "toggle",
      "description": "Toggle the 80% battery charge limit",
      "handler": "battery_charge_toggle",
      "requires_root": true
    },
    {
      "action": "navigate",
      "type": "ui",
      "description": "Navigate to a dashboard page",
      "handler": "ui_navigate",
      "requires_root": false,
      "params_schema": {
        "page": { "type": "string", "enum": ["home", "audio", "system", "settings"] }
      }
    },
    {
      "action": "exec_shell",
      "type": "shell",
      "description": "Execute a whitelisted shell command",
      "handler": "root_shell_exec",
      "requires_root": true,
      "params_schema": {
        "command": { "type": "string" }
      },
      "whitelist": [
        "reboot",
        "settings put system screen_brightness *",
        "input keyevent *"
      ]
    },
    {
      "action": "wol",
      "type": "network",
      "description": "Send Wake-on-LAN magic packet",
      "handler": "send_wol",
      "requires_root": false
    },
    {
      "action": "audio_toggle",
      "type": "toggle",
      "description": "Start/stop the audio receiver",
      "handler": "audio_stream_toggle",
      "requires_root": false
    },
    {
      "action": "set_brightness",
      "type": "system",
      "description": "Set screen brightness (0-255)",
      "handler": "set_brightness",
      "requires_root": false,
      "params_schema": {
        "value": { "type": "integer", "min": 0, "max": 255 }
      }
    }
  ]
}
```

---

### 4.5 Wake-On-LAN (WOL) Module

#### Magic Packet Structure

```
Bytes 0–5:    FF FF FF FF FF FF         (synchronization stream)
Bytes 6–101:  MAC × 16                  (target MAC repeated 16 times)
Total:        102 bytes
```

#### Implementation

```kotlin
suspend fun sendWolPacket(
    macAddress: String,
    broadcastIp: String = "255.255.255.255",
    port: Int = 9
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val macBytes = macAddress
            .split(":", "-")
            .map { it.toInt(16).toByte() }
            .toByteArray()
        
        require(macBytes.size == 6) { "Invalid MAC address: $macAddress" }
        
        val magicPacket = ByteArray(102)
        for (i in 0..5) magicPacket[i] = 0xFF.toByte()
        for (i in 0..15) System.arraycopy(macBytes, 0, magicPacket, 6 + i * 6, 6)
        
        val address = InetAddress.getByName(broadcastIp)
        val packet = DatagramPacket(magicPacket, magicPacket.size, address, port)
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(packet)
        }
        
        Timber.i("WOL packet sent to $macAddress via $broadcastIp:$port")
    }
}
```

---

### 4.6 Audio Passthrough Module

#### Protocol Specification

| Parameter | Value |
|-----------|-------|
| Transport | UDP unicast |
| Port | `8421` (configurable) |
| Codec | Opus (preferred) or raw PCM 16-bit fallback |
| Sample Rate | 48000 Hz |
| Channels | 2 (stereo) |
| Bit Depth (PCM) | 16-bit signed little-endian |
| Frame Size (Opus) | 960 samples (20ms) |
| Max Packet Size | 1400 bytes (stays under 1500 byte MTU) |

#### Packet Header Format

```
Offset  Size    Field               Description
0       1       codec_flag          0x01 = PCM, 0x02 = Opus
1       4       sequence_number     uint32 big-endian, wraps at 2^32
5       4       timestamp           uint32 big-endian (sample count)
9       N       payload             Audio data
```

#### Android Receiver Pipeline

```
UDP Socket (Dispatchers.IO)
    │
    ▼
Packet Parser (validate header, extract payload)
    │
    ▼
Jitter Buffer (ring buffer, ~50ms, reorder by seq#, discard stale)
    │
    ▼
Opus Decoder (or PCM passthrough)
    │
    ▼
AudioTrack.write() (PERFORMANCE_MODE_LOW_LATENCY)
    │
    ▼
Headphone Jack / USB-C Audio / Speaker
```

#### AudioTrack Configuration (API 28+ Compatible)

```kotlin
val sampleRate = 48000
val channelMask = AudioFormat.CHANNEL_OUT_STEREO
val encoding = AudioFormat.ENCODING_PCM_16BIT
val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)

val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(encoding)
                .setSampleRate(sampleRate)
                .setChannelMask(channelMask)
                .build()
        )
        .setBufferSizeInBytes(minBufferSize * 2)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
        }
        .build()
} else {
    @Suppress("DEPRECATION")
    AudioTrack(
        AudioManager.STREAM_MUSIC,
        sampleRate,
        channelMask,
        encoding,
        minBufferSize * 2,
        AudioTrack.MODE_STREAM
    )
}
```

---

## 5. Android Application Services

### 5.1 Service Compatibility Matrix

| Service | API 28 | API 29+ | Foreground Type |
|---------|--------|---------|-----------------|
| `BatteryGuardService` | `startForeground(id, notif)` | `startForeground(id, notif, SPECIAL_USE)` | Battery monitoring |
| `CommandBridgeService` | `startForeground(id, notif)` | `startForeground(id, notif, SPECIAL_USE)` | HTTP server |
| `AudioReceiverService` | `startForeground(id, notif)` | `startForeground(id, notif, MEDIA_PLAYBACK)` | Audio playback |

### 5.2 Service Lifecycle

```
App Launch
    │
    ├── Start BatteryGuardService (always)
    ├── Start CommandBridgeService (always)
    └── AudioReceiverService (on-demand, user toggle)
    
Screen Off / App Background
    │
    ├── Services continue (foreground notification visible)
    ├── On EMUI: may be killed → auto-restart via START_STICKY
    └── On Pixel: stable (battery optimization exempted)
    
Boot Complete
    │
    └── BootReceiver → start all services (if "start on boot" enabled)
```

---

## 6. Data Flow Diagrams

### 6.1 Spotify Polling

```
Timer (3–5s)
    │
    ▼
SpotifyRepository.fetchCurrentTrack()   [Dispatchers.IO]
    │
    ├── GET /currently-playing
    │     ├── 200 + body → parse → SpotifyTrack(isPlaying=true)
    │     ├── 200 + empty/204 → fallback to recently-played
    │     ├── 401 → refresh token → retry once
    │     ├── 429 → back off (Retry-After header)
    │     └── Network error → retain last known state, set error flag
    │
    ▼
MutableStateFlow<SpotifyUiState>.emit()
    │
    ▼
Composable recomposes via collectAsStateWithLifecycle()
```

### 6.2 Command Bridge

```
PC (AHK)                    Android
────────                    ───────
F13 pressed
    │
    ▼
HTTP POST /command          Ktor Server (IO thread)
{action:"toggle_charge"} ──────►  │
                                   ├── Validate auth token
                                   ├── Look up action in registry
                                   ├── Check root requirement
                                   ├── Execute handler
                                   │     ├── toggle → BatteryRepo.toggleChargeLimit()
                                   │     ├── shell → RootShell.exec(cmd)
                                   │     ├── ui → CommandChannel.send(NavAction)
                                   │     └── network → WolSender.send()
                                   ├── Log to ring buffer
                                   └── Return JSON response
                            ◄────── {"status":"ok","result":"..."}
```

---

## 7. Security Model

### 7.1 Threat Matrix

| Threat | Severity | Probability | Mitigation |
|--------|----------|-------------|------------|
| Unauthorized LAN command execution | High | Medium | Pre-shared token auth (`X-Auth-Token`). 256-bit random hex. |
| Spotify token theft | Medium | Low | PKCE flow (no client secret). Refresh token in `EncryptedSharedPreferences` (AES-256-GCM). |
| Root command injection | Critical | Low | Command whitelist in `commands.json`. Arbitrary shell disabled by default. Input sanitization. |
| Audio stream eavesdropping | Low | Low | Local network only. DTLS encryption is a v2 stretch goal. |
| Rogue HTTP requests from LAN | Medium | Medium | Rate limiting (30 req/min). Token regeneration in settings. |
| Physical device access | Medium | Low | No sensitive data displayed by default. Settings locked behind device PIN. |

### 7.2 Shell Command Whitelist

When `exec_shell` type commands are used, the command string is validated against the whitelist in `commands.json`. Wildcards (`*`) match any single argument segment.

Example whitelist:
```
"reboot"                              → allows "reboot" exactly
"settings put system screen_brightness *"  → allows any brightness value
"input keyevent *"                    → allows any keyevent code
```

Any command not matching a whitelist pattern is **rejected with 403**.

### 7.3 Permissions Required

| Permission | Min API | Reason |
|------------|---------|--------|
| `INTERNET` | 1 | Spotify API, HTTP server, audio, WOL |
| `ACCESS_NETWORK_STATE` | 1 | Connectivity checks |
| `ACCESS_WIFI_STATE` | 1 | Get device IP address |
| `FOREGROUND_SERVICE` | 28 | Background services |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 34 | Battery + command services (ignored on < 34) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 34 | Audio service (ignored on < 34) |
| `WAKE_LOCK` | 1 | Keep CPU awake for polling |
| `RECEIVE_BOOT_COMPLETED` | 1 | Auto-start on boot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 23 | Prevent Doze from killing services |
| `POST_NOTIFICATIONS` | 33 | Runtime notification permission (no-op on < 33) |
| Root (`su`) | — | sysfs write, shell exec (optional) |

---

## 8. Logging Architecture

### 8.1 Log Levels

| Level | Usage | Example |
|-------|-------|---------|
| `VERBOSE` | Detailed data flow (sysfs reads, packet counts) | `Battery sysfs: voltage_now=4120000` |
| `DEBUG` | State transitions, API responses | `Spotify: Track changed to "Song Name"` |
| `INFO` | User actions, service lifecycle | `CommandBridge: Server started on :8420` |
| `WARN` | Recoverable errors, degraded mode | `Root not available, falling back to BatteryManager` |
| `ERROR` | Failures requiring attention | `Spotify token refresh failed: 401` |

### 8.2 In-App Log Ring Buffer

A circular buffer of the last 200 log entries is maintained in memory for the Debug/System page.

```kotlin
object AppLog {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()
    
    private val buffer = ArrayDeque<LogEntry>(200)
    
    @Synchronized
    fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )
        if (buffer.size >= 200) buffer.removeFirst()
        buffer.addLast(entry)
        _entries.value = buffer.toList()
        
        // Also log via Timber
        when (level) {
            LogLevel.ERROR -> Timber.tag(tag).e(message)
            LogLevel.WARN -> Timber.tag(tag).w(message)
            LogLevel.INFO -> Timber.tag(tag).i(message)
            LogLevel.DEBUG -> Timber.tag(tag).d(message)
            LogLevel.VERBOSE -> Timber.tag(tag).v(message)
        }
    }
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)

enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }
```

---

## 9. Technology Stack & Dependencies

### Android App

| Component | Library | Version | Min API |
|-----------|---------|---------|---------|
| Language | Kotlin | 1.9.x | — |
| UI Framework | Jetpack Compose + Material 3 | BOM 2024.06.00 | 21 |
| Navigation | Compose Navigation | 2.7.7 | 21 |
| DI | Hilt | 2.51 | 24 |
| HTTP Client | Retrofit 2 + OkHttp 4 | 2.9.0 / 4.12.0 | 21 |
| JSON | Kotlinx Serialization | 1.6.3 | — |
| Image Loading | Coil Compose | 2.6.0 | 21 |
| HTTP Server | Ktor Server CIO | 2.3.9 | — |
| Coroutines | Kotlinx Coroutines | 1.8.0 | — |
| Preferences | Jetpack DataStore | 1.0.0 | 21 |
| Encrypted Storage | Security Crypto | 1.1.0-alpha06 | 23 |
| OAuth UI | AndroidX Browser | 1.8.0 | 21 |
| Logging | Timber | 5.0.1 | — |
| Audio | AudioTrack (platform) | API 26+ builder | 26 |
| Opus | Concentus (pure Java) | — | — |
| Lifecycle | Lifecycle Runtime Compose | 2.7.0 | 21 |

### PC Side

| Component | Technology | Version |
|-----------|-----------|---------|
| Macro Bridge | AutoHotkey | v2.0+ |
| Audio Capture | Python + sounddevice | 0.4.6+ |
| Audio Encoding | opuslib / pyogg | 3.0.1+ |
| Networking | Python `socket` | stdlib |
| NumPy | numpy | 1.26.0+ |
