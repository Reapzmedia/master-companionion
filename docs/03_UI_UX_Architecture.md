# UI/UX Architecture
## Master Companion — Standby Dashboard & PC Bridge
**Version:** 1.1  
**Date:** 2026-09-04  
**Status:** Draft  

---

## Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-09-04 | Initial draft |
| 1.1 | 2026-09-04 | Added notch handling for P20 Lite, IPS LCD color adjustments, memory-constrained image loading, smaller screen adaptations, onboarding flow, error state wireframes |

---

## 1. Design Principles

| Principle | Rationale |
|-----------|-----------|
| **Dark Theme First** | Pure black (`#000000`) for OLED power savings (Pixel). Near-black (`#0A0A0A`) for IPS LCD readability (P20 Lite — avoids IPS glow/banding on pure black). |
| **Glanceable at Arm's Length** | Primary text ≥ 24sp, secondary ≥ 16sp. High contrast (WCAG AAA on key metrics). |
| **Landscape-Locked** | All layouts target landscape orientation. Notch is on the left or right edge in landscape. |
| **Device-Adaptive** | Layout adapts to 6.7" (Pixel) and 5.84" (P20 Lite) screens. Components scale via `dp` and responsive weight allocation. |
| **Zero-Friction Interaction** | Primary mode is *looking*, not tapping. Touch targets are large (≥56dp) but infrequent. |
| **State-Reactive UI** | Every visual element driven by `StateFlow`. No imperative UI updates. |
| **Memory-Conscious** | Album art downsampled. No unbounded caches. Animations respect device capability. |
| **Graceful Degradation** | Root-only features show "Root required" badges. Unavailable features are dimmed, not hidden. |

---

## 2. Display Specifications

| Property | Pixel 7 Pro | Huawei P20 Lite |
|----------|------------|-----------------|
| Resolution | 3120 × 1440 (landscape) | 2280 × 1080 (landscape) |
| Aspect Ratio | 19.5:9 | 19:9 |
| Physical Size | 6.7" | 5.84" |
| DPI | ~512 dpi (xxxhdpi) | ~432 dpi (xxhdpi) |
| Panel Type | LTPO AMOLED | IPS LCD |
| Refresh Rate | 120Hz / 60Hz | 60Hz |
| Notch | None (punch-hole camera, minimal) | Display notch (center-top → left edge in landscape) |
| Safe Area (landscape) | Full width | Notch inset ~84dp on left edge |

### Notch Handling (P20 Lite)

In landscape mode, the P20 Lite's notch appears on the **left edge** (or right, depending on rotation). The app must:

1. Set `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` to render content behind the notch area.
2. Read `WindowInsets.displayCutout` to get the exact cutout bounds.
3. Add padding to avoid placing interactive elements or critical text within the cutout zone.

```kotlin
// In Compose
val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()

Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(cutoutPadding)
) {
    // Dashboard content — safe from notch
}
```

---

## 3. Screen Map & Navigation

