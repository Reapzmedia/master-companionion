# ⚡ Master Companion v1.0.0 — Stable Release

Transform any rooted Android device into a permanent smart desk dashboard, hardware telemetry monitor, Spotify Car View player with karaoke synced lyrics, and low-latency PC companion bridge.

### 🌟 Key Highlights & Features
- **🕒 5 Standby Clock Styles**: Minimalist Digital, Analog Precision Gauge, Retro Split-Flap, Cyberpunk Terminal, Word Matrix.
- **🎵 Fullscreen Spotify Player**: 5 Layouts (Car View, Spinning Vinyl Turntable, Progressive Blur Karaoke Lyrics, Minimal Standby Clock, Full-Bleed Artwork).
- **🎼 Multi-Source Synced Lyrics**: LRCLIB primary + Lyrics.ovh fallback with instant memory caching.
- **🔋 Root Battery Guard**: Hardware sysfs power bypass with 80% charging threshold and live wattage telemetry.
- **📅 Google Calendar Sync**: Built-in upcoming agenda widget.
- **🌐 Web Remote Control Dashboard**: Glassmorphic web UI on port `:8060` with real-time SSE telemetry.
- **🎧 Low-Latency PC Audio Passthrough**: 48kHz stereo WASAPI loopback receiver on UDP port `:8421`.
- **⌨️ Physical Macro-Pad Bridge**: AutoHotkey v2 script with rotary volume knob sync.

### 📦 Installation
Download **app-release.apk** below and install directly onto your device:
```powershell
adb install -r app-release.apk
```
