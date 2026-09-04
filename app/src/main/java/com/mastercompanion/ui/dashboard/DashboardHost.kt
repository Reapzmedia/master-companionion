package com.mastercompanion.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mastercompanion.domain.model.SpotifyTrack
import com.mastercompanion.ui.home.MusicPage

/**
 * Multi-page HorizontalPager container enabling smooth swiping between:
 * - Page 0: Standby Clock & Battery Telemetry
 * - Page 1: Dedicated Fullscreen Spotify Player (replicated reference UI)
 * - Page 2: PC Low-Latency Audio Streamer
 * - Page 3: System Status & Hardware Logs
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardHost(
    currentTrack: SpotifyTrack = SpotifyTrack(
        title = "Black and Yellow",
        artist = "Wiz Khalifa",
        album = "Rolling Papers",
        albumArtUrl = "https://i.scdn.co/image/ab67616d0000b2738b556f0827299a4e320f8c05",
        durationMs = 218000L,
        progressMs = 35000L,
        isPlaying = true
    ),
    onPlayPauseToggle: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    // Clock & Battery Dashboard
                    Box(modifier = Modifier.fillMaxSize())
                }
                1 -> {
                    // Dedicated Clean Spotify Music Player
                    MusicPage(
                        track = currentTrack,
                        onPlayPauseToggle = onPlayPauseToggle,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious
                    )
                }
                2 -> {
                    // Low-Latency PC Audio Stream Page
                    Box(modifier = Modifier.fillMaxSize())
                }
                3 -> {
                    // System & Bridge Command Logs
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
