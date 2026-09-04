# Product Requirements Document (PRD)
## Master Companion — Standby Dashboard & PC Bridge
**Version:** 1.1  
**Date:** 2026-09-04  
**Status:** Draft  
**Author:** Engineering Lead  
**Reviewers:** —  
**Approval:** Pending  

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-09-04 | — | Initial draft |
| 1.1 | 2026-09-04 | — | Added Huawei P20 Lite (Android 9) support, expanded acceptance criteria, added device compatibility matrix, glossary, and regulatory notes |

---

## 1. Executive Summary

Master Companion is a landscape-oriented, dark-themed Android application that transforms a docked Android phone into a persistent desk dashboard. It serves as a smart clock, hardware monitor, media hub, and command bridge to a desktop PC. The system communicates over local Wi-Fi (UDP/TCP) and USB OTG, enabling macro-driven workflows, audio passthrough, and root-level battery management.

**Primary target:** Google Pixel 7 Pro (rooted, Android 13/14, Tensor G2)  
**Secondary target:** Huawei P20 Lite (ANE-LX1, Android 9 / EMUI 9.1, Kirin 659)

The secondary target introduces constraints around lower compute power, aggressive OEM battery management (EMUI), different sysfs paths, display notch handling, and API level limitations. Features requiring root access degrade gracefully on devices where root is unavailable.

---

## 2. Target Device Matrix

| Property | Pixel 7 Pro | Huawei P20 Lite |
|----------|------------|-----------------|
| **Model** | GE2AE / GP4BC | ANE-LX1 |
| **SoC** | Google Tensor G2 | HiSilicon Kirin 659 |
| **RAM** | 12 GB | 4 GB |
| **Display** | 6.7" LTPO AMOLED, 3120×1440, 120Hz | 5.84" IPS LCD, 2280×1080, 60Hz, notch |
| **Android Version** | 13 / 14 (API 33/34) | 9.0 Pie (API 28) |
| **OEM Skin** | Pixel UI | EMUI 9.1 |
| **Root Method** | Magisk (bootloader unlockable) | Magisk (bootloader unlock via code, may be restricted) |
| **Google Play Services** | Yes | Yes (pre-Huawei ban device) |
| **Battery** | 5000 mAh | 3000 mAh |
| **Battery sysfs** | `/sys/class/power_supply/battery/` | `/sys/class/power_supply/battery/` (paths vary by kernel) |
| **Headphone Jack** | No (USB-C audio) | Yes (3.5mm) |
| **USB** | USB-C 3.2 | Micro-USB 2.0 |

### Device-Specific Constraints

#### Huawei P20 Lite
- **EMUI Aggressive Battery Management:** EMUI kills background services aggressively. The app must request manual battery optimization exemption from the user and use Huawei-specific intents to whitelist itself.
- **Notch:** The display has a notch. Layout must use `WindowInsets` and avoid placing critical UI elements in the notch cutout zone.
- **Lower RAM:** With 4 GB RAM, the app must maintain a strict memory budget (< 150 MB heap). Album art should be downsampled. No in-memory caches exceeding 30 MB.
- **60Hz LCD:** Animations targeting 120Hz on the Pixel must degrade to 60Hz. No animation should depend on high refresh rates.
- **Kirin 659:** A mid-range 2017 SoC. Audio decoding (Opus) and sysfs polling must be lightweight. CPU-intensive visualizations must be optional.
- **Micro-USB:** USB OTG support is limited. Audio passthrough over USB is not available; Wi-Fi only.
- **API 28 Limitations:** No `FOREGROUND_SERVICE_TYPE_*` (added in API 29). Foreground services must use the legacy API. `WindowInsetsController` is unavailable — use `systemUiVisibility` flags instead.

---

## 3. User Personas

### Persona 1: The Power User ("Alex")
- **Role:** Software developer / PC enthusiast with a multi-monitor desk setup.
- **Device:** Pixel 7 Pro (rooted, docked via USB-C stand).
- **Context:** Keeps a docked phone on the desk at all times. Wants a glanceable dashboard that shows music, time, and PC status without switching windows.
- **Pain Points:**
  - Alt-tabbing to check Spotify.
  - No visibility into phone battery degradation while always-docked.
  - No convenient way to trigger PC macros from the phone.