```
┌─────────────────────────────────────────────────────────────┐
│                     MAIN ACTIVITY                            │
│              (Landscape, Immersive, KEEP_SCREEN_ON)          │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              DASHBOARD HOST (HorizontalPager)         │  │
│  │                                                        │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐ │  │
│  │  │ Page 0  │  │ Page 1  │  │ Page 2  │  │ Page 3   │ │  │
│  │  │ HOME    │  │ AUDIO   │  │ SYSTEM  │  │ SETTINGS │ │  │
│  │  └─────────┘  └─────────┘  └─────────┘  └──────────┘ │  │
│  │                                                        │  │
│  │  ◄── Horizontal swipe / command-bridge nav ──►         │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              ONBOARDING OVERLAY                        │  │
│  │  Step 1: Root Check                                    │  │
│  │  Step 2: EMUI Battery Whitelist (if Huawei)           │  │
│  │  Step 3: Spotify Auth                                  │  │
│  │  Step 4: Network Config (show IP, set ports)          │  │
│  │  Step 5: Optional: WOL MAC address                    │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              ROOT DISCLAIMER DIALOG                    │  │
│  │  "Modifying battery charging via root access may      │  │
│  │   void your warranty and risk damage if misconfigured"│  │
│  │  [I Understand, Enable] [Keep Disabled]               │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Navigation State

```kotlin
data class NavigationState(
    val currentPage: Int = 0,     // 0=Home, 1=Audio, 2=System, 3=Settings
    val showOnboarding: Boolean = true,  // First launch
    val showRootDisclaimer: Boolean = false
)
```

Navigation can be triggered by:
1. **User swipe** — `HorizontalPager` gesture.
2. **Command bridge** — `POST /command {"action":"navigate","params":{"page":"audio"}}`.
3. **Programmatic** — From onboarding completion, settings changes, etc.

---

## 4. Page Layouts

### 4.1 Page 0 — Home Dashboard

The primary glanceable screen. Adapts to both screen sizes.

#### Pixel 7 Pro Layout (6.7", high density)

```
┌──────────────────────────────────────────────────────────────────────┐
│  3120 × 1440 px (Landscape)         24dp padding all sides          │
│                                                                      │
│  ┌──────────────────────────┐  ┌──────────────────────────────────┐  │
│  │                          │  │                                  │  │
│  │      ALBUM ART           │  │  TRACK TITLE (marquee if long)  │  │
│  │      (1:1, 300dp max)    │  │  28sp SemiBold                  │  │
│  │      Rounded 24dp        │  │                                  │  │
│  │      Blurred glow bg     │  │  ARTIST NAME                   │  │
│  │                          │  │  20sp Regular                    │  │
│  │                          │  │                                  │  │
│  │                          │  │  ━━━━━━━●━━━━━━━━━ 2:15 / 3:42  │  │
│  │                          │  │                                  │  │
│  │                          │  │  ◄◄    ▶/❚❚    ►►               │  │
│  │                          │  │  (56dp touch targets)            │  │
│  ├──────────────────────────┤  ├──────────────────────────────────┤  │
│  │                          │  │                                  │  │
│  │  🕐 14:32               │  │  ⚡ BATTERY                     │  │
│  │  72sp Thin               │  │  ████████████░░ 78%              │  │
│  │  Thursday, Sep 4         │  │  1.85W  │  28.5°C  │  ⚡ Limited│  │
│  │  18sp Regular            │  │                                  │  │
│  │                          │  │  [Toggle Charge Limit]  [Wake PC]│  │
│  │                          │  │  56dp height buttons             │  │
│  └──────────────────────────┘  └──────────────────────────────────┘  │
│                                                                      │
│  ● ○ ○ ○  (page indicator, bottom-center, 8dp dots)                │
└──────────────────────────────────────────────────────────────────────┘
```

#### Huawei P20 Lite Layout (5.84", notch)

```
┌───┬──────────────────────────────────────────────────────────────────┐
│   │  2280 × 1080 px (Landscape, notch on left)    16dp padding      │
│ N │                                                                  │
│ O │  ┌─────────────────────┐  ┌────────────────────────────────────┐│
│ T │  │                     │  │  TRACK TITLE (marquee)             ││
│ C │  │   ALBUM ART         │  │  24sp SemiBold                    ││
│ H │  │   (1:1, 220dp max)  │  │  ARTIST NAME  ·  Album           ││
│   │  │   Rounded 16dp      │  │  16sp Regular                     ││
│ 8 │  │   No blur glow      │  │                                    ││
│ 4 │  │   (save GPU)        │  │  ━━━━━━━●━━━━━ 2:15 / 3:42       ││
│ d │  │                     │  │  ◄◄   ▶/❚❚   ►►  (48dp targets)  ││
│ p │  ├─────────────────────┤  ├────────────────────────────────────┤│
│   │  │ 🕐 14:32           │  │  ⚡ 78% │ 1.85W │ 28.5°C          ││
│ p │  │ 56sp Thin           │  │  ████████████░░                    ││
│ a │  │ Sep 4               │  │  [Charge Limit] [Wake PC]          ││
│ d │  └─────────────────────┘  └────────────────────────────────────┘│
│   │                                                                  │
│   │  ● ○ ○ ○                                                        │
└───┴──────────────────────────────────────────────────────────────────┘
```

**Key differences on P20 Lite:**
- 84dp left padding for notch.
- Album art capped at 220dp (vs 300dp) to save RAM and layout space.
- Clock reduced to 56sp (vs 72sp).
- Date abbreviated ("Sep 4" vs "Thursday, September 4").
- No blurred glow behind album art (GPU/memory savings on Kirin 659).
- Battery info condensed to a single row.

#### Composable Tree

```
HomePage
├── WindowInsets.displayCutout padding
├── Column(modifier = Modifier.fillMaxSize().padding(pagePadding))
│   ├── Row(modifier = Modifier.weight(1f))   // Top half
│   │   ├── AlbumArtCard(modifier = Modifier.weight(0.4f))
│   │   │   ├── if (isHighEndDevice) BlurredGlowBackground
│   │   │   └── AsyncImage(
│   │   │         model = ImageRequest.Builder(context)
│   │   │             .data(albumUrl)
│   │   │             .size(if (isCompact) 440 else 600)  // Pixel density adjusted
│   │   │             .crossfade(500)
│   │   │             .memoryCachePolicy(CachePolicy.ENABLED)
│   │   │             .diskCachePolicy(CachePolicy.ENABLED)
│   │   │             .build()
│   │   │       )
│   │   └── Column(modifier = Modifier.weight(0.6f))
│   │       ├── MarqueeText(track.name, style = trackTitle)
│   │       ├── Text(track.artists, style = artistName)
│   │       ├── ProgressBar(track.progressMs, track.durationMs)
│   │       └── PlaybackControls(isPlaying, onPlay, onPause, onNext, onPrev)
│   │
│   ├── Spacer(height = gridGap)
│   │
│   ├── Row(modifier = Modifier.weight(1f))   // Bottom half
│   │   ├── ClockWidget(modifier = Modifier.weight(0.4f))
│   │   │   ├── Text(time, style = clockDisplay)  // 72sp or 56sp
│   │   │   └── Text(date, style = dateDisplay)
│   │   └── BatteryPanel(modifier = Modifier.weight(0.6f))
│   │       ├── BatteryBar(level, isCharging)
│   │       ├── MetricsRow(wattage, temperature, status)
│   │       └── ActionButtons(onToggleCharge, onWakePC)
│   │
│   └── PageIndicator(currentPage = 0, totalPages = 4)
```

---

### 4.2 Page 1 — Audio Passthrough

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────────┐│
│  │                     AUDIO VISUALIZER                            ││
│  │              (Canvas bars or waveform)                           ││
│  │              Pixel: 30fps  │  P20 Lite: 15fps                   ││
│  │              Optional: disabled via Settings                     ││
│  │                                                                  ││
│  └──────────────────────────────────────────────────────────────────┘│
│                                                                      │
│  ┌────────────────────────────────────┐  ┌──────────────────────────┐│
│  │  CONNECTION STATUS                  │  │  CONTROLS               ││
│  │  ● Connected (green)               │  │                          ││
│  │  ○ Disconnected (red, pulsing)     │  │  ──────●────── Vol  80% ││
│  │  ◐ Reconnecting... (amber, spin)  │  │                          ││
│  │                                    │  │  [🔇 Mute]   [⏹ Stop]  ││
│  │  Latency:  85ms                    │  │                          ││
│  │  Codec:    Opus 48kHz stereo       │  │  Audio output: 3.5mm    ││
│  │  Received: 12,345 pkts (0 lost)   │  │  (or: USB-C / Speaker)  ││
│  └────────────────────────────────────┘  └──────────────────────────┘│
│                                                                      │
│  ○ ● ○ ○                                                            │
└──────────────────────────────────────────────────────────────────────┘
```

