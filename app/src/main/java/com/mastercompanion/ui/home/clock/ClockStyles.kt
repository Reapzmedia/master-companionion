package com.mastercompanion.ui.home.clock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercompanion.domain.model.BatteryData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Suite of 5 distinct Standby Clock styles:
 * 0: Minimalist Digital
 * 1: Analog Precision Gauge (Chronograph Dial)
 * 2: Retro Split-Flap Mechanical Cards
 * 3: Cyberpunk Terminal Neon Monospace
 * 4: Word Clock Matrix
 */

enum class ClockStyle(val displayName: String) {
    MINIMALIST("Minimalist Digital"),
    ANALOG_GAUGE("Analog Precision Gauge"),
    SPLIT_FLAP("Retro Split-Flap"),
    WORD_CLOCK("Word Clock Matrix"),
    PURE_MINIMALIST("Pure Screen-Fit Clock")
}

@Composable
fun ClockStyleHost(
    style: ClockStyle,
    currentTime: Long,
    batteryData: BatteryData,
    whiteTheme: Boolean = false,
    use24Hour: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (style) {
            ClockStyle.MINIMALIST -> MinimalistDigitalClock(currentTime, batteryData, whiteTheme, use24Hour)
            ClockStyle.ANALOG_GAUGE -> AnalogPrecisionGaugeClock(currentTime, batteryData, whiteTheme, use24Hour)
            ClockStyle.SPLIT_FLAP -> RetroSplitFlapClock(currentTime, batteryData, whiteTheme, use24Hour)
            ClockStyle.WORD_CLOCK -> WordMatrixClock(currentTime, batteryData, whiteTheme, use24Hour)
            ClockStyle.PURE_MINIMALIST -> PureScreenFitClock(currentTime, whiteTheme, use24Hour)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Style 0: Minimalist Digital
// ═══════════════════════════════════════════════════════════════════
@Composable
fun MinimalistDigitalClock(
    currentTime: Long,
    batteryData: BatteryData,
    whiteTheme: Boolean,
    use24Hour: Boolean = true
) {
    val timePattern = if (use24Hour) "HH:mm" else "hh:mm"
    val timeFormat = remember(use24Hour) { SimpleDateFormat(timePattern, Locale.getDefault()) }
    val secFormat = remember { SimpleDateFormat(":ss", Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val formattedTime = remember(currentTime, use24Hour) { timeFormat.format(Date(currentTime)) }
    val formattedSec = remember(currentTime) { secFormat.format(Date(currentTime)) }
    val formattedAmPm = remember(currentTime) { amPmFormat.format(Date(currentTime)) }
    val formattedDate = remember(currentTime) { dateFormat.format(Date(currentTime)) }

    val primaryTextColor = if (whiteTheme) Color(0xFF111827) else Color.White
    val secondaryTextColor = if (whiteTheme) Color(0xFF4B5563) else Color.White.copy(alpha = 0.65f)
    val mutedTextColor = if (whiteTheme) Color(0xFF9CA3AF) else Color.White.copy(alpha = 0.4f)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // Battery status pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (whiteTheme) Color(0xFFE9ECEF) else Color(0xFF16181D))
                .border(1.dp, if (whiteTheme) Color(0xFFDEE2E6) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
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
                text = if (batteryData.isBypassed) "AC BYPASS ACTIVE • ${batteryData.level}%"
                else if (batteryData.isCharging) "DOCK CHARGING • ${batteryData.level}% • ${batteryData.formattedPower}"
                else "BATTERY • ${batteryData.level}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = primaryTextColor.copy(alpha = 0.85f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formattedTime,
                fontSize = 92.sp,
                fontWeight = FontWeight.Black,
                color = primaryTextColor,
                letterSpacing = (-2).sp,
                lineHeight = 92.sp
            )
            Text(
                text = formattedSec,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = mutedTextColor,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            if (!use24Hour) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp, start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF232733))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = formattedAmPm.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                }
            }
        }

        Text(
            text = formattedDate,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryTextColor,
            letterSpacing = 0.2.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Style 1: Analog Precision Gauge Clock (Chronograph Dial)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun AnalogPrecisionGaugeClock(
    currentTime: Long,
    batteryData: BatteryData,
    whiteTheme: Boolean,
    use24Hour: Boolean = true
) {
    val cal = remember(currentTime) {
        Calendar.getInstance().apply { timeInMillis = currentTime }
    }
    val hours12 = cal.get(Calendar.HOUR)
    val hours24 = cal.get(Calendar.HOUR_OF_DAY)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)
    val isPm = cal.get(Calendar.AM_PM) == Calendar.PM

    val hourAngle = (hours12 + minutes / 60f) * 30f // 360 / 12 = 30 deg
    val minuteAngle = (minutes + seconds / 60f) * 6f // 360 / 60 = 6 deg
    val secondAngle = (seconds + millis / 1000f) * 6f

    val animatedSecondAngle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = secondAngle,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.75f,
            stiffness = 400f
        ),
        label = "SecondHandChronoSpring"
    )

    val dialBg = if (whiteTheme) Color(0xFFF1F3F5) else Color(0xFF141720)
    val tickColor = if (whiteTheme) Color(0xFFADB5BD) else Color.White.copy(alpha = 0.35f)
    val majorTickColor = if (whiteTheme) Color(0xFF1F2937) else Color.White
    val hourHandColor = if (whiteTheme) Color(0xFF111827) else Color.White
    val minuteHandColor = if (whiteTheme) Color(0xFF374151) else Color.White.copy(alpha = 0.85f)
    val secondHandColor = Color(0xFFEF4444) // Bright red chronograph needle

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(dialBg)
                .border(2.dp, if (whiteTheme) Color(0xFFDEE2E6) else Color.White.copy(alpha = 0.12f), CircleShape)
        ) {
            Canvas(modifier = Modifier.size(210.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Draw 60 perimeter ticks
                for (i in 0 until 60) {
                    val angleRad = (i * 6f) * (PI / 180f).toFloat()
                    val isMajor = i % 5 == 0
                    val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                    val strokeW = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
                    val col = if (isMajor) majorTickColor else tickColor

                    val startX = center.x + (radius - tickLen) * sin(angleRad)
                    val startY = center.y - (radius - tickLen) * cos(angleRad)
                    val endX = center.x + (radius - 2.dp.toPx()) * sin(angleRad)
                    val endY = center.y - (radius - 2.dp.toPx()) * cos(angleRad)

                    drawLine(
                        color = col,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // Hour Hand
                val hRad = hourAngle * (PI / 180f).toFloat()
                val hLen = radius * 0.52f
                drawLine(
                    color = hourHandColor,
                    start = center,
                    end = Offset(center.x + hLen * sin(hRad), center.y - hLen * cos(hRad)),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Minute Hand
                val mRad = minuteAngle * (PI / 180f).toFloat()
                val mLen = radius * 0.72f
                drawLine(
                    color = minuteHandColor,
                    start = center,
                    end = Offset(center.x + mLen * sin(mRad), center.y - mLen * cos(mRad)),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Chrono Red Second Hand with tail (60 FPS mechanical spring damping)
                val sRad = animatedSecondAngle * (PI / 180f).toFloat()
                val sLen = radius * 0.85f
                val sTail = radius * 0.18f
                drawLine(
                    color = secondHandColor,
                    start = Offset(center.x - sTail * sin(sRad), center.y + sTail * cos(sRad)),
                    end = Offset(center.x + sLen * sin(sRad), center.y - sLen * cos(sRad)),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center hub cap
                drawCircle(color = secondHandColor, radius = 4.dp.toPx(), center = center)
                drawCircle(color = if (whiteTheme) Color.White else Color.Black, radius = 2.dp.toPx(), center = center)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val displayH = if (use24Hour) hours24 else (if (hours12 == 0) 12 else hours12)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d:%02d", displayH, minutes, seconds),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (whiteTheme) Color(0xFF111827) else Color.White
                )
                if (!use24Hour) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF232733))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPm) "PM" else "AM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (whiteTheme) Color(0xFF111827) else Color.White
                        )
                    }
                }
            }
            Text(
                text = SimpleDateFormat("EEEE • MMM d, yyyy", Locale.getDefault()).format(Date(currentTime)).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (whiteTheme) Color(0xFF4B5563) else Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = if (batteryData.isBypassed) "CHRONO GAUGED • ${batteryData.level}% • BYPASS"
                else "CHRONO GAUGED • ${batteryData.level}% • ${batteryData.formattedPower}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = if (batteryData.isBypassed) Color(0xFFF59E0B) else Color(0xFF10B981)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Style 2: Retro Split-Flap Mechanical Cards
// ═══════════════════════════════════════════════════════════════════
@Composable
fun RetroSplitFlapClock(
    currentTime: Long,
    batteryData: BatteryData,
    whiteTheme: Boolean,
    use24Hour: Boolean = true
) {
    val cal = remember(currentTime) { Calendar.getInstance().apply { timeInMillis = currentTime } }
    val hourVal = if (use24Hour) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val h = String.format(Locale.getDefault(), "%02d", hourVal)
    val m = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MINUTE))
    val s = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.SECOND))
    val isPm = cal.get(Calendar.AM_PM) == Calendar.PM

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "DEPARTURE STANDBY • TERMINAL CLOCK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (whiteTheme) Color(0xFF6B7280) else Color(0xFF94A3B8)
            )
            Text(
                text = "BAT: ${batteryData.level}% • ${batteryData.formattedPower}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                color = if (whiteTheme) Color(0xFF6B7280) else Color(0xFF94A3B8)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SplitFlapCard(char1 = h[0], char2 = h[1], whiteTheme = whiteTheme)
            Text(
                text = ":",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = if (whiteTheme) Color(0xFF374151) else Color.White.copy(alpha = 0.5f)
            )
            SplitFlapCard(char1 = m[0], char2 = m[1], whiteTheme = whiteTheme)
            Text(
                text = ":",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = if (whiteTheme) Color(0xFF374151) else Color.White.copy(alpha = 0.5f)
            )
            SplitFlapCard(char1 = s[0], char2 = s[1], whiteTheme = whiteTheme)

            if (!use24Hour) {
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (whiteTheme) Color(0xFFFFFFFF) else Color(0xFF1E212B))
                        .border(1.dp, if (whiteTheme) Color(0xFFCBD5E1) else Color(0xFF2C3242), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPm) "PM" else "AM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (whiteTheme) Color(0xFF0F172A) else Color.White,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(currentTime)),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (whiteTheme) Color(0xFF4B5563) else Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun SplitFlapCard(
    char1: Char,
    char2: Char,
    whiteTheme: Boolean
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleFlapDigit(char = char1, whiteTheme = whiteTheme)
        SingleFlapDigit(char = char2, whiteTheme = whiteTheme)
    }
}

