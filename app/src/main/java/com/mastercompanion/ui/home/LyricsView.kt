package com.mastercompanion.ui.home

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil.imageLoader
import coil.request.ImageRequest
import com.mastercompanion.domain.model.TrackLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class LyricPalette(
    val activeTextColor: Color,
    val upcomingColor1: Color,
    val upcomingColorRest: Color,
    val pastColor: Color,
    val accentColor: Color
)

/**
 * Safe helper to load a software Bitmap from an image URL for pixel sampling.
 */
suspend fun loadBitmapFromUrl(context: android.content.Context, url: String?): Bitmap? {
    if (url.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Force software bitmap to read pixels
                .size(96, 96)
                .build()
            val result = loader.execute(request)
            val drawable = result.drawable ?: return@withContext null
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val w = drawable.intrinsicWidth.coerceIn(1, 128)
                val h = drawable.intrinsicHeight.coerceIn(1, 128)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                bmp
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Samples the actual RGB pixels of the album art bitmap to discover the dominant,
 * vibrant color tone of the song.
 */
fun extractVibrantRgbColor(bitmap: Bitmap): Color {
    val scaled = if (bitmap.width <= 32 && bitmap.height <= 32) bitmap else Bitmap.createScaledBitmap(bitmap, 32, 32, false)
    val width = scaled.width
    val height = scaled.height

    var bestColor = Color(0xFF1DB954) // Default Spotify green
    var maxScore = -1f
    val hsv = FloatArray(3)

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = scaled.getPixel(x, y)
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < 100) continue
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val value = hsv[2]

            // Discard near-black and washed out whites
            if (value >= 0.15f && (sat >= 0.20f || value in 0.40f..0.85f)) {
                // Score: prioritize rich saturation and radiant luminance
                val score = (sat * 3.0f) + (value * 1.5f) - (abs(value - 0.70f) * 0.8f)
                if (score > maxScore) {
                    maxScore = score
                    bestColor = Color(r, g, b)
                }
            }
        }
    }
    return bestColor
}

fun calculateDynamicLyricPalette(
    sampledColor: Color?,
    albumArtUrl: String?,
    trackTitle: String,
    trackArtist: String,
    whiteTheme: Boolean
): LyricPalette {
    val baseColor = sampledColor ?: run {
        val hash = ((albumArtUrl?.hashCode() ?: 0) * 31 + trackTitle.hashCode() * 17 + trackArtist.hashCode())
        val hue = kotlin.math.abs(hash % 360).toFloat()
        Color.hsl(hue = hue, saturation = 0.85f, lightness = 0.65f)
    }

    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (baseColor.red * 255).toInt().coerceIn(0, 255),
        (baseColor.green * 255).toInt().coerceIn(0, 255),
        (baseColor.blue * 255).toInt().coerceIn(0, 255),
        hsv
    )
    val hue = hsv[0]
    val sat = hsv[1]
    val value = hsv[2]

    if (whiteTheme) {
        // High contrast dark-rich accent for white theme
        val activeSat = sat.coerceAtLeast(0.65f)
        val activeVal = value.coerceIn(0.20f, 0.40f)
        val activeTextColor = Color.hsv(hue = hue, saturation = activeSat, value = activeVal)
        return LyricPalette(
            activeTextColor = activeTextColor,
            upcomingColor1 = Color(0xFF1E293B).copy(alpha = 0.70f),
            upcomingColorRest = Color(0xFF1E293B).copy(alpha = 0.28f),
            pastColor = Color(0xFF1E293B).copy(alpha = 0.42f),
            accentColor = activeTextColor
        )
    }

    // Vibrant saturated tone tuned for dark OLED standby background
    val activeSat = sat.coerceAtLeast(0.50f)
    val activeVal = value.coerceAtLeast(0.78f)
    val activeTextColor = Color.hsv(hue = hue, saturation = activeSat, value = activeVal)
    return LyricPalette(
        activeTextColor = activeTextColor,
        upcomingColor1 = Color.White.copy(alpha = 0.78f),
        upcomingColorRest = Color.White.copy(alpha = 0.30f),
        pastColor = Color.White.copy(alpha = 0.42f),
        accentColor = activeTextColor
    )
}

/**
 * High-fidelity synchronized lyrics view matching Spotify Standby / Car View:
 * - Dynamic Lyric Colors derived from actual RGB values of album art
 * - Active Line: Guaranteed to always stay locked on vertical center
 * - Highlighted lyric line has dynamic theme color
 */
