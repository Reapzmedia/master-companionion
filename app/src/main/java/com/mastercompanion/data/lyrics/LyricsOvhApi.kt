package com.mastercompanion.data.lyrics

import com.mastercompanion.data.lyrics.dto.LyricsOvhResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface LyricsOvhApi {

    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): Response<LyricsOvhResponse>
}
