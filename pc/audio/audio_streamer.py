#!/usr/bin/env python3
"""
Master Companion — PC Audio Streamer
Captures system audio via Windows WASAPI loopback and streams it to the Android app over UDP.
Supports low-latency Opus compression (with automatic fallback to uncompressed 16-bit PCM).
"""

import argparse
import socket
import struct
import sys
import time
import signal

try:
    import numpy as np
    import sounddevice as sd
except ImportError as e:
    print(f"ERROR: Missing required Python dependency: {e}")
    print("Run: pip install sounddevice numpy")
    sys.exit(1)

# Safe Opus import check
OPUS_AVAILABLE = False
opus_error_message = ""
try:
    import opuslib
    # Try dummy encoder to verify opus.dll actually loads
    test_enc = opuslib.Encoder(48000, 2, opuslib.APPLICATION_AUDIO)
    OPUS_AVAILABLE = True
except Exception as e:
    OPUS_AVAILABLE = False
    opus_error_message = str(e)

# Audio Constants
SAMPLE_RATE = 48000
CHANNELS = 2
FRAME_SIZE = 960  # 20ms at 48kHz
CODEC_PCM = 0x01
CODEC_OPUS = 0x02


def list_audio_devices():
    """List all available audio input and output devices."""
    print("\nAvailable Audio Devices:")
    print("=" * 70)
    devices = sd.query_devices()
    for i, dev in enumerate(devices):
        direction = []
        if dev['max_input_channels'] > 0:
            direction.append("IN")
        if dev['max_output_channels'] > 0:
            direction.append("OUT")
        dir_str = "/".join(direction) if direction else "NONE"
        is_loopback = " [WASAPI LOOPBACK]" if "loopback" in dev['name'].lower() else ""
        print(f" [{i:2d}] {dev['name']}{is_loopback} ({dir_str}, HostAPI: {dev['hostapi']})")
    print("=" * 70)
    print("Tip: To capture PC audio, select the loopback or output device using --device <index>\n")


def create_packet(codec_flag: int, seq: int, timestamp: int, payload: bytes) -> bytes:
    """Pack header and audio payload: 1 byte codec flag, 4 bytes sequence, 4 bytes timestamp."""
    header = struct.pack('>BII', codec_flag, seq & 0xFFFFFFFF, timestamp & 0xFFFFFFFF)
    return header + payload