@Composable
fun LyricsView(
    lyrics: TrackLyrics?,
    progressMs: Long,
    whiteTheme: Boolean = false,
    albumArtUrl: String? = null,
    trackTitle: String = "",
    trackArtist: String = "",
    dynamicColor: Color? = null,
    onSeekToTimestamp: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeIndex = lyrics?.getCurrentLineIndex(progressMs) ?: -1

    var sampledColor by remember(albumArtUrl, dynamicColor) { mutableStateOf(dynamicColor) }

    // Load actual RGB pixels from the album art thumbnail if dynamicColor not yet provided
    LaunchedEffect(albumArtUrl, dynamicColor) {
        if (dynamicColor != null) {
            sampledColor = dynamicColor
            return@LaunchedEffect
        }
        if (!albumArtUrl.isNullOrBlank()) {
            val bmp = loadBitmapFromUrl(context, albumArtUrl)
            if (bmp != null) {
                val extracted = extractVibrantRgbColor(bmp)
                sampledColor = extracted
            }
        } else {
            sampledColor = null
        }
    }

    val palette = remember(sampledColor, albumArtUrl, trackTitle, trackArtist, whiteTheme) {
        calculateDynamicLyricPalette(sampledColor, albumArtUrl, trackTitle, trackArtist, whiteTheme)
    }

    if (lyrics == null || lyrics.lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No lyrics available for this track",
                style = MaterialTheme.typography.bodyLarge,
                color = if (whiteTheme) Color(0xFF0F172A).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
            )
        }
    } else {
        val fadeBrush = remember {
            Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.08f to Color.Black,
                0.92f to Color.Black,
                1.0f to Color.Transparent
            )
        }

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            val density = LocalDensity.current

            val halfViewportHeight = maxHeight / 2
            val halfViewportHeightPx = with(density) { halfViewportHeight.toPx() }

            // Store measured vertical center for every lyric line in absolute column space.
            // Plain Map remembered without Compose State so measurements do not cause recomposition loops during scroll!
            val lineCenters = remember(lyrics.trackId, maxWidth, maxHeight) { mutableMapOf<Int, Float>() }
            var hasInitiallyScrolled by remember(lyrics.trackId) { mutableStateOf(false) }

            val effectiveActiveIndex = if (activeIndex in lyrics.lines.indices) activeIndex else 0

            // Calculates the exact scroll position that places lineCenter at the vertical center of the viewport
            fun getTargetScroll(index: Int): Int {
                val centerPx = lineCenters[index] ?: run {
                    val approxLineHeightPx = with(density) { 54.dp.toPx() }
                    halfViewportHeightPx + (index * approxLineHeightPx) + (approxLineHeightPx / 2f)
                }
                val scroll = (centerPx - halfViewportHeightPx).roundToInt()
                return scroll.coerceIn(0, scrollState.maxValue)
            }

            // Precise Center-Locking: Glides active singing line directly to vertical center with Apple-style fluid glide curve
            LaunchedEffect(effectiveActiveIndex, lyrics.trackId, maxHeight) {
                val targetScroll = getTargetScroll(effectiveActiveIndex)
                if (!hasInitiallyScrolled) {
                    scrollState.scrollTo(targetScroll)
                    hasInitiallyScrolled = true
                } else {
                    scrollState.animateScrollTo(
                        targetScroll,
                        animationSpec = tween(
                            durationMillis = 520,
                            easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
                        )
                    )
                }
            }

            // Stable callback to measure line positions without reallocating lambdas in the line loop
            val onPositionMeasured: (Int, Float) -> Unit = remember(lyrics.trackId) {
                { lineIdx, centerPx ->
                    val prev = lineCenters[lineIdx]
                    if (prev == null || kotlin.math.abs(prev - centerPx) > 1.5f) {
                        lineCenters[lineIdx] = centerPx
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                    }
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Top spacer so first line sits at exact vertical center
                Spacer(modifier = Modifier.height(halfViewportHeight))

                lyrics.lines.forEachIndexed { index, line ->
                    val distance = index - activeIndex
                    val isActive = distance == 0

                    LyricLineRow(
                        line = line,
                        index = index,
                        isActive = isActive,
                        distance = distance,
                        palette = palette,
                        onSeekToTimestamp = onSeekToTimestamp,
                        onPositionMeasured = onPositionMeasured
                    )
                }

                // Bottom spacer so last line can also sit at exact vertical center
                Spacer(modifier = Modifier.height(halfViewportHeight))
            }
        }
    }
}

private val LyricMorphEasing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

/**
 * Isolated Composable for individual lyric lines to maximize Compose recomposition skipping
 * and maintain locked 60/120 FPS performance during playback.
 */
@Composable
private fun LyricLineRow(
    line: com.mastercompanion.domain.model.LyricLine,
    index: Int,
    isActive: Boolean,
    distance: Int,
    palette: LyricPalette,
    onSeekToTimestamp: (Long) -> Unit,
    onPositionMeasured: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetColor = when {
        isActive -> palette.activeTextColor
        distance == 1 -> palette.upcomingColor1
        distance > 1 -> palette.upcomingColorRest
        else -> palette.pastColor
    }

    val targetScale = when {
        isActive -> 1.08f
        distance == 1 -> 0.98f
        distance == -1 -> 0.95f
        else -> 0.92f
    }

    val targetAlpha = when {
        isActive -> 1.0f
        distance == 1 -> 0.74f
        distance == -1 -> 0.45f
        else -> 0.28f
    }

    val textColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 460, easing = LyricMorphEasing),
        label = "lineTextColor"
    )

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 460, easing = LyricMorphEasing),
        label = "lineScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 460, easing = LyricMorphEasing),
        label = "lineAlpha"
    )

    // Subtle optical depth-of-field blur on distant lines (API 31+ only, sharp on upcoming line)
    val shouldBlur = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && distance in 2..4

    var itemModifier = modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
            transformOrigin = TransformOrigin(0f, 0.5f)
        }
        .onGloballyPositioned { coordinates ->
            val lineTop = coordinates.positionInParent().y
            val lineCenter = lineTop + (coordinates.size.height / 2f)
            onPositionMeasured(index, lineCenter)
        }
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) {
            onSeekToTimestamp(line.startTimeMs)
        }

    if (shouldBlur) {
        itemModifier = itemModifier.blur(1.5.dp)
    }

    Text(
        text = line.words,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 38.sp
        ),
        color = textColor,
        modifier = itemModifier
    )
}
