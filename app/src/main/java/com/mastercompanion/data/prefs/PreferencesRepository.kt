package com.mastercompanion.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "master_companion_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // ═══ Keys ═══
    companion object {
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_CHARGE_LIMIT_ENABLED = booleanPreferencesKey("charge_limit_enabled")
        val KEY_CHARGE_STOP_THRESHOLD = intPreferencesKey("charge_stop_threshold")
        val KEY_CHARGE_RESUME_THRESHOLD = intPreferencesKey("charge_resume_threshold")
        val KEY_PC_IP = stringPreferencesKey("pc_ip")
        val KEY_PC_MAC = stringPreferencesKey("pc_mac")
        val KEY_SPOTIFY_CLIENT_ID = stringPreferencesKey("spotify_client_id")
        val KEY_SPOTIFY_ACCESS_TOKEN = stringPreferencesKey("spotify_access_token")
        val KEY_SPOTIFY_REFRESH_TOKEN = stringPreferencesKey("spotify_refresh_token")
        val KEY_SPOTIFY_EXPIRES_AT = longPreferencesKey("spotify_expires_at")
        val KEY_COMMAND_PORT = intPreferencesKey("command_port")
        val KEY_AUDIO_PORT = intPreferencesKey("audio_port")
    }

    // ═══ Flows ═══
    val authTokenFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN] ?: "master-companion-default-token"
    }

    val chargeLimitEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_CHARGE_LIMIT_ENABLED] ?: true
    }

    val chargeStopThresholdFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_CHARGE_STOP_THRESHOLD] ?: 80
    }

    val chargeResumeThresholdFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_CHARGE_RESUME_THRESHOLD] ?: 75
    }

    val pcIpFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PC_IP] ?: "192.168.1.100"
    }

    val pcMacFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PC_MAC] ?: "00:00:00:00:00:00"
    }

    val spotifyClientIdFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_SPOTIFY_CLIENT_ID] ?: ""
    }

    val spotifyAccessTokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_SPOTIFY_ACCESS_TOKEN]
    }

    val spotifyRefreshTokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_SPOTIFY_REFRESH_TOKEN]
    }

    val spotifyExpiresAtFlow: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_SPOTIFY_EXPIRES_AT] ?: 0L
    }

    val commandPortFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_COMMAND_PORT] ?: 8420
    }

    val audioPortFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_AUDIO_PORT] ?: 8421
    }

    // ═══ Updaters ═══
    suspend fun setAuthToken(token: String) {
        dataStore.edit { it[KEY_AUTH_TOKEN] = token }
    }

    suspend fun generateNewAuthToken(): String {
        val newToken = UUID.randomUUID().toString().replace("-", "")
        setAuthToken(newToken)
        return newToken
    }

    suspend fun setChargeLimitEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_CHARGE_LIMIT_ENABLED] = enabled }
    }

    suspend fun setChargeThresholds(stopAt: Int, resumeAt: Int) {
        dataStore.edit {
            it[KEY_CHARGE_STOP_THRESHOLD] = stopAt
            it[KEY_CHARGE_RESUME_THRESHOLD] = resumeAt
        }
    }

    suspend fun setPcNetworkDetails(ip: String, mac: String) {
        dataStore.edit {
            it[KEY_PC_IP] = ip
            it[KEY_PC_MAC] = mac
        }
    }

    suspend fun setSpotifyClientId(clientId: String) {
        dataStore.edit { it[KEY_SPOTIFY_CLIENT_ID] = clientId }
    }

    suspend fun saveSpotifyTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        dataStore.edit { prefs ->
            prefs[KEY_SPOTIFY_ACCESS_TOKEN] = accessToken
            if (refreshToken != null) {
                prefs[KEY_SPOTIFY_REFRESH_TOKEN] = refreshToken
            }
            prefs[KEY_SPOTIFY_EXPIRES_AT] = expiresAt
        }
    }

    suspend fun clearSpotifyTokens() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SPOTIFY_ACCESS_TOKEN)
            prefs.remove(KEY_SPOTIFY_REFRESH_TOKEN)
            prefs.remove(KEY_SPOTIFY_EXPIRES_AT)
        }
    }
}
