package com.mastercompanion.ui.system

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import com.mastercompanion.data.command.CommandLogEntry
import com.mastercompanion.platform.DeviceCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SystemPage(
    isRooted: Boolean = true,
    commandLogs: List<CommandLogEntry> = emptyList(),
    onTriggerTestCommand: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .padding(horizontal = if (isPortrait) 20.dp else 40.dp, vertical = if (isPortrait) 20.dp else 28.dp)
    ) {
        if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header badge
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
                            .background(Color(0xFF8B5CF6))
                    )
                    Text(
                        text = "HARDWARE & HTTP BRIDGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Text(
                    text = "System Diagnostics",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Diagnostic Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DiagItem(
                            label = "TARGET PROFILE",
                            value = if (DeviceCompat.isPixelDevice()) "Google Pixel 7 Pro" else "Huawei P20 Lite / Generic"
                        )
                        DiagItem(
                            label = "ROOT PRIVILEGES",
                            value = if (isRooted) "SU AVAILABLE (Kernel Control Active)" else "STANDARD (Non-root fallback)",
                            color = if (isRooted) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                        DiagItem(
                            label = "SYSFS CHARGE CONTROL",
                            value = DeviceCompat.getChargingControlPath(),
                            isMonospace = true
                        )
                        DiagItem(
                            label = "COMMAND BRIDGE SERVER",
                            value = "Ktor CIO HTTP Server (0.0.0.0:8420)",
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // Test Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionButton(
                        title = "Test Ping",
                        onClick = { onTriggerTestCommand("ping") }
                    )
                    ActionButton(
                        title = "Test WoL",
                        onClick = { onTriggerTestCommand("wol") }
                    )
                }

                // Live HTTP Command Bridge Logs (Height-constrained on portrait)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    BridgeLogsView(commandLogs = commandLogs, timeFormat = timeFormat)
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // ═══ LEFT COLUMN: Hardware & Root Diagnostics ═══
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // Header badge
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
                            .background(Color(0xFF8B5CF6))
                    )
                    Text(
                        text = "HARDWARE & HTTP BRIDGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "System Diagnostics",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Diagnostic Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DiagItem(
                            label = "TARGET PROFILE",
                            value = if (DeviceCompat.isPixelDevice()) "Google Pixel 7 Pro" else "Huawei P20 Lite / Generic"
                        )
                        DiagItem(
                            label = "ROOT PRIVILEGES",
                            value = if (isRooted) "SU AVAILABLE (Kernel Control Active)" else "STANDARD (Non-root fallback)",
                            color = if (isRooted) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                        DiagItem(
                            label = "SYSFS CHARGE CONTROL",
                            value = DeviceCompat.getChargingControlPath(),
                            isMonospace = true
                        )
                        DiagItem(
                            label = "COMMAND BRIDGE SERVER",
                            value = "Ktor CIO HTTP Server (0.0.0.0:8420)",
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Test Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionButton(
                        title = "Test Ping",
                        onClick = { onTriggerTestCommand("ping") }
                    )
                    ActionButton(
                        title = "Test WoL",
                        onClick = { onTriggerTestCommand("wol") }
                    )
                }
            }

            // ═══ RIGHT COLUMN: Live HTTP Command Bridge Logs ═══
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    BridgeLogsView(commandLogs = commandLogs, timeFormat = timeFormat)
                }
            }
        }
    }
}
}

@Composable
private fun BridgeLogsView(
    commandLogs: List<CommandLogEntry>,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BRIDGE ACTIVITY LOG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = "${commandLogs.size} events",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.35f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (commandLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bridge commands executed yet.\nCommands sent via :8420 will appear here.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.35f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(commandLogs) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF181B22))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.action,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = timeFormat.format(Date(entry.timestamp)),
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.35f)
                                )
                            }
                            Text(
                                text = entry.message,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                        }

                        Text(
                            text = entry.status.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.status.equals("ok", ignoreCase = true)) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagItem(
    label: String,
    value: String,
    color: Color = Color.White,
    isMonospace: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = color
        )
    }
}

@Composable
private fun ActionButton(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1D26))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
