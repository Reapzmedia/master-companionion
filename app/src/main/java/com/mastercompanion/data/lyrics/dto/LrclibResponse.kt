package com.mastercompanion.data.lyrics.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LrclibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Float? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    @SerialName("syncedLyrics") val syncedLyrics: String? = null
)
