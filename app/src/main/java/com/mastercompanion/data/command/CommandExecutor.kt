package com.mastercompanion.data.command

import com.mastercompanion.data.battery.BatteryRepository
import com.mastercompanion.data.network.WolSender
import com.mastercompanion.data.prefs.PreferencesRepository
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
    private val rootShell: RootShell
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
                    val ip = request.params["ip"]
                        ?: preferencesRepository.pcIpFlow.first().ifBlank { "255.255.255.255" }

                    if (mac.isNullOrBlank()) {
                        CommandResponse(
                            status = "error",
                            message = "No MAC address provided in params or preferences",
                            action = request.action
                        )
                    } else {
                        val success = wolSender.sendWol(mac, ip)
                        CommandResponse(
                            status = if (success) "ok" else "error",
                            message = if (success) "Magic packet broadcasted to $mac ($ip)" else "Failed to send WoL packet",
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
