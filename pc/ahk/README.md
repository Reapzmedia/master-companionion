# Master Companion — AutoHotkey Macro Bridge

This script enables physical USB macro keypads (such as Huali Tech, stream decks, or mechanical numpads) to trigger native functions on your rooted Android standby dashboard.

## Requirements
- Windows 10 / 11
- [AutoHotkey v2.0+](https://www.autohotkey.com/) installed

## Quick Start
1. Open `companion_bridge.ahk` in any text editor.
2. Set `ANDROID_IP` to your phone's Wi-Fi IP address (or `127.0.0.1` if using ADB reverse tethering).
3. Set `AUTH_TOKEN` from the Android app's Settings > Network screen.
4. Double-click `companion_bridge.ahk` to run.
5. A green tooltip `✅ Connected to Master Companion` will appear in the top-left if communication succeeds.

## Auto-Start with Windows
1. Press `Win + R`, type `shell:startup`, and press Enter.
2. Right-click and create a shortcut to `companion_bridge.ahk` in that folder.
