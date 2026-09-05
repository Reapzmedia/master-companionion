package com.mastercompanion.data.command

import com.mastercompanion.data.battery.BatteryRepository
import com.mastercompanion.data.network.WolSender
import com.mastercompanion.data.prefs.PreferencesRepository
import com.mastercompanion.data.spotify.SpotifyRepository
import com.mastercompanion.domain.model.CommandRequest
import com.mastercompanion.domain.model.CommandResponse
import com.mastercompanion.platform.root.RootShell
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class CommandLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val status: String,
    val message: String
)

@Singleton
class CommandExecutor @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val wolSender: WolSender,
    private val preferencesRepository: PreferencesRepository,
    private val rootShell: RootShell,
    private val spotifyRepository: SpotifyRepository
) {
    private val _navigationEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val navigationEvents: SharedFlow<Int> = _navigationEvents.asSharedFlow()

    private val _commandLogs = MutableStateFlow<List<CommandLogEntry>>(emptyList())
    val commandLogs: StateFlow<List<CommandLogEntry>> = _commandLogs.asStateFlow()

    suspend fun execute(request: CommandRequest): CommandResponse {
        Timber.i("Executing command action: ${request.action} with params: ${request.params}")
        val response = try {
            when (request.action.lowercase()) {
                "ping" -> {
                    CommandResponse(
                        status = "ok",
                        message = "Pong from Master Companion",
                        action = request.action
                    )
                }
                "toggle_charge_limit" -> {
                    val enabled = batteryRepository.toggleChargeLimit()
                    CommandResponse(
                        status = "ok",
                        message = "Charge limit bypass is now ${if (enabled) "ENABLED (80% stop)" else "DISABLED"}",
                        action = request.action
                    )
                }
                "navigate" -> {
                    val pageStr = request.params["page"] ?: request.params["index"] ?: "0"
                    val page = pageStr.toIntOrNull() ?: 0
                    _navigationEvents.tryEmit(page)
                    CommandResponse(
                        status = "ok",
                        message = "Navigated to page $page",
                        action = request.action
                    )
                }
                "wol" -> {
                    val mac = request.params["mac"]
                        ?: preferencesRepository.pcMacFlow.first().ifBlank { null }
                    val customIp = request.params["broadcast"] ?: request.params["ip"]
                        ?: preferencesRepository.wolBroadcastIpFlow.first().ifBlank { null }
                        ?: preferencesRepository.pcIpFlow.first().ifBlank { null }

                    if (mac.isNullOrBlank()) {
                        CommandResponse(
                            status = "error",
                            message = "No MAC address provided in params or preferences",
                            action = request.action
                        )
                    } else {
                        val success = wolSender.sendWol(mac, customIp)
                        CommandResponse(
                            status = if (success) "ok" else "error",
                            message = if (success) "Magic packet broadcasted to $mac (Target/Broadcast: ${customIp ?: "auto-detect"})" else "Failed to send WoL packet",
                            action = request.action
                        )
                    }
                }
                "set_brightness" -> {
                    val brightnessStr = request.params["value"] ?: request.params["brightness"] ?: "128"
                    val brightness = brightnessStr.toIntOrNull()?.coerceIn(0, 255) ?: 128
                    val result = rootShell.execute("settings put system screen_brightness $brightness")
                    CommandResponse(
                        status = if (result.isSuccess) "ok" else "error",
                        message = if (result.isSuccess) "Screen brightness set to $brightness" else "Failed to set brightness: ${result.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "exec_shell" -> {
                    val command = request.params["command"] ?: request.params["cmd"]
                    if (command.isNullOrBlank()) {
                        CommandResponse(
                            status = "error",
                            message = "Missing 'command' parameter",
                            action = request.action
                        )
                    } else {
                        val result = rootShell.execute(command)
                        CommandResponse(
                            status = if (result.isSuccess) "ok" else "error",
                            message = if (result.isSuccess) result.getOrNull()?.ifBlank { "Executed successfully" } ?: "Executed successfully" else result.exceptionOrNull()?.message ?: "Command failed",
                            action = request.action
                        )
                    }
                }
                "set_volume", "volume", "spotify_volume" -> {
                    val valueStr = request.params["value"]
                        ?: request.params["percent"]
                        ?: request.params["volume"]
                        ?: request.params["volume_percent"]
                    val deltaStr = request.params["delta"]

                    if (!deltaStr.isNullOrBlank()) {
                        val delta = deltaStr.toIntOrNull() ?: 0
                        val res = spotifyRepository.adjustVolume(delta)
                        CommandResponse(
                            status = if (res.isSuccess) "ok" else "error",
                            message = if (res.isSuccess) "Spotify volume adjusted by ${delta}%" else "Failed to adjust Spotify volume: ${res.exceptionOrNull()?.message}",
                            action = request.action
                        )
                    } else if (!valueStr.isNullOrBlank()) {
                        val vol = valueStr.toIntOrNull()?.coerceIn(0, 100) ?: 50
                        val res = spotifyRepository.setVolume(vol)
                        CommandResponse(
                            status = if (res.isSuccess) "ok" else "error",
                            message = if (res.isSuccess) "Spotify volume set to ${vol}%" else "Failed to set Spotify volume: ${res.exceptionOrNull()?.message}",
                            action = request.action
                        )
                    } else {
                        CommandResponse(
                            status = "error",
                            message = "Missing 'value' or 'delta' parameter for volume",
                            action = request.action
                        )
                    }
                }
                "volume_up" -> {
                    val delta = request.params["step"]?.toIntOrNull() ?: 5
                    val res = spotifyRepository.adjustVolume(delta)
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Spotify volume raised by +${delta}%" else "Failed to raise Spotify volume: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "volume_down" -> {
                    val delta = request.params["step"]?.toIntOrNull() ?: 5
                    val res = spotifyRepository.adjustVolume(-delta)
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Spotify volume lowered by -${delta}%" else "Failed to lower Spotify volume: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "spotify_play_pause", "play_pause" -> {
                    val res = spotifyRepository.togglePlayPause()
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Spotify playback toggled" else "Failed to toggle Spotify playback: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "spotify_next", "next" -> {
                    val res = spotifyRepository.skipNext()
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Skipped to next track" else "Failed to skip next: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "spotify_prev", "previous", "prev" -> {
                    val res = spotifyRepository.skipPrevious()
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Skipped to previous track" else "Failed to skip previous: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "spotify_shuffle", "shuffle" -> {
                    val res = spotifyRepository.toggleShuffle()
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Spotify shuffle toggled" else "Failed to toggle shuffle: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "spotify_repeat", "repeat" -> {
                    val res = spotifyRepository.toggleRepeat()
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Spotify repeat toggled" else "Failed to toggle repeat: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                "spotify_like", "like" -> {
                    val res = spotifyRepository.toggleLike()
                    CommandResponse(
                        status = if (res.isSuccess) "ok" else "error",
                        message = if (res.isSuccess) "Spotify like toggled" else "Failed to toggle like: ${res.exceptionOrNull()?.message}",
                        action = request.action
                    )
                }
                else -> {
                    CommandResponse(
                        status = "error",
                        message = "Unknown command action: '${request.action}'",
                        action = request.action
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error executing command action: ${request.action}")
            CommandResponse(
                status = "error",
                message = "Execution failed: ${e.message ?: "Unknown error"}",
                action = request.action
            )
        }

        recordLog(request.action, response.status, response.message)
        return response
    }

    private fun recordLog(action: String, status: String, message: String) {
        val entry = CommandLogEntry(
            action = action,
            status = status,
            message = message
        )
        _commandLogs.value = (listOf(entry) + _commandLogs.value).take(50)
    }
}
