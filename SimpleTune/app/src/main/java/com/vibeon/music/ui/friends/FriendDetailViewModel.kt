package com.vibeon.music.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.social.FriendsRepository
import com.vibeon.music.domain.model.FriendActivity
import com.vibeon.music.domain.model.FriendStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendsRepository: FriendsRepository,
) : ViewModel() {

    val userId: String = savedStateHandle["userId"] ?: ""

    private val _friend = MutableStateFlow<FriendActivity?>(null)
    val friend: StateFlow<FriendActivity?> = _friend.asStateFlow()

    private val _stats = MutableStateFlow<FriendStats?>(null)
    val stats: StateFlow<FriendStats?> = _stats.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        _friend.value = friendsRepository.friendActivity.value.firstOrNull { it.user.id == userId }
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _loading.value = true
            _stats.value = friendsRepository.fetchFriendStats(userId)
            _loading.value = false
        }
    }

    fun onFriendChanged(updated: List<FriendActivity>) {
        _friend.value = updated.firstOrNull { it.user.id == userId }
    }

    fun formatTime(minutes: Long): String = when {
        minutes < 60 -> "${minutes}m"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m > 0) "${h}h ${m}m" else "${h}h"
        }
    }
}