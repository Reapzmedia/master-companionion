package com.mastercompanion.data.network

import com.mastercompanion.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WolSender @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Sends a Wake-on-LAN Magic Packet to wake up a desktop PC on the local network.
     * @param macAddress Format "AA:BB:CC:DD:EE:FF" or "AA-BB-CC-DD-EE-FF"
     * @param broadcastIp Optional specific broadcast IP (default 255.255.255.255)
     */
    suspend fun sendMagicPacket(macAddress: String, broadcastIp: String = "255.255.255.255"): Result<Unit> = withContext(ioDispatcher) {
        try {
            val macBytes = parseMacAddress(macAddress)
            // Magic packet consists of 6 bytes of 0xFF followed by 16 repetitions of the target MAC address (102 bytes total)
            val bytes = ByteArray(6 + 16 * macBytes.size)

            for (i in 0 until 6) {
                bytes[i] = 0xFF.toByte()
            }

            var dest = 6
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, bytes, dest, macBytes.size)
                dest += macBytes.size
            }

            val address = InetAddress.getByName(broadcastIp)
            val packet = DatagramPacket(bytes, bytes.size, address, 9) // Port 9 standard WOL
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.send(packet)
            socket.close()

            Timber.i("Successfully sent WOL Magic Packet to MAC: $macAddress (Broadcast: $broadcastIp:9)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send WOL Magic Packet to $macAddress")
            Result.failure(e)
        }
    }

    suspend fun sendWol(macAddress: String, broadcastIp: String = "255.255.255.255"): Boolean {
        return sendMagicPacket(macAddress, broadcastIp).isSuccess
    }

    private fun parseMacAddress(macStr: String): ByteArray {
        val clean = macStr.replace(":", "").replace("-", "")
        require(clean.length == 12) { "Invalid MAC address length: $macStr" }
        val bytes = ByteArray(6)
        for (i in 0 until 6) {
            bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }
}
