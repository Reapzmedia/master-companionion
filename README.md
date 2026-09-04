# Master Companion

**Standby Dashboard & PC Bridge for Rooted Android Devices**

Master Companion transforms a docked, rooted Android device (optimized for Google Pixel 7 Pro, with support for older devices such as Huawei P20 Lite on Android 9) into a smart desk clock, hardware monitor, physical macro keypad receiver, and low-latency wireless/USB audio receiver.

---

## 📚 Project Documentation

The complete architectural and product specification is located in the [`docs/`](file:///c:/Users/panni/Downloads/master%20companionion/docs/) directory:

| Document | Title | Description |
|---|---|---|
| [01_PRD.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/01_PRD.md) | Product Requirements Document | User personas, stories, acceptance criteria, and v1.0 scope |
| [02_SRS.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/02_SRS.md) | Software Requirements Specification | Architecture, Ktor REST API, sysfs paths, and data models |
| [03_UI_UX_Architecture.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/03_UI_UX_Architecture.md) | UI/UX Architecture | Landscape dashboard layout, design tokens, and Compose state |
| [04_Implementation_Plan.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/04_Implementation_Plan.md) | Phased Implementation Plan | 4 development epics, tasks, and library stack |
| [05_Testing_Strategy.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/05_Testing_Strategy.md) | Testing & Quality Assurance | Unit tests, MockK, Turbine, and hardware testing |
| [06_Error_Handling_Spec.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/06_Error_Handling_Spec.md) | Error Handling & Resilience | Network dropouts, sysfs permission recovery, fallback states |
| [07_Project_Structure.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/07_Project_Structure.md) | Directory Structure Guide | Clean architecture package breakdown |
| [08_PC_Companion_Setup.md](file:///c:/Users/panni/Downloads/master%20companionion/docs/08_PC_Companion_Setup.md) | PC Companion Setup | AutoHotkey and Python audio streaming setup |

---

## 🛠️ Project Structure

```
master-companion/
├── app/                        # Android Application (Jetpack Compose, Hilt, Ktor)
│   ├── src/main/java/          # Kotlin source files
│   ├── src/main/res/           # Material3 resources & theme
│   └── build.gradle.kts        # App build configuration (minSdk 28, targetSdk 34)
├── pc/                         # Desktop Companion Utilities
│   ├── ahk/                    # AutoHotkey v2 Macro Keypad Bridge
│   ├── audio/                  # Python WASAPI loopback audio streamer
│   └── scripts/                # ADB reverse & testing scripts
├── gradle/                     # Gradle version catalog & wrapper
└── docs/                       # Complete specification & architecture guides
```

---

## 🚀 Getting Started

### Android App
1. Open this repository in **Android Studio Hedgehog / Jellyfish / Koala (or newer)**.
2. Ensure JDK 17 or JDK 21 is configured in Project Structure.
3. Build and install to your rooted Android device:
   ```bash
   ./gradlew installDebug
   ```

### PC Audio Streamer
Stream Windows audio output to the phone over UDP:
```powershell
cd pc/audio
python audio_streamer.py --target-ip <PHONE_IP>
```

### Macro Keypad Bridge
Trigger Android actions using physical macro keys:
1. Double-click `pc/ahk/companion_bridge.ahk`.
2. Configure your phone's IP and Auth Token.
