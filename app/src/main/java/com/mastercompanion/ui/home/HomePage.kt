package com.mastercompanion.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.util.Locale

@Composable
fun HomePage(
    batteryData: BatteryData,
    pcMac: String = "",
    pcIp: String = "192.168.1.100",
    onToggleChargeLimit: () -> Unit = {},
    onSendWol: (mac: String, ip: String) -> Unit = { _, _ -> },
    onSavePcNetwork: (ip: String, mac: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Current time update ticker
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val secondsFormat = remember { SimpleDateFormat(":ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val formattedTime = remember(currentTime) { timeFormat.format(Date(currentTime)) }
    val formattedSeconds = remember(currentTime) { secondsFormat.format(Date(currentTime)) }
    val formattedDate = remember(currentTime) { dateFormat.format(Date(currentTime)) }

    // Wake-on-LAN custom config dialog state
    var showWolDialog by remember { mutableStateOf(false) }
    var inputMac by remember(pcMac) { mutableStateOf(if (pcMac == "00:00:00:00:00:00") "" else pcMac) }
    var inputIp by remember(pcIp) { mutableStateOf(pcIp) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .padding(horizontal = 40.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ═══ LEFT COLUMN: Clean Hero Standby Clock ═══
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF16181D))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (batteryData.isBypassed) Color(0xFFF59E0B)
                                else if (batteryData.isCharging) Color(0xFF10B981)
                                else Color(0xFF6B7280)
                            )
                    )
                    Text(
                        text = if (batteryData.isBypassed) "AC BYPASS ACTIVE"
                        else if (batteryData.isCharging) "DOCK CHARGING"
                        else "BATTERY POWER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Digits
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formattedTime,
                        fontSize = 92.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-2).sp,
                        lineHeight = 92.sp
                    )
                    Text(
                        text = formattedSeconds,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                }

                Text(
                    text = formattedDate,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.65f),
                    letterSpacing = 0.2.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Quick Action: Wake-on-LAN Button + Edit Target PC
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E212B))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable {
                                if (pcMac.isBlank() || pcMac == "00:00:00:00:00:00") {
                                    showWolDialog = true
                                } else {
                                    Toast.makeText(context, "Broadcasting WoL packet to $pcMac...", Toast.LENGTH_SHORT).show()
                                    onSendWol(pcMac, pcIp)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Computer,
                            contentDescription = "Wake PC",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (pcMac.isNotBlank() && pcMac != "00:00:00:00:00:00") "Wake PC ($pcMac)" else "Set & Wake PC",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontFamily = if (pcMac.isNotBlank() && pcMac != "00:00:00:00:00:00") FontFamily.Monospace else FontFamily.Default
                        )
                    }

                    // Edit target PC icon
                    IconButton(
                        onClick = { showWolDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF161820))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit PC Network Details",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ═══ RIGHT COLUMN: Real-Time Battery & Hardware Telemetry ═══
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        // Header row with Level % and Bypass Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BATTERY GUARD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${batteryData.level}",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when {
                                            batteryData.isBypassed -> Color(0xFFF59E0B)
                                            batteryData.level > 20 -> Color(0xFF10B981)
                                            else -> Color(0xFFEF4444)
                                        }
                                    )
                                    Text(
                                        text = "%",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                    )
                                }
                            }

                            // 80% Bypass Switch
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (batteryData.isBypassed) "80% Bypass ON" else "Standard",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (batteryData.isBypassed) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
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
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Telemetry Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TelemetryMetric(
                                label = "POWER",
                                value = String.format(Locale.US, "%.1f W", kotlin.math.abs(batteryData.wattage)),
                                icon = Icons.Filled.Bolt,
                                tint = Color(0xFFFACC15)
                            )
                            TelemetryMetric(
                                label = "VOLTAGE",
                                value = String.format(Locale.US, "%.2f V", batteryData.voltageVolts),
                                icon = Icons.Filled.Power,
                                tint = Color(0xFF60A5FA)
                            )
                            TelemetryMetric(
                                label = "TEMP",
                                value = String.format(Locale.US, "%.1f °C", batteryData.temperatureC),
                                icon = Icons.Filled.Thermostat,
                                tint = if (batteryData.temperatureC > 38f) Color(0xFFEF4444) else Color(0xFF34D399)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Flow Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF191C24))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Flow",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${if (batteryData.currentMa >= 0) "+" else ""}${batteryData.currentMa} mA",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (batteryData.currentMa >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
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
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Enter your PC's network card details to send Wake-on-LAN packets. (Run 'getmac' in PowerShell on Windows to find it).",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = inputMac,
                        onValueChange = { inputMac = it },
                        label = { Text("PC MAC Address (e.g. 14:D6:4D:xx:xx:xx)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputIp,
                        onValueChange = { inputIp = it },
                        label = { Text("Target IP / Broadcast (default: 255.255.255.255)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanMac = inputMac.trim()
                        val cleanIp = inputIp.trim().ifBlank { "255.255.255.255" }
                        if (cleanMac.isNotBlank()) {
                            onSavePcNetwork(cleanIp, cleanMac)
                            onSendWol(cleanMac, cleanIp)
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
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF141722)
        )
    }
}

@Composable
private fun TelemetryMetric(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color
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
                color = Color.White.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