**P20 Lite adaptation:** Visualizer runs at 15fps or is replaced with a static spectrum icon. Reduces CPU/GPU load on Kirin 659.

---

### 4.3 Page 2 — System & Debug

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  ┌──────────────────────────┐  ┌──────────────────────────────────┐  │
│  │  DEVICE INFO              │  │  COMMAND LOG                    │  │
│  │                           │  │                                  │  │
│  │  Model:    P20 Lite      │  │  19:42:01 toggle_charge → OK    │  │
│  │  Android:  9 (API 28)    │  │  19:41:55 GET /ping ← 200      │  │
│  │  OEM:      EMUI 9.1      │  │  19:41:30 wol → sent           │  │
│  │  Root:     ✗ Unavailable │  │  19:40:12 navigate:audio → OK   │  │
│  │  Uptime:   14h 22m       │  │  ...                             │  │
│  │                           │  │                                  │  │
│  │  SERVER STATUS            │  │  (scrollable, 200 entries)       │  │
│  │  Bridge:  :8420 ✓        │  │                                  │  │
│  │  Audio:   :8421 ✓        │  │  [Clear Log]  [Copy to Clipboard]│  │
│  │  IP:      192.168.1.42   │  │                                  │  │
│  │  [📋 Copy IP]            │  │                                  │  │
│  │                           │  │                                  │  │
│  │  MEMORY                   │  │                                  │  │
│  │  Heap: 85 / 150 MB       │  │                                  │  │
│  │  Native: 12 MB           │  │                                  │  │
│  └──────────────────────────┘  └──────────────────────────────────┘  │
│                                                                      │
│  ○ ○ ● ○                                                            │
└──────────────────────────────────────────────────────────────────────┘
```

---

### 4.4 Page 3 — Settings

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │  SETTINGS                                        (scrollable)  │ │
│  │                                                                 │ │
│  │  ─── Spotify ───────────────────────────────────────────────── │ │
│  │  Account: user@email.com                        [Sign Out]     │ │
│  │  Status: ● Connected                                           │ │
│  │  Poll interval: [3s ▼] (3s / 5s / 10s)                       │ │
│  │                                                                 │ │
│  │  ─── Network ──────────────────────────────────────────────── │ │
│  │  Server Port:     [8420]           Audio Port:     [8421]      │ │
│  │  Auth Token:      ●●●●●●●●  [Regenerate]  [📋 Copy]          │ │
│  │                                                                 │ │
│  │  ─── Battery ──────────────────────────────────────────────── │ │
│  │  Charge Limit:    [80] %           Hysteresis:     [5] %      │ │
│  │  sysfs path:      [auto-detect ▼]                             │ │
│  │  ⚠ Root required for charge limiting                          │ │
│  │                                                                 │ │
│  │  ─── Wake-On-LAN ────────────────────────────────────────── │ │
│  │  PC MAC Address:  [AA:BB:CC:DD:EE:FF]                         │ │
│  │  Broadcast IP:    [255.255.255.255]                            │ │
│  │                                                                 │ │
│  │  ─── Display ─────────────────────────────────────────────── │ │
│  │  Keep screen on:          [✓]                                 │ │
│  │  Audio visualizer:        [✓]  (disable to save battery)     │ │
│  │  Album art blur glow:     [✓]  (disable on low-end devices)  │ │
│  │  Visualizer FPS:          [30 ▼] (15 / 30)                   │ │
│  │                                                                 │ │
│  │  ─── Device ──────────────────────────────────────────────── │ │
│  │  Start on boot:           [✓]                                 │ │
│  │  EMUI battery whitelist:  [Open Settings →]                   │ │
│  │                                                                 │ │
│  │  ─── About ───────────────────────────────────────────────── │ │
│  │  Version 1.0.0 • Build 1 • API 28                            │ │
│  │  [Open Source Licenses]  [Privacy Notice]                      │ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ○ ○ ○ ●                                                            │
└──────────────────────────────────────────────────────────────────────┘
```

