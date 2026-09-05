package com.mastercompanion.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercompanion.domain.model.BatteryData
import com.mastercompanion.domain.model.ChargingStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.material.icons.filled.Style
import com.mastercompanion.ui.common.PixelShiftContainer
import com.mastercompanion.ui.home.clock.ClockStyle
import com.mastercompanion.ui.home.clock.ClockStyleHost
import java.util.Locale

import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mastercompanion.domain.model.CalendarEvent

@Composable
fun HomePage(
    batteryData: BatteryData,
    pcMac: String = "",
    pcIp: String = "192.168.1.100",
    wolBroadcastIp: String = "192.168.1.255",
    whiteTheme: Boolean = false,
    pixelShiftEnabled: Boolean = true,
    use24Hour: Boolean = true,
    calendarEnabled: Boolean = true,
    autoFullscreenClockEnabled: Boolean = true,
    initialClockStyle: Int = 0,
    latestReminder: CalendarEvent? = null,
    upcomingEvents: List<CalendarEvent> = emptyList(),
    hasCalendarPermission: Boolean = true,
    syncedAccount: String? = null,
    onToggleChargeLimit: () -> Unit = {},
    onSendWol: (mac: String, ip: String, broadcastIp: String) -> Unit = { _, _, _ -> },
    onSavePcNetwork: (ip: String, mac: String) -> Unit = { _, _ -> },
    onSaveWolBroadcast: (String) -> Unit = {},
    onClockStyleChanged: (Int) -> Unit = {},
    onToggleCalendar: (Boolean) -> Unit = {},
    onFullscreenChanged: (Boolean) -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    onTriggerCalendarSync: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Current time update ticker
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    var isFullscreenClock by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 5-second inactivity timer to automatically go fullscreen clock
    LaunchedEffect(lastInteractionTime, isFullscreenClock, autoFullscreenClockEnabled) {
        if (!isFullscreenClock && autoFullscreenClockEnabled) {
            while (true) {
                val idle = System.currentTimeMillis() - lastInteractionTime
                if (idle >= 5000L) {
                    isFullscreenClock = true
                    onFullscreenChanged(true)
                    break
                }
                delay(500L)
            }
        }
    }

    val styles = ClockStyle.values()
    var styleIndex by remember(initialClockStyle) { mutableStateOf(initialClockStyle.coerceIn(0, styles.size - 1)) }
    val currentStyle = styles[styleIndex]

    // Wake-on-LAN custom config dialog state
    var showWolDialog by remember { mutableStateOf(false) }
    var inputMac by remember(pcMac) { mutableStateOf(if (pcMac == "00:00:00:00:00:00") "" else pcMac) }
    var inputIp by remember(pcIp) { mutableStateOf(pcIp) }
    var inputBroadcastIp by remember(wolBroadcastIp) { mutableStateOf(wolBroadcastIp) }

    val bgColor = if (whiteTheme) Color(0xFFF8F9FA) else Color(0xFF0A0A0C)
    val cardBg = if (whiteTheme) Color(0xFFFFFFFF) else Color(0xFF12141A)
    val cardBorder = if (whiteTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)
    val textColor = if (whiteTheme) Color(0xFF111827) else Color.White
    val textSubColor = if (whiteTheme) Color(0xFF4B5563) else Color.White.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { lastInteractionTime = System.currentTimeMillis() }
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        PixelShiftContainer(
            enabled = pixelShiftEnabled,
            modifier = Modifier.fillMaxSize()
        ) {
            Crossfade(
                targetState = isFullscreenClock,
                animationSpec = tween(400),
                label = "FullscreenClockCrossfade"
            ) { fullscreen ->
                if (fullscreen) {
                    // ═══ FULLSCREEN CLOCK ONLY MODE ═══
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isFullscreenClock = false
                                onFullscreenChanged(false)
                                lastInteractionTime = System.currentTimeMillis()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Exit Fullscreen floating pill in top-right
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (whiteTheme) Color(0xFFE2E8F0).copy(alpha = 0.9f) else Color(0xFF1E212B).copy(alpha = 0.9f))
                                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    isFullscreenClock = false
                                    onFullscreenChanged(false)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Exit Fullscreen",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }

                        // Centered Clock Display enlarged
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .graphicsLayer(scaleX = 1.25f, scaleY = 1.25f),
                            contentAlignment = Alignment.Center
                        ) {
                            ClockStyleHost(
                                style = currentStyle,
                                currentTime = currentTime,
                                batteryData = batteryData,
                                whiteTheme = whiteTheme,
                                use24Hour = use24Hour
                            )
                        }
                    }
                } else if (isPortrait) {
                    // ═══ VERTICAL PORTRAIT STANDBY HOME VIEW ═══
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Clock Style Switcher Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (whiteTheme) Color(0xFFEDF2F7) else Color(0xFF161822))
                                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    styleIndex = (styleIndex + 1) % styles.size
                                    onClockStyleChanged(styleIndex)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Style,
                                contentDescription = "Change Clock Style",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "STYLE: ${currentStyle.displayName.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }

                        // Hero Clock Composable
                        ClockStyleHost(
                            style = currentStyle,
                            currentTime = currentTime,
                            batteryData = batteryData,
                            whiteTheme = whiteTheme,
                            use24Hour = use24Hour
                        )

                        // Quick Actions Row
                        QuickActionsRow(
                            pcMac = pcMac,
                            calendarEnabled = calendarEnabled,
                            isFullscreenClock = isFullscreenClock,
                            whiteTheme = whiteTheme,
                            cardBorder = cardBorder,
                            textColor = textColor,
                            textSubColor = textSubColor,
                            onWakePc = {
                                if (pcMac.isBlank() || pcMac == "00:00:00:00:00:00") {
                                    showWolDialog = true
                                } else {
                                    Toast.makeText(context, "Broadcasting WoL packet to $pcMac...", Toast.LENGTH_SHORT).show()
                                    onSendWol(pcMac, pcIp, wolBroadcastIp)
                                }
                            },
                            onEditPc = { showWolDialog = true },
                            onToggleCalendar = onToggleCalendar,
                            onToggleFullscreen = {
                                lastInteractionTime = System.currentTimeMillis()
                                isFullscreenClock = !isFullscreenClock
                                onFullscreenChanged(isFullscreenClock)
                            }
                        )

                        // Google Calendar Agenda Widget
                        GoogleCalendarWidget(
                            calendarEnabled = calendarEnabled,
                            currentTime = currentTime,
                            hasCalendarPermission = hasCalendarPermission,
                            syncedAccount = syncedAccount,
                            upcomingEvents = upcomingEvents,
                            latestReminder = latestReminder,
                            use24Hour = use24Hour,
                            whiteTheme = whiteTheme,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textColor = textColor,
                            textSubColor = textSubColor,
                            onTriggerCalendarSync = onTriggerCalendarSync,
                            onToggleCalendar = onToggleCalendar,
                            onRequestCalendarPermission = onRequestCalendarPermission,
                            onOpenCalendar = onOpenCalendar
                        )

                        // Battery Guard & Rotary Knob Card
                        BatteryTelemetryCard(
                            batteryData = batteryData,
                            whiteTheme = whiteTheme,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textColor = textColor,
                            textSubColor = textSubColor,
                            onToggleChargeLimit = onToggleChargeLimit
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else {
                    // ═══ LANDSCAPE 2-COLUMN DASHBOARD VIEW ═══
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column: Clock + Style Switcher + Quick Actions
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (whiteTheme) Color(0xFFEDF2F7) else Color(0xFF161822))
                                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        styleIndex = (styleIndex + 1) % styles.size
                                        onClockStyleChanged(styleIndex)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Style,
                                    contentDescription = "Change Clock Style",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "STYLE: ${currentStyle.displayName.uppercase()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            ClockStyleHost(
                                style = currentStyle,
                                currentTime = currentTime,
                                batteryData = batteryData,
                                whiteTheme = whiteTheme,
                                use24Hour = use24Hour
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            QuickActionsRow(
                                pcMac = pcMac,
                                calendarEnabled = calendarEnabled,
                                isFullscreenClock = isFullscreenClock,
                                whiteTheme = whiteTheme,
                                cardBorder = cardBorder,
                                textColor = textColor,
                                textSubColor = textSubColor,
                                onWakePc = {
                                    if (pcMac.isBlank() || pcMac == "00:00:00:00:00:00") {
                                        showWolDialog = true
                                    } else {
                                        Toast.makeText(context, "Broadcasting WoL packet to $pcMac...", Toast.LENGTH_SHORT).show()
                                        onSendWol(pcMac, pcIp, wolBroadcastIp)
                                    }
                                },
                                onEditPc = { showWolDialog = true },
                                onToggleCalendar = onToggleCalendar,
                                onToggleFullscreen = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    isFullscreenClock = !isFullscreenClock
                                    onFullscreenChanged(isFullscreenClock)
                                }
                            )
                        }

                        // Right Column: Calendar Widget + Battery Telemetry Card
                        Column(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GoogleCalendarWidget(
                                calendarEnabled = calendarEnabled,
                                currentTime = currentTime,
                                hasCalendarPermission = hasCalendarPermission,
                                syncedAccount = syncedAccount,
                                upcomingEvents = upcomingEvents,
                                latestReminder = latestReminder,
                                use24Hour = use24Hour,
                                whiteTheme = whiteTheme,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textColor = textColor,
                                textSubColor = textSubColor,
                                onTriggerCalendarSync = onTriggerCalendarSync,
                                onToggleCalendar = onToggleCalendar,
                                onRequestCalendarPermission = onRequestCalendarPermission,
                                onOpenCalendar = onOpenCalendar
                            )

                            BatteryTelemetryCard(
                                batteryData = batteryData,
                                whiteTheme = whiteTheme,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textColor = textColor,
                                textSubColor = textSubColor,
                                onToggleChargeLimit = onToggleChargeLimit
                            )
                        }
                    }
                }
            }
        }
    }

    // ═══ Wake-on-LAN Configuration Dialog ═══
    if (showWolDialog) {
        AlertDialog(
            onDismissRequest = { showWolDialog = false },
            title = {
                Text(
                    text = "Configure Desktop PC Target",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Enter your PC's network card MAC address and Subnet Broadcast IP to send Layer 2 Wake-on-LAN packets across your Wi-Fi router.",
                        fontSize = 12.sp,
                        color = textSubColor
                    )

                    OutlinedTextField(
                        value = inputMac,
                        onValueChange = { inputMac = it },
                        label = { Text("PC MAC Address (e.g. 14:D6:4D:xx:xx:xx)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textSubColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputIp,
                        onValueChange = { inputIp = it },
                        label = { Text("Target IP (e.g. 192.168.1.100)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textSubColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputBroadcastIp,
                        onValueChange = { inputBroadcastIp = it },
                        label = { Text("Subnet Broadcast (e.g. 192.168.1.255 or 192.168.1.254)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textSubColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanMac = inputMac.trim()
                        val cleanIp = inputIp.trim().ifBlank { "192.168.1.100" }
                        val cleanBroadcast = inputBroadcastIp.trim().ifBlank { "192.168.1.255" }
                        if (cleanMac.isNotBlank()) {
                            onSavePcNetwork(cleanIp, cleanMac)
                            onSaveWolBroadcast(cleanBroadcast)
                            onSendWol(cleanMac, cleanIp, cleanBroadcast)
                            Toast.makeText(context, "Saved & sent WoL packet to $cleanMac", Toast.LENGTH_SHORT).show()
                            showWolDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a valid MAC address", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save & Wake PC", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWolDialog = false }) {
                    Text("Cancel", color = textSubColor)
                }
            },
            containerColor = cardBg
        )
    }
}

/**
 * Reusable Battery Telemetry & Chronograph Knob Card.
 */
@Composable
private fun BatteryTelemetryCard(
    batteryData: BatteryData,
    whiteTheme: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    textSubColor: Color,
    onToggleChargeLimit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (batteryData.isBypassed) "AC POWER BYPASS" else if (batteryData.isCharging) "CHARGING" else "DISCHARGING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = textSubColor
                )

                Switch(
                    checked = batteryData.isBypassed,
                    onCheckedChange = { onToggleChargeLimit() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF59E0B),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                        uncheckedTrackColor = Color(0xFF262933)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rotary Chronograph Charging Knob + Live Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ChargingKnobGauge(
                    batteryData = batteryData,
                    whiteTheme = whiteTheme,
                    size = 110.dp,
                    onToggleChargeLimit = onToggleChargeLimit
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMetric(
                        label = "LIVE POWER",
                        value = String.format(Locale.US, "%.1f W", kotlin.math.abs(batteryData.wattage)),
                        icon = Icons.Filled.Bolt,
                        tint = Color(0xFFFACC15),
                        textColor = textColor
                    )
                    TelemetryMetric(
                        label = "VOLTAGE",
                        value = String.format(Locale.US, "%.2f V", batteryData.voltageVolts),
                        icon = Icons.Filled.Power,
                        tint = Color(0xFF60A5FA),
                        textColor = textColor
                    )
                    TelemetryMetric(
                        label = "BATTERY TEMP",
                        value = String.format(Locale.US, "%.1f °C", batteryData.temperatureC),
                        icon = Icons.Filled.Thermostat,
                        tint = if (batteryData.temperatureC > 38f) Color(0xFFEF4444) else Color(0xFF34D399),
                        textColor = textColor
                    )
                }
            }
        }
    }
}

/**
 * Reusable Quick Actions Row for Wake-on-LAN, Calendar, and Fullscreen toggle.
 */
@Composable
private fun QuickActionsRow(
    pcMac: String,
    calendarEnabled: Boolean,
    isFullscreenClock: Boolean,
    whiteTheme: Boolean,
    cardBorder: Color,
    textColor: Color,
    textSubColor: Color,
    onWakePc: () -> Unit,
    onEditPc: () -> Unit,
    onToggleCalendar: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF1E212B))
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .clickable { onWakePc() }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Computer,
                contentDescription = "Wake PC",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (pcMac.isNotBlank() && pcMac != "00:00:00:00:00:00") "Wake PC ($pcMac)" else "Set & Wake PC",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontFamily = if (pcMac.isNotBlank() && pcMac != "00:00:00:00:00:00") FontFamily.Monospace else FontFamily.Default
            )
        }

        IconButton(
            onClick = onEditPc,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (whiteTheme) Color(0xFFEDF2F7) else Color(0xFF161820))
                .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit PC Network Details",
                tint = textSubColor,
                modifier = Modifier.size(15.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (calendarEnabled) (if (whiteTheme) Color(0xFFE0F2FE) else Color(0xFF0C2A40)) else (if (whiteTheme) Color(0xFFEDF2F7) else Color(0xFF1E212B)))
                .border(1.dp, if (calendarEnabled) Color(0xFF38BDF8).copy(alpha = 0.5f) else cardBorder, RoundedCornerShape(12.dp))
                .clickable { onToggleCalendar(!calendarEnabled) }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "Toggle Calendar",
                tint = if (calendarEnabled) Color(0xFF38BDF8) else textSubColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (calendarEnabled) "Cal: ON" else "Cal: OFF",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (calendarEnabled) (if (whiteTheme) Color(0xFF0369A1) else Color(0xFF38BDF8)) else textSubColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isFullscreenClock) (if (whiteTheme) Color(0xFFE0F2FE) else Color(0xFF0C2A40)) else (if (whiteTheme) Color(0xFFEDF2F7) else Color(0xFF1E212B)))
                .border(1.dp, if (isFullscreenClock) Color(0xFF38BDF8).copy(alpha = 0.5f) else cardBorder, RoundedCornerShape(12.dp))
                .clickable { onToggleFullscreen() }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = if (isFullscreenClock) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = "Toggle Fullscreen Clock",
                tint = if (isFullscreenClock) Color(0xFF38BDF8) else textSubColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (isFullscreenClock) "Full" else "Clock",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFullscreenClock) (if (whiteTheme) Color(0xFF0369A1) else Color(0xFF38BDF8)) else textSubColor
            )
        }
    }
}

