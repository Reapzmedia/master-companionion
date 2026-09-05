package com.mastercompanion.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercompanion.BuildConfig
import com.mastercompanion.ui.home.clock.ClockStyle

@Composable
fun SettingsPage(
    authToken: String = "master-companion-default-token",
    chargeStopThreshold: Int = 80,
    chargeResumeThreshold: Int = 75,
    pcIp: String = "192.168.1.100",
    pcMac: String = "00:00:00:00:00:00",
    wolBroadcastIp: String = "192.168.1.255",
    spotifyClientId: String = "",
    isSpotifyConnected: Boolean = false,
    isLibraryScopeMissing: Boolean = false,
    whiteTheme: Boolean = false,
    pixelShift: Boolean = true,
    isServerRunning: Boolean = true,
    deviceIp: String = "127.0.0.1",
    use24Hour: Boolean = true,
    audioPassthroughEnabled: Boolean = true,
    chargeLimitEnabled: Boolean = true,
    calendarEnabled: Boolean = true,
    hasCalendarPermission: Boolean = true,
    syncedAccount: String? = null,
    clockStyleIndex: Int = 0,
    autoLaunchOnCharging: Boolean = true,
    autoFullscreenClockEnabled: Boolean = true,
    autoCheckUpdates: Boolean = true,
    currentAppVersion: String = BuildConfig.VERSION_NAME,
    onSaveThresholds: (Int, Int) -> Unit = { _, _ -> },
    onRegenerateToken: () -> Unit = {},
    onSavePcNetwork: (String, String) -> Unit = { _, _ -> },
    onSaveWolBroadcastIp: (String) -> Unit = {},
    onSaveSpotifyClientId: (String) -> Unit = {},
    onConnectSpotify: () -> Unit = {},
    onDisconnectSpotify: () -> Unit = {},
    onToggleWhiteTheme: (Boolean) -> Unit = {},
    onTogglePixelShift: (Boolean) -> Unit = {},
    onToggle24Hour: (Boolean) -> Unit = {},
    onToggleAudioPassthrough: (Boolean) -> Unit = {},
    onToggleChargeLimit: (Boolean) -> Unit = {},
    onToggleCalendar: (Boolean) -> Unit = {},
    onToggleAutoLaunchOnCharging: (Boolean) -> Unit = {},
    onToggleAutoFullscreenClock: (Boolean) -> Unit = {},
    onToggleAutoCheckUpdates: (Boolean) -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onClockStyleChanged: (Int) -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    onRefreshCalendar: () -> Unit = {},
    onStartServer: () -> Unit = {},
    onStopServer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var stopThreshold by remember(chargeStopThreshold) { mutableFloatStateOf(chargeStopThreshold.toFloat()) }
    var resumeThreshold by remember(chargeResumeThreshold) { mutableFloatStateOf(chargeResumeThreshold.toFloat()) }

    var ipInput by remember(pcIp) { mutableStateOf(pcIp) }
    var macInput by remember(pcMac) { mutableStateOf(pcMac) }
    var broadcastIpInput by remember(wolBroadcastIp) { mutableStateOf(wolBroadcastIp) }
    var clientIdInput by remember(spotifyClientId) { mutableStateOf(spotifyClientId) }

    var showAuthToken by remember { mutableStateOf(false) }
    var showClientId by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }

    // Collapsible drawer states
    var expandUpdates by remember { mutableStateOf(false) }
    var expandDisplay by remember { mutableStateOf(false) }
    var expandServer by remember { mutableStateOf(!isPortrait) }
    var expandCalendar by remember { mutableStateOf(false) }
    var expandAudio by remember { mutableStateOf(false) }
    var expandBattery by remember { mutableStateOf(false) }
    var expandSpotify by remember { mutableStateOf(false) }
    var expandNetwork by remember { mutableStateOf(false) }

    val bgColor = if (whiteTheme) Color(0xFFF8F9FA) else Color(0xFF0A0A0C)
    val cardBg = if (whiteTheme) Color(0xFFFFFFFF) else Color(0xFF12141A)
    val cardBorder = if (whiteTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)
    val textPrimary = if (whiteTheme) Color(0xFF111827) else Color.White
    val textSecondary = if (whiteTheme) Color(0xFF4B5563) else Color.White.copy(alpha = 0.7f)

    if (showLegalDialog) {
        LegalDialog(onDismiss = { showLegalDialog = false })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = if (isPortrait) 20.dp else 36.dp, vertical = if (isPortrait) 16.dp else 20.dp)
    ) {
        if (isPortrait) {
            // ═══════════════════════════════════════════════════════════════
            // PORTRAIT (VERTICAL) LAYOUT
            // ═══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (whiteTheme) Color(0xFFEDF2F7) else Color(0xFF16181D))
                                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Text(
                                    text = "SETTINGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = textPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Text(
                            text = "Preferences",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    // Legal button in header
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF1E222D))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .clickable { showLegalDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Policy, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(14.dp))
                            Text("Legal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }
                }

                // Quick Toggles Bar (Horizontal Scroll on Portrait)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickTogglePill(
                        icon = Icons.Filled.PowerSettingsNew,
                        label = "Server",
                        sublabel = if (isServerRunning) ":8060 ON" else "OFF",
                        checked = isServerRunning,
                        activeColor = Color(0xFF10B981),
                        onCheckedChange = { if (isServerRunning) onStopServer() else onStartServer() },
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Audio RX",
                        sublabel = if (audioPassthroughEnabled) ":8421 ON" else "OFF",
                        checked = audioPassthroughEnabled,
                        activeColor = Color(0xFF38BDF8),
                        onCheckedChange = onToggleAudioPassthrough,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.BatteryChargingFull,
                        label = "Bypass",
                        sublabel = if (chargeLimitEnabled) "80% Halt" else "Off",
                        checked = chargeLimitEnabled,
                        activeColor = Color(0xFFF59E0B),
                        onCheckedChange = onToggleChargeLimit,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.Shield,
                        label = "Anti-Burn",
                        sublabel = if (pixelShift) "Active" else "Off",
                        checked = pixelShift,
                        activeColor = Color(0xFF10B981),
                        onCheckedChange = onTogglePixelShift,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.AccessTime,
                        label = "Format",
                        sublabel = if (use24Hour) "24-Hour" else "12-Hour",
                        checked = use24Hour,
                        activeColor = Color(0xFFA855F7),
                        onCheckedChange = onToggle24Hour,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.Palette,
                        label = "White Mode",
                        sublabel = if (whiteTheme) "Porcelain" else "Dark OLED",
                        checked = whiteTheme,
                        activeColor = Color(0xFF818CF8),
                        onCheckedChange = onToggleWhiteTheme,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.CalendarToday,
                        label = "Calendar",
                        sublabel = if (calendarEnabled) "Synced" else "Off",
                        checked = calendarEnabled,
                        activeColor = Color(0xFF4285F4),
                        onCheckedChange = onToggleCalendar,
                        whiteTheme = whiteTheme
                    )
                }

                // All Feature Drawers in Vertical Stack
                AppUpdatesDrawer(
                    currentAppVersion = currentAppVersion,
                    autoCheckUpdates = autoCheckUpdates,
                    onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
                    onCheckForUpdates = onCheckForUpdates,
                    isExpanded = expandUpdates,
                    onToggleExpand = { expandUpdates = !expandUpdates },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    whiteTheme = whiteTheme
                )

                ServerDrawer(
                    deviceIp = deviceIp,
                    isServerRunning = isServerRunning,
                    authToken = authToken,
                    showAuthToken = showAuthToken,
                    onToggleShowToken = { showAuthToken = !showAuthToken },
                    onRegenerateToken = onRegenerateToken,
                    isExpanded = expandServer,
                    onToggleExpand = { expandServer = !expandServer },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    whiteTheme = whiteTheme,
                    context = context
                )

                NetworkDrawer(
                    ipInput = ipInput,
                    onIpChange = {
                        ipInput = it
                        onSavePcNetwork(ipInput, macInput)
                    },
                    macInput = macInput,
                    onMacChange = {
                        macInput = it
                        onSavePcNetwork(ipInput, macInput)
                    },
                    broadcastIpInput = broadcastIpInput,
                    onBroadcastIpChange = {
                        broadcastIpInput = it
                        onSaveWolBroadcastIp(broadcastIpInput)
                    },
                    isExpanded = expandNetwork,
                    onToggleExpand = { expandNetwork = !expandNetwork },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                BatteryDrawer(
                    stopThreshold = stopThreshold,
                    resumeThreshold = resumeThreshold,
                    onStopThresholdChange = {
                        stopThreshold = it
                        if (resumeThreshold >= stopThreshold) resumeThreshold = stopThreshold - 5f
                        onSaveThresholds(stopThreshold.toInt(), resumeThreshold.toInt())
                    },
                    onResumeThresholdChange = {
                        resumeThreshold = it.coerceAtMost(stopThreshold - 2f)
                        onSaveThresholds(stopThreshold.toInt(), resumeThreshold.toInt())
                    },
                    autoLaunchOnCharging = autoLaunchOnCharging,
                    onToggleAutoLaunchOnCharging = onToggleAutoLaunchOnCharging,
                    isExpanded = expandBattery,
                    onToggleExpand = { expandBattery = !expandBattery },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    whiteTheme = whiteTheme
                )

                SpotifyDrawer(
                    clientIdInput = clientIdInput,
                    onClientIdChange = {
                        clientIdInput = it
                        onSaveSpotifyClientId(it)
                    },
                    showClientId = showClientId,
                    onToggleShowClientId = { showClientId = !showClientId },
                    isSpotifyConnected = isSpotifyConnected,
                    isLibraryScopeMissing = isLibraryScopeMissing,
                    onConnectSpotify = onConnectSpotify,
                    onDisconnectSpotify = onDisconnectSpotify,
                    isExpanded = expandSpotify,
                    onToggleExpand = { expandSpotify = !expandSpotify },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    whiteTheme = whiteTheme,
                    context = context
                )

                CalendarDrawer(
                    hasCalendarPermission = hasCalendarPermission,
                    syncedAccount = syncedAccount,
                    onRequestCalendarPermission = onRequestCalendarPermission,
                    onRefreshCalendar = onRefreshCalendar,
                    isExpanded = expandCalendar,
                    onToggleExpand = { expandCalendar = !expandCalendar },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    whiteTheme = whiteTheme
                )

                DisplayDrawer(
                    clockStyleIndex = clockStyleIndex,
                    onClockStyleChanged = onClockStyleChanged,
                    autoFullscreenClockEnabled = autoFullscreenClockEnabled,
                    onToggleAutoFullscreenClock = onToggleAutoFullscreenClock,
                    isExpanded = expandDisplay,
                    onToggleExpand = { expandDisplay = !expandDisplay },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    whiteTheme = whiteTheme
                )

                AudioDrawer(
                    audioPassthroughEnabled = audioPassthroughEnabled,
                    isExpanded = expandAudio,
                    onToggleExpand = { expandAudio = !expandAudio },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    whiteTheme = whiteTheme
                )

                LegalCard(
                    onOpenLegal = { showLegalDialog = true },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // ═══════════════════════════════════════════════════════════════
            // LANDSCAPE (2-COLUMN) LAYOUT
            // ═══════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Title & Legal Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (whiteTheme) Color(0xFFEDF2F7) else Color(0xFF16181D))
                                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Text(
                                    text = "CONTROL HUB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = textPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Desk Preferences & Toggles",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Tap any category to expand details",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF1E222D))
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .clickable { showLegalDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Policy, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp))
                            Text("Legal, EULA & Privacy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        }
                    }
                }

                // Master Quick-Toggles Bar (moves up with page scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickTogglePill(
                        icon = Icons.Filled.PowerSettingsNew,
                        label = "Server",
                        sublabel = if (isServerRunning) ":8060 ON" else "OFF",
                        checked = isServerRunning,
                        activeColor = Color(0xFF10B981),
                        onCheckedChange = { if (isServerRunning) onStopServer() else onStartServer() },
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Audio RX",
                        sublabel = if (audioPassthroughEnabled) ":8421 ON" else "OFF",
                        checked = audioPassthroughEnabled,
                        activeColor = Color(0xFF38BDF8),
                        onCheckedChange = onToggleAudioPassthrough,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.BatteryChargingFull,
                        label = "Bypass",
                        sublabel = if (chargeLimitEnabled) "80% Halt" else "Off",
                        checked = chargeLimitEnabled,
                        activeColor = Color(0xFFF59E0B),
                        onCheckedChange = onToggleChargeLimit,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.Shield,
                        label = "Anti-Burn",
                        sublabel = if (pixelShift) "Active" else "Off",
                        checked = pixelShift,
                        activeColor = Color(0xFF10B981),
                        onCheckedChange = onTogglePixelShift,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.AccessTime,
                        label = "Format",
                        sublabel = if (use24Hour) "24-Hour" else "12-Hour",
                        checked = use24Hour,
                        activeColor = Color(0xFFA855F7),
                        onCheckedChange = onToggle24Hour,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.Palette,
                        label = "White Mode",
                        sublabel = if (whiteTheme) "Porcelain" else "Dark OLED",
                        checked = whiteTheme,
                        activeColor = Color(0xFF818CF8),
                        onCheckedChange = onToggleWhiteTheme,
                        whiteTheme = whiteTheme
                    )

                    QuickTogglePill(
                        icon = Icons.Filled.CalendarToday,
                        label = "Calendar",
                        sublabel = if (calendarEnabled) "Synced" else "Off",
                        checked = calendarEnabled,
                        activeColor = Color(0xFF4285F4),
                        onCheckedChange = onToggleCalendar,
                        whiteTheme = whiteTheme
                    )
                }

                // 2-Column Split Drawers (Unified with full-page scroll)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── LEFT COLUMN ──
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AppUpdatesDrawer(
                            currentAppVersion = currentAppVersion,
                            autoCheckUpdates = autoCheckUpdates,
                            onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
                            onCheckForUpdates = onCheckForUpdates,
                            isExpanded = expandUpdates,
                            onToggleExpand = { expandUpdates = !expandUpdates },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            whiteTheme = whiteTheme
                        )

                        ServerDrawer(
                            deviceIp = deviceIp,
                            isServerRunning = isServerRunning,
                            authToken = authToken,
                            showAuthToken = showAuthToken,
                            onToggleShowToken = { showAuthToken = !showAuthToken },
                            onRegenerateToken = onRegenerateToken,
                            isExpanded = expandServer,
                            onToggleExpand = { expandServer = !expandServer },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            whiteTheme = whiteTheme,
                            context = context
                        )

                        CalendarDrawer(
                            hasCalendarPermission = hasCalendarPermission,
                            syncedAccount = syncedAccount,
                            onRequestCalendarPermission = onRequestCalendarPermission,
                            onRefreshCalendar = onRefreshCalendar,
                            isExpanded = expandCalendar,
                            onToggleExpand = { expandCalendar = !expandCalendar },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            whiteTheme = whiteTheme
                        )

                        DisplayDrawer(
                            clockStyleIndex = clockStyleIndex,
                            onClockStyleChanged = onClockStyleChanged,
                            autoFullscreenClockEnabled = autoFullscreenClockEnabled,
                            onToggleAutoFullscreenClock = onToggleAutoFullscreenClock,
                            isExpanded = expandDisplay,
                            onToggleExpand = { expandDisplay = !expandDisplay },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            whiteTheme = whiteTheme
                        )
                    }

                    // ── RIGHT COLUMN ──
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BatteryDrawer(
                            stopThreshold = stopThreshold,
                            resumeThreshold = resumeThreshold,
                            onStopThresholdChange = {
                                stopThreshold = it
                                if (resumeThreshold >= stopThreshold) resumeThreshold = stopThreshold - 5f
                                onSaveThresholds(stopThreshold.toInt(), resumeThreshold.toInt())
                            },
                            onResumeThresholdChange = {
                                resumeThreshold = it.coerceAtMost(stopThreshold - 2f)
                                onSaveThresholds(stopThreshold.toInt(), resumeThreshold.toInt())
                            },
                            autoLaunchOnCharging = autoLaunchOnCharging,
                            onToggleAutoLaunchOnCharging = onToggleAutoLaunchOnCharging,
                            isExpanded = expandBattery,
                            onToggleExpand = { expandBattery = !expandBattery },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            whiteTheme = whiteTheme
                        )

                        SpotifyDrawer(
                            clientIdInput = clientIdInput,
                            onClientIdChange = {
                                clientIdInput = it
                                onSaveSpotifyClientId(it)
                            },
                            showClientId = showClientId,
                            onToggleShowClientId = { showClientId = !showClientId },
                            isSpotifyConnected = isSpotifyConnected,
                            isLibraryScopeMissing = isLibraryScopeMissing,
                            onConnectSpotify = onConnectSpotify,
                            onDisconnectSpotify = onDisconnectSpotify,
                            isExpanded = expandSpotify,
                            onToggleExpand = { expandSpotify = !expandSpotify },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            whiteTheme = whiteTheme,
                            context = context
                        )

                        NetworkDrawer(
                            ipInput = ipInput,
                            onIpChange = {
                                ipInput = it
                                onSavePcNetwork(ipInput, macInput)
                            },
                            macInput = macInput,
                            onMacChange = {
                                macInput = it
                                onSavePcNetwork(ipInput, macInput)
                            },
                            broadcastIpInput = broadcastIpInput,
                            onBroadcastIpChange = {
                                broadcastIpInput = it
                                onSaveWolBroadcastIp(broadcastIpInput)
                            },
                            isExpanded = expandNetwork,
                            onToggleExpand = { expandNetwork = !expandNetwork },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )

                        AudioDrawer(
                            audioPassthroughEnabled = audioPassthroughEnabled,
                            isExpanded = expandAudio,
                            onToggleExpand = { expandAudio = !expandAudio },
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            whiteTheme = whiteTheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Modular Drawers
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun AppUpdatesDrawer(
    currentAppVersion: String,
    autoCheckUpdates: Boolean,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    whiteTheme: Boolean
) {
    CollapsibleDrawer(
        title = "APPLICATION UPDATES & OTA",
        subtitle = "v$currentAppVersion • Direct GitHub Releases",
        icon = Icons.Filled.SystemUpdate,
        iconTint = Color(0xFF10B981),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Automatically checks for over-the-air updates published to GitHub. Installs silently on rooted devices or via standard Package Installer.",
                fontSize = 11.sp,
                color = textSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF191C24))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Installed Version: v$currentAppVersion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981))
                        .clickable { onCheckForUpdates() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Text("Check Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Check on App Launch",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Periodically queries GitHub API for hotfixes and updates",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                }
                Switch(
                    checked = autoCheckUpdates,
                    onCheckedChange = onToggleAutoCheckUpdates,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981)
                    )
                )
            }
        }
    }
}

