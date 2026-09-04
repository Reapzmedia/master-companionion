package com.mastercompanion.data.lyrics.dto

import kotlinx.serialization.Serializable

@Serializable
data class LyricsOvhResponse(
    val lyrics: String? = null,
    val error: String? = null
)
