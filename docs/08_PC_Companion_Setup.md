# PC Companion Setup Guide
## Master Companion — Desktop Side Configuration
**Version:** 1.0  
**Date:** 2026-09-04  
**Status:** Draft  

---

## Overview

The PC side of Master Companion consists of two independent utilities:

1. **AutoHotkey Macro Bridge** — Intercepts physical macro keypad inputs and sends HTTP commands to the Android app.
2. **Python Audio Streamer** — Captures system audio and streams it to the Android app over UDP.

Both are optional. Each can be used independently.

---

## Part 1: AutoHotkey Macro Bridge

### Prerequisites

| Requirement | Version | Download |
|-------------|---------|----------|
| AutoHotkey | v2.0+ | [autohotkey.com](https://www.autohotkey.com/) |
| USB Macro Keypad | Any (e.g., Huali Tech) | — |
| Windows | 10 / 11 | — |

### Installation

1. Install AutoHotkey v2.0 or later.
2. Copy `companion_bridge.ahk` to a convenient location (e.g., `C:\Tools\`).
3. Edit the configuration section at the top of the script.
4. Double-click the script to run, or add to Windows startup.

### Configuration

Edit these variables at the top of `companion_bridge.ahk`:

```ahk
#Requires AutoHotkey v2.0

; ═══════════════════════════════════════════════════
; CONFIGURATION — Edit these values
; ═══════════════════════════════════════════════════

; Android device IP (find it in the app: System page or onboarding)
ANDROID_IP := "192.168.1.42"

; Command Bridge port (default: 8420, must match app Settings)
ANDROID_PORT := 8420

; Auth token (copy from app: Settings > Network > Auth Token > Copy)
AUTH_TOKEN := "paste-your-256-bit-hex-token-here"

; ═══════════════════════════════════════════════════
; KEY BINDINGS — Map your macro keys to actions
; ═══════════════════════════════════════════════════

; Macro key → Action
; Common macro keys: F13-F24, Numpad keys, custom scan codes
; Use AHK's Key History (Window Menu > View > Key History) to find your key codes

F13::SendCommand("toggle_charge_limit", '{"enabled": true}')
F14::SendCommand("navigate", '{"page": "audio"}')
F15::SendCommand("navigate", '{"page": "home"}')
F16::SendCommand("wol", '{}')
F17::SendCommand("audio_toggle", '{}')
F18::SendCommand("navigate", '{"page": "system"}')

; Volume control example (with Numpad)
; NumpadAdd::SendCommand("volume_up", '{}')
; NumpadSub::SendCommand("volume_down", '{}')

; ═══════════════════════════════════════════════════
; DO NOT EDIT BELOW THIS LINE
; ═══════════════════════════════════════════════════

SendCommand(action, paramsJson) {
    url := "http://" ANDROID_IP ":" ANDROID_PORT "/command"
    body := '{"action":"' action '","params":' paramsJson '}'
    
    try {
        http := ComObject("WinHttp.WinHttpRequest.5.1")
        http.Open("POST", url, false)
        http.SetRequestHeader("Content-Type", "application/json")
        http.SetRequestHeader("X-Auth-Token", AUTH_TOKEN)
        http.SetTimeouts(2000, 2000, 2000, 2000)  ; 2s timeout
        http.Send(body)
        
        if (http.Status = 200) {
            ; Success — silent
        } else if (http.Status = 401) {
            ToolTip("⚠ Auth failed — check token", , , 2)
            SetTimer(() => ToolTip(, , , 2), -3000)
        } else {
            ToolTip("⚠ Command failed: " http.Status, , , 2)
            SetTimer(() => ToolTip(, , , 2), -2000)
        }
    } catch as e {
        ToolTip("❌ Connection error — is the app running?", , , 2)
        SetTimer(() => ToolTip(, , , 2), -3000)
    }
}

; Health check on script start
CheckConnection() {
    url := "http://" ANDROID_IP ":" ANDROID_PORT "/ping"
    try {
        http := ComObject("WinHttp.WinHttpRequest.5.1")
        http.Open("GET", url, false)
        http.SetTimeouts(3000, 3000, 3000, 3000)
        http.Send()
        if (http.Status = 200) {
            ToolTip("✅ Connected to Master Companion", , , 1)
        } else {
            ToolTip("⚠ Device responded but status: " http.Status, , , 1)
        }
    } catch {
        ToolTip("❌ Cannot reach device at " ANDROID_IP ":" ANDROID_PORT, , , 1)
    }
    SetTimer(() => ToolTip(, , , 1), -5000)
}

; Run health check on startup
CheckConnection()
```

### Finding Your Macro Key Codes

1. Run the AHK script.
2. Right-click the AHK tray icon → **Open** → **View** → **Key History**.
3. Press your macro keys — note the key names that appear (e.g., `F13`, `SC065`, `vk46`).
4. Map these in the key bindings section.

### Adding to Windows Startup

1. Press `Win + R`, type `shell:startup`, press Enter.
2. Create a shortcut to `companion_bridge.ahk` in the opened folder.
3. The script will now start automatically on login.

### Available Commands

| Action | Type | Description | Params |
|--------|------|-------------|--------|
| `toggle_charge_limit` | toggle | Toggle battery charge limit | `{"enabled": bool}` |
| `navigate` | ui | Switch dashboard page | `{"page": "home\|audio\|system\|settings"}` |
| `wol` | network | Wake PC via WOL | `{}` |
| `audio_toggle` | toggle | Start/stop audio receiver | `{}` |
| `exec_shell` | shell | Run shell command (root) | `{"command": "..."}` |
| `set_brightness` | system | Set screen brightness | `{"value": 0-255}` |

### Troubleshooting

| Problem | Solution |
|---------|----------|
| "Connection error" on every key | Verify `ANDROID_IP` is correct. Check both devices are on the same Wi-Fi. |
| "Auth failed" | Copy a fresh token from the app: Settings → Network → Copy Token. |
| Key presses not detected | Use AHK Key History to find correct key codes. Some macro boards use scan codes. |
| Script won't start | Ensure AutoHotkey v2 is installed, not v1. Right-click → Run as Administrator if needed. |
| Lag / slow response | Reduce PC firewall rules. The 2s timeout should be more than enough on local network. |

---

## Part 2: Python Audio Streamer

### Prerequisites

| Requirement | Version | Download |
|-------------|---------|----------|
| Python | 3.9+ | [python.org](https://www.python.org/) |
| pip | Latest | Included with Python |
| Windows | 10 / 11 | — |
| WASAPI Loopback support | Built-in | — |

### Installation

```bash
cd pc/audio/
pip install -r requirements.txt
```

**`requirements.txt`:**
```
sounddevice>=0.4.6
numpy>=1.26.0
opuslib>=3.0.1
```

> **Note:** `opuslib` requires the Opus shared library (`opus.dll` on Windows). If not found, install it:
> ```bash
> pip install opuslib
> ```
> If it still fails, download `opus.dll` from [opus-codec.org](https://opus-codec.org/downloads/) and place it in the script directory or your `PATH`.

### Usage

```bash
# Basic usage
python audio_streamer.py --target-ip 192.168.1.42

# With custom port
python audio_streamer.py --target-ip 192.168.1.42 --port 8421

# List available audio devices
python audio_streamer.py --list-devices

# Use a specific audio device
python audio_streamer.py --target-ip 192.168.1.42 --device 3

# Raw PCM mode (no Opus encoding, higher bandwidth)
python audio_streamer.py --target-ip 192.168.1.42 --codec pcm

# Verbose output
python audio_streamer.py --target-ip 192.168.1.42 --verbose
```

### Audio Streamer Script

```python
#!/usr/bin/env python3
"""
Master Companion — PC Audio Streamer
Captures system audio via WASAPI loopback and streams to Android over UDP.
"""

import argparse
import socket
import struct
import sys
import time
import signal

import numpy as np
import sounddevice as sd

# Try to import Opus encoder
try:
    import opuslib
    OPUS_AVAILABLE = True
except ImportError:
    OPUS_AVAILABLE = False
    print("WARNING: opuslib not available. Using raw PCM mode.")

# Constants
SAMPLE_RATE = 48000
CHANNELS = 2
FRAME_SIZE = 960  # 20ms at 48kHz
CODEC_PCM = 0x01
CODEC_OPUS = 0x02


def list_audio_devices():
    """List all available audio devices."""
    print("\nAvailable audio devices:")
    print("-" * 60)
    devices = sd.query_devices()
    for i, dev in enumerate(devices):
        direction = ""
        if dev['max_input_channels'] > 0:
            direction += "IN "
        if dev['max_output_channels'] > 0:
            direction += "OUT"
        loopback = " [LOOPBACK]" if "loopback" in dev['name'].lower() else ""
        print(f"  {i:3d}: {dev['name']}{loopback} ({direction.strip()})")
    print()
    
    # Try to find default loopback
    try:
        default = sd.default.device[1]  # Output device
        print(f"Default output device: {default}")
        print("Use the WASAPI loopback device to capture system audio.")
    except Exception:
        pass


def create_packet(codec_flag: int, seq: int, timestamp: int, payload: bytes) -> bytes:
    """Create a packet with header."""
    header = struct.pack('>BII', codec_flag, seq & 0xFFFFFFFF, timestamp & 0xFFFFFFFF)
    return header + payload


def stream_audio(target_ip: str, port: int, device: int = None,
                 codec: str = 'opus', verbose: bool = False):
    """Main streaming loop."""
    
    # Select codec
    use_opus = (codec == 'opus') and OPUS_AVAILABLE
    if codec == 'opus' and not OPUS_AVAILABLE:
        print("Opus not available, falling back to PCM")
        use_opus = False
    
    codec_flag = CODEC_OPUS if use_opus else CODEC_PCM
    
    # Initialize Opus encoder
    encoder = None
    if use_opus:
        encoder = opuslib.Encoder(SAMPLE_RATE, CHANNELS, opuslib.APPLICATION_AUDIO)
    
    # Open UDP socket
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    
    # Determine audio device
    if device is not None:
        input_device = device
    else:
        # Try to find WASAPI loopback automatically
        input_device = None
        devices = sd.query_devices()
        for i, dev in enumerate(devices):
            if 'loopback' in dev['name'].lower() and dev['max_input_channels'] >= 2:
                input_device = i
                break
        
        if input_device is None:
            print("ERROR: No loopback device found. Use --list-devices and --device to specify.")
            print("       On Windows, enable 'Stereo Mix' or use a virtual audio cable.")
            sys.exit(1)
    
    device_info = sd.query_devices(input_device)
    print(f"Audio device: {device_info['name']}")
    print(f"Codec: {'Opus' if use_opus else 'PCM'}")
    print(f"Target: {target_ip}:{port}")
    print(f"Format: {SAMPLE_RATE}Hz, {CHANNELS}ch, {'Opus' if use_opus else '16-bit PCM'}")
    print(f"Frame size: {FRAME_SIZE} samples ({FRAME_SIZE / SAMPLE_RATE * 1000:.0f}ms)")
    print("Streaming... (Ctrl+C to stop)")
    print()
    
    seq = 0
    timestamp = 0
    packets_sent = 0
    start_time = time.time()
    
    def audio_callback(indata, frames, time_info, status):
        nonlocal seq, timestamp, packets_sent
        
        if status and verbose:
            print(f"  [WARN] {status}")
        
        # Convert to int16
        audio_data = (indata * 32767).astype(np.int16)
        
        if use_opus:
            # Encode with Opus
            pcm_bytes = audio_data.tobytes()
            try:
                encoded = encoder.encode(pcm_bytes, FRAME_SIZE)
                payload = encoded
            except Exception as e:
                if verbose:
                    print(f"  [ERR] Opus encode: {e}")
                return
        else:
            # Raw PCM
            payload = audio_data.tobytes()
        
        # Build and send packet
        packet = create_packet(codec_flag, seq, timestamp, payload)
        try:
            sock.sendto(packet, (target_ip, port))
            seq += 1
            timestamp += FRAME_SIZE
            packets_sent += 1
            
            if verbose and packets_sent % 250 == 0:
                elapsed = time.time() - start_time
                rate = packets_sent / elapsed if elapsed > 0 else 0
                print(f"  Sent {packets_sent} packets ({rate:.0f}/s, "
                      f"seq={seq}, payload={len(payload)}B)")
        except Exception as e:
            if verbose:
                print(f"  [ERR] Send: {e}")
    
    # Graceful shutdown
    running = True
    def signal_handler(sig, frame):
        nonlocal running
        running = False
        print("\nStopping...")
    
    signal.signal(signal.SIGINT, signal_handler)
    
    try:
        with sd.InputStream(
            device=input_device,
            samplerate=SAMPLE_RATE,
            channels=CHANNELS,
            blocksize=FRAME_SIZE,
            dtype='float32',
            callback=audio_callback
        ):
            while running:
                time.sleep(0.1)
    except Exception as e:
        print(f"ERROR: {e}")
        print("Try --list-devices to see available devices.")
        sys.exit(1)
    finally:
        sock.close()
        elapsed = time.time() - start_time
        print(f"\nDone. Sent {packets_sent} packets in {elapsed:.1f}s")


def main():
    parser = argparse.ArgumentParser(
        description='Master Companion PC Audio Streamer',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --target-ip 192.168.1.42
  %(prog)s --target-ip 192.168.1.42 --port 8421 --device 3
  %(prog)s --list-devices
  %(prog)s --target-ip 192.168.1.42 --codec pcm --verbose
        """
    )
    
    parser.add_argument('--target-ip', type=str, help='Android device IP address')
    parser.add_argument('--port', type=int, default=8421, help='UDP port (default: 8421)')
    parser.add_argument('--device', type=int, default=None, help='Audio device index (use --list-devices)')
    parser.add_argument('--codec', choices=['opus', 'pcm'], default='opus', help='Audio codec (default: opus)')
    parser.add_argument('--list-devices', action='store_true', help='List available audio devices and exit')
    parser.add_argument('--verbose', action='store_true', help='Enable verbose output')
    
    args = parser.parse_args()
    
    if args.list_devices:
        list_audio_devices()
        sys.exit(0)
    
    if not args.target_ip:
        parser.error("--target-ip is required (or use --list-devices)")
    
    stream_audio(
        target_ip=args.target_ip,
        port=args.port,
        device=args.device,
        codec=args.codec,
        verbose=args.verbose
    )


if __name__ == '__main__':
    main()
```

### Troubleshooting

| Problem | Solution |
|---------|----------|
| "No loopback device found" | Enable "Stereo Mix" in Windows Sound Settings → Recording. Or install [VB-CABLE](https://vb-audio.com/Cable/) virtual audio cable. |
| `opuslib` import error | Install Opus DLL: `pip install opuslib`. If it still fails, use `--codec pcm`. |
| No audio heard on phone | Check target IP and port match the app's settings. Ensure both on same network. |
| Audio crackling | Reduce network congestion. Try `--codec pcm` for lower CPU usage. |
| High latency | Check for VPN/firewall interference. Use a 5GHz Wi-Fi band if available. |
| `sounddevice` PortAudio error | Install PortAudio: `pip install sounddevice` should include it. On some systems, install separately. |

---

## Part 3: Testing the Connection

### Quick Test (No Macro Board Needed)

You can test the Command Bridge without a macro board using `curl` or PowerShell:

**PowerShell:**
```powershell
# Ping test
Invoke-RestMethod -Uri "http://192.168.1.42:8420/ping"

# Send a command
$headers = @{ "X-Auth-Token" = "your-token-here"; "Content-Type" = "application/json" }
$body = '{"action":"navigate","params":{"page":"audio"}}'
Invoke-RestMethod -Method POST -Uri "http://192.168.1.42:8420/command" -Headers $headers -Body $body

# Get full status
Invoke-RestMethod -Uri "http://192.168.1.42:8420/status"

# List available commands
Invoke-RestMethod -Uri "http://192.168.1.42:8420/commands"
```

**curl (if installed):**
```bash
# Ping
curl http://192.168.1.42:8420/ping

# Send command
curl -X POST http://192.168.1.42:8420/command \
  -H "X-Auth-Token: your-token" \
  -H "Content-Type: application/json" \
  -d '{"action":"navigate","params":{"page":"audio"}}'
```

### Verifying Audio Stream

1. Start the Python streamer: `python audio_streamer.py --target-ip 192.168.1.42 --verbose`
2. Play audio on your PC (music, video, etc.).
3. Check the app's Audio page — status should show "Connected".
4. Plug headphones into the phone (3.5mm or USB-C) — you should hear PC audio.

### Network Checklist

- [ ] Both devices on the same Wi-Fi network
- [ ] No AP isolation enabled on the router
- [ ] Windows Firewall allows outbound UDP on port 8421
- [ ] Windows Firewall allows outbound TCP on port 8420
- [ ] Android device IP hasn't changed (consider setting a static IP or DHCP reservation)
