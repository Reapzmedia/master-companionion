package com.mastercompanion.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mastercompanion.MainActivity
import com.mastercompanion.MasterCompanionApp
import com.mastercompanion.R
import com.mastercompanion.data.audio.AudioPlayer
import com.mastercompanion.data.audio.JitterBuffer
import com.mastercompanion.data.audio.PacketParser
import com.mastercompanion.data.prefs.PreferencesRepository
import com.mastercompanion.domain.model.AudioCodec
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import javax.inject.Inject

@AndroidEntryPoint
class AudioReceiverService : Service() {

    @Inject
    lateinit var audioPlayer: AudioPlayer

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var receiverJob: Job? = null
    private var statsJob: Job? = null
    private var socket: DatagramSocket? = null
    private val jitterBuffer = JitterBuffer(bufferSizePackets = 2)

    override fun onCreate() {
        super.onCreate()
        Timber.i("AudioReceiverService created")
        startForeground(NOTIFICATION_ID, buildNotification())
        audioPlayer.start()
        startReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("AudioReceiverService started (UDP receiver)")
        return START_STICKY
    }

    private fun startReceiver() {
        receiverJob?.cancel()
        receiverJob = serviceScope.launch {
            val port = preferencesRepository.audioPortFlow.first()
            val buffer = ByteArray(8192)

            try {
                socket = DatagramSocket(port).apply {
                    soTimeout = 2000 // 2s timeout for non-blocking loop checks
                    receiveBufferSize = 65536
                }
                Timber.i("AudioReceiver UDP socket bound on port $port")

                var lastSenderIp: String? = null
                var lastCodec = AudioCodec.PCM
                var lastPacketTime = 0L

                // Monitor stream health and silence
                statsJob = serviceScope.launch {
                    while (isActive) {
                        delay(1000)
                        val elapsedSinceLastPacket = System.currentTimeMillis() - lastPacketTime
                        val isStreaming = lastPacketTime > 0 && elapsedSinceLastPacket < 3000

                        audioPlayer.updateStats(
                            isReceiving = isStreaming,
                            codec = lastCodec,
                            packetsReceived = jitterBuffer.packetsReceived,
                            packetsLost = jitterBuffer.packetsLost,
                            latencyMs = if (isStreaming) 20.0f * 2 else 0.0f,
                            clientIp = if (isStreaming) lastSenderIp else null
                        )
                    }
                }

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket?.receive(packet)
                        lastPacketTime = System.currentTimeMillis()
                        lastSenderIp = packet.address?.hostAddress

                        val audioPacket = PacketParser.parse(packet.data, packet.length)
                        if (audioPacket != null) {
                            lastCodec = audioPacket.codec
                            jitterBuffer.push(audioPacket)

                            // Drain jitter buffer to audio track
                            var queued = jitterBuffer.pop()
                            while (queued != null) {
                                if (queued.codec == AudioCodec.PCM) {
                                    audioPlayer.writePcm(queued.payload)
                                }
                                queued = jitterBuffer.pop()
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        // Expected timeout during silence, continue loop
                    } catch (e: Exception) {
                        if (isActive) {
                            Timber.e(e, "Error receiving audio packet")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start AudioReceiver UDP socket on port $port")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        receiverJob?.cancel()
        statsJob?.cancel()
        socket?.close()
        socket = null
        audioPlayer.stop()
        serviceScope.cancel()
        Timber.i("AudioReceiverService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MasterCompanionApp.CHANNEL_AUDIO_RECEIVER)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("PC Audio Passthrough Receiver Active (:8421)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1003
    }
}
