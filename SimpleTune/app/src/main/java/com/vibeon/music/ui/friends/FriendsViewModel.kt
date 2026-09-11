package com.vibeon.music.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.social.FriendsRepository
import com.vibeon.music.domain.model.FriendActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
) : ViewModel() {

    private val _friends = MutableStateFlow<List<FriendActivity>>(emptyList())
    val friends: StateFlow<List<FriendActivity>> = _friends.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        viewModelScope.launch {
            _friends.value = friendsRepository.friendActivity.value
            _loading.value = false
        }
    }

    fun startPolling() {
        if (pollingJob != null) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                poll()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            poll()
            _refreshing.value = false
        }
    }

    private suspend fun poll() {
        val listed = friendsRepository.fetchFriendsActivity()
        _friends.value = listed
        _loading.value = false
    }

    companion object {
        const val POLL_INTERVAL_MS = 30_000L
    }

    private var pollingJob: kotlinx.coroutines.Job? = null
}