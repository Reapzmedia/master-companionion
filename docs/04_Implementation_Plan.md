# Phased Implementation Plan
## Master Companion — Standby Dashboard & PC Bridge
**Version:** 1.1  
**Date:** 2026-09-04  
**Status:** Draft  

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-09-04 | Initial draft |
| 1.1 | 2026-09-04 | Added API 28 compat tasks, EMUI handling, device-adaptive UI, onboarding flow, non-root fallback paths, expanded testing matrix |

---

## Overview

Development is structured into **4 sequential Epics** plus a **cross-cutting compatibility epic** that runs in parallel. Each Epic builds on the previous. Total estimated duration: **7–9 weeks** solo development.

```
Epic 0 (Parallel)
┌──────────────────────────────────────────────────────────────────┐
│ DEVICE COMPATIBILITY LAYER (API 28, EMUI, root detection)       │
└──────────────────────────────────────────────────────────────────┘

Epic 1             Epic 2              Epic 3               Epic 4
┌──────────┐      ┌────────────┐      ┌──────────────┐     ┌──────────────┐
│ UI Shell │ ───▶ │ Root Power │ ───▶ │ Network      │ ──▶ │ Audio        │
│ & Media  │      │ & Battery  │      │ Bridge &     │     │ Passthrough  │
│ Dashboard│      │ Management │      │ Macros + WOL │     │              │
└──────────┘      └────────────┘      └──────────────┘     └──────────────┘
   ~2 weeks          ~1.5 weeks          ~1.5 weeks           ~1.5 weeks

                                                         Polish & Hardening
                                                         ┌──────────────┐
                                                         │ Testing,     │
                                                         │ Soak, Ship   │
                                                         └──────────────┘
                                                            ~1 week
```

---

## Epic 0: Device Compatibility Layer (Parallel — Start Day 1)

**Duration:** Ongoing (started in Epic 1, completed by Epic 2)  
**Goal:** Build the detection, abstraction, and compatibility layer that all features depend on.

### Tasks

#### 0.1 Project Configuration for API 28

- [ ] Set `minSdk = 28` in `build.gradle.kts`
- [ ] Set `targetSdk = 34`
- [ ] Set `compileSdk = 34`
- [ ] Configure `compileOptions` for Java 8 source compatibility:
  ```kotlin
  compileOptions {
      sourceCompatibility = JavaVersion.VERSION_1_8
      targetCompatibility = JavaVersion.VERSION_1_8
      isCoreLibraryDesugaringEnabled = true  // For java.time on API 28
  }
  ```
- [ ] Add core library desugaring dependency:
  ```kotlin
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
  ```

#### 0.2 Device Capability Detector

- [ ] Create `DeviceCompat` object with:
  - `isHuawei`, `isEMUI`, `oemSkin` detection
  - `hasRoot()` async check (try `su -c id`)
  - `isLowRam` (`ActivityManager.isLowRamDevice()`)
  - `hasNotch` (API 28 `DisplayCutout`)
  - `isOLED` (heuristic based on Build.MODEL / manufacturer)
  - `screenCategory` (compact < 700dp, standard ≥ 700dp)
- [ ] Create `DeviceCapabilities` data class exposed via `StateFlow`
- [ ] Wire into Hilt as `@Singleton`

#### 0.3 Foreground Service Compat Wrapper

- [ ] Create `ForegroundServiceCompat.startForeground()`:
  ```kotlin
  fun Service.startForegroundCompat(id: Int, notification: Notification, type: Int) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(id, notification, type)
      } else {
          startForeground(id, notification)
      }
  }
  ```

#### 0.4 Immersive Mode Compat

- [ ] Create `Activity.enterImmersiveMode()` extension with API 30+ and legacy paths
- [ ] Handle notch via `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` (API 28+)

#### 0.5 EMUI Background Survival

- [ ] Detect EMUI and prompt user for battery optimization exemptions
- [ ] Build `HuaweiCompatActivity` with intents for:
  - Protected Apps
  - Startup Manager  
  - Battery Optimization settings
- [ ] Store "dismissed" state in DataStore

#### 0.6 Network Security Config

- [ ] Create `res/xml/network_security_config.xml` allowing cleartext for LAN ranges
- [ ] Reference in `AndroidManifest.xml`

#### 0.7 Notification Permission (API 33+)

- [ ] Request `POST_NOTIFICATIONS` at runtime on API 33+
- [ ] On API 28–32: no runtime request needed (auto-granted)

---