def stream_audio(target_ip: str, port: int, device: int = None,
                 codec: str = 'opus', verbose: bool = False):
    """Main audio streaming loop capturing from WASAPI loopback and transmitting via UDP."""
    global OPUS_AVAILABLE

    use_opus = (codec.lower() == 'opus')
    if use_opus and not OPUS_AVAILABLE:
        print("\n[NOTE] Opus library (opus.dll) not detected on Windows.")
        if opus_error_message:
            print(f"       Details: {opus_error_message}")
        print("       Automatically falling back to uncompressed 16-bit PCM streaming (high quality, ~1.5 Mbps).")
        print("       Tip: Install opus.dll in PATH or use '--codec pcm' to suppress this message.\n")
        use_opus = False

    codec_flag = CODEC_OPUS if use_opus else CODEC_PCM

    encoder = None
    if use_opus:
        try:
            encoder = opuslib.Encoder(SAMPLE_RATE, CHANNELS, opuslib.APPLICATION_AUDIO)
        except Exception as e:
            print(f"Failed to initialize Opus encoder ({e}). Falling back to PCM.")
            use_opus = False
            codec_flag = CODEC_PCM

    # Resolve target device
    input_device = device
    if input_device is None:
        devices = sd.query_devices()
        # Look for default WASAPI loopback device
        for i, dev in enumerate(devices):
            name_lower = dev['name'].lower()
            if ('loopback' in name_lower or 'stereo mix' in name_lower) and dev['max_input_channels'] >= 2:
                input_device = i
                break

        if input_device is None:
            # Check default output device
            try:
                default_out = sd.default.device[1]
                if default_out is not None and default_out >= 0:
                    print(f"Default loopback not explicitly listed, checking default output device #{default_out}...")
                    input_device = default_out
            except Exception:
                pass

        if input_device is None:
            print("ERROR: No default WASAPI loopback or stereo mix audio device found.")
            print("Run with --list-devices to inspect available hardware and specify with --device <num>.")
            sys.exit(1)

    device_info = sd.query_devices(input_device)
    dev_name = device_info['name']

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    print("═══════════════════════════════════════════════════════════════════")
    print(" Master Companion — PC Audio Streamer Running")
    print("═══════════════════════════════════════════════════════════════════")
    print(f" Audio Device : [{input_device}] {dev_name}")
    print(f" Target       : {target_ip}:{port}")
    print(f" Codec        : {'Opus (48kHz stereo, 20ms frame)' if use_opus else 'PCM 16-bit stereo uncompressed'}")
    print(f" Frame Size   : {FRAME_SIZE} samples ({FRAME_SIZE / SAMPLE_RATE * 1000:.1f}ms)")
    print(" Press Ctrl+C in this terminal to stop streaming.")
    print("═══════════════════════════════════════════════════════════════════\n")

    seq = 0
    timestamp = 0
    packets_sent = 0
    start_time = time.time()
    running = True

    def audio_callback(indata, frames, time_info, status):
        nonlocal seq, timestamp, packets_sent
        if status and verbose:
            print(f"  [STREAM WARNING] {status}")

        # Scale float32 (-1.0 to 1.0) to int16 PCM
        audio_int16 = (np.clip(indata, -1.0, 1.0) * 32767.0).astype(np.int16)

        if use_opus:
            try:
                payload = encoder.encode(audio_int16.tobytes(), FRAME_SIZE)
            except Exception as e:
                if verbose:
                    print(f"  [ERR] Opus encode error: {e}")
                return
        else:
            payload = audio_int16.tobytes()

        packet = create_packet(codec_flag, seq, timestamp, payload)
        try:
            sock.sendto(packet, (target_ip, port))
            seq += 1
            timestamp += FRAME_SIZE
            packets_sent += 1

            if verbose and packets_sent % 250 == 0:
                elapsed = time.time() - start_time
                pps = packets_sent / elapsed if elapsed > 0 else 0
                print(f"  [STATS] Sent {packets_sent} packets ({pps:.1f} pkt/s, seq={seq}, size={len(payload)}B)")
        except Exception as e:
            if verbose:
                print(f"  [ERR] UDP socket send error: {e}")

    def handle_sigint(sig, frame):
        nonlocal running
        running = False
        print("\nStopping audio stream...")

    signal.signal(signal.SIGINT, handle_sigint)

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
        print(f"\nAudio capture error: {e}")
        print("Tip: If WASAPI loopback fails, verify 'Stereo Mix' or a virtual audio cable is enabled.")
    finally:
        sock.close()
        total_time = time.time() - start_time
        print(f"Streaming stopped. Sent {packets_sent} packets over {total_time:.1f} seconds.")


def main():
    parser = argparse.ArgumentParser(
        description="Master Companion PC Audio Streamer (WASAPI Loopback to Android UDP)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python audio_streamer.py --target-ip 192.168.1.42
  python audio_streamer.py --target-ip 192.168.1.42 --port 8421
  python audio_streamer.py --target-ip 192.168.1.42 --codec pcm
  python audio_streamer.py --list-devices
        """
    )
    parser.add_argument('--target-ip', type=str, help='Android device IP address')
    parser.add_argument('--port', type=int, default=8421, help='UDP target port (default: 8421)')
    parser.add_argument('--device', type=int, default=None, help='Audio capture device index')
    parser.add_argument('--codec', choices=['opus', 'pcm'], default='opus', help='Audio codec (default: opus)')
    parser.add_argument('--list-devices', action='store_true', help='List available audio devices and exit')
    parser.add_argument('--verbose', action='store_true', help='Print continuous transfer statistics')

    args = parser.parse_args()

    if args.list_devices:
        list_audio_devices()
        sys.exit(0)

    if not args.target_ip:
        parser.error("Missing --target-ip argument. Specify your Android phone's IP address (e.g. 192.168.1.42).")

    stream_audio(
        target_ip=args.target_ip,
        port=args.port,
        device=args.device,
        codec=args.codec,
        verbose=args.verbose
    )


if __name__ == '__main__':
    main()