@Composable
private fun ServerDrawer(
    deviceIp: String,
    isServerRunning: Boolean,
    authToken: String,
    showAuthToken: Boolean,
    onToggleShowToken: () -> Unit,
    onRegenerateToken: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    whiteTheme: Boolean,
    context: Context
) {
    CollapsibleDrawer(
        title = "EMBEDDED WEB & COMMAND SERVER",
        subtitle = if (isServerRunning) "● Port 8060 / 8420 Online" else "○ Stopped",
        icon = Icons.Filled.Lan,
        iconTint = Color(0xFF38BDF8),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val webUrl = "http://$deviceIp:8060"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF191C24))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = webUrl,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Dashboard URL", webUrl))
                            Toast.makeText(context, "URL copied: $webUrl", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy URL",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInBrowser,
                            contentDescription = "Open in Browser",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Command Bridge Security Token
            Text(
                text = "AUTH SECURITY TOKEN",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF191C24))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showAuthToken) authToken else "••••••••••••••••••••••••••••••••",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleShowToken,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (showAuthToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Auth Token", authToken))
                            Toast.makeText(context, "Auth token copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (whiteTheme) Color(0xFFE0F2FE) else Color(0xFF1D222E))
                    .clickable { onRegenerateToken() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Regenerate Token",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}

@Composable
private fun CalendarDrawer(
    hasCalendarPermission: Boolean,
    syncedAccount: String?,
    onRequestCalendarPermission: () -> Unit,
    onRefreshCalendar: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    whiteTheme: Boolean
) {
    CollapsibleDrawer(
        title = "GOOGLE CALENDAR & REMINDERS",
        subtitle = if (hasCalendarPermission) "● Synced with Android System" else "Action Required",
        icon = Icons.Filled.CalendarToday,
        iconTint = Color(0xFF4285F4),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Reads your device's synced Google Calendar events and reminders to show on the Standby Home page.",
                fontSize = 12.sp,
                color = textSecondary
            )

            if (!syncedAccount.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF161822))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Text(text = "Google Account: $syncedAccount", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!hasCalendarPermission) {
                    Button(
                        onClick = onRequestCalendarPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Calendar Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onRefreshCalendar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF1E222D),
                        contentColor = textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh Events", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DisplayDrawer(
    clockStyleIndex: Int,
    onClockStyleChanged: (Int) -> Unit,
    autoFullscreenClockEnabled: Boolean,
    onToggleAutoFullscreenClock: (Boolean) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    whiteTheme: Boolean
) {
    CollapsibleDrawer(
        title = "DISPLAY & CLOCK STYLES",
        subtitle = "6 Clock faces (incl. Pure Minimalist), Anti-Burn, 12h/24h",
        icon = Icons.Filled.Tune,
        iconTint = Color(0xFFA855F7),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "ACTIVE CLOCK FACE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClockStyle.values().forEachIndexed { index, style ->
                    val isSelected = clockStyleIndex == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFFA855F7)
                                else if (whiteTheme) Color(0xFFE2E8F0)
                                else Color(0xFF1D2029)
                            )
                            .clickable { onClockStyleChanged(index) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = style.displayName,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else textSecondary
                        )
                    }
                }
            }

            // Auto-Fullscreen Clock (5s idle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF13151D))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Fullscreen Clock (5s Idle)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Automatically enlarges the clock to full screen after 5 seconds of inactivity",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                }
                Switch(
                    checked = autoFullscreenClockEnabled,
                    onCheckedChange = onToggleAutoFullscreenClock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFA855F7)
                    )
                )
            }
        }
    }
}

