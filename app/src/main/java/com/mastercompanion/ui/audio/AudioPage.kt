package com.mastercompanion.ui.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercompanion.domain.model.AudioStreamState
import java.util.Locale

@Composable
fun AudioPage(
    streamState: AudioStreamState,
    onVolumeChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var volume by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { androidx.compose.runtime.mutableStateOf(false) }

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
            // ═══ LEFT COLUMN: Live Stream Monitor ═══
            Column(
                modifier = Modifier
                    .weight(1f)
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
                            .background(if (streamState.isReceiving) Color(0xFF10B981) else Color(0xFF6B7280))
                    )
                    Text(
                        text = if (streamState.isReceiving) "STREAMING ACTIVE" else "LISTENING (UDP :8421)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "PC Audio Passthrough",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Ultra low-latency WASAPI loopback receiver over Wi-Fi or USB tethering",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Stats Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF12141A))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AudioStatItem(
                                label = "CODEC",
                                value = "${streamState.codec.name} 48kHz Stereo",
                                color = Color(0xFF38BDF8)
                            )
                            AudioStatItem(
                                label = "LATENCY",
                                value = if (streamState.isReceiving) String.format(Locale.US, "%.0f ms", streamState.bufferLatencyMs) else "—",
                                color = Color(0xFF10B981)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AudioStatItem(
                                label = "PACKETS RECV",
                                value = "${streamState.packetsReceived}",
                                color = Color.White
                            )
                            AudioStatItem(
                                label = "PACKET LOSS",
                                value = if (streamState.packetsReceived > 0) {
                                    val lossRate = (streamState.packetsLost.toFloat() / (streamState.packetsReceived + streamState.packetsLost)) * 100f
                                    String.format(Locale.US, "%.1f%%", lossRate)
                                } else "0.0%",
                                color = if (streamState.packetsLost > 0) Color(0xFFF59E0B) else Color(0xFF34D399)
                            )
                        }

                        if (streamState.clientIp != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1D24))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Connected Source",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = streamState.clientIp,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF60A5FA)
                                )
                            }
                        }
                    }
                }
            }

            // ═══ RIGHT COLUMN: Volume & Quick Launch Guide ═══
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // Volume Card
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
                                text = "OUTPUT VOLUME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (isMuted) "MUTED" else "${(volume * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMuted) Color(0xFFEF4444) else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                isMuted = !isMuted
                                onVolumeChange(if (isMuted) 0f else volume)
                            }) {
                                Icon(
                                    imageVector = if (isMuted || volume == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }

                            Slider(
                                value = if (isMuted) 0f else volume,
                                onValueChange = {
                                    volume = it
                                    isMuted = false
                                    onVolumeChange(it)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = Color(0xFF262933)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Setup Instructions Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141720))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = "PC STREAMING COMMAND",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "python pc/audio/audio_streamer.py --target-ip <PHONE_IP>",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F1117))
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "For USB: Run pc/scripts/setup_adb_reverse.bat first",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
