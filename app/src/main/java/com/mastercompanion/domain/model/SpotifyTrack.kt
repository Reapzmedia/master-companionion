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
    val timestamp: Long = System.currentTimeMillis()
)