---

### 4.5 Onboarding Flow (First Run)

```
┌─────────────────────────────────────────────────────────────────┐
│                     STEP 1 of 5                                  │
│                                                                  │
│              🔍 Checking Device Capabilities                    │
│                                                                  │
│              ┌─────────────────────────────────┐                │
│              │  Root Access:    ✗ Not detected  │                │
│              │  Device:         Huawei P20 Lite │                │
│              │  Android:        9 (API 28)      │                │
│              │  OEM Skin:       EMUI 9.1        │                │
│              │  Google Play:    ✓ Available     │                │
│              └─────────────────────────────────┘                │
│                                                                  │
│  ⚠ Root is not available. Battery charge limiting and shell     │
│    execution will be disabled. All other features will work.     │
│                                                                  │
│                                           [Continue →]           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     STEP 2 of 5 (Huawei only)                    │
│                                                                  │
│              ⚠️ Huawei Battery Optimization                     │
│                                                                  │
│  Your Huawei device may kill this app in the background.        │
│  Please complete these steps to keep the dashboard running:      │
│                                                                  │
│  [1] ☐ Disable battery optimization  [Open Settings →]          │
│  [2] ☐ Enable auto-launch           [Open Settings →]          │
│  [3] ☐ Add to Protected Apps        [Open Settings →]          │
│                                                                  │
│  [Skip — I'll do this later]              [Continue →]           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     STEP 3 of 5                                  │
│                                                                  │
│              🎵 Connect Spotify                                  │
│                                                                  │
│  Sign in to display your currently playing track on the          │
│  dashboard. A Spotify Premium account enables playback controls. │
│                                                                  │
│              [Sign in with Spotify]                              │
│                                                                  │
│  [Skip — I'll set this up later]          [Continue →]           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     STEP 4 of 5                                  │
│                                                                  │
│              🌐 Network Configuration                            │
│                                                                  │
│  Your device's IP address:                                       │
│  ┌────────────────────────────────┐                              │
│  │  📋 192.168.1.42              │  [Copy]                      │
│  └────────────────────────────────┘                              │
│                                                                  │
│  Command Bridge Port:  [8420]                                    │
│  Audio Stream Port:    [8421]                                    │
│                                                                  │
│  Use this IP and port in your PC's AHK script and audio          │
│  streamer configuration.                                         │
│                                                                  │
│                                           [Continue →]           │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     STEP 5 of 5                                  │
│                                                                  │
│              ✅ Setup Complete!                                   │
│                                                                  │
│  Your dashboard is ready. Features enabled:                      │
│                                                                  │
│  ✓ Clock & Date                                                  │
│  ✓ Spotify Display (connected)                                   │
│  ✓ Command Bridge (port 8420)                                    │
│  ✓ Audio Passthrough (port 8421)                                │
│  ✓ Wake-On-LAN                                                  │
│  ✗ Battery Charge Limit (root required)                          │
│  ✗ Shell Execution (root required)                               │
│                                                                  │
│                                    [Launch Dashboard →]          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Design System

### 5.1 Color Palette

| Token | Hex | OLED (Pixel) | IPS (P20 Lite) | Usage |
|-------|-----|-------------|-----------------|-------|
| `surface` | `#000000` | Pure black (0 power) | Use `#0A0A0A` variant to reduce IPS banding | Background |
| `surfaceContainer` | `#1A1A1A` | Card bg | Card bg | Cards, panels |
| `surfaceContainerHigh` | `#2A2A2A` | Elevated | Elevated | Modals, sheets |
| `onSurface` | `#E8E8E8` | Primary text | Primary text | — |
| `onSurfaceVariant` | `#9E9E9E` | Secondary text | Secondary text | Labels |
| `primary` | `#BB86FC` | Accent | Accent | Active indicators, primary buttons |
| `primaryContainer` | `#3700B3` | Button bg | Button bg | Filled buttons |
| `secondary` | `#03DAC6` | Secondary accent | Secondary accent | Links, secondary actions |
| `error` | `#CF6679` | Error | Error | Disconnected, failures |
| `success` | `#4CAF50` | Success | Success | Connected, healthy |
| `warning` | `#FFB74D` | Warning | Warning | Charge limited, caution |
| `batteryGreen` | `#76FF03` | — | — | Battery bar >50% |
| `batteryAmber` | `#FFAB00` | — | — | Battery bar 20–50% |
| `batteryRed` | `#FF1744` | — | — | Battery bar <20% |

