package com.vibeon.music.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.auth.AuthRepository
import com.vibeon.music.data.network.SocialApiClient
import com.vibeon.music.data.settings.AudioQuality
import com.vibeon.music.data.settings.SettingsRepository
import com.vibeon.music.domain.model.AppUser
import com.vibeon.music.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val playbackManager: PlaybackManager,
    private val api: SocialApiClient,
) : ViewModel() {

    val user: StateFlow<AppUser?> = authRepository.currentUser

    val audioQuality: StateFlow<AudioQuality> = MutableStateFlow(AudioQuality.NORMAL).also { holder ->
        viewModelScope.launch {
            settingsRepository.audioQuality.collect { holder.value = it }
        }
    }

    val sleepTimerMinutes: StateFlow<Int?> = playbackManager.sleepTimerMinutes
    val sleepTimerRemainingSec: StateFlow<Long?> = playbackManager.sleepTimerRemainingSec

    private val _busySignOut = MutableStateFlow(false)
    val busySignOut: StateFlow<Boolean> = _busySignOut.asStateFlow()

    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate: StateFlow<Boolean> = _checkingUpdate.asStateFlow()

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality)
        }
    }

    fun setSleepTimer(minutes: Int?) {
        playbackManager.setSleepTimer(minutes)
    }

    fun signOut() {
        viewModelScope.launch {
            _busySignOut.value = true
            authRepository.signOut()
            _busySignOut.value = false
        }
    }

    fun checkForUpdates(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _checkingUpdate.value = true
            val tag = api.latestReleaseTag()
            _checkingUpdate.value = false
            onResult(tag)
        }
    }
}