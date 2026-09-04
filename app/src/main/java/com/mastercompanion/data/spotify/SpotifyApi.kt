package com.mastercompanion.data.spotify

import com.mastercompanion.data.spotify.dto.CurrentlyPlayingResponse
import com.mastercompanion.data.spotify.dto.RecentlyPlayedResponse
import com.mastercompanion.data.spotify.dto.SpotifyTokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface SpotifyApi {

    @GET("v1/me/player/currently-playing")
    suspend fun getCurrentlyPlaying(
        @Header("Authorization") bearerToken: String
    ): Response<CurrentlyPlayingResponse>

    @GET("v1/me/player/recently-played")
    suspend fun getRecentlyPlayed(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 1
    ): Response<RecentlyPlayedResponse>

    @PUT("v1/me/player/play")
    suspend fun play(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>

    @PUT("v1/me/player/pause")
    suspend fun pause(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>

    @POST("v1/me/player/next")
    suspend fun next(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>

    @POST("v1/me/player/previous")
    suspend fun previous(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>

    @PUT("v1/me/player/seek")
    suspend fun seek(
        @Header("Authorization") bearerToken: String,
        @Query("position_ms") positionMs: Long
    ): Response<Unit>

    @PUT("v1/me/player/shuffle")
    suspend fun setShuffle(
        @Header("Authorization") bearerToken: String,
        @Query("state") state: Boolean
    ): Response<Unit>

    @PUT("v1/me/player/repeat")
    suspend fun setRepeat(
        @Header("Authorization") bearerToken: String,
        @Query("state") state: String // "track", "context", "off"
    ): Response<Unit>

    // ═══ Token Accounts API ═══
    @FormUrlEncoded
    @POST("https://accounts.spotify.com/api/token")
    suspend fun exchangePkceCode(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String
    ): Response<SpotifyTokenResponse>

    @FormUrlEncoded
    @POST("https://accounts.spotify.com/api/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): Response<SpotifyTokenResponse>
}