/**
 * Reusable Google Calendar Widget.
 */
@Composable
private fun GoogleCalendarWidget(
    calendarEnabled: Boolean,
    currentTime: Long,
    hasCalendarPermission: Boolean,
    syncedAccount: String?,
    upcomingEvents: List<CalendarEvent>,
    latestReminder: CalendarEvent?,
    use24Hour: Boolean,
    whiteTheme: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    textSubColor: Color,
    onTriggerCalendarSync: () -> Unit,
    onToggleCalendar: (Boolean) -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (calendarEnabled) {
        val dateString = remember(currentTime) {
            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(currentTime)).uppercase()
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Minimalist Header: Date Pill + Synced Account + Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4285F4).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = dateString,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4),
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = if (hasCalendarPermission) (syncedAccount ?: "Google Calendar") else "Not Connected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasCalendarPermission) textSubColor else Color(0xFFF59E0B),
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (hasCalendarPermission) {
                            IconButton(
                                onClick = onTriggerCalendarSync,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Sync Calendar",
                                    tint = textSubColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onToggleCalendar(false) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Hide Calendar",
                                tint = textSubColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                if (!hasCalendarPermission) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Sync with Google Calendar to display your live agenda here.",
                            fontSize = 12.sp,
                            color = textSubColor
                        )
                        Button(
                            onClick = onRequestCalendarPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Connect Google Calendar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (upcomingEvents.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF161822))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "No upcoming events",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Text(
                                text = "Your schedule is clear",
                                fontSize = 11.sp,
                                color = textSubColor
                            )
                        }

                        TextButton(
                            onClick = onOpenCalendar,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Open App ↗",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4)
                            )
                        }
                    }
                } else {
                    val nextEvent = latestReminder ?: upcomingEvents.first()
                    val eventColor = if (nextEvent.color != 0) Color(nextEvent.color) else Color(0xFF4285F4)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (whiteTheme) Color(0xFFF8FAFC) else Color(0xFF171A24))
                            .border(1.dp, eventColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { onOpenCalendar() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(eventColor)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = nextEvent.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = nextEvent.formattedTimeRange(use24Hour),
                                    fontSize = 11.sp,
                                    color = textSubColor
                                )
                                if (nextEvent.location.isNotBlank()) {
                                    Text(
                                        text = "• ${nextEvent.location}",
                                        fontSize = 11.sp,
                                        color = textSubColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(eventColor.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = nextEvent.relativeTimeString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = eventColor
                            )
                        }
                    }

                    val otherEvents = upcomingEvents.filter { it.id != nextEvent.id }.take(2)
                    if (otherEvents.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            otherEvents.forEach { ev ->
                                val evColor = if (ev.color != 0) Color(ev.color) else Color(0xFF38BDF8)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF13151D))
                                    .clickable { onOpenCalendar() }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(evColor)
                                    )
                                    Text(
                                        text = ev.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = ev.formattedTimeRange(use24Hour),
                                    fontSize = 10.sp,
                                    color = textSubColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} else {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF161822))
            .clickable { onToggleCalendar(true) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarToday,
            contentDescription = "Show Calendar",
            tint = Color(0xFF4285F4),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "Show Google Calendar",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4285F4)
        )
    }
}
}

@Composable
private fun TelemetryMetric(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    textColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textColor.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
