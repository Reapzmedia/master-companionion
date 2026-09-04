# Master Companion — PC Audio Streamer

This utility captures system audio playing on Windows using low-latency WASAPI loopback or Stereo Mix, and transmits it via UDP to your Master Companion Android phone dashboard.

## Usage

### 1. View Audio Devices
```powershell
python audio_streamer.py --list-devices
```

### 2. Stream Audio to Android Phone
```powershell
# Stream to phone over local Wi-Fi (default Opus with fallback to PCM)
python audio_streamer.py --target-ip 192.168.1.42

# Stream using specific port or audio device
python audio_streamer.py --target-ip 192.168.1.42 --port 8421 --device 45

# Stream raw uncompressed 16-bit PCM (for zero CPU encoding overhead)
python audio_streamer.py --target-ip 192.168.1.42 --codec pcm --verbose
```

### 3. USB Tethering Mode (Zero Latency)
When connected over USB cable with ADB:
```powershell
# Forward UDP packets to the device
python audio_streamer.py --target-ip 127.0.0.1 --port 8421
```