- **Goals:** Single-glance dashboard, battery health preservation, seamless PC–phone interaction.
- **Technical Comfort:** High — comfortable with ADB, root, and AHK scripting.

### Persona 2: The Tinkerer ("Jordan")
- **Role:** Hobbyist who owns a USB macro keypad and enjoys automating workflows.
- **Device:** Pixel 7 Pro or similar rooted device.
- **Context:** Uses AutoHotkey on PC to remap macro keys. Wants physical buttons to trigger actions on the Android device.
- **Pain Points:**
  - No native way to bridge a PC macro board to Android actions.
  - Existing solutions require cloud services or complex setups.
- **Goals:** Zero-latency local command bridge, extensible action system, no cloud dependency.
- **Technical Comfort:** High — writes AHK scripts and Python utilities.

### Persona 3: The Repurposer ("Sam")
- **Role:** User who repurposes an older phone (Huawei P20 Lite) as a dedicated desk clock / dashboard.
- **Device:** Huawei P20 Lite (may or may not be rooted).
- **Context:** Has an old phone sitting in a drawer. Wants to give it a second life as a desk companion without needing root or advanced setup.
- **Pain Points:**
  - Old phone feels slow and cluttered with outdated apps.
  - No purpose for the device after upgrading.
  - Afraid of rooting and bricking the device.
- **Goals:** Functional dashboard with Spotify and clock. Root features are a bonus, not a requirement.
- **Technical Comfort:** Medium — can install APKs and connect to Wi-Fi, but avoids ADB/root.

---

## 4. Feature Tiers (Root vs. Non-Root)

Not all features require root. The app must be functional — and valuable — without root access.

| Feature | Without Root | With Root |
|---------|-------------|-----------|
| Spotify Dashboard | ✅ Full | ✅ Full |
| Clock / Date | ✅ Full | ✅ Full |
| Battery Level (%) | ✅ Via `BatteryManager` API | ✅ Via `BatteryManager` API |
| Battery Wattage (live) | ⚠️ Estimated (API-level voltage × current, may be unavailable) | ✅ Precise from sysfs |
| Battery Temperature | ✅ Via `BatteryManager` API | ✅ Via `BatteryManager` API or sysfs |
| Charge Limit (80% cap) | ❌ Not possible | ✅ sysfs write |
| Command Bridge (HTTP Server) | ✅ Full (except shell commands) | ✅ Full |
| Shell Command Execution | ❌ Blocked | ✅ Full |
| Wake-On-LAN | ✅ Full | ✅ Full |
| Audio Passthrough | ✅ Full | ✅ Full |
| Keep Screen On | ✅ `FLAG_KEEP_SCREEN_ON` | ✅ Same |

---

## 5. Core User Stories & Acceptance Criteria

### Epic 1: Media Dashboard (Spotify)

| ID | User Story | Acceptance Criteria | Priority |
|----|-----------|---------------------|----------|
| US-1.1 | As a user, I want to see the currently playing Spotify track on my docked phone so I can glance at song info without touching my PC. | Album art (≥300×300px, downsampled to 400dp max), track title (scrolling marquee if truncated), and artist name are displayed. Data refreshes every 3–5 seconds via polling. Works on API 28+. | **P0** |
| US-1.2 | As a user, I want the app to authenticate with Spotify using OAuth PKCE so my credentials are never stored on disk. | OAuth PKCE flow completes via Custom Tabs (API 28+). Access token is stored in-memory with refresh token in EncryptedSharedPreferences. Token auto-refreshes 60s before expiry. | **P0** |
| US-1.3 | As a user, I want a fallback display when nothing is playing so the dashboard never looks broken. | If `/me/player/currently-playing` returns 204/empty, the app queries `/me/player/recently-played` and displays the last track with a "Recently Played" badge. If both fail, a default clock/idle screen is shown. | **P1** |
| US-1.4 | As a user, I want playback controls (play/pause, skip forward, skip back) on the dashboard. | Controls send Spotify Web API requests. Controls reflect current playback state. Disabled state shown if no active device. | **P1** |
| US-1.5 | As Sam, I want the Spotify login to work on my Huawei P20 Lite without issues. | OAuth Custom Tab opens in default browser. Deep link callback works on EMUI 9.1. Fallback to WebView if Custom Tabs fail. | **P1** |

