package com.vibeon.music.data.auth

import com.vibeon.music.data.network.AuthResponse
import com.vibeon.music.data.network.ServerUser
import com.vibeon.music.data.network.SocialApiClient
import com.vibeon.music.data.settings.SettingsRepository
import com.vibeon.music.domain.model.AppUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: SocialApiClient,
    private val settings: SettingsRepository,
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    init {
        appScope.launch {
            val token = settings.authToken.first()
            val userJson = settings.authUser.first()
            _currentUser.value = userJson
                ?.let { runCatching { SocialApiClient.json.decodeFromString<AppUser>(it) }.getOrNull() }
            if (token == null) _currentUser.value = null
            _sessionReady.value = true

            // Session tokens expire after 30 days. If the server rejects the stored token,
            // renew it silently with the stored name (the server re-issues a token for the
            // same first + last name). Only sign out if renewal fails, so the user does not
            // silently get empty friends lists and failed now-playing updates.
            if (token != null && _currentUser.value != null && api.validateSession() == false) {
                val user = _currentUser.value
                val renewed = user != null &&
                    user.firstName.isNotBlank() &&
                    user.lastName.isNotBlank() &&
                    signInWithName(user.firstName, user.lastName).isSuccess
                if (!renewed) {
                    settings.clearAuth()
                    _currentUser.value = null
                }
            }
        }
    }

    suspend fun signInWithName(firstName: String, lastName: String): Result<AppUser> =
        api.signInWithName(firstName, lastName)
            .onSuccess { response ->
                val user = response.user.toAppUser()
                settings.setAuth(response.token, SocialApiClient.json.encodeToString(user))
                _currentUser.value = user
            }
            .map { it.user.toAppUser() }

    suspend fun signOut() {
        api.clearNowPlaying()
        settings.clearAuth()
        _currentUser.value = null
    }
}

private fun ServerUser.toAppUser(): AppUser =
    AppUser(
        id = id,
        name = name ?: listOfNotNull(firstName, lastName).joinToString(" "),
        firstName = firstName ?: "",
        lastName = lastName ?: "",
        code = code ?: "",
        avatarUrl = avatarUrl,
    )