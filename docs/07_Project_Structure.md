# Project Structure & Architecture Guide
## Master Companion — Standby Dashboard & PC Bridge
**Version:** 1.0  
**Date:** 2026-09-04  
**Status:** Draft  

---

## 1. Project Directory Structure

```
master-companion/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mastercompanion/
│       │   │   ├── MasterCompanionApp.kt              ← @HiltAndroidApp
│       │   │   ├── MainActivity.kt                     ← Single Activity, landscape
│       │   │   │
│       │   │   ├── di/                                 ← Hilt modules
│       │   │   │   ├── AppModule.kt                    ← Singletons (DataStore, OkHttp, etc.)
│       │   │   │   ├── NetworkModule.kt                ← Retrofit, OkHttp, Ktor
│       │   │   │   └── RepositoryModule.kt             ← Repo bindings
│       │   │   │
│       │   │   ├── domain/                             ← Business logic (no Android deps)
│       │   │   │   ├── model/                          ← Domain models
│       │   │   │   │   ├── SpotifyTrack.kt
│       │   │   │   │   ├── BatteryData.kt
│       │   │   │   │   ├── CommandAction.kt
│       │   │   │   │   ├── AudioStreamState.kt
│       │   │   │   │   └── DeviceCapabilities.kt
│       │   │   │   └── usecase/                        ← Use cases
│       │   │   │       ├── GetCurrentTrackUseCase.kt
│       │   │   │       ├── ToggleChargeLimitUseCase.kt
│       │   │   │       ├── ExecuteCommandUseCase.kt
│       │   │   │       ├── SendWolPacketUseCase.kt
│       │   │   │       └── CheckRootAccessUseCase.kt
│       │   │   │
│       │   │   ├── data/                               ← Data layer
│       │   │   │   ├── spotify/
│       │   │   │   │   ├── SpotifyApi.kt               ← Retrofit interface
│       │   │   │   │   ├── SpotifyAuthManager.kt       ← OAuth PKCE flow
│       │   │   │   │   ├── SpotifyRepository.kt
│       │   │   │   │   └── dto/                        ← API response DTOs
│       │   │   │   │       ├── CurrentlyPlayingResponse.kt
│       │   │   │   │       ├── RecentlyPlayedResponse.kt
│       │   │   │   │       └── SpotifyTokenResponse.kt
│       │   │   │   ├── battery/
│       │   │   │   │   ├── BatteryRepository.kt
│       │   │   │   │   ├── RootBatteryDataSource.kt
│       │   │   │   │   ├── StandardBatteryDataSource.kt
│       │   │   │   │   └── BatteryDataSource.kt        ← Interface
│       │   │   │   ├── command/
│       │   │   │   │   ├── CommandRegistry.kt
│       │   │   │   │   ├── CommandExecutor.kt
│       │   │   │   │   └── CommandRepository.kt
│       │   │   │   ├── audio/
│       │   │   │   │   ├── AudioRepository.kt
│       │   │   │   │   ├── JitterBuffer.kt
│       │   │   │   │   ├── PacketParser.kt
│       │   │   │   │   └── OpusDecoder.kt
│       │   │   │   ├── network/
│       │   │   │   │   ├── WolSender.kt
│       │   │   │   │   └── NetworkRepository.kt
│       │   │   │   └── prefs/
│       │   │   │       └── PreferencesRepository.kt     ← DataStore wrapper
│       │   │   │
│       │   │   ├── service/                            ← Foreground services
│       │   │   │   ├── BatteryGuardService.kt
│       │   │   │   ├── CommandBridgeService.kt
│       │   │   │   ├── AudioReceiverService.kt
│       │   │   │   └── BootReceiver.kt
│       │   │   │
│       │   │   ├── server/                             ← Ktor embedded server
│       │   │   │   ├── CommandBridgeServer.kt          ← Ktor application setup
│       │   │   │   ├── routes/
│       │   │   │   │   ├── PingRoute.kt
│       │   │   │   │   ├── StatusRoute.kt
│       │   │   │   │   ├── CommandRoute.kt
│       │   │   │   │   └── CommandsListRoute.kt
│       │   │   │   └── middleware/
│       │   │   │       ├── AuthInterceptor.kt
│       │   │   │       └── RateLimiter.kt
│       │   │   │
│       │   │   ├── platform/                           ← Device-specific abstractions
│       │   │   │   ├── RootShell.kt
│       │   │   │   ├── DeviceCompat.kt                 ← Huawei, OEM detection
│       │   │   │   ├── ForegroundServiceCompat.kt
│       │   │   │   └── ImmersiveModeCompat.kt
│       │   │   │
│       │   │   ├── ui/                                 ← Presentation layer
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Theme.kt                    ← Material 3 dark theme
│       │   │   │   │   ├── Color.kt                    ← Color tokens
│       │   │   │   │   ├── Type.kt                     ← Typography scale
│       │   │   │   │   └── Dimensions.kt               ← Device-adaptive spacing
│       │   │   │   ├── dashboard/
│       │   │   │   │   ├── DashboardHost.kt            ← HorizontalPager scaffold
│       │   │   │   │   └── PageIndicator.kt
│       │   │   │   ├── home/
│       │   │   │   │   ├── HomePage.kt
│       │   │   │   │   ├── HomeViewModel.kt
│       │   │   │   │   ├── AlbumArtCard.kt
│       │   │   │   │   ├── MarqueeText.kt
│       │   │   │   │   ├── PlaybackControls.kt
│       │   │   │   │   ├── ClockWidget.kt
│       │   │   │   │   ├── BatteryPanel.kt
│       │   │   │   │   └── BatteryBar.kt
│       │   │   │   ├── audio/
│       │   │   │   │   ├── AudioPage.kt
│       │   │   │   │   ├── AudioViewModel.kt
│       │   │   │   │   ├── AudioVisualizer.kt
│       │   │   │   │   └── VolumeSlider.kt
│       │   │   │   ├── system/
│       │   │   │   │   ├── SystemPage.kt
│       │   │   │   │   ├── SystemViewModel.kt
│       │   │   │   │   ├── DeviceInfoPanel.kt
│       │   │   │   │   └── CommandLogPanel.kt
│       │   │   │   ├── settings/
│       │   │   │   │   ├── SettingsPage.kt
│       │   │   │   │   └── SettingsViewModel.kt
│       │   │   │   ├── onboarding/
│       │   │   │   │   ├── OnboardingFlow.kt
│       │   │   │   │   └── OnboardingViewModel.kt
│       │   │   │   └── common/                         ← Shared composables
│       │   │   │       ├── StatusBadge.kt
│       │   │   │       ├── ActionButton.kt
│       │   │   │       ├── ErrorBanner.kt
│       │   │   │       └── RootRequiredBadge.kt
│       │   │   │
│       │   │   └── util/                               ← Utilities
│       │   │       ├── AppLog.kt                       ← Ring buffer logger
│       │   │       ├── NetworkUtils.kt                 ← IP address helpers
│       │   │       └── Extensions.kt                   ← Kotlin extensions
│       │   │
│       │   ├── res/
│       │   │   ├── values/
│       │   │   │   ├── strings.xml
│       │   │   │   ├── colors.xml
│       │   │   │   └── themes.xml
│       │   │   ├── xml/
│       │   │   │   └── network_security_config.xml
│       │   │   ├── drawable/                           ← Vector icons, placeholders
│       │   │   └── mipmap-*/                           ← App icon
│       │   │
│       │   └── assets/
│       │       └── commands.json                       ← Default command registry
│       │
│       └── test/                                       ← Unit tests
│           └── java/com/mastercompanion/
│               ├── data/spotify/SpotifyRepositoryTest.kt
│               ├── data/battery/BatteryRepositoryTest.kt
│               ├── data/command/CommandExecutorTest.kt
│               ├── data/audio/JitterBufferTest.kt
│               ├── data/audio/PacketParserTest.kt
│               ├── data/network/WolSenderTest.kt
│               ├── server/AuthInterceptorTest.kt
│               ├── server/RateLimiterTest.kt
│               └── platform/DeviceCompatTest.kt
│
├── pc/                                                 ← PC-side utilities
│   ├── ahk/
│   │   ├── companion_bridge.ahk                        ← AHK v2 macro script
│   │   └── README.md
│   └── audio/
│       ├── audio_streamer.py                           ← Python audio streamer
│       ├── requirements.txt
│       └── README.md
│
├── docs/                                               ← This documentation
│   ├── 01_PRD.md
│   ├── 02_SRS.md
│   ├── 03_UI_UX_Architecture.md
│   ├── 04_Implementation_Plan.md
│   ├── 05_Testing_Strategy.md
│   ├── 06_Error_Handling_Spec.md
│   ├── 07_Project_Structure.md                         ← This file
│   └── 08_PC_Companion_Setup.md
│
├── build.gradle.kts                                    ← Root build script
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml                              ← Version catalog
├── .gitignore
└── README.md
```

