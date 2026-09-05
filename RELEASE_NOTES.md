# ⚡ Master Companion v1.0.3 — Landscape Standby Perfection & Autonomous OTA

### 🌟 What's New in v1.0.3
- **🕒 Full-Size Landscape Clock with Slide-Over Pop-In Drawer**:
  - **100% Full Canvas Clock**: In horizontal standby mode, the Hero Clock now occupies the full display width and height without being cut in half by side widgets.
  - **Minimalist Widgets Button**: Top-right corner features a sleek, compact status pill (`[Dot] WIDGETS • [Level]% • [Power]`) with real-time status color coding (green = charging, amber = 80% bypass, cyan = standard, red = low).
  - **Non-Squishing Overlay Drawer**: Tapping the widgets pill slides in the Calendar Agenda and Battery Hardware Telemetry card as a smooth modal drawer over the right side with a dimming backdrop. The clock and quick action buttons underneath are never squished or resized.
  - **Tap-to-Dismiss**: Easily dismiss the widgets drawer by tapping the backdrop or the `✕` close button.
- **✨ Clean Quick Action Controls**:
  - Removed the confusing fullscreen toggle button from the clock's quick actions bar, giving Wake PC and Calendar controls generous breathing room without crowding.
- **🛡️ Autonomous Self-Installing & Auto-Restart OTA Engine**:
  - **Cryptographic Security Verification**: Checks APK integrity, exact package name (`com.mastercompanion`), anti-downgrade version check, and signature matching against the running app.
  - **Zero-Friction Self-Installation**: Automatically stages and installs via root (`su` -> `pm install -r -d`) or native `PackageInstaller` session.
  - **Auto-Restart**: Seamlessly restarts the app immediately upon update via `ACTION_MY_PACKAGE_REPLACED`.

### 📦 Installation & OTA
- **Direct Download**: Download `app-release.apk` below and install directly onto your device.
- **OTA Auto-Updater**: If already running v1.0.2, simply open the app, go to Settings, and tap Check for Updates to test the end-to-end auto-download, cryptographic verification, silent install, and auto-restart into v1.0.3!
