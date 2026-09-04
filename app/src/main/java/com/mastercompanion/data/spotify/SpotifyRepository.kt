package com.mastercompanion.data.spotify

import com.mastercompanion.di.DefaultDispatcher
import com.mastercompanion.domain.model.SpotifyTrack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepository @Inject constructor(
    private val spotifyApi: SpotifyApi,
    private val authManager: SpotifyAuthManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(defaultDispatcher)

    // Empty initial state when no active Spotify session
    private val _currentTrack = MutableStateFlow<SpotifyTrack?>(null)
    val currentTrack: StateFlow<SpotifyTrack?> = _currentTrack.asStateFlow()

    init {
        scope.launch {
            startPollingLoop()
        }
    }

    private suspend fun startPollingLoop() {
        while (scope.isActive) {
            val bearerToken = authManager.getValidBearerToken()

            if (bearerToken != null) {
                try {
                    val response = spotifyApi.getCurrentlyPlaying(bearerToken)

                    if (response.isSuccessful && response.body()?.item != null) {
                        val body = response.body()!!
                        val item = body.item!!
                        val artUrl = item.album?.images?.firstOrNull()?.url
                        val artistName = item.artists.joinToString(", ") { it.name }
                        val year = item.album?.releaseDate?.take(4) ?: ""

                        _currentTrack.value = SpotifyTrack(
                            id = item.id,
                            title = item.name,
                            artist = artistName,
                            album = item.album?.name ?: "",
                            albumArtUrl = artUrl,
                            durationMs = item.durationMs,
                            progressMs = body.progressMs,
                            isPlaying = body.isPlaying,
                            isRecentFallback = false,
                            releaseYear = year,
                            timestamp = System.currentTimeMillis()
                        )
                    } else if (response.code() == 204 || response.body()?.item == null) {
                        // Nothing actively playing, check recently played
                        fetchRecentlyPlayed(bearerToken)
                    }
                } catch (e: Exception) {
                    Timber.d("Polling Spotify playback: ${e.message}")
                }
            } else {
                _currentTrack.value = null
            }

            // Adaptive polling rate: 1.5s if playing, 4s if idle/disconnected
            val delayMs = if (_currentTrack.value?.isPlaying == true) 1500L else 4000L
            delay(delayMs)
        }
    }

    private suspend fun fetchRecentlyPlayed(bearerToken: String) {
        try {
            val recentResp = spotifyApi.getRecentlyPlayed(bearerToken, limit = 1)
            if (recentResp.isSuccessful && recentResp.body()?.items?.isNotEmpty() == true) {
                val item = recentResp.body()!!.items.first().track
                val artUrl = item.album?.images?.firstOrNull()?.url
                val artistName = item.artists.joinToString(", ") { it.name }
                val year = item.album?.releaseDate?.take(4) ?: ""

                _currentTrack.value = SpotifyTrack(
                    id = item.id,
                    title = item.name,
                    artist = artistName,
                    album = item.album?.name ?: "",
                    albumArtUrl = artUrl,
                    durationMs = item.durationMs,
                    progressMs = 0L,
                    isPlaying = false,
                    isRecentFallback = true,
                    releaseYear = year
                )
            } else {
                _currentTrack.value = null
            }
        } catch (e: Exception) {
            Timber.d("Error fetching recently played: ${e.message}")
            _currentTrack.value = null
        }
    }

    suspend fun togglePlayPause(): Result<Unit> {
        val current = _currentTrack.value ?: return Result.success(Unit)
        val bearerToken = authManager.getValidBearerToken()
        if (bearerToken == null) {
            // Local state toggle if not logged in
            _currentTrack.value = current.copy(isPlaying = !current.isPlaying)
            return Result.success(Unit)
        }

        return try {
            val resp = if (current.isPlaying) {
                spotifyApi.pause(bearerToken)
            } else {
                spotifyApi.play(bearerToken)
            }
            if (resp.isSuccessful) {
                _currentTrack.value = current.copy(isPlaying = !current.isPlaying)
                Result.success(Unit)
            } else {
                Result.failure(RuntimeException("Spotify playback error ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun skipNext(): Result<Unit> {
        val bearerToken = authManager.getValidBearerToken() ?: return Result.success(Unit)
        return try {
            val resp = spotifyApi.next(bearerToken)
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(RuntimeException("Skip next failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun skipPrevious(): Result<Unit> {
        val bearerToken = authManager.getValidBearerToken() ?: return Result.success(Unit)
        return try {
            val resp = spotifyApi.previous(bearerToken)
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(RuntimeException("Skip previous failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seekTo(positionMs: Long): Result<Unit> {
        _currentTrack.value = _currentTrack.value?.copy(progressMs = positionMs)
        val bearerToken = authManager.getValidBearerToken() ?: return Result.success(Unit)
        return try {
            val resp = spotifyApi.seek(bearerToken, positionMs)
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(RuntimeException("Seek failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