### Epic 2: Root-Level Power & Battery Management

| ID | User Story | Acceptance Criteria | Priority |
|----|-----------|---------------------|----------|
| US-2.1 | As a user, I want to see live charge/discharge wattage on my dashboard. | **Rooted:** Voltage and current read from sysfs, wattage calculated and displayed (update ≤ 2s). **Non-rooted:** Battery level, charging status, and estimated current from `BatteryManager` API displayed instead. | **P0** |
| US-2.2 | As Alex, I want to cap my battery at 80% while docked to preserve long-term battery health. | **Rooted only.** A toggle writes `0` to `charging_enabled` sysfs node when battery ≥ 80%. Writes `1` when battery ≤ 75% (5% hysteresis). Toggle hidden/disabled on non-rooted devices. | **P0** |
| US-2.3 | As Alex, I want the charge limit to persist across app restarts and screen-off states. | Foreground service monitors battery level. On API 28, uses legacy `startForeground(id, notification)` without type parameter. On API 29+, uses typed foreground service. | **P1** |
| US-2.4 | As Sam, I want battery info even without root on my Huawei. | `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW` and `ACTION_BATTERY_CHANGED` broadcast used to display level, temperature, and charging status. Wattage section shows "Root required" badge. | **P1** |
| US-2.5 | As a user, I want the app to survive EMUI's aggressive battery optimization. | On Huawei devices, the app detects EMUI and prompts the user to: (1) Disable battery optimization for this app, (2) Enable "Allow background activity", (3) Add to "Protected Apps". Deep links to Huawei-specific settings intents are provided. | **P1** |

### Epic 3: Command Bridge (HTTP Server & Macros)

| ID | User Story | Acceptance Criteria | Priority |
|----|-----------|---------------------|----------|
| US-3.1 | As Jordan, I want the app to host a local HTTP server so my PC can send commands to it. | Server starts on app launch, listens on configurable port (default: `8420`). `GET /ping` returns `200 OK` with JSON status. Works on all target devices. | **P0** |
| US-3.2 | As Jordan, I want to press a physical macro key on my PC and have it trigger an action on the Android app. | PC-side AHK sends `POST /command` with JSON body. App executes mapped action within 200ms (on Pixel) / 500ms (on P20 Lite). | **P0** |
| US-3.3 | As Jordan, I want a registry of available commands so I can extend the system without recompiling. | Commands defined in `commands.json` on device. Each entry maps an `action` string to a handler type. | **P1** |
| US-3.4 | As Jordan, I want command execution to be authenticated. | `X-Auth-Token` header required on POST requests. Unauthorized requests return `401`. | **P1** |
| US-3.5 | As a user, I want to see the device's IP address clearly so I can configure the PC scripts. | The System page and a first-run prompt display the device's current Wi-Fi IP address prominently. A "Copy IP" button is available. | **P2** |

### Epic 4: Wake-On-LAN & Remote Execution

| ID | User Story | Acceptance Criteria | Priority |
|----|-----------|---------------------|----------|
| US-4.1 | As Alex, I want to wake my PC from the phone dashboard. | UDP Magic Packet broadcast. Confirmation toast shown. Works on API 28+. | **P0** |
| US-4.2 | As Jordan, I want the app to execute local shell scripts (root only). | Command bridge invokes `su -c <script>`. Output logged to ring buffer. Disabled on non-rooted devices. | **P1** |

### Epic 5: Audio Passthrough

