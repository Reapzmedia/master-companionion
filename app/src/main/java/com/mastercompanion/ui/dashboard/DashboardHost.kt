package com.mastercompanion.ui.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mastercompanion.ui.audio.AudioPage
import com.mastercompanion.ui.home.HomePage
import com.mastercompanion.ui.home.MusicPage
import com.mastercompanion.ui.settings.SettingsPage
import com.mastercompanion.ui.system.SystemPage
import kotlinx.coroutines.launch

/**
 * Multi-page HorizontalPager container enabling smooth swiping between:
 * - Page 0: Standby Clock & Battery Telemetry
 * - Page 1: Dedicated Fullscreen Spotify Player (replicated reference UI + Karaoke Lyrics)
 * - Page 2: PC Low-Latency Audio Passthrough Receiver
 * - Page 3: System Status, Root Diagnostics & HTTP Command Logs
 * - Page 4: Configuration & Preferences (Thresholds, Tokens, Spotify/PC IP)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardHost(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    // Observe state from ViewModel
    val batteryData by viewModel.batteryData.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val audioStreamState by viewModel.audioStreamState.collectAsStateWithLifecycle()
    val commandLogs by viewModel.commandLogs.collectAsStateWithLifecycle()
    val isRootAvailable by viewModel.isRootAvailable.collectAsStateWithLifecycle()

    val authToken by viewModel.authToken.collectAsStateWithLifecycle()
    val chargeStopThreshold by viewModel.chargeStopThreshold.collectAsStateWithLifecycle()
    val chargeResumeThreshold by viewModel.chargeResumeThreshold.collectAsStateWithLifecycle()
    val pcIp by viewModel.pcIp.collectAsStateWithLifecycle()
    val pcMac by viewModel.pcMac.collectAsStateWithLifecycle()
    val spotifyClientId by viewModel.spotifyClientId.collectAsStateWithLifecycle()
    val isSpotifyConnected by viewModel.isSpotifyConnected.collectAsStateWithLifecycle()

    // Listen for remote navigate commands from AHK / HTTP bridge
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { targetPage ->
            val clampedPage = targetPage.coerceIn(0, 4)
            pagerState.animateScrollToPage(clampedPage)
        }
    }

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
                    // Page 0: Hero Standby Clock & Battery Telemetry
                    HomePage(
                        batteryData = batteryData,
                        onToggleChargeLimit = { viewModel.toggleChargeLimit() },
                        onSendWol = { viewModel.executeBridgeAction("wol") }
                    )
                }
                1 -> {
                    // Page 1: Replicated Native Spotify Standby View & Lyrics
                    MusicPage(
                        track = currentTrack,
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onSkipPrevious = { viewModel.skipPrevious() }
                    )
                }
                2 -> {
                    // Page 2: PC Low-Latency UDP Audio Receiver
                    AudioPage(
                        streamState = audioStreamState,
                        onVolumeChange = { viewModel.setAudioVolume(it) }
                    )
                }
                3 -> {
                    // Page 3: System Status, Root Diagnostics & HTTP Command Logs
                    SystemPage(
                        isRooted = isRootAvailable,
                        commandLogs = commandLogs,
                        onTriggerTestCommand = { action -> viewModel.executeBridgeAction(action) }
                    )
                }
                4 -> {
                    // Page 4: Settings & Configuration
                    SettingsPage(
                        authToken = authToken,
                        chargeStopThreshold = chargeStopThreshold,
                        chargeResumeThreshold = chargeResumeThreshold,
                        pcIp = pcIp,
                        pcMac = pcMac,
                        spotifyClientId = spotifyClientId,
                        isSpotifyConnected = isSpotifyConnected,
                        onSaveThresholds = { stop, resume -> viewModel.updateChargeThresholds(stop, resume) },
                        onRegenerateToken = { viewModel.regenerateAuthToken() },
                        onSavePcNetwork = { ip, mac -> viewModel.updatePcNetwork(ip, mac) },
                        onSaveSpotifyClientId = { viewModel.updateSpotifyClientId(it) },
                        onConnectSpotify = { viewModel.connectSpotify() },
                        onDisconnectSpotify = { viewModel.disconnectSpotify() }
                    )
                }
            }
        }

        // ═══ Bottom Navigation Dots Indicator ═══
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 6.dp,
                    label = "dot_width"
                )

                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.9f)
                            else Color.White.copy(alpha = 0.25f)
                        )
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                )
            }
        }
    }
}
