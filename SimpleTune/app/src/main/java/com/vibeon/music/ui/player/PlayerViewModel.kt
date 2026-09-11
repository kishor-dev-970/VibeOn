package com.vibeon.music.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.vibeon.music.data.local.LibraryRepository
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val libraryRepository: LibraryRepository,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    val player = playbackManager.player

    val song: StateFlow<Song?> = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val isBuffering: StateFlow<Boolean> = playbackManager.isBuffering
    val queueSongs: StateFlow<List<Song>> = playbackManager.queue
    val queueIndex: StateFlow<Int> = playbackManager.queueIndex
    val error: StateFlow<String?> = playbackManager.error

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _position.value = player.currentPosition
                _durationMs.value = if (player.duration > 0) player.duration else 0L
                delay(500)
            }
        }
        viewModelScope.launch {
            playbackManager.currentSong.collect { song ->
                if (song != null) {
                    _isFavorite.value = libraryRepository.isFavorite(song.videoId)
                }
            }
        }
    }

    fun onTogglePlay() = playbackManager.togglePlayPause()

    fun onNext() = playbackManager.playNext()

    fun onPrevious() = playbackManager.playPrevious()

    fun onSeek(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun onToggleFavorite() {
        val song = song.value ?: return
        viewModelScope.launch {
            libraryRepository.toggleFavorite(song)
            _isFavorite.value = libraryRepository.isFavorite(song.videoId)
        }
    }

    fun cleanError() = playbackManager.resetError()

    fun toggleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}