# Error Handling & Recovery Specification
## Master Companion — Standby Dashboard & PC Bridge
**Version:** 1.0  
**Date:** 2026-09-04  
**Status:** Draft  

---

## 1. Error Classification

| Severity | Definition | User Impact | Response |
|----------|-----------|-------------|----------|
| **CRITICAL** | App cannot function | Dashboard unusable | Block UI with error + recovery action |
| **HIGH** | Major feature broken | One feature unavailable | Show error state on affected panel, rest works |
| **MEDIUM** | Degraded experience | Feature works with limitations | Show warning badge, log, continue |
| **LOW** | Minor issue | User may not notice | Log only, no UI impact |

---

## 2. Error Catalog

### 2.1 Spotify Errors

| Code | Error | Severity | UI State | Recovery Strategy |
|------|-------|----------|----------|-------------------|
| SP-001 | OAuth flow cancelled by user | MEDIUM | `NotAuthenticated` | Show "Sign in" button. No auto-retry. |
| SP-002 | OAuth token exchange failed | HIGH | `Error("Auth failed")` | Show error + "Try Again" button. Log details. |
| SP-003 | Access token expired + refresh succeeded | LOW | No change | Transparent to user. Log refresh event. |
| SP-004 | Refresh token expired / revoked | HIGH | `NotAuthenticated` | Prompt re-authentication. Clear stored tokens. |
| SP-005 | Rate limited (429) | MEDIUM | `RateLimited` | Back off per `Retry-After` header. Show "Paused" badge. |
| SP-006 | Network unreachable | MEDIUM | `Error("No network")` | Retain last known track. Retry every 10s. Show offline badge. |
| SP-007 | API returned unexpected format | MEDIUM | `Error("Parse error")` | Log full response. Retain last known state. |
| SP-008 | No active Spotify device | LOW | `Idle` or `RecentlyPlayed` | Show last track or idle clock. Normal behavior. |

### 2.2 Battery / Root Errors

| Code | Error | Severity | UI State | Recovery Strategy |
|------|-------|----------|----------|-------------------|
| BT-001 | Root not available | MEDIUM | chargeLimitSupported=false | Show "Root required" badge. Disable charge toggle. Use BatteryManager API. |
| BT-002 | sysfs path not found | MEDIUM | dataSource="api" | Fall back to BatteryManager. Log attempted paths. |
| BT-003 | sysfs write failed (permission denied) | HIGH | chargeLimitEnabled=false | Show error toast. Suggest re-granting root. |
| BT-004 | sysfs read returned garbage | MEDIUM | Previous values retained | Skip this reading. Log warning. Retry next cycle. |
| BT-005 | Charge limit service killed by OS | HIGH | Service stopped | `START_STICKY` auto-restart. On EMUI: show notification asking user to whitelist. |
| BT-006 | `su` binary missing or broken | HIGH | hasRoot=false | Fall back to non-root mode entirely. |

### 2.3 Command Bridge Errors

| Code | Error | Severity | UI State | Recovery Strategy |
|------|-------|----------|----------|-------------------|
| CB-001 | Port already in use | HIGH | ServerStatus.Error | Show error with "Change Port" option. Suggest port 8421–8430. |
| CB-002 | Server failed to start (unknown) | HIGH | ServerStatus.Error | Log stack trace. Show "Retry" button. |
| CB-003 | Invalid auth token in request | LOW | N/A (server-side only) | Return 401. Log source IP. |
| CB-004 | Unknown command action | LOW | N/A | Return 404. Log action name. |
| CB-005 | Command execution failed | MEDIUM | Log error entry | Return 500 with detail. Log full stack. |
| CB-006 | Request body parse error | LOW | N/A | Return 400. Log raw body (truncated to 1KB). |
| CB-007 | Rate limit exceeded | LOW | N/A | Return 429 with retry_after_ms. |

### 2.4 Audio Passthrough Errors

| Code | Error | Severity | UI State | Recovery Strategy |
|------|-------|----------|----------|-------------------|
| AU-001 | UDP socket bind failed | HIGH | AudioConnectionStatus.Error | Port in use. Show error + "Change Port". |
| AU-002 | No packets received (timeout) | MEDIUM | Disconnected | Enter reconnect loop. Show "Disconnected" with help text. |
| AU-003 | Opus decode error | MEDIUM | Audio glitch | Skip frame. Use PLC (Packet Loss Concealment). Log. |
| AU-004 | AudioTrack initialization failed | CRITICAL | Error | Show error dialog. May require app restart. |
| AU-005 | Packet loss > 5% | MEDIUM | Show packet loss stat | Log, display warning. Check network. |
| AU-006 | Jitter buffer underrun | LOW | Brief silence | Request more buffering (increase jitter buffer). |
| AU-007 | Audio output disconnected | MEDIUM | audioOutput="Speaker" | Notify user that headphones were disconnected. |

