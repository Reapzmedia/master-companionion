package com.mastercompanion.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Modern, cinematic Loading Splash Screen.
 * Solves the initial black screen lag by displaying an animated Master Companion emblem,
 * pulsating radar glow, and dynamic system initialization telemetry.
 */
@Composable
fun SplashScreen(
    onLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("INITIALIZING CORE HARDWARE...") }

    // Pulsing halo animation
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        // Step 1: Core System
        progress = 0.2f
        statusText = "INITIALIZING CORE HARDWARE INTERFACE..."
        delay(350L)

        // Step 2: Battery Guard & Sysfs
        progress = 0.45f
        statusText = "PROBING BATTERY SYSFS & 80% BYPASS SWITCH..."
        delay(350L)

        // Step 3: Network & Servers
        progress = 0.75f
        statusText = "STARTING EMBEDDED WEB SERVER (:8060 / :8420)..."
        delay(350L)

        // Step 4: Finalizing
        progress = 1.0f
        statusText = "STANDBY DASHBOARD READY"
        delay(300L)

        onLoaded()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = LinearEasing),
        label = "progress_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080B)),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = 0.15f * pulseAlpha),
                        Color(0xFF38BDF8).copy(alpha = 0.08f * pulseAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width / 2f
                ),
                radius = size.width / 2f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // High-Tech Companion Emblem Container
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating ring
                Canvas(modifier = Modifier.size(110.dp)) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF10B981),
                                Color(0xFF38BDF8),
                                Color(0xFFF59E0B),
                                Color(0xFF10B981)
                            )
                        ),
                        radius = size.width / 2f - 4.dp.toPx(),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner core box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF12151F))
                        .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "Master Companion",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Title
            Text(
                text = "MASTER COMPANION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle Tag
            Text(
                text = "SMART DESK STANDBY & PC BRIDGE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Linear Progress Bar
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2433))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF10B981), Color(0xFF38BDF8))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live status telemetry
            Text(
                text = statusText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // Bottom dock footer info
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Text("PIXEL STANDBY DOCK", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text("•", fontSize = 10.sp, color = Color(0xFF334155))
            Text("KTOR CIO :8060", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
        }
    }
}
