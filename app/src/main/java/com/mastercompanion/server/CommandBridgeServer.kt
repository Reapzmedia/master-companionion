package com.mastercompanion.server

import com.mastercompanion.data.battery.BatteryRepository
import com.mastercompanion.data.command.CommandExecutor
import com.mastercompanion.data.command.CommandRegistry
import com.mastercompanion.data.prefs.PreferencesRepository
import com.mastercompanion.data.spotify.SpotifyRepository
import com.mastercompanion.domain.model.BatteryData
import com.mastercompanion.domain.model.CommandRequest
import com.mastercompanion.domain.model.CommandResponse
import com.mastercompanion.domain.model.SpotifyTrack
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BridgeStatus(
    val status: String = "ok",
    val battery: BatteryData? = null,
    val currentTrack: SpotifyTrack? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class CommandBridgeServer @Inject constructor(
    private val commandExecutor: CommandExecutor,
    private val commandRegistry: CommandRegistry,
    private val batteryRepository: BatteryRepository,
    private val spotifyRepository: SpotifyRepository,
    private val preferencesRepository: PreferencesRepository,
    private val json: Json
) {
    private var server: ApplicationEngine? = null

    fun start(port: Int = 8420) {
        if (server != null) {
            Timber.w("Ktor Command Bridge Server is already running")
            return
        }

        Timber.i("Starting Ktor Command Bridge Server on port $port...")
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(json)
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    Timber.e(cause, "Unhandled error in Ktor server route")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        CommandResponse(
                            status = "error",
                            message = "Internal server error: ${cause.localizedMessage}"
                        )
                    )
                }
            }

            routing {
                get("/ping") {
                    call.respond(
                        CommandResponse(
                            status = "ok",
                            message = "Pong from Master Companion",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                get("/commands") {
                    call.respond(commandRegistry.getAllCommands())
                }

                get("/status") {
                    val status = BridgeStatus(
                        status = "ok",
                        battery = batteryRepository.batteryData.value,
                        currentTrack = spotifyRepository.currentTrack.value,
                        timestamp = System.currentTimeMillis()
                    )
                    call.respond(status)
                }

                post("/command") {
                    val expectedToken = preferencesRepository.authTokenFlow.first()
                    val clientToken = call.request.header("X-Auth-Token")

                    if (expectedToken.isNotBlank() && expectedToken != "master-companion-default-token") {
                        if (clientToken != expectedToken) {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                CommandResponse(
                                    status = "error",
                                    message = "Unauthorized: Invalid or missing X-Auth-Token header"
                                )
                            )
                            return@post
                        }
                    }

                    val request = try {
                        call.receive<CommandRequest>()
                    } catch (e: Exception) {
                        Timber.e(e, "Malformed JSON in /command request")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            CommandResponse(
                                status = "error",
                                message = "Invalid JSON body: ${e.message}"
                            )
                        )
                        return@post
                    }

                    val result = commandExecutor.execute(request)
                    call.respond(result)
                }
            }
        }.start(wait = false)
        Timber.i("Ktor Command Bridge Server started on port $port")
    }

    fun stop() {
        server?.let {
            Timber.i("Stopping Ktor Command Bridge Server...")
            it.stop(gracePeriodMillis = 1000, timeoutMillis = 2000)
            server = null
            Timber.i("Ktor Command Bridge Server stopped")
        }
    }
}
