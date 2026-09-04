# Testing Strategy
## Master Companion — Standby Dashboard & PC Bridge
**Version:** 1.0  
**Date:** 2026-09-04  
**Status:** Draft  

---

## 1. Test Pyramid

```
                    ┌─────────┐
                    │  Manual  │  ← Soak tests, UX validation, device-specific
                    │   E2E   │     ~10 tests per epic
                   ┌┴─────────┴┐
                   │ Integration│  ← HTTP endpoints, service lifecycle
                   │   Tests    │     ~15 tests
                  ┌┴───────────┴┐
                  │   Unit Tests │  ← Pure logic, state machines, parsing
                  │              │     ~40+ tests
                  └──────────────┘
```

---

## 2. Unit Tests

### 2.1 Spotify Module

| Test ID | Description | Input | Expected Output |
|---------|-------------|-------|-----------------|
| SP-U01 | PKCE code verifier length | Generate verifier | 43–128 chars, matches `[A-Za-z0-9\-._~]+` |
| SP-U02 | PKCE code challenge | Known verifier | Base64URL SHA-256 matches expected |
| SP-U03 | Fallback: currently-playing returns 204 | Mock 204 response | State transitions to `RecentlyPlayed` |
| SP-U04 | Fallback: both endpoints fail | Mock errors | State transitions to `Idle` with clock |
| SP-U05 | Token refresh trigger | Token expiring in 30s | Refresh initiated |
| SP-U06 | Token refresh failure | Mock 401 on refresh | State = `NotAuthenticated` |
| SP-U07 | Rate limit handling | Mock 429 + Retry-After: 5 | Polling pauses for 5s, state = `RateLimited` |
| SP-U08 | Track parsing: null album art | Missing images array | Fallback placeholder image URL |
| SP-U09 | Poll interval device-adaptive | isCompact = true | Interval = 5000ms (not 3000ms) |

### 2.2 Battery Module

| Test ID | Description | Input | Expected Output |
|---------|-------------|-------|-----------------|
| BT-U01 | Wattage from µV + µA | voltage=4120000, current=450000 | wattage = 1.854W |
| BT-U02 | Wattage negative current (discharging) | voltage=3800000, current=-200000 | wattage = -0.76W |
| BT-U03 | Temperature conversion | temp=285 | 28.5°C |
| BT-U04 | Charge limit: at 80% | level=80, enabled=true | Write `0` to sysfs |
| BT-U05 | Charge limit: at 78% (within hysteresis) | level=78, limited=true | No state change |
| BT-U06 | Charge limit: at 75% (resume) | level=75, limited=true | Write `1` to sysfs |
| BT-U07 | Charge limit: at 76% (above resume) | level=76, limited=true | No state change |
| BT-U08 | sysfs path auto-detect | Mock writable path at index 2 | Returns candidate path 2 |
| BT-U09 | No writable sysfs path | All candidates return error | Returns null, chargeLimitSupported=false |
| BT-U10 | BatteryManager fallback | No root | Data sourced from BatteryManager, wattage=null |

### 2.3 Command Bridge Module

| Test ID | Description | Input | Expected Output |
|---------|-------------|-------|-----------------|
| CB-U01 | Valid auth token | Correct X-Auth-Token | Request passes validation |
| CB-U02 | Missing auth token | No header | 401 response |
| CB-U03 | Wrong auth token | Incorrect token | 401 response |
| CB-U04 | Known action lookup | action="wol" | Handler found |
| CB-U05 | Unknown action | action="nonexistent" | 404 response |
| CB-U06 | Shell command whitelist: allowed | command="reboot" | Allowed |
| CB-U07 | Shell command whitelist: blocked | command="rm -rf /" | Blocked, 403 |
| CB-U08 | Shell on non-root device | action="exec_shell" | 403 "requires root" |
| CB-U09 | Rate limiter: under limit | 29 requests/min | All pass |
| CB-U10 | Rate limiter: over limit | 31 requests/min | 31st returns 429 |
| CB-U11 | Command registry JSON parse | Valid JSON | All commands parsed |
| CB-U12 | Malformed command JSON | Invalid JSON body | 400 response |

### 2.4 Network Module

| Test ID | Description | Input | Expected Output |
|---------|-------------|-------|-----------------|
| NW-U01 | WOL magic packet structure | MAC "AA:BB:CC:DD:EE:FF" | 102 bytes: 6×FF + 16×MAC |
| NW-U02 | WOL invalid MAC | "ZZ:BB:CC" | Exception thrown |
| NW-U03 | WOL MAC with dashes | "AA-BB-CC-DD-EE-FF" | Correctly parsed |

### 2.5 Audio Module

| Test ID | Description | Input | Expected Output |
|---------|-------------|-------|-----------------|
| AU-U01 | Packet header parse: Opus | Valid header bytes | codec=Opus, seq=N, ts=M |
| AU-U02 | Packet header parse: PCM | Valid header bytes | codec=PCM, seq=N, ts=M |
| AU-U03 | Jitter buffer: in-order | Seq 1,2,3 | Output 1,2,3 |
| AU-U04 | Jitter buffer: out-of-order | Seq 3,1,2 | Output 1,2,3 |
| AU-U05 | Jitter buffer: duplicate | Seq 1,1,2 | Output 1,2 (dup dropped) |
| AU-U06 | Jitter buffer: overflow | 100 packets, buffer=50 | Oldest dropped |
| AU-U07 | Disconnect detection | No packets for 5s | Status = DISCONNECTED |
| AU-U08 | Reconnect backoff | 1st,2nd,3rd retry | 1s, 2s, 4s delays |
| AU-U09 | Reconnect backoff cap | 6th retry | 30s (not 64s) |