### 5.2 Typography Scale

| Style | Font | Pixel 7 Pro | P20 Lite | Weight | Usage |
|-------|------|-------------|----------|--------|-------|
| `clockDisplay` | Roboto Mono | 72sp | 56sp | Thin (100) | Clock time |
| `trackTitle` | Inter | 28sp | 24sp | SemiBold (600) | Song name |
| `artistName` | Inter | 20sp | 16sp | Regular (400) | Artist, album |
| `metricValue` | JetBrains Mono | 24sp | 20sp | Medium (500) | Wattage, temp |
| `metricLabel` | Inter | 14sp | 12sp | Regular (400) | Labels |
| `logEntry` | JetBrains Mono | 12sp | 11sp | Regular (400) | Debug log |
| `buttonLabel` | Inter | 16sp | 14sp | Medium (500) | Button text |
| `settingsHeader` | Inter | 14sp | 13sp | Bold (700) | Section headers |
| `settingsBody` | Inter | 16sp | 14sp | Regular (400) | Setting descriptions |
| `onboardingTitle` | Inter | 24sp | 20sp | SemiBold (600) | Onboarding step title |
| `badge` | Inter | 12sp | 11sp | Medium (500) | "Recently Played", "Root Required" |

**Device-adaptive typography helper:**

```kotlin
@Composable
fun adaptiveTextSize(pixelSize: TextUnit, compactSize: TextUnit): TextUnit {
    val config = LocalConfiguration.current
    return if (config.screenWidthDp < 700) compactSize else pixelSize
}
```