### 2.5 Network / WOL Errors

| Code | Error | Severity | UI State | Recovery Strategy |
|------|-------|----------|----------|-------------------|
| NW-001 | Wi-Fi disconnected | HIGH | Affects all network features | Show persistent banner "No Wi-Fi". Clock + battery still work. |
| NW-002 | WOL send failed | LOW | Toast "Failed to send" | Verify MAC address. Check network connectivity. |
| NW-003 | Cannot determine device IP | MEDIUM | IP shows "Unknown" | Show manual IP entry option. |

### 2.6 System / Platform Errors

| Code | Error | Severity | UI State | Recovery Strategy |
|------|-------|----------|----------|-------------------|
| SY-001 | EMUI killed background service | HIGH | Service stopped | Auto-restart via `START_STICKY`. Prompt user for whitelist. |
| SY-002 | Out of memory | CRITICAL | App crash | LeakCanary in debug. Reduce caches on low-RAM devices. |
| SY-003 | Disk full (DataStore write fail) | MEDIUM | Prefs not saved | Log warning. Toast "Storage full". |
| SY-004 | Custom Tabs unavailable (EMUI) | MEDIUM | OAuth fallback | Use browser Intent. If no browser: embedded WebView. |

---

## 3. Recovery Patterns

### 3.1 Exponential Backoff

Used for: Spotify polling after error, audio reconnect, token refresh retry.

```kotlin
suspend fun withExponentialBackoff(
    maxRetries: Int = Int.MAX_VALUE,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 30000,
    factor: Double = 2.0,
    block: suspend () -> Boolean  // returns true if successful
) {
    var delay = initialDelayMs
    var attempt = 0
    while (attempt < maxRetries) {
        if (block()) return
        delay(delay)
        delay = (delay * factor).toLong().coerceAtMost(maxDelayMs)
        attempt++
    }
}
```

### 3.2 Graceful Degradation Chain

```
Full Feature Set (Root + Network + Spotify)
    │
    ├── No Root → Disable: charge limit, shell exec, sysfs wattage
    │              Enable: BatteryManager data, all network features
    │
    ├── No Spotify → Disable: track display, playback controls
    │                Enable: clock, battery, command bridge, audio
    │
    ├── No Network → Disable: command bridge, audio, WOL, Spotify
    │                Enable: clock, battery (local)
    │
    └── No Root + No Network + No Spotify
                   Enable: clock only (minimal desk clock mode)
```

### 3.3 Last-Known-Good State

For Spotify and battery data, the app retains the last successfully fetched state when errors occur. This prevents the UI from flashing between data and error states during transient failures.

```kotlin
class SpotifyRepository {
    private var lastKnownTrack: SpotifyTrack? = null
    
    suspend fun fetchCurrentTrack(): SpotifyUiState {
        return try {
            val track = api.getCurrentlyPlaying()
            lastKnownTrack = track
            SpotifyUiState.Playing(track)
        } catch (e: Exception) {
            // Return last known state with error flag
            lastKnownTrack?.let { 
                SpotifyUiState.Playing(it)  // Keep showing, log error
            } ?: SpotifyUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

---

## 4. User-Facing Error Messages

| Context | Message | Tone |
|---------|---------|------|
| Spotify auth failed | "Couldn't connect to Spotify. Check your internet and try again." | Helpful |
| Root not found | "Root access not detected. Some features require a rooted device." | Informative |
| Charge limit failed | "Couldn't control charging. Root access may have been revoked." | Diagnostic |
| Server port in use | "Port 8420 is already in use. Try a different port in Settings." | Actionable |
| Audio disconnected | "Audio stream disconnected. Reconnecting..." | Status update |
| Network lost | "No Wi-Fi connection. Network features are paused." | Status update |
| EMUI killed service | "Battery optimization may have stopped background services. Tap to fix." | Actionable |
| Rate limited | "Spotify requests paused. Resuming shortly." | Brief |

**Principles:**
- Never show raw exception messages or stack traces to the user.
- Always suggest an action the user can take (if applicable).
- Use consistent tone: calm, helpful, non-technical.
- Error states should be visually distinct but not alarming (amber, not red, for recoverable issues).