| ID | User Story | Acceptance Criteria | Priority |
|----|-----------|---------------------|----------|
| US-5.1 | As Alex, I want to route PC audio to my phone's headphone jack. | PC streams Opus-encoded audio via UDP. Android decodes and plays via `AudioTrack`. Latency < 150ms (Pixel) / < 300ms (P20 Lite). | **P0** |
| US-5.2 | As a user, I want volume and mute controls for the audio stream. | Volume slider adjusts `AudioTrack` gain. Mute stops buffer playback without disconnecting. | **P1** |
| US-5.3 | As a user, I want the stream to auto-reconnect on network drop. | 5-second timeout triggers reconnect loop (exponential backoff, 30s cap). "Disconnected" indicator shown. | **P1** |
| US-5.4 | As Sam, I want audio passthrough to work on my P20 Lite's 3.5mm jack. | Standard `AudioTrack` output routes to 3.5mm jack when connected. No USB audio path needed on P20 Lite. | **P1** |

---

## 6. Out of Scope (v1.0)

| Item | Rationale |
|------|-----------|
| Cloud sync or remote access outside LAN | Security & privacy — local-only by design. |
| iOS / cross-platform support | Root dependency. Kotlin/Compose is Android-only. |
| Spotify on-device playback | App is display/remote-control only. |
| Video streaming from PC | Bandwidth and latency too high for v1. |
| Google Assistant / voice control | Not aligned with physical-macro-first model. |
| Multi-user / multi-device pairing | v1 = single PC ↔ single phone. |
| OTA update mechanism | Manual APK sideload for v1. |
| Widgets / Quick Settings tiles | Stretch goal for v1.1. |
| Wear OS companion | Out of scope. |
| Bluetooth audio streaming (from PC) | Wi-Fi/USB only for v1. |
| Device-to-device communication (phone-to-phone) | Single topology only. |

---

## 7. Non-Functional Requirements

| Category | Requirement | Pixel 7 Pro Target | P20 Lite Target |
|----------|-------------|--------------------|-----------------| 
| **Min SDK** | API 28 (Android 9 Pie) | ✅ | ✅ |
| **Target SDK** | API 34 (Android 14) | ✅ | ✅ (runtime on API 28) |
| **UI Performance** | Smooth rendering | 60fps (120Hz capable) | 60fps |
| **Command Latency** | HTTP POST → action executed | < 200ms | < 500ms |
| **Memory Budget** | Max heap allocation | < 256 MB | < 150 MB |
| **Battery Draw** | Foreground with screen on | < 5%/hour | < 7%/hour |
| **Background Service** | Battery consumption | < 1%/hour | < 2%/hour |
| **Continuous Uptime** | Without crash or OOM | ≥ 72 hours | ≥ 24 hours |
| **Cold Start Time** | App launch to dashboard visible | < 2 seconds | < 4 seconds |
| **APK Size** | Release APK | < 15 MB | < 15 MB |
| **Security** | Token auth, encrypted storage | ✅ | ✅ |
| **Usability** | Legible from arm's length (60cm) | ✅ | ✅ (adjusted for 5.84") |
| **Accessibility** | Touch targets ≥ 48dp, contrast ≥ 4.5:1 | ✅ | ✅ |

---

## 8. Success Metrics (v1.0)

| Metric | Pixel 7 Pro Target | P20 Lite Target |
|--------|--------------------|-----------------| 
| Spotify data refresh | < 5s from track change | < 8s from track change |
| Command bridge round-trip | < 200ms | < 500ms |
| Audio passthrough latency | < 150ms | < 300ms |
| Continuous uptime | ≥ 72 hours | ≥ 24 hours |
| Battery health (30 days docked) | < 2% wear | N/A (no charge limit without root) |
| Memory (after 4 hours) | < 200 MB | < 120 MB |
| Frame drops per minute | < 2 | < 5 |

---

## 9. Dependencies & Assumptions

### External Dependencies

| Dependency | Required For | Fallback |
|------------|-------------|----------|
| Spotify Premium account | Web API playback control | Display-only mode (no controls) works with Free |
| Spotify Developer App registration | OAuth credentials | None — hard requirement |
| Rooted device (Magisk) | Battery charge limit, shell execution | Graceful degradation to non-root features |
| Same LAN (Wi-Fi) | Command bridge, audio passthrough, WOL | USB tethering as alternative |
| AutoHotkey v2 (PC) | Macro bridge | Any HTTP client (curl, PowerShell) can substitute |
| Python 3.x (PC) | Audio streaming server | Any tool that sends UDP audio (e.g., ffmpeg) |
| Google Play Services | OAuth Custom Tabs | WebView fallback |

