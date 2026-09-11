package com.vibeon.music.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vibeon_settings")

enum class AudioQuality { LOW, NORMAL, HIGH }

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val IS_GUEST = stringPreferencesKey("is_guest")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val AUTH_USER = stringPreferencesKey("auth_user")
    }

    val audioQuality: Flow<AudioQuality> = context.settingsDataStore.data.map { prefs ->
        when (prefs[Keys.AUDIO_QUALITY]) {
            "low" -> AudioQuality.LOW
            "high" -> AudioQuality.HIGH
            else -> AudioQuality.NORMAL
        }
    }

    val isGuest: Flow<Boolean> = context.settingsDataStore.data.map {
        it[Keys.IS_GUEST]?.toBoolean() ?: false
    }

    val authToken: Flow<String?> = context.settingsDataStore.data.map {
        it[Keys.AUTH_TOKEN]
    }

    val authUser: Flow<String?> = context.settingsDataStore.data.map {
        it[Keys.AUTH_USER]
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUDIO_QUALITY] = quality.name.lowercase()
        }
    }

    suspend fun setGuest(guest: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IS_GUEST] = guest.toString()
        }
    }

    suspend fun setAuth(token: String, userJson: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token
            prefs[Keys.AUTH_USER] = userJson
        }
    }

    suspend fun clearAuth() {
        context.settingsDataStore.edit { prefs ->
            prefs -= Keys.AUTH_TOKEN
            prefs -= Keys.AUTH_USER
        }
    }
}