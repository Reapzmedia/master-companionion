package com.mastercompanion.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercompanion.domain.model.BatteryData
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity rotary chronograph charging knob and telemetry gauge.
 * Displays real-time battery percentage arc, live wattage (W), and bypass mode status.
 */
@Composable
fun ChargingKnobGauge(
    batteryData: BatteryData,
    whiteTheme: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    onToggleChargeLimit: () -> Unit = {}
) {
    val animatedPercent by animateFloatAsState(
        targetValue = batteryData.level / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "BatteryArcSpring"
    )

    // Dynamic accent color based on status
    val arcColor = when {
        batteryData.isBypassed -> Color(0xFFF59E0B) // Amber AC Bypass
        batteryData.isCharging -> Color(0xFF10B981) // Emerald Fast Charge
        batteryData.level <= 20 -> Color(0xFFEF4444) // Crimson Low Battery
        else -> Color(0xFF38BDF8) // Sky Blue Battery
    }

    val trackBgColor = if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF1A1D27)
    val dialBg = if (whiteTheme) Color(0xFFF8FAFC) else Color(0xFF11141E)
    val textPrimary = if (whiteTheme) Color(0xFF0F172A) else Color.White
    val textSecondary = if (whiteTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleChargeLimit
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = size.toPx() - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Outer dial background
            drawCircle(
                color = dialBg,
                radius = size.toPx() / 2f
            )

            // 270 degree sweep from 135 deg to 405 deg
            val startAngle = 135f
            val totalSweep = 270f

            // Background track
            drawArc(
                color = trackBgColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active animated progress arc
            val sweep = (animatedPercent * totalSweep).coerceIn(0f, totalSweep)
            if (sweep > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Outer perimeter graduation tick marks
            val tickCount = 27
            val radius = size.toPx() / 2f - strokeWidth - 6.dp.toPx()
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)

            for (i in 0..tickCount) {
                val angleDeg = startAngle + (i.toFloat() / tickCount) * totalSweep
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 9 == 0
                val tickLen = if (isMajor) 6.dp.toPx() else 3.dp.toPx()
                val tickColor = if (isMajor) arcColor.copy(alpha = 0.8f) else trackBgColor.copy(alpha = 0.6f)

                val start = Offset(
                    (center.x + (radius - tickLen) * cos(angleRad)).toFloat(),
                    (center.y + (radius - tickLen) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + radius * cos(angleRad)).toFloat(),
                    (center.y + radius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center telemetry readouts: Percentage + Live Wattage + Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${batteryData.level}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Default,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Live wattage chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = arcColor,
                    modifier = Modifier.size(13.dp)
                )
                val wattageDisplay = if (batteryData.isBypassed) {
                    "BYPASS"
                } else {
                    batteryData.formattedPower
                }
                Text(
                    text = wattageDisplay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = arcColor,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (batteryData.isBypassed) "AC POWER" else if (batteryData.isCharging) "CHARGING" else "DISCHARGING",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = textSecondary
            )
        }
    }
}
