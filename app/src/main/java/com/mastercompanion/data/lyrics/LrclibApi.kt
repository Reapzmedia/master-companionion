package com.mastercompanion.data.lyrics

import com.mastercompanion.data.lyrics.dto.LrclibResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface LrclibApi {

    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") durationSeconds: Int? = null,
        @Header("User-Agent") userAgent: String = "MasterCompanion/1.0 (Android; Standby Dashboard)"
    ): Response<LrclibResponse>

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String? = null,
        @Query("artist_name") artistName: String? = null,
        @Query("q") query: String? = null,
        @Header("User-Agent") userAgent: String = "MasterCompanion/1.0 (Android; Standby Dashboard)"
    ): Response<List<LrclibResponse>>
}
