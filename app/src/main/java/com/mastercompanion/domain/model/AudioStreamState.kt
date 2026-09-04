package com.mastercompanion.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AudioCodec {
    OPUS,
    PCM
}

@Serializable
data class AudioStreamState(
    val isReceiving: Boolean = false,
    val codec: AudioCodec = AudioCodec.PCM,
    val sampleRate: Int = 48000,
    val channels: Int = 2,
    val packetsReceived: Long = 0L,
    val packetsLost: Long = 0L,
    val bufferLatencyMs: Float = 0.0f,
    val clientIp: String? = null
)