@Composable
private fun BatteryDrawer(
    stopThreshold: Float,
    resumeThreshold: Float,
    onStopThresholdChange: (Float) -> Unit,
    onResumeThresholdChange: (Float) -> Unit,
    autoLaunchOnCharging: Boolean,
    onToggleAutoLaunchOnCharging: (Boolean) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    whiteTheme: Boolean
) {
    CollapsibleDrawer(
        title = "BATTERY GUARD & POWER BYPASS",
        subtitle = "Auto-halt at ${stopThreshold.toInt()}% • Resume at ${resumeThreshold.toInt()}%",
        icon = Icons.Filled.BatteryChargingFull,
        iconTint = Color(0xFFF59E0B),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Halt Charging At", fontSize = 12.sp, color = textSecondary)
                Text("${stopThreshold.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            }
            Slider(
                value = stopThreshold,
                onValueChange = onStopThresholdChange,
                valueRange = 60f..95f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF59E0B),
                    activeTrackColor = Color(0xFFF59E0B),
                    inactiveTrackColor = if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF262933)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Resume Charging At", fontSize = 12.sp, color = textSecondary)
                Text("${resumeThreshold.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
            Slider(
                value = resumeThreshold,
                onValueChange = onResumeThresholdChange,
                valueRange = 50f..90f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF10B981),
                    activeTrackColor = Color(0xFF10B981),
                    inactiveTrackColor = if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF262933)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Launch on Dock / Charging",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Wakes screen and opens companion when power connects",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                }
                Switch(
                    checked = autoLaunchOnCharging,
                    onCheckedChange = onToggleAutoLaunchOnCharging,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF59E0B)
                    )
                )
            }
        }
    }
}

