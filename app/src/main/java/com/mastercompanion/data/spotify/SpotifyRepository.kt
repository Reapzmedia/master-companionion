package com.mastercompanion.data.spotify

import com.mastercompanion.data.prefs.PreferencesRepository
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
    private val preferencesRepository: PreferencesRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(defaultDispatcher)

    // Empty initial state when no active Spotify session
    private val _currentTrack = MutableStateFlow<SpotifyTrack?>(null)
    val currentTrack: StateFlow<SpotifyTrack?> = _currentTrack.asStateFlow()

    private val _isCurrentTrackLiked = MutableStateFlow(false)
    val isCurrentTrackLiked: StateFlow<Boolean> = _isCurrentTrackLiked.asStateFlow()

    private val _activeDeviceName = MutableStateFlow("")
    val activeDeviceName: StateFlow<String> = _activeDeviceName.asStateFlow()

    private val _activeDeviceVolume = MutableStateFlow<Int?>(null)
    val activeDeviceVolume: StateFlow<Int?> = _activeDeviceVolume.asStateFlow()

    private val _isLibraryScopeMissing = MutableStateFlow(false)
    val isLibraryScopeMissing: StateFlow<Boolean> = _isLibraryScopeMissing.asStateFlow()

    private var lastCheckedTrackId: String? = null

    init {
        scope.launch {
            preferencesRepository.spotifyScopesFlow.collect { scopes ->
                if (scopes.contains("user-library-modify") && scopes.contains("user-library-read")) {
                    _isLibraryScopeMissing.value = false
                    lastCheckedTrackId = null
                }
            }
        }
        scope.launch {
            startPollingLoop()
        }
    }

    private suspend fun startPollingLoop() {
        while (scope.isActive) {
            val bearerToken = authManager.getValidBearerToken()

            if (bearerToken != null) {
                try {
                    val response = spotifyApi.getPlaybackState(bearerToken)

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!

                        // Sync active playing device info & volume
                        body.device?.let { dev ->
                            _activeDeviceName.value = dev.name
                            if (dev.volumePercent != null) {
                                _activeDeviceVolume.value = dev.volumePercent
                            }
                        }

                        // Sync shuffle & repeat states directly from active player
                        body.shuffleState?.let { shuffle ->
                            _isShuffle.value = shuffle
                        }
                        body.repeatState?.let { rep ->
                            _repeatMode.value = when (rep.lowercase()) {
                                "track" -> 2
                                "context" -> 1
                                else -> 0
                            }
                        }

                        val item = body.item
                        if (item != null) {
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

                            checkLikedStatus(item.id, bearerToken)
                        } else {
                            // Device connected/active but no queue, check recently played
                            fetchRecentlyPlayed(bearerToken)
                        }
                    } else if (response.code() == 204 || response.body() == null) {
                        // Nothing actively playing, check recently played
                        _activeDeviceVolume.value = null
                        _activeDeviceName.value = ""
                        fetchRecentlyPlayed(bearerToken)
                    }
                } catch (e: Exception) {
                    Timber.d("Polling Spotify playback: ${e.message}")
                }
            } else {
                _currentTrack.value = null
                _isCurrentTrackLiked.value = false
                _activeDeviceName.value = ""
                _activeDeviceVolume.value = null
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

    private suspend fun checkLikedStatus(trackId: String, bearerToken: String) {
        if (trackId.isBlank()) return
        if (trackId == lastCheckedTrackId) return
        try {
            val trackUri = "spotify:track:$trackId"
            var resp = spotifyApi.checkLibraryContains(bearerToken, trackUri)
            if (!resp.isSuccessful && resp.code() != 403) {
                resp = spotifyApi.checkUserSavedTracks(bearerToken, trackId)
            }
            if (resp.isSuccessful) {
                lastCheckedTrackId = trackId
                _isCurrentTrackLiked.value = resp.body()?.firstOrNull() ?: false
                _isLibraryScopeMissing.value = false
            } else if (resp.code() == 403) {
                Timber.w("checkLikedStatus returned 403 Forbidden - library scope missing")
                _isLibraryScopeMissing.value = true
                lastCheckedTrackId = trackId
            }
        } catch (e: Exception) {
            Timber.d("Error checking liked status: ${e.message}")
        }
    }

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0 = off, 1 = context, 2 = track
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    suspend fun toggleShuffle(): Result<Unit> {
        val next = !_isShuffle.value
        _isShuffle.value = next
        val bearerToken = authManager.getValidBearerToken() ?: return Result.success(Unit)
        return try {
            val resp = spotifyApi.setShuffle(bearerToken, next)
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(RuntimeException("Shuffle error ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleRepeat(): Result<Unit> {
        val nextMode = (_repeatMode.value + 1) % 3
        _repeatMode.value = nextMode
        val stateStr = when (nextMode) {
            1 -> "context"
            2 -> "track"
            else -> "off"
        }
        val bearerToken = authManager.getValidBearerToken() ?: return Result.success(Unit)
        return try {
            val resp = spotifyApi.setRepeat(bearerToken, stateStr)
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(RuntimeException("Repeat error ${resp.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(): Result<Unit> {
        val track = _currentTrack.value ?: return Result.success(Unit)
        val trackId = track.id.takeIf { it.isNotBlank() } ?: return Result.success(Unit)
        val currentlyLiked = _isCurrentTrackLiked.value
        val newLiked = !currentlyLiked

        val bearerToken = authManager.getValidBearerToken()
        if (bearerToken == null) {
            return Result.failure(IllegalStateException("Not logged in to Spotify"))
        }

        // Optimistic instant UI update
        _isCurrentTrackLiked.value = newLiked

        return try {
            val trackUri = "spotify:track:$trackId"
            val resp = if (newLiked) {
                var r = spotifyApi.saveToLibrary(bearerToken, trackUri)
                if (!r.isSuccessful && r.code() != 403) {
                    r = spotifyApi.saveTrack(bearerToken, trackId)
                }
                r
            } else {
                var r = spotifyApi.removeFromLibrary(bearerToken, trackUri)
                if (!r.isSuccessful && r.code() != 403) {
                    r = spotifyApi.removeTrack(bearerToken, trackId)
                }
                r
            }

            if (resp.isSuccessful) {
                Timber.i("Successfully toggled like for $trackId to $newLiked")
                lastCheckedTrackId = trackId
                _isCurrentTrackLiked.value = newLiked
                _isLibraryScopeMissing.value = false
                Result.success(Unit)
            } else {
                val errBody = resp.errorBody()?.string() ?: ""
                Timber.w("Spotify like response code ${resp.code()}: $errBody")
                // Revert optimistic update so UI does not show false liked state!
                _isCurrentTrackLiked.value = currentlyLiked
                if (resp.code() == 403) {
                    Timber.e("Spotify like failed with 403 Forbidden! Token lacks user-library-modify scope.")
                    _isLibraryScopeMissing.value = true
                }
                Result.failure(RuntimeException("Spotify like error ${resp.code()}: $errBody"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception toggling like: ${e.message}")
            _isCurrentTrackLiked.value = currentlyLiked
            Result.failure(e)
        }
    }

    suspend fun setVolume(volumePercent: Int): Result<Unit> {
        val clamped = volumePercent.coerceIn(0, 100)
        _activeDeviceVolume.value = clamped // Immediate optimistic local update
        val bearerToken = authManager.getValidBearerToken() ?: return Result.success(Unit)
        return try {
            val resp = spotifyApi.setVolume(bearerToken, clamped)
            if (resp.isSuccessful) {
                Timber.d("Spotify volume set to $clamped% on ${_activeDeviceName.value}")
                Result.success(Unit)
            } else {
                Timber.w("Spotify setVolume returned ${resp.code()}")
                Result.failure(RuntimeException("Spotify set volume error ${resp.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception setting Spotify volume: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun adjustVolume(deltaPercent: Int): Result<Unit> {
        val current = _activeDeviceVolume.value ?: 50
        return setVolume(current + deltaPercent)
    }
}