### 5.3 Spacing & Layout Tokens

| Token | Pixel 7 Pro | P20 Lite | Usage |
|-------|-------------|----------|-------|
| `paddingPage` | 24dp | 16dp | Outer page padding |
| `paddingCard` | 16dp | 12dp | Inner card padding |
| `gapGrid` | 16dp | 12dp | Grid cell gap |
| `cornerRadiusCard` | 16dp | 12dp | Card corners |
| `cornerRadiusAlbumArt` | 24dp | 16dp | Album art corners |
| `touchTargetMin` | 48dp | 48dp | Minimum (same both) |
| `touchTargetPreferred` | 56dp | 48dp | Preferred button height |
| `iconSize` | 28dp | 24dp | Standard icons |
| `iconSizeLarge` | 40dp | 32dp | Playback controls |
| `pageIndicatorDot` | 8dp | 6dp | Dot diameter |
| `notchPadding` | 0dp | 84dp (left edge) | Notch safe area |

### 5.4 Animation Specifications

| Element | Pixel 7 Pro | P20 Lite | Duration | Easing |
|---------|-------------|----------|----------|--------|
| Page transition | Horizontal slide | Horizontal slide | 300ms | `FastOutSlowIn` |
| Album art swap | Crossfade | Crossfade | 500ms | `LinearOutSlowIn` |
| Battery bar | Width animation | Width animation | 400ms | `FastOutSlowIn` |
| Track title | Marquee scroll | Marquee scroll | Continuous | Linear, 50dp/s |
| Status chip | Color tween | Color tween | 250ms | `FastOutSlowIn` |
| Audio visualizer | 30fps bars | 15fps bars (or static) | Per-frame | Direct mapping |
| Album glow | Blur pulse | **Disabled** | 2000ms | `EaseInOutSine` |
| Loading spinner | Rotate | Rotate | Continuous | Linear |
| Onboarding step | Fade + slide up | Fade + slide up | 400ms | `FastOutSlowIn` |

**Animation performance guard:**

```kotlin
@Composable
fun shouldAnimate(): Boolean {
    val context = LocalContext.current
    val animatorScale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1.0f
    )
    return animatorScale > 0f
}

@Composable
fun isHighEndDevice(): Boolean {
    val config = LocalConfiguration.current
    val activityManager = LocalContext.current.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)
    return memInfo.totalMem > 6L * 1024 * 1024 * 1024  // > 6GB RAM
}
```

