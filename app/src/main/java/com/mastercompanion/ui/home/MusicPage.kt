package com.mastercompanion.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mastercompanion.domain.model.SpotifyTrack
import com.mastercompanion.ui.theme.MasterCompanionTheme

/**
 * Landscape Full-Screen Music Player Page.
 * Replicates the clean Spotify Car/Standby View with dimmed ambient backdrop,
 * bold typography, native playback controls, seekbar, and secondary action row.
 */
@Composable
fun MusicPage(
    track: SpotifyTrack,
    onPlayPauseToggle: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenDevices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLiked by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(true) }
    var repeatMode by remember { mutableStateOf(0) } // 0 = off, 1 = all, 2 = one

    val progressFraction = if (track.durationMs > 0) {
        (track.progressMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0.25f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ═══ 1. Ambient Dimmed & Blurred Album Art Background ═══
        if (!track.albumArtUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.albumArtUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
            )
        }

        // Heavy dark overlay with radial vignette to keep controls legible and punchy
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // ═══ 2. Main Landscape Two-Column Content ═══
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Left: Album Artwork with Floating Queue Button ──
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(1f)
            ) {
                AsyncImage(
                    model = track.albumArtUrl,
                    contentDescription = track.album,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                )

                // Floating Queue / Playlist Pill Button (bottom-right of artwork)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onOpenQueue() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Queue",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(44.dp))

            // ── Right: Track Metadata, Playback Controls, Progress & Actions ──
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Top: Title & Artist
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = track.title.ifBlank { "Black and Yellow" },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist.ifBlank { "Wiz Khalifa" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center: Seekbar & Primary Playback Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Timestamps: Elapsed on left, Remaining on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(track.progressMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        val remainingMs = (track.durationMs - track.progressMs).coerceAtLeast(0L)
                        Text(
                            text = "-${formatTime(remainingMs)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Playback Controls: Skip Back, Large Play/Pause, Skip Forward
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(36.dp))

                        IconButton(
                            onClick = onPlayPauseToggle,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Icon(
                                imageVector = if (track.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (track.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(36.dp))

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Secondary Action Row: Smart Shuffle, Like (+), Connect, Loop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Smart Shuffle
                    IconButton(onClick = {
                        isShuffle = !isShuffle
                        onToggleShuffle()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffle) Color(0xFF1DB954) else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Add / Like (+) Button
                    IconButton(onClick = {
                        isLiked = !isLiked
                        onToggleLike()
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                            contentDescription = "Like Track",
                            tint = if (isLiked) Color(0xFF1DB954) else Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Spotify Connect / Devices
                    IconButton(onClick = onOpenDevices) {
                        Icon(
                            imageVector = Icons.Filled.Cast,
                            contentDescription = "Devices",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Repeat / Loop
                    IconButton(onClick = {
                        repeatMode = (repeatMode + 1) % 3
                        onToggleRepeat()
                    }) {
                        Icon(
                            imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode > 0) Color(0xFF1DB954) else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom Footer: Source device ("^ SPOTIFY") and Volume icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenDevices() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SPOTIFY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ═══ 3. Subtle Bottom Slideable Multi-Page Indicator Dots ═══
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home indicator dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
            // Music page (current / active wide pill)
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
            )
            // Audio Stream dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
            // System dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

/** Formats milliseconds into M:SS timestamp string */
private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Preview(
    name = "Spotify Landscape Standby View",
    device = "spec:width=892dp,height=412dp,dpi=560,isRound=false",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun MusicPagePreview() {
    MasterCompanionTheme {
        MusicPage(
            track = SpotifyTrack(
                title = "Black and Yellow",
                artist = "Wiz Khalifa",
                album = "Rolling Papers",
                albumArtUrl = "https://i.scdn.co/image/ab67616d0000b2738b556f0827299a4e320f8c05",
                durationMs = 218000L,
                progressMs = 35000L,
                isPlaying = true
            )
        )
    }
}
