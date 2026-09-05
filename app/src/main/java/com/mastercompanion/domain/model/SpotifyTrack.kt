package com.mastercompanion.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtUrl: String? = null,
    val durationMs: Long = 0L,
    val progressMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isRecentFallback: Boolean = false,
    val playlistContext: String = "Liked Songs",
    val contextType: String = "playlist",
    val releaseYear: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val contextHeader: String
        get() = when (contextType.lowercase()) {
            "album" -> "PLAYING FROM ALBUM"
            "artist" -> "PLAYING FROM ARTIST"
            "collection" -> "PLAYING FROM LIBRARY"
            "show", "episode" -> "PLAYING FROM PODCAST"
            else -> "PLAYING FROM PLAYLIST"
        }
}
