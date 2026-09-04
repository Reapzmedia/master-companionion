package com.mastercompanion.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * OLED Anti-Burn-In Engine:
 * Periodically translates UI elements by ±1 to ±2 pixels every 60 seconds
 * in an imperceptible 4-quadrant orbital trajectory.
 * Prevents organic LED subpixel degradation on docked standby displays.
 */
private val SHIFT_SEQUENCE = listOf(
    IntOffset(0, 0),
    IntOffset(2, 1),
    IntOffset(-1, 2),
    IntOffset(1, -2),
    IntOffset(-2, -1),
    IntOffset(2, -1),
    IntOffset(-1, 1),
    IntOffset(0, 0)
)

fun Modifier.pixelShift(
    enabled: Boolean = true,
    intervalMs: Long = 60_000L
): Modifier = composed {
    if (!enabled) return@composed Modifier

    var stepIndex by remember { mutableIntStateOf(0) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(enabled, intervalMs) {
        while (enabled) {
            delay(intervalMs)
            stepIndex = (stepIndex + 1) % SHIFT_SEQUENCE.size
            val target = SHIFT_SEQUENCE[stepIndex]
            // Smooth imperceptible 1000ms transition between pixel offsets
            offsetX.animateTo(target.x.toFloat(), tween(1000))
            offsetY.animateTo(target.y.toFloat(), tween(1000))
        }
    }

    this.offset {
        IntOffset(offsetX.value.toInt(), offsetY.value.toInt())
    }
}

@Composable
fun PixelShiftContainer(
    enabled: Boolean = true,
    intervalMs: Long = 60_000L,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.pixelShift(enabled = enabled, intervalMs = intervalMs)
    ) {
        content()
    }
}