---

## 2. Module Dependency Graph

```
┌──────────────────────────────────────────────────────┐
│                    :app module                        │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │              ui/ (Compose)                    │   │
│  │  Depends on: ViewModels, theme, domain models │   │
│  └───────────────────────┬──────────────────────┘   │
│                          │                           │
│  ┌───────────────────────▼──────────────────────┐   │
│  │           ViewModel layer                     │   │
│  │  Depends on: Use Cases, Repositories          │   │
│  └───────────────────────┬──────────────────────┘   │
│                          │                           │
│  ┌───────────────────────▼──────────────────────┐   │
│  │           domain/ (Use Cases + Models)         │   │
│  │  Pure Kotlin. No Android dependencies.         │   │
│  │  Depends on: Repository interfaces             │   │
│  └───────────────────────┬──────────────────────┘   │
│                          │                           │
│  ┌───────────────────────▼──────────────────────┐   │
│  │           data/ (Repositories + DTOs)          │   │
│  │  Depends on: platform/, Retrofit, DataStore    │   │
│  └───────────────────────┬──────────────────────┘   │
│                          │                           │
│  ┌───────────────────────▼──────────────────────┐   │
│  │  platform/ (RootShell, DeviceCompat)           │   │
│  │  Android-specific abstractions                 │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │  service/ (Foreground Services)               │   │
│  │  Depends on: data/, platform/                  │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │  server/ (Ktor Routes + Middleware)            │   │
│  │  Depends on: data/, platform/                  │   │
│  └──────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

**Dependency Rules:**
1. `ui/` → `domain/` (never directly to `data/`)
2. `domain/` → Repository interfaces (no concrete implementations)
3. `data/` → `platform/`, external libraries
4. `service/` → `data/`, `platform/`
5. `server/` → `data/`, `platform/`
6. `platform/` → Android SDK only

---

## 3. AndroidManifest.xml Skeleton

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"
        android:minSdkVersion="34" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"
        android:minSdkVersion="34" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"
        android:minSdkVersion="33" />

    <application
        android:name=".MasterCompanionApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.MasterCompanion"
        android:networkSecurityConfig="@xml/network_security_config"
        android:usesCleartextTraffic="true">

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:screenOrientation="landscape"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:windowSoftInputMode="adjustResize">
            
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- Spotify OAuth callback -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="mastercompanion"
                    android:host="spotify"
                    android:path="/callback" />
            </intent-filter>
        </activity>

        <!-- Services -->
        <service
            android:name=".service.BatteryGuardService"
            android:foregroundServiceType="specialUse"
            android:exported="false" />

        <service
            android:name=".service.CommandBridgeService"
            android:foregroundServiceType="specialUse"
            android:exported="false" />

        <service
            android:name=".service.AudioReceiverService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="false" />

        <!-- Boot Receiver -->
        <receiver
            android:name=".service.BootReceiver"
            android:exported="true"
            android:enabled="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

---

## 4. Key Design Patterns

### 4.1 Repository Pattern with Dual Data Sources

```kotlin
interface BatteryDataSource {
    fun getBatteryData(): Flow<BatteryData>
}

