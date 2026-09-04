$ErrorActionPreference = "Stop"

# Get GitHub token from git credentials
$credOutput = @("protocol=https", "host=github.com", "") | git credential fill
$tokenLine = $credOutput | Where-Object { $_ -like "password=*" }
if (-not $tokenLine) {
    throw "Could not retrieve GitHub token from git credential manager."
}
$token = $tokenLine.Substring(9)

$headers = @{
    "Authorization" = "Bearer $token"
    "Accept" = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
    "User-Agent" = "MasterCompanion-Deployer"
}

$releaseBody = @"
# ⚡ Master Companion v1.0.0 — Stable Release

Transform any rooted Android device into a permanent smart desk dashboard, hardware telemetry monitor, Spotify Car View player with karaoke synced lyrics, and low-latency PC companion bridge.

### 🌟 Key Highlights & Features
- **🕒 5 Standby Clock Styles**: Minimalist Digital, Analog Precision Gauge, Retro Split-Flap, Cyberpunk Terminal, Word Matrix.
- **🎵 Fullscreen Spotify Player**: 5 Layouts (Car View, Spinning Vinyl Turntable, Progressive Blur Karaoke Lyrics, Minimal Standby Clock, Full-Bleed Artwork).
- **🎼 Multi-Source Synced Lyrics**: LRCLIB primary + Lyrics.ovh fallback with instant memory caching.
- **🔋 Root Battery Guard**: Hardware sysfs power bypass with 80% charging threshold and live wattage telemetry.
- **📅 Google Calendar Sync**: Built-in upcoming agenda widget.
- **🌐 Web Remote Control Dashboard**: Glassmorphic web UI on port \`:8060\` with real-time SSE telemetry.
- **🎧 Low-Latency PC Audio Passthrough**: 48kHz stereo WASAPI loopback receiver on UDP port \`:8421\`.
- **⌨️ Physical Macro-Pad Bridge**: AutoHotkey v2 script with rotary volume knob sync.

### 📦 Installation
Download **app-release.apk** below and install directly onto your device:
\`\`\`powershell
adb install -r app-release.apk
\`\`\`
"@

$payload = @{
    tag_name = "v1.0.0"
    target_commitish = "master"
    name = "Master Companion v1.0.0 Stable"
    body = $releaseBody
    draft = $false
    prerelease = $false
} | ConvertTo-Json

# Check if release already exists
$existingRelease = $null
try {
    $existingRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/EZ4Reapz/master-companionion/releases/tags/v1.0.0" -Method Get -Headers $headers
    Write-Host "Found existing release for v1.0.0: $($existingRelease.html_url)"
} catch {
    Write-Host "No existing release found for v1.0.0, creating new release..."
}

if (-not $existingRelease) {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/EZ4Reapz/master-companionion/releases" -Method Post -Headers $headers -Body $payload -ContentType "application/json"
} else {
    $release = $existingRelease
}

Write-Host "Release URL: $($release.html_url)"

$apkPath = Resolve-Path "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) {
    throw "Release APK not found at $apkPath"
}

# Check if asset already uploaded
$existingAsset = $release.assets | Where-Object { $_.name -eq "app-release.apk" }
if ($existingAsset) {
    Write-Host "Deleting existing asset..."
    Invoke-RestMethod -Uri $existingAsset.url -Method Delete -Headers $headers
}

$uploadUrl = $release.upload_url -replace '\{\?name,label\}', '?name=app-release.apk'

Write-Host "Uploading app-release.apk ($( (Get-Item $apkPath).Length / 1MB ) MB)..."
$uploadHeaders = @{
    "Authorization" = "Bearer $token"
    "Accept" = "application/vnd.github+json"
    "X-GitHub-Api-Version" = "2022-11-28"
    "Content-Type" = "application/vnd.android.package-archive"
    "User-Agent" = "MasterCompanion-Deployer"
}

$assetResponse = Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers $uploadHeaders -InFile $apkPath
Write-Host "Asset uploaded successfully!"
Write-Host "Download URL: $($assetResponse.browser_download_url)"
