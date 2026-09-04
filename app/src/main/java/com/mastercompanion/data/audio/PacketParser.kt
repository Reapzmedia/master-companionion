package com.mastercompanion.data.audio

import com.mastercompanion.domain.model.AudioCodec
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AudioPacket(
    val codec: AudioCodec,
    val sequence: Long,
    val timestamp: Long,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioPacket
        if (codec != other.codec) return false
        if (sequence != other.sequence) return false
        if (timestamp != other.timestamp) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = codec.hashCode()
        result = 31 * result + sequence.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

object PacketParser {
    private const val HEADER_SIZE = 9
    private const val CODEC_PCM: Byte = 0x01
    private const val CODEC_OPUS: Byte = 0x02

    fun parse(buffer: ByteArray, length: Int): AudioPacket? {
        if (length < HEADER_SIZE) return null

        val byteBuffer = ByteBuffer.wrap(buffer, 0, length).order(ByteOrder.BIG_ENDIAN)
        val codecByte = byteBuffer.get()
        val seq = byteBuffer.int.toLong() and 0xFFFFFFFFL
        val ts = byteBuffer.int.toLong() and 0xFFFFFFFFL

        val codec = when (codecByte) {
            CODEC_PCM -> AudioCodec.PCM
            CODEC_OPUS -> AudioCodec.OPUS
            else -> AudioCodec.PCM
        }

        val payloadSize = length - HEADER_SIZE
        val payload = ByteArray(payloadSize)
        byteBuffer.get(payload)

        return AudioPacket(
            codec = codec,
            sequence = seq,
            timestamp = ts,
            payload = payload
        )
    }
}
