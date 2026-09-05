# ⚡ Master Companion v1.0.2 — Autonomous Self-Installer & Full-Size Clock

### 🌟 What's New in v1.0.2
- **🛡️ Autonomous Self-Installing & Auto-Restart Updater**:
  - **Self-Install & Auto-Restart**: The application now seamlessly installs update packages directly and automatically relaunches via `ACTION_MY_PACKAGE_REPLACED` upon completion.
  - **Enterprise-Grade Cryptographic Security**: Before any installation, the APK is strictly verified for valid archive structure, exact package identity (`com.mastercompanion`), anti-downgrade progression, and cryptographic certificate matching against the active running application.
  - **Silent Root Staging (`su`)**: On rooted hardware (such as docked Pixel 7 Pro), automatically stages the package to `/data/local/tmp`, executes `pm install -r -d` with zero prompts, and relaunches `MainActivity`.
  - **Native PackageInstaller Session**: On non-root hardware, directly executes a PackageInstaller session without third-party file manager or FileProvider friction.
- **🕒 Full-Size Landscape Clock & Pop-in Widgets Drawer**:
  - **Unconstrained Clock Display**: In horizontal mode, the Hero Clock now expands to the full screen with uncrowded, spacious typography.
  - **Pop-in Minimalist Sidebar**: The Calendar and Hardware Battery telemetry cards now tuck into a clean, smooth slide-in drawer toggled via the `WIDGETS` pill button in the top right.
  - **Unsquished Quick Actions**: Bottom quick action controls (Wake-on-LAN, Calendar, Fullscreen) now have generous breathing room without crowding the display.

### 📦 Installation & OTA
- **Direct Download**: Download `app-release.apk` below and install directly onto your device.
- **OTA Auto-Updater**: If already running v1.0.0 or v1.0.1, navigate to Settings > Check for Updates to automatically download, verify, install, and restart into v1.0.2!