@Composable
private fun SingleFlapDigit(
    char: Char,
    whiteTheme: Boolean
) {
    val cardBg = if (whiteTheme) Color(0xFFFFFFFF) else Color(0xFF1E212B)
    val cardBorder = if (whiteTheme) Color(0xFFCBD5E1) else Color(0xFF2C3242)
    val numColor = if (whiteTheme) Color(0xFF0F172A) else Color.White
    val seamColor = if (whiteTheme) Color(0xFFE2E8F0) else Color(0xFF0D0F14)

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 76.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = char,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(160)) { -it / 2 } + fadeIn(animationSpec = tween(160)))
                    .togetherWith(slideOutVertically(animationSpec = tween(160)) { it / 2 } + fadeOut(animationSpec = tween(160)))
            },
            label = "SplitFlapFlip"
        ) { targetChar ->
            Text(
                text = targetChar.toString(),
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = numColor,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        // Mechanical horizontal seam dividing upper/lower flaps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(seamColor)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Style 3: Word Clock Matrix
// ═══════════════════════════════════════════════════════════════════
@Composable
fun WordMatrixClock(
    currentTime: Long,
    batteryData: BatteryData,
    whiteTheme: Boolean,
    use24Hour: Boolean = true
) {
    val cal = remember(currentTime) { Calendar.getInstance().apply { timeInMillis = currentTime } }
    val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val minute = cal.get(Calendar.MINUTE)
    val isPm = cal.get(Calendar.AM_PM) == Calendar.PM

    val isTo = minute > 30
    val displayHour = if (isTo) (hour12 % 12) + 1 else hour12
    val roundedMin = (minute / 5) * 5

    val activeColor = if (whiteTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val inactiveColor = if (whiteTheme) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.12f)

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        WordRow(listOf("IT" to true, "IS" to true, "HALF" to (roundedMin == 30), "TEN" to (roundedMin in listOf(10, 50))), activeColor, inactiveColor)
        WordRow(listOf("QUARTER" to (roundedMin in listOf(15, 45)), "TWENTY" to (roundedMin in listOf(20, 25, 35, 40))), activeColor, inactiveColor)
        WordRow(listOf("FIVE" to (roundedMin in listOf(5, 25, 35, 55)), "MINUTES" to (roundedMin % 15 != 0 && roundedMin != 0), "TO" to isTo), activeColor, inactiveColor)
        WordRow(listOf("PAST" to (!isTo && roundedMin != 0), "ONE" to (displayHour == 1), "TWO" to (displayHour == 2), "SIX" to (displayHour == 6)), activeColor, inactiveColor)
        WordRow(listOf("THREE" to (displayHour == 3), "FOUR" to (displayHour == 4), "FIVE" to (displayHour == 5)), activeColor, inactiveColor)
        WordRow(listOf("SEVEN" to (displayHour == 7), "EIGHT" to (displayHour == 8), "NINE" to (displayHour == 9)), activeColor, inactiveColor)
        WordRow(listOf("TEN" to (displayHour == 10), "ELEVEN" to (displayHour == 11), "TWELVE" to (displayHour == 12)), activeColor, inactiveColor)
        val amPmTag = if (isPm) "PM" else "AM"
        WordRow(listOf("O'CLOCK" to (roundedMin == 0), "•" to true, "${batteryData.level}%" to true, amPmTag to (!use24Hour)), activeColor, inactiveColor)
    }
}

@Composable
private fun WordRow(
    words: List<Pair<String, Boolean>>,
    activeColor: Color,
    inactiveColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        words.forEach { (word, isActive) ->
            Text(
                text = word,
                fontSize = 17.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isActive) activeColor else inactiveColor,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// Style 4: Pure Minimalist Screen-Fit Clock (Auto-Scaling, No Charging UI)
// ═══════════════════════════════════════════════════════════════════
@Composable
fun PureScreenFitClock(
    currentTime: Long,
    whiteTheme: Boolean,
    use24Hour: Boolean = true
) {
    val timePattern = if (use24Hour) "HH:mm" else "hh:mm"
    val timeFormat = remember(use24Hour) { SimpleDateFormat(timePattern, Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val formattedTime = remember(currentTime, use24Hour) { timeFormat.format(Date(currentTime)) }
    val formattedAmPm = remember(currentTime) { amPmFormat.format(Date(currentTime)) }
    val formattedDate = remember(currentTime) { dateFormat.format(Date(currentTime)) }

    val primaryTextColor = if (whiteTheme) Color(0xFF0F172A) else Color.White
    val secondaryTextColor = if (whiteTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.55f)

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val availWidth = maxWidth
        val availHeight = maxHeight
        val isPortrait = availHeight > availWidth

        // Dynamically scale font based on available box boundaries
        val baseFontSize = if (isPortrait) {
            (availWidth.value * 0.28f).sp
        } else {
            (availHeight.value * 0.42f).sp
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedTime,
                    fontSize = baseFontSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    color = primaryTextColor,
                    lineHeight = baseFontSize
                )
                if (!use24Hour) {
                    Text(
                        text = " $formattedAmPm",
                        fontSize = (baseFontSize.value * 0.28f).sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryTextColor,
                        modifier = Modifier.padding(bottom = (baseFontSize.value * 0.12f).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formattedDate.uppercase(),
                fontSize = (baseFontSize.value * 0.15f).coerceIn(11f, 18f).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                color = secondaryTextColor
            )
        }
    }
}