## Epic 1: UI Shell & Media Dashboard

**Duration:** ~2 weeks  
**Goal:** Functional landscape dashboard with Spotify integration, clock, and pager navigation. Runs on both Pixel 7 Pro and P20 Lite.

### Tasks

#### 1.1 Project Bootstrap

- [ ] Create Android project (Android Studio, Kotlin DSL)
- [ ] Configure version catalog (`libs.versions.toml`):

```toml
[versions]
compose-bom = "2024.06.00"
hilt = "2.51"
retrofit = "2.9.0"
okhttp = "4.12.0"
ktor = "2.3.9"
coil = "2.6.0"
kotlinx-serialization = "1.6.3"
coroutines = "1.8.0"
datastore = "1.0.0"
timber = "5.0.1"
security-crypto = "1.1.0-alpha06"
browser = "1.8.0"
lifecycle = "2.7.0"
navigation = "2.7.7"
desugar = "2.0.4"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-service = { group = "androidx.lifecycle", name = "lifecycle-service", version.ref = "lifecycle" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
retrofit-kotlinx = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version = "1.0.0" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
ktor-server-core = { group = "io.ktor", name = "ktor-server-core", version.ref = "ktor" }
ktor-server-cio = { group = "io.ktor", name = "ktor-server-cio", version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-server-status-pages = { group = "io.ktor", name = "ktor-server-status-pages", version.ref = "ktor" }
desugar-jdk = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugar" }
# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
mockk = { group = "io.mockk", name = "mockk", version = "1.13.10" }
turbine = { group = "app.cash.turbine", name = "turbine", version = "1.1.0" }
```

- [ ] Configure Hilt `@HiltAndroidApp` and entry points
- [ ] Set landscape, immersive, keep-screen-on
- [ ] Configure Material 3 dark theme with design tokens

#### 1.2 Dashboard Shell (HorizontalPager)

- [ ] Implement `HorizontalPager(pageCount = 4)`
- [ ] Build animated `PageIndicator` composable
- [ ] Create empty composables for Pages 1–3
- [ ] `MainViewModel` with `currentPage: StateFlow<Int>`
- [ ] Support programmatic page change (for command bridge)

#### 1.3 Clock Widget

- [ ] `ClockWidget` composable with device-adaptive font size (72sp / 56sp)
- [ ] `LaunchedEffect` + `delay(1000)` tick
- [ ] Date formatting: full on Pixel ("Thursday, September 4"), compact on P20 Lite ("Sep 4")
- [ ] 12h/24h from system setting

#### 1.4 Spotify OAuth PKCE

