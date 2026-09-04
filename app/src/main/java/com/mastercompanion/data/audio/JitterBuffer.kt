package com.mastercompanion.data.audio

import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicLong

class JitterBuffer(
    private val bufferSizePackets: Int = 3
) {
    private val packetMap = ConcurrentSkipListMap<Long, AudioPacket>()
    private var lastSequence: Long = -1L
    private val _packetsReceived = AtomicLong(0)
    private val _packetsLost = AtomicLong(0)

    val packetsReceived: Long get() = _packetsReceived.get()
    val packetsLost: Long get() = _packetsLost.get()

    @Synchronized
    fun push(packet: AudioPacket) {
        _packetsReceived.incrementAndGet()

        if (lastSequence == -1L || packet.sequence < lastSequence || packet.sequence - lastSequence > 1000L) {
            // Stream initialization or reset
            packetMap.clear()
            lastSequence = packet.sequence
        } else {
            val gap = packet.sequence - lastSequence - 1L
            if (gap > 0) {
                _packetsLost.addAndGet(gap)
            }
        }

        packetMap[packet.sequence] = packet
    }

    @Synchronized
    fun pop(): AudioPacket? {
        if (packetMap.size < bufferSizePackets && lastSequence == -1L) {
            // Still prebuffering initial burst
            return null
        }

        if (packetMap.isEmpty()) {
            return null
        }

        val firstEntry = packetMap.pollFirstEntry() ?: return null
        lastSequence = firstEntry.key
        return firstEntry.value
    }

    @Synchronized
    fun clear() {
        packetMap.clear()
        lastSequence = -1L
    }

    fun resetStats() {
        _packetsReceived.set(0)
        _packetsLost.set(0)
    }
}