### Assumptions

| Assumption | Risk if Wrong |
|-----------|---------------|
| Pixel 7 Pro sysfs paths stable across Android 13/14 | Battery features break — mitigated by path auto-detection |
| Huawei P20 Lite sysfs paths follow standard Linux `/sys/class/power_supply/` | Root battery features may not work — mitigated by `BatteryManager` API fallback |
| Device is docked in landscape with screen always on | Battery drain higher than expected — documented trade-off |
| User accepts root risks (voided warranty, potential brick) | Feature degradation — non-root mode available |
| EMUI battery optimization can be manually disabled | Background services may be killed — detection and user prompt implemented |
| Local network latency < 5ms | Audio latency target may be missed on congested networks |

---

## 10. Glossary

| Term | Definition |
|------|-----------|
| **AHK** | AutoHotkey — a Windows scripting language for automating keystrokes and mouse actions. |
| **Command Bridge** | The embedded HTTP server on the Android app that receives and executes commands from the PC. |
| **EMUI** | Emotion UI — Huawei's Android skin, known for aggressive background process management. |
| **Magic Packet** | A 102-byte UDP broadcast used to wake a PC from sleep/hibernate (Wake-On-LAN). |
| **OLED** | Organic Light-Emitting Diode — display technology where black pixels are truly off (zero power). |
| **Opus** | An open, royalty-free audio codec designed for low-latency interactive audio over the internet. |
| **PKCE** | Proof Key for Code Exchange — an OAuth 2.0 extension for public clients (no client secret). |
| **sysfs** | A pseudo filesystem in Linux (Android) exposing kernel/hardware info as files under `/sys/`. |
| **WOL** | Wake-On-LAN — a protocol to power on a PC remotely via a network packet. |
| **Hysteresis** | A dead-band in the charge limiter (e.g., stop at 80%, resume at 75%) to prevent rapid toggling. |
| **Jitter Buffer** | A buffer that reorders and smooths out network packet arrival timing for audio playback. |
| **DataStore** | Jetpack library replacing SharedPreferences for type-safe, async, coroutine-based storage. |

---

## 11. Regulatory & Compliance Notes

| Area | Note |
|------|------|
| **Spotify Developer Terms** | The app must comply with Spotify's [Developer Terms of Service](https://developer.spotify.com/terms/). Album art must link back to Spotify. Spotify branding guidelines must be followed for the play button icon. |
| **Root Access Disclaimer** | The app must display a clear disclaimer that root-level battery modification may void the device warranty and carries risk of misconfiguration. User must explicitly acknowledge before enabling charge limit. |
| **Data Privacy** | No user data leaves the local network. No analytics, no telemetry, no crash reporting to external services. OAuth tokens are stored encrypted. This should be documented in a privacy notice within the app's Settings page. |
| **Open Source Licenses** | All third-party libraries must have their licenses bundled in the app (accessible from Settings > About > Open Source Licenses). Use the Gradle OSS Licenses plugin. |
| **Network Permissions** | The app accesses the local network only. On Android 13+, the `NEARBY_WIFI_DEVICES` permission may be required for certain network discovery features. |

---

## 12. Release Criteria (v1.0 GA)

All of the following must be satisfied before v1.0 is considered shippable:

- [ ] All P0 user stories pass acceptance criteria on **both** target devices.
- [ ] All P1 user stories pass acceptance criteria on the Pixel 7 Pro (P20 Lite: best effort).
- [ ] No P0/P1 bugs open.
- [ ] 72-hour soak test passes on Pixel 7 Pro without crash or OOM.
- [ ] 24-hour soak test passes on Huawei P20 Lite without crash or OOM.
- [ ] Memory profile shows no upward trend after 4 hours.
- [ ] APK size < 15 MB (release, minified).
- [ ] All open-source licenses documented.
- [ ] Root disclaimer reviewed and approved.
- [ ] PC-side companion scripts (AHK + Python) documented and tested.