class BatteryRepository @Inject constructor(
    private val rootSource: RootBatteryDataSource,
    private val standardSource: StandardBatteryDataSource,
    private val deviceCapabilities: DeviceCapabilities
) {
    val batteryData: Flow<BatteryData> = if (deviceCapabilities.hasRoot) {
        rootSource.getBatteryData()
    } else {
        standardSource.getBatteryData()
    }
}
```

### 4.2 ViewModel → UI via StateFlow

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val spotifyRepo: SpotifyRepository,
    private val batteryRepo: BatteryRepository
) : ViewModel() {
    
    val uiState: StateFlow<HomeUiState> = combine(
        spotifyRepo.currentTrack,
        batteryRepo.batteryData
    ) { spotify, battery ->
        HomeUiState(spotify = spotify, battery = battery)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
```

### 4.3 Command Bridge → UI Channel

```kotlin
// In CommandExecutor (runs on Ktor IO thread)
object CommandChannel {
    private val _commands = MutableSharedFlow<UiCommand>(extraBufferCapacity = 10)
    val commands: SharedFlow<UiCommand> = _commands.asSharedFlow()
    
    suspend fun send(command: UiCommand) = _commands.emit(command)
}

sealed interface UiCommand {
    data class Navigate(val page: Int) : UiCommand
    data object RefreshSpotify : UiCommand
}

// In ViewModel (collects on Main thread)
init {
    viewModelScope.launch {
        CommandChannel.commands.collect { command ->
            when (command) {
                is UiCommand.Navigate -> _currentPage.value = command.page
                is UiCommand.RefreshSpotify -> spotifyRepo.refreshNow()
            }
        }
    }
}
```

