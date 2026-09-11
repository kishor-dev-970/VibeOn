package com.vibeon.music.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.auth.AuthRepository
import com.vibeon.music.domain.model.AppUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUser: StateFlow<AppUser?> = authRepository.currentUser
    val sessionReady: StateFlow<Boolean> = authRepository.sessionReady

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signIn(firstName: String, lastName: String) {
        val first = firstName.trim()
        val last = lastName.trim()
        if (first.isEmpty() || last.isEmpty()) {
            _error.value = "Please enter your first and last name."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            authRepository.signInWithName(first, last)
                .onFailure { _error.value = it.message ?: "Could not sign in" }
            _busy.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}