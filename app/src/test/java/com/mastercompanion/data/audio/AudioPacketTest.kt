package com.mastercompanion.data.audio

import com.mastercompanion.domain.model.AudioCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioPacketTest {

    @Test
    fun `parse valid PCM packet extracts header and payload correctly`() {
        val payloadData = byteArrayOf(10, 20, 30, 40, 50)
        val buffer = ByteBuffer.allocate(9 + payloadData.size).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01.toByte()) // PCM
        buffer.putInt(42)         // sequence
        buffer.putInt(96000)      // timestamp
        buffer.put(payloadData)

        val packet = PacketParser.parse(buffer.array(), buffer.array().size)

        assertNotNull(packet)
        assertEquals(AudioCodec.PCM, packet?.codec)
        assertEquals(42L, packet?.sequence)
        assertEquals(96000L, packet?.timestamp)
        assertEquals(payloadData.size, packet?.payload?.size)
        assertEquals(10.toByte(), packet?.payload?.get(0))
        assertEquals(50.toByte(), packet?.payload?.get(4))
    }

    @Test
    fun `parse packet with size smaller than 9 bytes returns null`() {
        val smallBuffer = byteArrayOf(1, 2, 3, 4)
        val packet = PacketParser.parse(smallBuffer, smallBuffer.size)
        assertNull(packet)
    }

    @Test
    fun `jitter buffer correctly orders packets and tracks packet loss`() {
        val jitterBuffer = JitterBuffer(bufferSizePackets = 2)

        val p1 = AudioPacket(AudioCodec.PCM, 1L, 0L, byteArrayOf(1))
        val p3 = AudioPacket(AudioCodec.PCM, 3L, 40L, byteArrayOf(3))

        jitterBuffer.push(p1)
        jitterBuffer.push(p3) // Sequence 2 was skipped/lost

        assertEquals(2L, jitterBuffer.packetsReceived)
        assertEquals(1L, jitterBuffer.packetsLost)

        val popped = jitterBuffer.pop()
        assertEquals(1L, popped?.sequence)
    }
}
