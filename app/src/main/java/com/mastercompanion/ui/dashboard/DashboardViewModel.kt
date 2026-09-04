package com.mastercompanion.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mastercompanion.data.audio.AudioPlayer
import com.mastercompanion.data.battery.BatteryRepository
import com.mastercompanion.data.command.CommandExecutor
import com.mastercompanion.data.command.CommandLogEntry
import com.mastercompanion.data.prefs.PreferencesRepository
import com.mastercompanion.data.spotify.SpotifyAuthManager
import com.mastercompanion.data.spotify.SpotifyRepository
import com.mastercompanion.domain.model.AudioStreamState
import com.mastercompanion.domain.model.BatteryData
import com.mastercompanion.domain.model.CommandRequest
import com.mastercompanion.domain.model.SpotifyTrack
import com.mastercompanion.platform.root.RootShell
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryRepository: BatteryRepository,
    private val spotifyRepository: SpotifyRepository,
    private val audioPlayer: AudioPlayer,
    private val commandExecutor: CommandExecutor,
    private val preferencesRepository: PreferencesRepository,
    private val spotifyAuthManager: SpotifyAuthManager,
    private val rootShell: RootShell
) : ViewModel() {

    // ═══ State Flows ═══
    val batteryData: StateFlow<BatteryData> = batteryRepository.batteryData
    val currentTrack: StateFlow<SpotifyTrack?> = spotifyRepository.currentTrack
    val audioStreamState: StateFlow<AudioStreamState> = audioPlayer.streamState
    val commandLogs: StateFlow<List<CommandLogEntry>> = commandExecutor.commandLogs
    val navigationEvents: SharedFlow<Int> = commandExecutor.navigationEvents
    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            _isRootAvailable.value = rootShell.isRootAvailable()
        }
    }

    val authToken: StateFlow<String> = preferencesRepository.authTokenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "master-companion-default-token")

    val chargeStopThreshold: StateFlow<Int> = preferencesRepository.chargeStopThresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80)

    val chargeResumeThreshold: StateFlow<Int> = preferencesRepository.chargeResumeThresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75)

    val pcIp: StateFlow<String> = preferencesRepository.pcIpFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "192.168.1.100")

    val pcMac: StateFlow<String> = preferencesRepository.pcMacFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00:00:00:00:00")

    val spotifyClientId: StateFlow<String> = preferencesRepository.spotifyClientIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isSpotifyConnected: StateFlow<Boolean> = preferencesRepository.spotifyAccessTokenFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ═══ Battery Actions ═══
    fun toggleChargeLimit() {
        viewModelScope.launch {
            batteryRepository.toggleChargeLimit()
        }
    }

    // ═══ Spotify Actions ═══
    fun togglePlayPause() {
        viewModelScope.launch {
            spotifyRepository.togglePlayPause()
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            spotifyRepository.skipNext()
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            spotifyRepository.skipPrevious()
        }
    }

    fun seekTo(positionMs: Long) {
        viewModelScope.launch {
            spotifyRepository.seekTo(positionMs)
        }
    }

    fun connectSpotify() {
        viewModelScope.launch {
            spotifyAuthManager.startAuthFlow()
        }
    }

    fun disconnectSpotify() {
        viewModelScope.launch {
            preferencesRepository.clearSpotifyTokens()
        }
    }

    // ═══ Audio Actions ═══
    fun setAudioVolume(volume: Float) {
        audioPlayer.setVolume(volume)
    }

    // ═══ Bridge Actions ═══
    fun sendWol(mac: String? = null, ip: String? = null) {
        viewModelScope.launch {
            val targetMac = mac?.takeIf { it.isNotBlank() } ?: pcMac.value
            val targetIp = ip?.takeIf { it.isNotBlank() } ?: pcIp.value
            commandExecutor.execute(
                CommandRequest(
                    action = "wol",
                    params = buildMap {
                        if (targetMac.isNotBlank()) put("mac", targetMac)
                        if (targetIp.isNotBlank()) put("ip", targetIp)
                    }
                )
            )
        }
    }

    fun executeBridgeAction(action: String, params: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            commandExecutor.execute(CommandRequest(action, params))
        }
    }

    fun regenerateAuthToken() {
        viewModelScope.launch {
            preferencesRepository.generateNewAuthToken()
        }
    }

    fun updateChargeThresholds(stopAt: Int, resumeAt: Int) {
        viewModelScope.launch {
            preferencesRepository.setChargeThresholds(stopAt, resumeAt)
        }
    }

    fun updatePcNetwork(ip: String, mac: String) {
        viewModelScope.launch {
            preferencesRepository.setPcNetworkDetails(ip, mac)
        }
    }

    fun updateSpotifyClientId(clientId: String) {
        viewModelScope.launch {
            preferencesRepository.setSpotifyClientId(clientId)
        }
    }
}