@Composable
private fun SpotifyDrawer(
    clientIdInput: String,
    onClientIdChange: (String) -> Unit,
    showClientId: Boolean,
    onToggleShowClientId: () -> Unit,
    isSpotifyConnected: Boolean,
    isLibraryScopeMissing: Boolean,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    whiteTheme: Boolean,
    context: Context
) {
    CollapsibleDrawer(
        title = "SPOTIFY WEB API INTEGRATION",
        subtitle = if (isLibraryScopeMissing) "⚠️ Library Permission Missing" else if (isSpotifyConnected) "● Connected & Synced" else "○ Not Connected",
        icon = Icons.Filled.MusicNote,
        iconTint = if (isLibraryScopeMissing) Color(0xFFF59E0B) else Color(0xFF1DB954),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = if (isLibraryScopeMissing) Color(0xFFF59E0B).copy(alpha = 0.4f) else cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Multi-user & Developer Mode Explanatory Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (whiteTheme) Color(0xFFF0FDF4) else Color(0xFF10261A))
                    .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(15.dp))
                        Text(
                            text = "Multi-User & Developer Mode Notice",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (whiteTheme) Color(0xFF166534) else Color(0xFF4ADE80)
                        )
                    }
                    Text(
                        text = "In Spotify's Developer Mode, only whitelisted accounts can connect. If connecting a different account, add their email under 'User Management' in your Spotify Developer Dashboard, or enter their personal Spotify Client ID below.",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1DB954).copy(alpha = 0.15f))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.spotify.com/dashboard"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(13.dp))
                        Text("Open Developer Dashboard", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1DB954))
                    }
                }
            }

            if (isLibraryScopeMissing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (whiteTheme) Color(0xFFFEF3C7) else Color(0xFF2A2010))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Library Permission Required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (whiteTheme) Color(0xFF92400E) else Color(0xFFFCD34D)
                            )
                            Text(
                                text = "Saving songs to Liked Songs requires Spotify library permissions. Tap Reconnect below to grant permission.",
                                fontSize = 10.sp,
                                color = if (whiteTheme) Color(0xFFB45309) else Color(0xFFFDE68A)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = clientIdInput,
                onValueChange = onClientIdChange,
                label = { Text("Spotify Client ID", fontSize = 11.sp) },
                singleLine = true,
                visualTransformation = if (showClientId) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onToggleShowClientId) {
                        Icon(
                            imageVector = if (showClientId) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = textSecondary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary.copy(alpha = 0.85f),
                    focusedLabelColor = Color(0xFF1DB954),
                    unfocusedLabelColor = textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLibraryScopeMissing) Color(0xFFF59E0B) else Color(0xFF1DB954))
                        .clickable { onConnectSpotify() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isLibraryScopeMissing) "Reconnect (Grant Library Access)" else if (isSpotifyConnected) "Reconnect Account" else "Connect Account (PKCE)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (isSpotifyConnected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (whiteTheme) Color(0xFFFEE2E2) else Color(0xFF26191D))
                            .clickable { onDisconnectSpotify() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Disconnect",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkDrawer(
    ipInput: String,
    onIpChange: (String) -> Unit,
    macInput: String,
    onMacChange: (String) -> Unit,
    broadcastIpInput: String,
    onBroadcastIpChange: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    CollapsibleDrawer(
        title = "DESKTOP PC NETWORK TARGET",
        subtitle = "IP: $ipInput • MAC: ${if (macInput == "00:00:00:00:00:00") "Not Set" else macInput}",
        icon = Icons.Filled.Computer,
        iconTint = Color(0xFF818CF8),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = ipInput,
                onValueChange = onIpChange,
                label = { Text("PC IP Address", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary.copy(alpha = 0.85f),
                    focusedLabelColor = Color(0xFF818CF8),
                    unfocusedLabelColor = textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = macInput,
                onValueChange = onMacChange,
                label = { Text("PC MAC Address (for Wake-on-LAN)", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary.copy(alpha = 0.85f),
                    focusedLabelColor = Color(0xFF818CF8),
                    unfocusedLabelColor = textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = broadcastIpInput,
                onValueChange = onBroadcastIpChange,
                label = { Text("Subnet Broadcast IP (Layer 2 WoL, e.g. 192.168.1.255 / 192.168.1.254)", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF818CF8),
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary.copy(alpha = 0.85f),
                    focusedLabelColor = Color(0xFF818CF8),
                    unfocusedLabelColor = textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AudioDrawer(
    audioPassthroughEnabled: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    whiteTheme: Boolean
) {
    CollapsibleDrawer(
        title = "PC AUDIO PASSTHROUGH RECEIVER",
        subtitle = "UDP Port 8421 • 48kHz Stereo",
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        iconTint = Color(0xFF38BDF8),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Receives 20ms low-latency WASAPI loopback audio stream from your PC via audio_streamer.py over UDP port 8421.",
                fontSize = 12.sp,
                color = textSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF191C24))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status", fontSize = 11.sp, color = textSecondary)
                Text(if (audioPassthroughEnabled) "READY / LISTENING" else "DISABLED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (audioPassthroughEnabled) Color(0xFF10B981) else Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun LegalCard(
    onOpenLegal: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable { onOpenLegal() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Policy, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                Column {
                    Text("LEGAL, EULA & PRIVACY POLICY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("Terms of Service, Kernel Disclaimer, Privacy", fontSize = 10.sp, color = textSecondary)
                }
            }
            Text("Review", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Helper UI Components
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun QuickTogglePill(
    icon: ImageVector,
    label: String,
    sublabel: String,
    checked: Boolean,
    activeColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    whiteTheme: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) activeColor.copy(alpha = 0.12f) else if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF161922))
            .border(1.dp, if (checked) activeColor.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) activeColor else if (whiteTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF),
            modifier = Modifier.size(15.dp)
        )
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (whiteTheme) Color(0xFF111827) else Color.White
            )
            Text(
                text = sublabel,
                fontSize = 9.sp,
                color = if (checked) activeColor else if (whiteTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun CollapsibleDrawer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(cardBorder)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}
