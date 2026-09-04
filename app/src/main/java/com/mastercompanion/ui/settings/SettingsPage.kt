package com.mastercompanion.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPage(
    authToken: String = "master-companion-default-token",
    chargeStopThreshold: Int = 80,
    chargeResumeThreshold: Int = 75,
    pcIp: String = "192.168.1.100",
    pcMac: String = "00:00:00:00:00:00",
    spotifyClientId: String = "",
    isSpotifyConnected: Boolean = false,
    onSaveThresholds: (Int, Int) -> Unit = { _, _ -> },
    onRegenerateToken: () -> Unit = {},
    onSavePcNetwork: (String, String) -> Unit = { _, _ -> },
    onSaveSpotifyClientId: (String) -> Unit = {},
    onConnectSpotify: () -> Unit = {},
    onDisconnectSpotify: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var stopThreshold by remember(chargeStopThreshold) { mutableFloatStateOf(chargeStopThreshold.toFloat()) }
    var resumeThreshold by remember(chargeResumeThreshold) { mutableFloatStateOf(chargeResumeThreshold.toFloat()) }

    var ipInput by remember(pcIp) { mutableStateOf(pcIp) }
    var macInput by remember(pcMac) { mutableStateOf(pcMac) }
    var clientIdInput by remember(spotifyClientId) { mutableStateOf(spotifyClientId) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ═══ LEFT COLUMN: Battery Guard & Bridge Auth ═══
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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
                            .background(Color(0xFFF59E0B))
                    )
                    Text(
                        text = "CONFIGURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Text(
                    text = "Preferences",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Battery Guard Threshold Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "BATTERY GUARD THRESHOLDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Halt Charging At",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${stopThreshold.toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Slider(
                            value = stopThreshold,
                            onValueChange = {
                                stopThreshold = it
                                if (resumeThreshold >= stopThreshold) {
                                    resumeThreshold = stopThreshold - 5f
                                }
                                onSaveThresholds(stopThreshold.toInt(), resumeThreshold.toInt())
                            },
                            valueRange = 60f..95f,
                            steps = 6,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFF59E0B),
                                inactiveTrackColor = Color(0xFF262933)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Resume Charging At",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${resumeThreshold.toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                        Slider(
                            value = resumeThreshold,
                            onValueChange = {
                                resumeThreshold = it.coerceAtMost(stopThreshold - 2f)
                                onSaveThresholds(stopThreshold.toInt(), resumeThreshold.toInt())
                            },
                            valueRange = 50f..90f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF10B981),
                                inactiveTrackColor = Color(0xFF262933)
                            )
                        )
                    }
                }

                // Bridge Auth Token Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "COMMAND BRIDGE SECURITY TOKEN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF191C24))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = authToken,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Auth Token", authToken))
                                    Toast.makeText(context, "Auth token copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1D222E))
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

            // ═══ RIGHT COLUMN: Spotify PKCE & PC Network Details ═══
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Spotify Web API Integration Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SPOTIFY WEB API",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color(0xFF1DB954)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSpotifyConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isSpotifyConnected) "CONNECTED" else "NOT CONNECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpotifyConnected) Color(0xFF10B981) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = clientIdInput,
                            onValueChange = {
                                clientIdInput = it
                                onSaveSpotifyClientId(it)
                            },
                            label = { Text("Spotify Client ID", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1DB954),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1DB954))
                                    .clickable { onConnectSpotify() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Connect Account (PKCE)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            if (isSpotifyConnected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF26191D))
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

                // PC Network Config Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "DESKTOP PC NETWORK TARGET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF818CF8)
                        )

                        OutlinedTextField(
                            value = ipInput,
                            onValueChange = {
                                ipInput = it
                                onSavePcNetwork(ipInput, macInput)
                            },
                            label = { Text("PC IP Address", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = macInput,
                            onValueChange = {
                                macInput = it
                                onSavePcNetwork(ipInput, macInput)
                            },
                            label = { Text("PC MAC Address (for Wake-on-LAN)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
