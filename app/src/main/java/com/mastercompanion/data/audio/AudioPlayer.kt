package com.mastercompanion.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.mastercompanion.domain.model.AudioCodec
import com.mastercompanion.domain.model.AudioStreamState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor() {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var currentVolume = 1.0f

    private val _streamState = MutableStateFlow(AudioStreamState())
    val streamState: StateFlow<AudioStreamState> = _streamState.asStateFlow()

    @Synchronized
    fun start(sampleRate: Int = 48000) {
        if (audioTrack != null) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Ensure buffer is sized for low latency (around 40-60ms)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(960 * 2 * 2 * 4)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            track.setVolume(currentVolume)
            track.play()
            audioTrack = track
            isPlaying = true
            Timber.i("AudioTrack initialized and playing (sampleRate=$sampleRate, bufferSize=$bufferSize)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize AudioTrack")
        }
    }

    @Synchronized
    fun writePcm(data: ByteArray, offset: Int = 0, size: Int = data.size): Int {
        val track = audioTrack ?: return 0
        return try {
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            track.write(data, offset, size, AudioTrack.WRITE_BLOCKING)
        } catch (e: Exception) {
            Timber.e(e, "AudioTrack write error")
            0
        }
    }

    @Synchronized
    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0.0f, 1.0f)
        audioTrack?.setVolume(currentVolume)
    }

    fun updateStats(
        isReceiving: Boolean,
        codec: AudioCodec,
        packetsReceived: Long,
        packetsLost: Long,
        latencyMs: Float,
        clientIp: String?
    ) {
        _streamState.value = AudioStreamState(
            isReceiving = isReceiving,
            codec = codec,
            sampleRate = 48000,
            channels = 2,
            packetsReceived = packetsReceived,
            packetsLost = packetsLost,
            bufferLatencyMs = latencyMs,
            clientIp = clientIp
        )
    }

    @Synchronized
    fun stop() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.flush()
                it.release()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error releasing AudioTrack")
        } finally {
            audioTrack = null
            isPlaying = false
            _streamState.value = _streamState.value.copy(isReceiving = false)
        }
    }
}
