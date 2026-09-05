package com.mastercompanion.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LegalDialog(
    onDismiss: () -> Unit,
    whiteTheme: Boolean = false
) {
    val containerBg = if (whiteTheme) Color.White else Color(0xFF0F1117)
    val cardBg = if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF161922)
    val cardBorder = if (whiteTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)
    val textPrimary = if (whiteTheme) Color(0xFF0F172A) else Color.White
    val textSecondary = if (whiteTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.65f)
    val accentColor = Color(0xFF38BDF8)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerBg,
        shape = RoundedCornerShape(24.dp),
        title = {
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Policy,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Legal, EULA & Privacy",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Master Companion License & Terms",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = textPrimary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. EULA
                LegalSectionCard(
                    icon = Icons.Filled.Gavel,
                    title = "End User License Agreement (EULA)",
                    tint = Color(0xFF8B5CF6),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    content = """
                        Master Companion is open-source software licensed under the Apache License, Version 2.0. You are granted a personal, non-exclusive license to run and modify this software on compatible Android devices.
                        
                        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER LIABILITY ARISING FROM THE USE OF THIS SOFTWARE.
                    """.trimIndent()
                )

                // 2. Hardware Charge Control & Root Safety Disclaimer
                LegalSectionCard(
                    icon = Icons.Filled.WarningAmber,
                    title = "Root Charge Control & Hardware Safety",
                    tint = Color(0xFFF59E0B),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    content = """
                        The Battery Guard features of this application interact directly with Linux kernel sysfs power supply nodes (e.g. charging_enabled) via root privileges (su -c).
                        
                        While designed to prevent battery swelling and degradation through 80% charge limits and AC power bypass, you acknowledge that hardware charging behavior varies across device manufacturers and custom ROMs. You assume full responsibility for enabling root charge control on your device.
                    """.trimIndent()
                )

                // 3. Privacy Policy
                LegalSectionCard(
                    icon = Icons.Filled.Lock,
                    title = "100% Local-First Privacy Policy",
                    tint = Color(0xFF10B981),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    content = """
                        Master Companion is built with a strict Zero-Telemetry, 100% Local-First architecture:
                        
                        • No Analytics: We do not collect, store, or transmit your device identifier, IP address, usage habits, or personal information.
                        • Local Storage Only: Spotify OAuth tokens, Wi-Fi credentials, and IP/MAC preferences are stored exclusively on your device within encrypted Jetpack DataStore.
                        • Local Network Server: The embedded Ktor Web Dashboard (:8060) and Command Bridge (:8420) operate strictly on your local Wi-Fi / USB subnet.
                        • Third-Party APIs: The app only communicates with Spotify (for music telemetry via your authenticated session) and LRCLIB/Lyrics.ovh (for lyric lookup).
                    """.trimIndent()
                )

                // 4. Spotify Multi-User & Dev Mode Restriction
                LegalSectionCard(
                    icon = Icons.Filled.Security,
                    title = "Spotify Multi-User & Developer Mode Notice",
                    tint = Color(0xFF1DB954),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    content = """
                        Spotify Web API applications in "Development Mode" restrict authorization to accounts explicitly whitelisted by the app owner under developer.spotify.com > Dashboard > Settings > User Management.
                        
                        If connecting from another person's Spotify account:
                        1. The app owner can add that email under "User Management".
                        2. Alternatively, any user can create their own free Spotify App at developer.spotify.com, set the redirect URI to mastercompanion://spotify/callback, and paste their Client ID in Settings.
                    """.trimIndent()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "I Understand & Accept", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun LegalSectionCard(
    icon: ImageVector,
    title: String,
    tint: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        Text(
            text = content,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = textSecondary
        )
    }
}
