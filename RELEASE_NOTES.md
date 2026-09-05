# ⚡ Master Companion v1.0.1 — Hotfix & Telemetry Release

### 🌟 What's New in v1.0.1
- **🎵 Dynamic Spotify Context Header**: Fixed persistent "Liked Songs" display — accurately resolves and displays real playlist names, album titles (`PLAYING FROM ALBUM`), artist radio (`PLAYING FROM ARTIST`), and podcasts across Car View and Lyrics views.
- **🔋 High-Precision Milliamps (< 1W)**: When standby power draw or trickle charge drops below 1.0 Watt, telemetry seamlessly switches from `0.0 W` to high-resolution milliamps (e.g. `+140 mA` or `250 mA`) across all gauges, clocks, and battery cards.
- **🕒 Clock Fullscreen Polish**: Removed intrusive floating "Exit Fullscreen" pill from the Minimalist Digital Clock — clean tap-to-exit gesture.
- **🎧 PC Passthrough Auto-IP**: Auto-detects device Wi-Fi IP address directly in the PC audio passthrough command with 1-tap copy.
- **🛡️ Enhanced Hardware Battery Telemetry**: Complete live diagnostics card featuring signed current ($mA$), voltage ($V$), dual-unit thermals ($°C$/$°F$), battery health status, and motherboard AC bypass indicators.

### 📦 Installation & OTA
- **Direct Download**: Download `app-release.apk` below and install via ADB or direct package installer.
- **OTA Auto-Updater**: If already running v1.0.0, open Settings > Check for Updates to automatically download and install this release over the air.
