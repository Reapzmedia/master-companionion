package com.mastercompanion.data.command

import android.content.Context
import com.mastercompanion.domain.model.CommandDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val commands: List<CommandDefinition> by lazy {
        loadCommands()
    }

    fun getAllCommands(): List<CommandDefinition> = commands

    fun findCommand(action: String): CommandDefinition? =
        commands.firstOrNull { it.action.equals(action, ignoreCase = true) }

    private fun loadCommands(): List<CommandDefinition> {
        return try {
            context.assets.open("commands.json").use { stream ->
                val content = InputStreamReader(stream).readText()
                json.decodeFromString<List<CommandDefinition>>(content)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load commands.json from assets, using defaults")
            defaultCommands
        }
    }

    companion object {
        val defaultCommands = listOf(
            CommandDefinition(
                action = "toggle_charge_limit",
                name = "Toggle Charge Limit",
                description = "Enables or disables 80% battery bypass charging via sysfs",
                category = "battery",
                requiresRoot = true
            ),
            CommandDefinition(
                action = "navigate",
                name = "Switch Dashboard Page",
                description = "Navigates between Home, Audio, System, and Settings pages",
                category = "navigation",
                requiresRoot = false
            ),
            CommandDefinition(
                action = "wol",
                name = "Wake-on-LAN",
                description = "Sends a UDP Magic Packet to wake up the desktop PC",
                category = "network",
                requiresRoot = false
            ),
            CommandDefinition(
                action = "audio_toggle",
                name = "Toggle Audio Stream",
                description = "Starts or stops listening for PC UDP audio stream",
                category = "audio",
                requiresRoot = false
            ),
            CommandDefinition(
                action = "set_brightness",
                name = "Set Screen Brightness",
                description = "Adjusts display brightness (0-255)",
                category = "system",
                requiresRoot = true
            ),
            CommandDefinition(
                action = "exec_shell",
                name = "Execute Shell Command",
                description = "Runs an arbitrary root shell script on Android",
                category = "system",
                requiresRoot = true
            )
        )
    }
}