### 4.4 Device-Adaptive Composable Pattern

```kotlin
@Composable
fun AdaptiveLayout(
    content: @Composable (isCompact: Boolean) -> Unit
) {
    val config = LocalConfiguration.current
    val isCompact = config.screenWidthDp < 700
    content(isCompact)
}

// Usage
AdaptiveLayout { isCompact ->
    AlbumArtCard(
        maxSize = if (isCompact) 220.dp else 300.dp,
        showGlow = !isCompact && isHighEndDevice()
    )
}
```

---

## 5. ProGuard / R8 Rules

```proguard
# ═══ Kotlin Serialization ═══
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mastercompanion.data.**$$serializer { *; }
-keepclassmembers class com.mastercompanion.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.mastercompanion.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ═══ Retrofit ═══
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ═══ OkHttp ═══
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ═══ Ktor ═══
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ═══ Coroutines ═══
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

---

## 6. Git Conventions

### Branch Strategy

```
main              ← Stable releases
  └── develop     ← Integration branch
       ├── feature/epic-1-spotify
       ├── feature/epic-2-battery
       ├── feature/epic-3-bridge
       ├── feature/epic-4-audio
       └── fix/emui-service-kill
```

### Commit Message Format

```
type(scope): short description

- feat(spotify): add OAuth PKCE flow with Custom Tabs fallback
- fix(battery): handle missing sysfs path on P20 Lite
- refactor(compat): extract foreground service wrapper for API 28
- test(command): add auth token validation unit tests
- docs(srs): expand API 28 compatibility section
```

### .gitignore

```
*.iml
.gradle
/local.properties
/.idea/
/build/
/captures
.externalNativeBuild
.cxx
local.properties

# Spotify secrets (NEVER commit)
spotify_client_id.txt
```