### 2.6 Device Compat Module

| Test ID | Description | Input | Expected Output |
|---------|-------------|-------|-----------------|
| DC-U01 | Huawei detection | Build.MANUFACTURER="HUAWEI" | isHuawei=true |
| DC-U02 | Non-Huawei detection | Build.MANUFACTURER="Google" | isHuawei=false |
| DC-U03 | Compact screen | screenWidthDp=600 | isCompact=true |
| DC-U04 | Standard screen | screenWidthDp=800 | isCompact=false |

---

## 3. Integration Tests

| Test ID | Description | Method | Expected |
|---------|-------------|--------|----------|
| IT-01 | Ping endpoint | `curl http://device:8420/ping` | 200 + JSON with status="ok" |
| IT-02 | Status endpoint | `curl http://device:8420/status` | 200 + complete system snapshot |
| IT-03 | Command with auth | `curl -X POST -H "X-Auth-Token: ..." ...` | 200 + action executed |
| IT-04 | Command without auth | `curl -X POST ...` (no header) | 401 |
| IT-05 | Commands list | `curl http://device:8420/commands` | 200 + list of registered commands |
| IT-06 | Navigate command | POST navigate:audio | UI switches to audio page |
| IT-07 | WOL command | POST wol | Magic packet sent (wireshark verify) |
| IT-08 | Service restart | Kill and restart CommandBridgeService | Server re-binds, endpoints work |
| IT-09 | Spotify token refresh | Wait for token expiry | Auto-refresh, polling continues |
| IT-10 | Audio stream connection | Start Python streamer, check status | AudioConnectionStatus = Connected |

---

## 4. Manual / E2E Test Procedures

### 4.1 Device-Specific Test Matrix

| Test | Pixel 7 Pro | P20 Lite | Pass Criteria |
|------|-------------|----------|---------------|
| Cold start time | < 2s | < 4s | Measure with stopwatch |
| Landscape lock | ✓ | ✓ | No rotation on device turn |
| Notch safe area | N/A | ✓ | No UI hidden behind notch |
| Immersive mode | ✓ | ✓ | Status/nav bars hidden |
| Album art display | ✓ (300dp, glow) | ✓ (220dp, no glow) | Image loads, correct size |
| Marquee scroll | ✓ | ✓ | Long titles scroll smoothly |
| Clock accuracy | ✓ | ✓ | Matches system clock ±1s |
| Battery data | ✓ (sysfs) | ✓ (API fallback) | Values displayed, update every 2s |
| Charge limit toggle | ✓ (root) | ✗ (badge shown) | Charger stops at 80%, resumes at 75% |
| OAuth flow | ✓ | ✓ (EMUI compat) | Token obtained, polling starts |
| AHK macro command | ✓ | ✓ | Action executes < 200ms / < 500ms |
| WOL packet | ✓ | ✓ | PC wakes from sleep |
| Audio playback | ✓ (USB-C) | ✓ (3.5mm) | Clear audio, < 150ms / < 300ms latency |
| Audio reconnect | ✓ | ✓ | Recovers within 30s of network restore |
| EMUI survival (24h) | N/A | ✓ | Services still running after 24h |
| Pixel soak (72h) | ✓ | N/A | No crash, no OOM, no memory leak |
| Onboarding | ✓ | ✓ | All 5 steps complete correctly |

### 4.2 Soak Test Protocol

**72-hour soak (Pixel 7 Pro):**
1. Fresh install, complete onboarding.
2. Connect to Spotify, start playback on PC.
3. Enable charge limit at 80%.
4. Start command bridge.
5. Start audio passthrough.
6. Send AHK command every 10 minutes (via script).
7. Record: memory (every hour), battery level, crash count.
8. After 72h: check logcat for exceptions, verify memory trend.

**24-hour soak (P20 Lite):**
1. Same setup, but without charge limit.
2. EMUI battery optimization disabled.
3. Audio visualizer set to 15fps.
4. After 24h: verify services still running, check for ANR/crash.

---

## 5. Performance Benchmarks

| Metric | Tool | Pixel Target | P20 Lite Target |
|--------|------|-------------|-----------------|
| Cold start | Android Profiler → Startup | < 2s | < 4s |
| Frame render time | GPU Rendering Profile | < 16ms (60fps) | < 16ms (60fps) |
| Dropped frames/min | FrameMetrics API | < 2 | < 5 |
| Heap usage (steady) | Android Profiler → Memory | < 200 MB | < 120 MB |
| Heap growth (4h) | Memory dump diff | < 10 MB | < 10 MB |
| Battery drain/hour | Battery Stats (adb) | < 5% | < 7% |
| HTTP response time | Client-side timing | < 50ms | < 100ms |
| Audio latency | Sync test (click → hear) | < 150ms | < 300ms |

---

## 6. Test Tools

| Tool | Purpose |
|------|---------|
| JUnit 4 | Unit test runner |
| MockK | Kotlin mocking |
| Turbine | Flow testing assertions |
| `curl` / PowerShell `Invoke-WebRequest` | HTTP endpoint testing |
| Wireshark | WOL packet and UDP audio verification |
| Android Profiler | Memory, CPU, network profiling |
| `adb shell dumpsys battery` | Battery stats |
| `adb logcat` | Runtime log monitoring |
| LeakCanary (debug only) | Memory leak detection |
| Strict Mode (debug only) | Disk/network on main thread detection |