- [ ] Register Spotify Developer app, configure redirect URI
- [ ] Deep link in `AndroidManifest.xml`:
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.VIEW" />
      <category android:name="android.intent.category.DEFAULT" />
      <category android:name="android.intent.category.BROWSABLE" />
      <data android:scheme="mastercompanion" android:host="spotify" android:path="/callback" />
  </intent-filter>
  ```
- [ ] PKCE verifier/challenge generation
- [ ] Custom Tabs launch with fallback to browser Intent (for EMUI)
- [ ] Handle callback in `onNewIntent` → exchange code for tokens
- [ ] Refresh token → `EncryptedSharedPreferences`
- [ ] Auto-refresh logic with 60s pre-expiry buffer

#### 1.5 Spotify API Client

- [ ] Retrofit interface with all endpoints
- [ ] `SpotifyRepository` with device-aware polling interval (3s Pixel / 5s P20 Lite)
- [ ] Fallback chain: currently-playing → recently-played → idle
- [ ] Rate limit handling (429 + `Retry-After`)
- [ ] Map responses to `SpotifyTrack` domain model

#### 1.6 Home Dashboard UI

- [ ] `AlbumArtCard` with Coil `AsyncImage`:
  - Pixel: 300dp, blur glow, 600px decode
  - P20 Lite: 220dp, no glow, 440px decode
- [ ] `MarqueeText` with `basicMarquee()` modifier
- [ ] `PlaybackControlRow` (48dp min touch targets)
- [ ] `BatteryPanel` placeholder (wired in Epic 2)
- [ ] Wire all to `HomeViewModel` via `collectAsStateWithLifecycle()`
- [ ] Handle all `SpotifyUiState` variants including error states

#### 1.7 Onboarding Flow (First Run)

- [ ] Build 5-step onboarding pager:
  1. Device capability check
  2. EMUI whitelist (conditional)
  3. Spotify auth
  4. Network config (show IP)
  5. Summary
- [ ] Store `onboardingComplete` in DataStore
- [ ] Skip on subsequent launches

#### 1.8 Testing (Epic 1)

- [ ] Unit: PKCE challenge generation
- [ ] Unit: Token refresh logic
- [ ] Unit: SpotifyRepository fallback chain (use Turbine for Flow testing)
- [ ] Unit: Device capability detection mocking
- [ ] Manual: OAuth flow on Pixel 7 Pro
- [ ] Manual: OAuth flow on P20 Lite (EMUI Custom Tab / browser)
- [ ] Manual: Dashboard renders at 60fps on both devices
- [ ] Manual: Marquee scrolling, album art loading
- [ ] Manual: Notch does not obscure content on P20 Lite
- [ ] Manual: Onboarding flow works end-to-end

**Exit Criteria:** Dashboard launches on both devices in landscape. Spotify data displays with album art. Clock ticks. Pager swipe works. Onboarding completes.

---

## Epic 2: Root Power & Battery Management

**Duration:** ~1.5 weeks  
**Goal:** Real-time battery metrics with root (sysfs) and non-root (BatteryManager) paths. Charge limiter on rooted devices.

### Tasks

#### 2.1 Root Shell Utility

- [ ] `RootShell` singleton: `exec()`, `readSysfs()`, `writeSysfs()`
- [ ] Root check: `su -c 'id'` → look for `uid=0`
- [ ] 5s command timeout
- [ ] Timber logging for all commands

#### 2.2 Battery Data Sources

- [ ] `RootBatteryDataSource` — reads sysfs, calculates wattage
- [ ] `StandardBatteryDataSource` — uses `BatteryManager` API + broadcast receiver
- [ ] `BatteryRepository` selects source based on `DeviceCapabilities.hasRoot`
- [ ] sysfs path auto-detection with candidate list
- [ ] 2s polling interval (both sources)

#### 2.3 Charge Limiter Service

- [ ] `BatteryGuardService` foreground service
- [ ] API 28 compat: `startForegroundCompat()` wrapper
- [ ] State machine: MONITORING → CHARGING → LIMITED → CHARGING
- [ ] Hysteresis: 75%–80%
- [ ] DataStore persistence (limit %, hysteresis %, enabled)
- [ ] Notification with battery level + limit status
- [ ] `START_STICKY` for restart after OOM kill
- [ ] EMUI: warn user if service is killed

#### 2.4 Root Disclaimer Dialog

- [ ] Show dialog before first charge limit toggle
- [ ] "This may void your warranty" warning text
- [ ] User must tap "I Understand, Enable"
- [ ] Acknowledged state saved in DataStore

#### 2.5 Battery Dashboard UI

- [ ] `BatteryBar` composable (custom Canvas, animated width, color-coded)
- [ ] Metrics: wattage (or "Root required"), temperature, status
- [ ] Charge limit toggle (hidden on non-root, disabled with badge)
- [ ] "Wake PC" button (placeholder, wired in Epic 3)
- [ ] Wire `BatteryViewModel`

#### 2.6 Boot Receiver

- [ ] `BootReceiver` registered for `RECEIVE_BOOT_COMPLETED`
- [ ] Starts `BatteryGuardService` if charge limit was enabled
- [ ] Works on API 28+

#### 2.7 Testing (Epic 2)

- [ ] Unit: Wattage calculation (µV × µA → W)
- [ ] Unit: Charge limit state machine transitions
- [ ] Unit: sysfs path auto-detection with mocked paths
- [ ] Unit: BatteryManager fallback data extraction
- [ ] Manual (Pixel 7 Pro): sysfs reads, toggle charging, verify charger stops
- [ ] Manual (Pixel 7 Pro): Service survives screen off and app background
- [ ] Manual (P20 Lite, no root): BatteryManager data displays, charge limit shows "Root required"
- [ ] Manual (P20 Lite): Service survives EMUI background kill (with whitelist)
- [ ] Manual: Boot receiver restarts service

**Exit Criteria:** Dashboard shows live battery metrics on both devices. Root devices can toggle charge limiting. Non-root devices show available data with appropriate badges. Service persists across lifecycle.

---

## Epic 3: Network Bridge, Macros & WOL

**Duration:** ~1.5 weeks  
**Goal:** HTTP command bridge, AHK integration, and Wake-On-LAN.

### Tasks

#### 3.1 Ktor Embedded Server

- [ ] `CommandBridgeService` foreground service
- [ ] Ktor CIO server on configurable port
- [ ] Routes: `GET /ping`, `GET /status`, `GET /commands`, `POST /command`
- [ ] `X-Auth-Token` auth interceptor
- [ ] Rate limiting (30 req/min)
- [ ] Default 256-bit hex token on first launch
- [ ] Network security config allows cleartext for LAN

#### 3.2 Command Registry & Executor

- [ ] JSON config parser for `commands.json`
- [ ] `CommandExecutor` with handler dispatch (toggle, ui, shell, network)
- [ ] Shell command whitelist enforcement
- [ ] Root-required check → 403 on non-root devices
- [ ] Ring buffer logging (200 entries)
- [ ] ViewModel command channel for UI actions (`SharedFlow`)

#### 3.3 Wake-On-LAN

- [ ] `WolSender` with Magic Packet builder
- [ ] MAC address config in DataStore/Settings
- [ ] "Wake PC" button on home dashboard
- [ ] WOL as command bridge action

#### 3.4 PC-Side AutoHotkey Script

- [ ] Create `companion_bridge.ahk` (AHK v2):
  ```ahk
  #Requires AutoHotkey v2.0
  
  ; ═══ Configuration ═══
  ANDROID_IP   := "192.168.1.42"
  ANDROID_PORT := 8420
  AUTH_TOKEN   := "paste-your-token-here"
  
  ; ═══ Macro Key Bindings ═══
  F13::SendCommand("toggle_charge_limit", '{"enabled": true}')
  F14::SendCommand("navigate", '{"page": "audio"}')
  F15::SendCommand("navigate", '{"page": "home"}')
  F16::SendCommand("wol", '{}')
  F17::SendCommand("audio_toggle", '{}')
  
  ; ═══ HTTP POST Function ═══
  SendCommand(action, paramsJson) {
      url := "http://" ANDROID_IP ":" ANDROID_PORT "/command"
      body := '{"action":"' action '","params":' paramsJson '}'
      
      try {
          http := ComObject("WinHttp.WinHttpRequest.5.1")
          http.Open("POST", url, false)
          http.SetRequestHeader("Content-Type", "application/json")
          http.SetRequestHeader("X-Auth-Token", AUTH_TOKEN)
          http.Send(body)
          
          if (http.Status != 200) {
              ToolTip("Command failed: " http.Status, , , 2)
              SetTimer(() => ToolTip(, , , 2), -2000)
          }
      } catch as e {
          ToolTip("Connection error: " e.Message, , , 2)
          SetTimer(() => ToolTip(, , , 2), -3000)
      }
  }
  ```
- [ ] Document: `docs/pc_setup/ahk_guide.md`

#### 3.5 System & Debug Page (Page 2)

- [ ] Device info panel (model, Android, OEM, root, uptime)
- [ ] Server status (ports, IP, running state)
- [ ] Memory stats (heap, native)
- [ ] Scrollable command log
- [ ] "Copy IP" button

#### 3.6 Settings Page (Page 3)

- [ ] Spotify section (account, sign out, poll interval)
- [ ] Network section (ports, auth token with mask/regenerate/copy)
- [ ] Battery section (charge limit %, hysteresis, sysfs path)
- [ ] WOL section (MAC, broadcast IP)
- [ ] Display section (keep screen on, visualizer toggle, blur glow toggle, visualizer FPS)
- [ ] Device section (start on boot, EMUI whitelist link)
- [ ] About section (version, licenses, privacy)

#### 3.7 Testing (Epic 3)

- [ ] Unit: Auth token validation
- [ ] Unit: Command registry parsing
- [ ] Unit: WOL magic packet byte structure
- [ ] Unit: Rate limiter logic
- [ ] Integration: `curl` / PowerShell to all endpoints
- [ ] Manual: AHK script → command → Android action (both devices)
- [ ] Manual: Unauthorized request → 401
- [ ] Manual: Unknown action → 404
- [ ] Manual: Rate limit → 429
- [ ] Manual: WOL wakes PC
- [ ] Manual: Settings changes persist across restart

**Exit Criteria:** PC AHK commands trigger Android actions. Endpoints authenticated. WOL works. Settings page functional. System page shows diagnostics.

---

## Epic 4: Audio Passthrough

**Duration:** ~1.5 weeks  
**Goal:** PC audio plays through Android phone's headphone jack / USB-C audio.

### Tasks

#### 4.1 PC-Side Python Audio Streamer

- [ ] Create `audio_streamer.py`:
  ```
  pip install sounddevice numpy opuslib
  ```
- [ ] WASAPI loopback capture
- [ ] Opus encoding (48kHz, stereo, 20ms frames)
- [ ] Packet construction (header + payload)
- [ ] UDP send to Android
- [ ] CLI args: `--device`, `--target-ip`, `--port`, `--codec`
- [ ] Graceful shutdown (Ctrl+C)
- [ ] Document: `docs/pc_setup/audio_streamer_guide.md`

#### 4.2 Android Audio Receiver

- [ ] `AudioReceiverService` foreground service
- [ ] UDP `DatagramSocket` listener on port 8421
- [ ] Packet header parsing (codec flag, seq#, timestamp)
- [ ] Jitter buffer (~50ms, reorder by seq#)
- [ ] `AudioTrack` with `PERFORMANCE_MODE_LOW_LATENCY` (API 26+ builder)

#### 4.3 Opus Decoder

- [ ] Integrate Concentus (pure Java Opus decoder):
  - No NDK required
  - Higher CPU than JNI, but compatible with API 28 and Kirin 659
  - Acceptable for 48kHz stereo at 20ms frames
- [ ] PCM passthrough mode (codec flag 0x01)

#### 4.4 Connection Management

- [ ] 5s no-packet timeout → DISCONNECTED
- [ ] Exponential backoff reconnection (1s→2s→4s→8s→16s→30s)
- [ ] `StateFlow<AudioConnectionStatus>`
- [ ] Packet statistics (received, lost, out-of-order)

#### 4.5 Audio Page UI (Page 1)

- [ ] Audio visualizer (Canvas):
  - Pixel: 30fps spectrum bars
  - P20 Lite: 15fps or static icon (configurable in Settings)
- [ ] Connection status indicator (color-coded, animated)
- [ ] Latency, codec, packet stats
- [ ] Volume slider → `AudioTrack.setVolume()`
- [ ] Mute toggle, stop button
- [ ] Audio output display (3.5mm / USB-C / Speaker)

#### 4.6 Testing (Epic 4)

- [ ] Unit: Packet header parsing
- [ ] Unit: Jitter buffer ordering, overflow, underflow
- [ ] Unit: Opus decode → PCM output validation
- [ ] Manual (Pixel): PC Python → Android plays audio via USB-C DAC
- [ ] Manual (P20 Lite): PC Python → Android plays audio via 3.5mm jack
- [ ] Manual: Latency measurement (target <150ms Pixel, <300ms P20 Lite)
- [ ] Manual: Disconnect Wi-Fi → reconnect → audio resumes
- [ ] Manual: Volume slider and mute
- [ ] Manual: 1-hour sustained playback (no glitches, no OOM)
- [ ] Manual: Visualizer disabled → reduced CPU usage on P20 Lite

**Exit Criteria:** PC audio audible through phone headphones. Latency within targets. Controls work. Auto-reconnect recovers. Stable for 1+ hour.

---

## Post-Epic: Polish, Hardening & Ship (Week 8–9)

### Stability

- [ ] Memory profiling (Android Profiler): 4+ hours, no upward trend
- [ ] 72-hour soak test on Pixel 7 Pro
- [ ] 24-hour soak test on P20 Lite
- [ ] Strict mode enabled in debug builds (detect disk/network on main thread)
- [ ] LeakCanary integration for debug builds

### Edge Cases

- [ ] No Wi-Fi → show error, disable network features, keep clock/battery
- [ ] No Spotify auth → show "Not connected" on home, rest works
- [ ] No root → all root features disabled with badges
- [ ] No charger → battery panel shows discharge data
- [ ] No audio stream → audio page shows disconnected help text
- [ ] Rapid config changes → debounce settings writes

### Performance Budget Validation

| Metric | Pixel 7 Pro | P20 Lite | Tool |
|--------|-------------|----------|------|
| Cold start | < 2s | < 4s | Android Profiler startup trace |
| Frame drops/min | < 2 | < 5 | GPU rendering profiler |
| Memory (4h) | < 200 MB | < 120 MB | Android Profiler memory |
| Battery/hour (screen on) | < 5% | < 7% | Battery Stats |
| APK size | < 15 MB | < 15 MB | `./gradlew assembleRelease` |

### Release Build

- [ ] Enable R8/ProGuard minification
- [ ] Configure ProGuard rules for Retrofit, Ktor, Kotlinx Serialization
- [ ] Generate signed release APK
- [ ] Verify release APK on both devices
- [ ] Open source license bundling (OSS Licenses Gradle plugin)

### Documentation

- [ ] README.md with project overview, build instructions, screenshots
- [ ] PC setup guide (AHK + Python)
- [ ] Troubleshooting FAQ

---

## Complete Dependency List

### Android (`build.gradle.kts`)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")  // or kapt
}

android {
    namespace = "com.mastercompanion"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.mastercompanion"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core library desugaring (java.time on API 28)
    coreLibraryDesugaring(libs.desugar.jdk)
    
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    
    // Navigation
    implementation(libs.navigation.compose)
    
    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.service)
    
    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)  // or kapt
    implementation(libs.hilt.navigation.compose)
    
    // Networking (Spotify API)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.kotlinx)
    
    // Ktor Server (Command Bridge)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.status.pages)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Coroutines
    implementation(libs.coroutines.android)
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // Security
    implementation(libs.security.crypto)
    
    // Browser (OAuth)
    implementation(libs.browser)
    
    // Logging
    implementation(libs.timber)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
}
```

