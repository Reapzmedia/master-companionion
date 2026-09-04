package com.mastercompanion.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercompanion.domain.model.TrackLyrics

/**
 * High-fidelity synchronized lyrics view matching the reference design:
 * - Active Line: Bold, crisp pure white (30-34sp)
 * - Upcoming Line 1: Soft white (alpha ~0.48f, slight blur)
 * - Upcoming Lines 2+: Heavily dimmed and blurred (alpha ~0.22f, heavy blur)
 * - Smooth auto-scrolling with tap-to-seek playback.
 */
@Composable
fun LyricsView(
    lyrics: TrackLyrics,
    progressMs: Long,
    onSeekToTimestamp: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val activeIndex = lyrics.getCurrentLineIndex(progressMs)

    // Smoothly scroll to keep active lyric line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lyrics.lines.size) {
            val targetScroll = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    if (lyrics.lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No lyrics available for this track",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 64.dp, horizontal = 16.dp)
        ) {
            itemsIndexed(lyrics.lines) { index, line ->
                val distance = index - activeIndex
                val isActive = distance == 0

                val targetColor = when {
                    isActive -> Color.White
                    distance == 1 -> Color.White.copy(alpha = 0.45f)
                    distance > 1 -> Color.White.copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.32f) // Past lines
                }

                val targetBlur = when {
                    isActive -> 0.dp
                    distance == 1 -> 1.5.dp
                    distance > 1 -> 3.dp
                    else -> 1.dp
                }

                val textColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 350),
                    label = "textColor"
                )

                Text(
                    text = line.words,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = if (isActive) 32.sp else 24.sp,
                        lineHeight = if (isActive) 42.sp else 34.sp
                    ),
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .blur(targetBlur)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onSeekToTimestamp(line.startTimeMs)
                        }
                )
            }
        }
    }
}
