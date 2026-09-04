package com.mastercompanion.ui.home

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mastercompanion.domain.model.SpotifyTrack
import com.mastercompanion.domain.model.TrackLyrics
import com.mastercompanion.ui.theme.MasterCompanionTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Universal Music Player Screen supporting:
 * 1. Smooth 600ms Crossfade background transitions on album art change.
 * 2. Adaptive orientation: Landscape (2-column standby) & Vertical Portrait.
 * 3. Multiple layouts: Standard (Spotify Car View), Synced Lyrics, and Minimal Clock Desk Standby.
 */
@Composable
fun MusicPage(
    track: SpotifyTrack,
    lyrics: TrackLyrics = TrackLyrics.MockBlackAndYellow,
    initialLayout: MusicPlayerLayout = MusicPlayerLayout.STANDARD,
    onPlayPauseToggle: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onOpenDevices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var layoutMode by remember { mutableStateOf(initialLayout) }
    var isLiked by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(true) }
    var repeatMode by remember { mutableStateOf(0) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
        // ═══ 1. Animated Crossfade Background on Album Art Change ═══
        Crossfade(
            targetState = track.albumArtUrl,
            animationSpec = tween(durationMillis = 600),
            label = "BackgroundCrossfade"
        ) { artUrl ->
            if (!artUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF121212))
                )
            }
        }

        // Dark radial vignette overlay ensuring high contrast and legibility
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

        // ═══ 2. Content Layouts (Standard, Lyrics, Minimal) ═══
        when (layoutMode) {
            MusicPlayerLayout.STANDARD -> {
                if (isLandscape) {
                    LandscapeStandardView(
                        track = track,
                        progressFraction = progressFraction,
                        isLiked = isLiked,
                        isShuffle = isShuffle,
                        repeatMode = repeatMode,
                        onPlayPauseToggle = onPlayPauseToggle,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onToggleShuffle = {
                            isShuffle = !isShuffle
                            onToggleShuffle()
                        },
                        onToggleLike = {
                            isLiked = !isLiked
                            onToggleLike()
                        },
                        onToggleRepeat = {
                            repeatMode = (repeatMode + 1) % 3
                            onToggleRepeat()
                        },
                        onOpenDevices = onOpenDevices,
                        onToggleLyrics = { layoutMode = MusicPlayerLayout.LYRICS },
                        onToggleMinimal = { layoutMode = MusicPlayerLayout.MINIMAL }
                    )
                } else {
                    PortraitStandardView(
                        track = track,
                        progressFraction = progressFraction,
                        isLiked = isLiked,
                        isShuffle = isShuffle,
                        repeatMode = repeatMode,
                        onPlayPauseToggle = onPlayPauseToggle,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onToggleShuffle = {
                            isShuffle = !isShuffle
                            onToggleShuffle()
                        },
                        onToggleLike = {
                            isLiked = !isLiked
                            onToggleLike()
                        },
                        onToggleRepeat = {
                            repeatMode = (repeatMode + 1) % 3
                            onToggleRepeat()
                        },
                        onOpenDevices = onOpenDevices,
                        onToggleLyrics = { layoutMode = MusicPlayerLayout.LYRICS },
                        onToggleMinimal = { layoutMode = MusicPlayerLayout.MINIMAL }
                    )
                }
            }

            MusicPlayerLayout.LYRICS -> {
                LyricsModeView(
                    track = track,
                    lyrics = lyrics,
                    progressFraction = progressFraction,
                    isLandscape = isLandscape,
                    onSeekToTimestamp = onSeek,
                    onCloseLyrics = { layoutMode = MusicPlayerLayout.STANDARD }
                )
            }

            MusicPlayerLayout.MINIMAL -> {
                MinimalStandbyView(
                    track = track,
                    progressFraction = progressFraction,
                    onPlayPauseToggle = onPlayPauseToggle,
                    onCloseMinimal = { layoutMode = MusicPlayerLayout.STANDARD }
                )
            }
        }

        // ═══ 3. Bottom Slideable Page Indicators ═══
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 1. LANDSCAPE STANDARD VIEW (Original Spotify Car View)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun LandscapeStandardView(
    track: SpotifyTrack,
    progressFraction: Float,
    isLiked: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenDevices: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleMinimal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Album Artwork with Floating Lyrics/Queue Button
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

            // Queue / Lyrics Toggle Button
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
                    ) { onToggleLyrics() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatQuote,
                    contentDescription = "Lyrics",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(44.dp))

        // Right: Track Metadata, Playback Controls, Progress & Actions
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Title & Artist with Minimal Desk Clock button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
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

                IconButton(onClick = onToggleMinimal) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Standby Clock Mode",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Progress Bar & Primary Playback Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

            // Secondary Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFF1DB954) else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                        contentDescription = "Like Track",
                        tint = if (isLiked) Color(0xFF1DB954) else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onOpenDevices) {
                    Icon(
                        imageVector = Icons.Filled.Cast,
                        contentDescription = "Devices",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) Color(0xFF1DB954) else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Footer
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
}