### PC Side

**`requirements.txt`**
```
sounddevice>=0.4.6
numpy>=1.26.0
opuslib>=3.0.1
```

**AutoHotkey:** v2.0+ from [autohotkey.com](https://www.autohotkey.com/)

---

## Risk Register

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| sysfs paths differ on P20 Lite kernel | Battery root features fail | Medium | Auto-detect with 5 candidate paths + manual override in Settings |
| EMUI kills services despite whitelist | Background features unreliable | High | `START_STICKY` + restart logic + user education. Monitor EMUI-specific forums for workarounds. |
| Spotify rate limiting | Polling fails intermittently | Low | Adaptive poll interval (3–10s). Exponential backoff on 429. |
| AudioTrack latency > 300ms on Kirin 659 | Poor audio experience on P20 Lite | Medium | Tune jitter buffer. Use larger audio frames. Document P20 Lite latency as "best effort". |
| Opus decoding too slow on Kirin 659 | Audio glitches on P20 Lite | Low | Concentus (Java) should handle 48kHz stereo. Fall back to raw PCM if needed. |
| Compose perf on 4GB RAM | Frame drops on P20 Lite | Medium | Disable blur glow, reduce visualizer FPS, cap image cache at 30MB. |
| Custom Tabs unavailable on EMUI | OAuth flow breaks | Low | Fallback to browser Intent, then WebView. |
| Boot receiver not triggered on EMUI | Services don't auto-start | Medium | Guide user to enable auto-launch in EMUI Startup Manager. |
| Foreground service type not supported on API 28 | Service crashes | Low | `startForegroundCompat()` wrapper already handles this. |
| Root revoked by OTA | All root features break | Low | Pin Android version. Magisk OTA survival module. |

---

## Device Test Matrix

Every feature must be tested on both devices before marking as complete.

| Feature | Pixel 7 Pro (Root) | Pixel 7 Pro (No Root) | P20 Lite (Root) | P20 Lite (No Root) |
|---------|-------------------|----------------------|-----------------|-------------------|
| Clock + Date | ✓ | ✓ | ✓ | ✓ |
| Spotify Display | ✓ | ✓ | ✓ | ✓ |
| Spotify Controls | ✓ | ✓ | ✓ | ✓ |
| OAuth Flow | ✓ | ✓ | ✓ (EMUI) | ✓ (EMUI) |
| Battery % | ✓ | ✓ | ✓ | ✓ |
| Battery Wattage | ✓ (sysfs) | ⚠ (API est.) | ✓ (sysfs) | ⚠ (API est.) |
| Charge Limit | ✓ | ❌ (hidden) | ✓ (if path exists) | ❌ (hidden) |
| Command Bridge | ✓ | ✓ | ✓ | ✓ |
| Shell Commands | ✓ | ❌ (403) | ✓ | ❌ (403) |
| WOL | ✓ | ✓ | ✓ | ✓ |
| Audio Passthrough | ✓ | ✓ | ✓ | ✓ |
| Audio Visualizer | ✓ (30fps) | ✓ (30fps) | ✓ (15fps) | ✓ (15fps) |
| Notch Handling | N/A | N/A | ✓ | ✓ |
| EMUI Whitelist | N/A | N/A | ✓ | ✓ |
| Onboarding | ✓ | ✓ | ✓ | ✓ |
| 72h Soak | ✓ | — | — | — |
| 24h Soak | — | — | ✓ | ✓ |