---

## 6. State Management Architecture

### 6.1 Complete UI State Tree

```kotlin
// ═══════════════════════════════════════════════════
//  ROOT APP STATE
// ═══════════════════════════════════════════════════

data class AppState(
    val navigation: NavigationState = NavigationState(),
    val device: DeviceCapabilities = DeviceCapabilities(),
    val onboardingComplete: Boolean = false
)

data class DeviceCapabilities(
    val hasRoot: Boolean = false,
    val isHuawei: Boolean = false,
    val isEMUI: Boolean = false,
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val screenWidthDp: Int = 0,
    val isCompact: Boolean = false,    // < 700dp width
    val isOLED: Boolean = false,
    val hasNotch: Boolean = false,
    val chargingControlPath: String? = null
)

// ═══════════════════════════════════════════════════
//  HOME PAGE STATE
// ═══════════════════════════════════════════════════

data class HomeUiState(
    val spotify: SpotifyUiState = SpotifyUiState.Idle,
    val battery: BatteryUiState = BatteryUiState(),
    val clock: ClockUiState = ClockUiState(),
    val serverStatus: ServerStatus = ServerStatus.Starting
)

sealed interface SpotifyUiState {
    data object Idle : SpotifyUiState
    data object Loading : SpotifyUiState
    data class Playing(val track: SpotifyTrack) : SpotifyUiState
    data class RecentlyPlayed(val track: SpotifyTrack) : SpotifyUiState
    data class Error(val message: String, val isRetrying: Boolean = false) : SpotifyUiState
    data object NotAuthenticated : SpotifyUiState
    data object RateLimited : SpotifyUiState
}

data class BatteryUiState(
    val level: Int = 0,
    val voltageV: Float = 0f,
    val currentMa: Float = 0f,
    val wattageW: Float? = null,        // null if not calculable
    val temperatureC: Float = 0f,
    val status: String = "Unknown",
    val health: String = "Unknown",
    val chargeLimitEnabled: Boolean = false,
    val chargeLimitSupported: Boolean = false,  // false on non-root
    val isCharging: Boolean = false,
    val dataSource: String = "api"       // "sysfs" or "api"
)

data class ClockUiState(
    val time: String = "",
    val date: String = "",
    val is24Hour: Boolean = true
)

// ═══════════════════════════════════════════════════
//  AUDIO PAGE STATE
// ═══════════════════════════════════════════════════

data class AudioUiState(
    val connectionStatus: AudioConnectionStatus = AudioConnectionStatus.Disconnected,
    val latencyMs: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val codec: String = "Opus",
    val sampleRate: Int = 48000,
    val channels: Int = 2,
    val packetsReceived: Long = 0,
    val packetsLost: Long = 0,
    val waveformData: FloatArray = FloatArray(0),
    val audioOutput: String = "Unknown"  // "3.5mm", "USB-C", "Speaker"
)

enum class AudioConnectionStatus { 
    Connected, 
    Disconnected, 
    Reconnecting,
    Error 
}

// ═══════════════════════════════════════════════════
//  SYSTEM PAGE STATE
// ═══════════════════════════════════════════════════

data class SystemUiState(
    val deviceModel: String = "",
    val androidVersion: String = "",
    val oemSkin: String = "",
    val rootStatus: String = "Checking...",
    val appUptime: String = "",
    val serverPort: Int = 8420,
    val audioPort: Int = 8421,
    val serverRunning: Boolean = false,
    val audioRunning: Boolean = false,
    val ipAddress: String = "Unknown",
    val heapUsageMb: Int = 0,
    val heapMaxMb: Int = 0,
    val nativeMemoryMb: Int = 0,
    val commandLog: List<LogEntry> = emptyList()
)

enum class ServerStatus { Starting, Running, Error }
```

### 6.2 Threading Model