// ═══════════════════════════════════════════════════════════════════
// 2. VERTICAL (PORTRAIT) STANDARD VIEW
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun PortraitStandardView(
    track: SpotifyTrack,
    progressFraction: Float,
    isLiked: Boolean,
    isShuffle: Boolean,
    repeatMode: Int,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenDevices: () -> Unit,
    onToggleLyrics: () -> Unit,
    onToggleMinimal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
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
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "PLAYING FROM SPOTIFY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Row {
                IconButton(onClick = onToggleLyrics) {
                    Icon(
                        imageVector = Icons.Filled.FormatQuote,
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onToggleMinimal) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Minimal Mode",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Center Album Cover
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
        ) {
            AsyncImage(
                model = track.albumArtUrl,
                contentDescription = track.album,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E1E))
            )
        }

        // Track Details & Like Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "Black and Yellow" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist.ifBlank { "Wiz Khalifa" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFF1DB954) else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Seek Bar & Timestamps
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(track.progressMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                val remainingMs = (track.durationMs - track.progressMs).coerceAtLeast(0L)
                Text(
                    text = "-${formatTime(remainingMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Primary Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) Color(0xFF1DB954) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onSkipPrevious) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(
                onClick = onPlayPauseToggle,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (track.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(onClick = onToggleRepeat) {
                Icon(
                    imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode > 0) Color(0xFF1DB954) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom devices / volume bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDevices) {
                Icon(
                    imageVector = Icons.Filled.Cast,
                    contentDescription = "Devices",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Volume",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 3. SYNCHRONIZED LYRICS VIEW (Replicating Spotify Standby Lyrics)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun LyricsModeView(
    track: SpotifyTrack,
    lyrics: TrackLyrics,
    progressFraction: Float,
    isLandscape: Boolean,
    onSeekToTimestamp: (Long) -> Unit,
    onCloseLyrics: () -> Unit,
    onPlayPauseToggle: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onToggleRepeat: () -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(true) }
    var isShuffle by remember { mutableStateOf(true) }
    var repeatMode by remember { mutableStateOf(0) }

    val accentRed = Color(0xFFE50914) // Vibrant Spotify/Sickick red accent from reference

    if (isLandscape) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp)
        ) {
            // Top-Left Playlist Context Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCloseLyrics() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        text = track.playlistContext.ifBlank { "Liked Songs" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Two-Column Row: Left (Art + Info + Compact Controls), Right (Live Lyrics)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Artwork, Metadata (Title, Artist, Album/Year), Compact Controls & Progress
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.44f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Album Cover with rounded corners
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title with Music Note icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = track.title.ifBlank { "Intro" },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Artist with Profile/Note icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "👤",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = track.artist.ifBlank { "Sickick" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Album & Release Year
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💿",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        val albumYear = if (track.releaseYear.isNotBlank()) {
                            "${track.album} • ${track.releaseYear}"
                        } else {
                            "Talksick 3 • 2016"
                        }
                        Text(
                            text = albumYear,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color.White.copy(alpha = 0.62f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Compact Red Action Row (Heart, Shuffle, Prev, Play, Next, Loop, Queue)
                    Row(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Heart / Like
                        IconButton(
                            onClick = {
                                isLiked = !isLiked
                                onToggleLike()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(text = if (isLiked) "❤️" else "🤍", fontSize = 18.sp)
                        }

                        // Shuffle
                        IconButton(
                            onClick = {
                                isShuffle = !isShuffle
                                onToggleShuffle()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) accentRed else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Skip Back
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = accentRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Play/Pause
                        IconButton(
                            onClick = onPlayPauseToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (track.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = accentRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Skip Forward
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = accentRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Repeat
                        IconButton(
                            onClick = {
                                repeatMode = (repeatMode + 1) % 3
                                onToggleRepeat()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode > 0) accentRed else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Exit Lyrics / Back to Standard View
                        IconButton(
                            onClick = onCloseLyrics,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Exit Lyrics",
                                tint = accentRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar & Duration Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(track.progressMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTime(track.durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Right Column: Large Synced Karaoke Lyrics
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.56f)
                ) {
                    LyricsView(
                        lyrics = lyrics,
                        progressMs = track.progressMs,
                        onSeekToTimestamp = onSeekToTimestamp
                    )
                }
            }
        }
    } else {
        // Vertical Portrait Lyrics View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        text = track.playlistContext.ifBlank { "Liked Songs" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                IconButton(onClick = onCloseLyrics) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Portrait Lyrics View
            LyricsView(
                lyrics = lyrics,
                progressMs = track.progressMs,
                onSeekToTimestamp = onSeekToTimestamp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 4. MINIMAL STANDBY CLOCK VIEW (Desk Companion)
// ═══════════════════════════════════════════════════════════════════
@Composable
private fun MinimalStandbyView(
    track: SpotifyTrack,
    progressFraction: Float,
    onPlayPauseToggle: () -> Unit,
    onCloseMinimal: () -> Unit
) {
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onCloseMinimal() }
            .padding(48.dp)
    ) {
        // Hero Clock Centered
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 110.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${track.title} • ${track.artist}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .width(280.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }

        Text(
            text = "TAP ANYWHERE TO EXPAND",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Formats milliseconds into M:SS timestamp string */
private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

// ═══════════════════════════════════════════════════════════════════
// COMPOSE PREVIEWS
// ═══════════════════════════════════════════════════════════════════

@Preview(
    name = "1. Landscape Standard Preview",
    device = "spec:width=892dp,height=412dp,dpi=560,isRound=false",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun MusicPageLandscapePreview() {
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

@Preview(
    name = "2. Vertical Portrait Preview",
    device = "spec:width=412dp,height=892dp,dpi=560,isRound=false",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun MusicPagePortraitPreview() {
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

@Preview(
    name = "3. Synced Lyrics Mode Preview",
    device = "spec:width=892dp,height=412dp,dpi=560,isRound=false",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun MusicPageLyricsPreview() {
    MasterCompanionTheme {
        MusicPage(
            track = SpotifyTrack(
                title = "Intro",
                artist = "Sickick",
                album = "Talksick 3",
                releaseYear = "2016",
                playlistContext = "Liked Songs",
                albumArtUrl = "https://i.scdn.co/image/ab67616d0000b2734190c1cb279b940e4ba77579",
                durationMs = 294000L,
                progressMs = 51000L,
                isPlaying = true
            ),
            lyrics = TrackLyrics.MockSickickIntro,
            initialLayout = MusicPlayerLayout.LYRICS
        )
    }
}
