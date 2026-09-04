package com.mastercompanion.data.spotify

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import com.mastercompanion.data.prefs.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val spotifyApi: SpotifyApi
) {
    private var codeVerifier: String? = null

    companion object {
        const val REDIRECT_URI = "mastercompanion://spotify/callback"
        const val SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing user-read-recently-played"
        // User Spotify Client ID
        const val DEFAULT_CLIENT_ID = "5871091323a84960a2ee5b9d6cb2644f"
    }

    /**
     * Starts the OAuth 2.0 PKCE flow by launching Chrome Custom Tabs.
     */
    suspend fun startAuthFlow(customClientId: String? = null) {
        val clientId = customClientId?.takeIf { it.isNotBlank() }
            ?: preferencesRepository.spotifyClientIdFlow.first().takeIf { it.isNotBlank() }
            ?: DEFAULT_CLIENT_ID

        val verifier = generateCodeVerifier()
        codeVerifier = verifier
        val challenge = generateCodeChallenge(verifier)

        val authUri = Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("scope", SCOPES)
            .build()

        Timber.i("Launching Spotify PKCE authorization in Custom Tab: $authUri")
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(context, authUri)
    }

    /**
     * Handles authorization code from deep-link callback and exchanges for tokens.
     */
    suspend fun handleAuthCallback(code: String): Result<Unit> {
        val verifier = codeVerifier ?: return Result.failure(IllegalStateException("No code_verifier found. Please restart auth."))
        val clientId = preferencesRepository.spotifyClientIdFlow.first().ifBlank { DEFAULT_CLIENT_ID }

        return try {
            val response = spotifyApi.exchangePkceCode(
                clientId = clientId,
                code = code,
                redirectUri = REDIRECT_URI,
                codeVerifier = verifier
            )

            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                preferencesRepository.saveSpotifyTokens(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresInSeconds = token.expiresIn
                )
                Timber.i("Successfully exchanged Spotify PKCE code for tokens!")
                Result.success(Unit)
            } else {
                val err = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Timber.e("Failed to exchange PKCE code: $err")
                Result.failure(RuntimeException(err))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during PKCE token exchange")
            Result.failure(e)
        }
    }

    /**
     * Resolves a valid Bearer token, refreshing automatically if close to expiration.
     */
    suspend fun getValidBearerToken(): String? {
        val accessToken = preferencesRepository.spotifyAccessTokenFlow.first() ?: return null
        val expiresAt = preferencesRepository.spotifyExpiresAtFlow.first()
        val bufferMs = 60_000L // Refresh 1 minute before expiry

        if (System.currentTimeMillis() < expiresAt - bufferMs) {
            return "Bearer $accessToken"
        }

        // Token expired, attempt refresh
        val refreshToken = preferencesRepository.spotifyRefreshTokenFlow.first() ?: return null
        val clientId = preferencesRepository.spotifyClientIdFlow.first().ifBlank { DEFAULT_CLIENT_ID }

        return try {
            val response = spotifyApi.refreshToken(
                clientId = clientId,
                refreshToken = refreshToken
            )
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                preferencesRepository.saveSpotifyTokens(
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresInSeconds = token.expiresIn
                )
                "Bearer ${token.accessToken}"
            } else {
                Timber.w("Token refresh failed, user may need to re-authenticate.")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during token refresh")
            null
        }
    }

    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(64)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes, 0, bytes.size)
        val hash = digest.digest()
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