| Task | Dispatcher | Device Note |
|------|-----------|-------------|
| Spotify API polling | `Dispatchers.IO` | P20 Lite: increase poll interval to 5s |
| sysfs reads (root) | `Dispatchers.IO` | — |
| BatteryManager API (non-root) | `Dispatchers.Main` (broadcast receiver) | P20 Lite: primary path |
| Root shell execution | `Dispatchers.IO` | 5s timeout |
| Ktor server | CIO engine (own threads) | — |
| UDP audio receive | `newSingleThreadContext("AudioReceiver")` | Dedicated thread |
| Opus decoding | Same as audio receive | Avoid context switch |
| AudioTrack write | `newSingleThreadContext("AudioPlayer")` | Dedicated thread |
| UI recomposition | `Dispatchers.Main` | — |
| DataStore | `Dispatchers.IO` | — |
| WOL packet | `Dispatchers.IO` | — |
| Image loading (Coil) | Coil's default dispatcher | Limit memory cache on P20 Lite |
| Clock tick | `Dispatchers.Main` + `delay(1000)` | — |

### 6.3 Memory Management (P20 Lite)

```kotlin
// Coil ImageLoader configuration for low-memory devices
val imageLoader = ImageLoader.Builder(context)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(if (DeviceCompat.isLowRam) 0.15 else 0.25)
            .build()
    }
    .diskCachePolicy(CachePolicy.ENABLED)
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("coil_cache"))
            .maxSizeBytes(if (DeviceCompat.isLowRam) 30L * 1024 * 1024 else 100L * 1024 * 1024)
            .build()
    }
    .build()
```

---

## 7. Error State Wireframes

### 7.1 Spotify Not Authenticated

```
┌──────────────────────────┐
│                          │
│  🎵 Spotify              │
│                          │
│  Not connected.          │
│  [Sign in with Spotify]  │
│                          │
└──────────────────────────┘
```

### 7.2 Spotify Error / Rate Limited

```
┌──────────────────────────┐
│                          │
│  ⚠️ Spotify Error        │
│                          │
│  Could not fetch track.  │
│  Retrying in 15s...      │
│                          │
│  Last known:             │
│  "Song Name" - Artist    │
│                          │
└──────────────────────────┘
```

### 7.3 Root Not Available (Battery Panel)

```
┌──────────────────────────────────┐
│  ⚡ BATTERY                     │
│  ████████████░░ 78%              │
│  Charging │ 28.5°C               │
│                                  │
│  ┌────────────────────────────┐  │
│  │ 🔒 Charge limit requires  │  │
│  │    root access             │  │
│  └────────────────────────────┘  │
│                                  │
│  [Wake PC]                       │
└──────────────────────────────────┘
```

### 7.4 Audio Disconnected

```
┌──────────────────────────────────┐
│                                  │
│  ○ Audio Stream Disconnected     │
│                                  │
│  No packets received.            │
│  Reconnecting... (attempt 3/∞)   │
│  Next retry in 8s                │
│                                  │
│  Check that the PC audio         │
│  streamer is running and both    │
│  devices are on the same network.│
│                                  │
│  PC IP: ___.___.___.___ :8421   │
│                                  │
└──────────────────────────────────┘
```

### 7.5 Command Bridge Error

```
┌──────────────────────────────────┐
│  ⚠ Server Error                 │
│                                  │
│  Command Bridge failed to start  │
│  Port 8420 may be in use.        │
│                                  │
│  [Change Port]  [Retry]          │
└──────────────────────────────────┘
```

---

## 8. Accessibility

| Requirement | Implementation |
|-------------|---------------|
| Touch targets | All interactive elements ≥ 48dp × 48dp |
| Color contrast | Key text: 7:1 (AAA). Secondary: 4.5:1 (AA). Verified on both OLED and IPS. |
| Screen reader | `contentDescription` on all icons and album art. `semantics { stateDescription }` on toggles. |
| Reduced motion | Honor `ANIMATOR_DURATION_SCALE == 0`. Skip marquee, glow, and visualizer animations. |
| Focus indicators | Visible border on D-pad / keyboard focus. |
| Text scaling | Layout tested at 1.0×, 1.3×, and 1.5× font scale. Marquee handles overflow. |
| Disabled states | Dimmed to 38% opacity with "unavailable" content description. |
