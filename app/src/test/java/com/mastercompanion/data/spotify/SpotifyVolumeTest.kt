package com.mastercompanion.data.spotify

import com.mastercompanion.data.spotify.dto.PlaybackStateResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyVolumeTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `deserialize Spotify PlaybackState with active device and volume`() {
        val rawJson = """
            {
              "device": {
                "id": "abc123device",
                "is_active": true,
                "name": "DESKTOP-GAMING-PC",
                "type": "Computer",
                "volume_percent": 82,
                "supports_volume": true
              },
              "shuffle_state": true,
              "repeat_state": "track",
              "timestamp": 1690000000000,
              "progress_ms": 45000,
              "is_playing": true,
              "item": {
                "id": "track_789",
                "name": "Black and Yellow",
                "duration_ms": 218000,
                "artists": [{"name": "Wiz Khalifa"}],
                "album": {
                  "name": "Rolling Papers",
                  "release_date": "2011-03-29",
                  "images": [{"url": "https://example.com/art.jpg"}]
                }
              }
            }
        """.trimIndent()

        val state = json.decodeFromString<PlaybackStateResponse>(rawJson)

        assertNotNull(state.device)
        assertEquals("DESKTOP-GAMING-PC", state.device?.name)
        assertEquals(82, state.device?.volumePercent)
        assertTrue(state.device?.isActive == true)
        assertTrue(state.shuffleState == true)
        assertEquals("track", state.repeatState)
        assertEquals(true, state.isPlaying)
        assertEquals("Black and Yellow", state.item?.name)
    }

    @Test
    fun `volume percent clamping logic stays within 0 to 100`() {
        fun clampVolume(v: Int) = v.coerceIn(0, 100)

        assertEquals(0, clampVolume(-15))
        assertEquals(100, clampVolume(140))
        assertEquals(75, clampVolume(75))
        assertEquals(0, clampVolume(0))
        assertEquals(100, clampVolume(100))
    }

    @Test
    fun `volume delta calculations adjust smoothly`() {
        var volume = 50
        fun adjustVolume(delta: Int) {
            volume = (volume + delta).coerceIn(0, 100)
        }

        adjustVolume(5)
        assertEquals(55, volume)

        adjustVolume(-10)
        assertEquals(45, volume)

        adjustVolume(70)
        assertEquals(100, volume)

        adjustVolume(-150)
        assertEquals(0, volume)
    }
}
