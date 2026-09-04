package com.mastercompanion.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mastercompanion.data.spotify.SpotifyApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideSpotifyRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.spotify.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideSpotifyApi(retrofit: Retrofit): SpotifyApi {
        return retrofit.create(SpotifyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLrclibApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): com.mastercompanion.data.lyrics.LrclibApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.mastercompanion.data.lyrics.LrclibApi::class.java)
    }

    @Provides
    @Singleton
    fun provideLyricsOvhApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): com.mastercompanion.data.lyrics.LyricsOvhApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.lyrics.ovh/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.mastercompanion.data.lyrics.LyricsOvhApi::class.java)
    }
}
