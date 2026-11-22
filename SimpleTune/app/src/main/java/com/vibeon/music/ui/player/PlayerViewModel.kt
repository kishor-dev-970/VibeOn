package com.vibeon.music.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.local.LibraryRepository
import com.vibeon.music.data.lyrics.LyricsRepository
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.data.settings.AudioQuality
import com.vibeon.music.data.settings.SettingsRepository
import com.vibeon.music.domain.model.LyricLine
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class LyricsUiState(
    val lines: List<LyricLine> = emptyList(),
    val synced: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val libraryRepository: LibraryRepository,
    private val musicRepository: MusicRepository,
    private val lyricsRepository: LyricsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val player = playbackManager.player

    val song: StateFlow<Song?> = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val isBuffering: StateFlow<Boolean> = playbackManager.isBuffering
    val queueSongs: StateFlow<List<Song>> = playbackManager.queue
    val queueIndex: StateFlow<Int> = playbackManager.queueIndex
    val shuffle: StateFlow<Boolean> = playbackManager.shuffle
    val repeatMode: StateFlow<Int> = playbackManager.repeatMode
    val error: StateFlow<String?> = playbackManager.error

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    val audioQuality: StateFlow<AudioQuality> = MutableStateFlow(AudioQuality.NORMAL).also { holder ->
        viewModelScope.launch {
            settingsRepository.audioQuality.collect { holder.value = it }
        }
    }

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _lyrics = MutableStateFlow(LyricsUiState())
    val lyrics: StateFlow<LyricsUiState> = _lyrics.asStateFlow()

    private val _activeLyricIndex = MutableStateFlow(-1)
    val activeLyricIndex: StateFlow<Int> = _activeLyricIndex.asStateFlow()

    private var lyricsSongId: String? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                _position.value = player.currentPosition
                _durationMs.value = if (player.duration > 0) player.duration else 0L
                updateActiveLyric()
                delay(500)
            }
        }
        viewModelScope.launch {
            playbackManager.currentSong.collect { current ->
                if (current == null) {
                    lyricsSongId = null
                    _lyrics.value = LyricsUiState()
                    _activeLyricIndex.value = -1
                    return@collect
                }
                _isFavorite.value = libraryRepository.isFavorite(current.videoId)
                if (lyricsSongId == current.videoId) return@collect
                loadLyrics(current)
            }
        }
    }

    private suspend fun loadLyrics(song: Song) {
        lyricsSongId = song.videoId
        _activeLyricIndex.value = -1
        _lyrics.value = LyricsUiState(loading = true)
        val data = withContext(Dispatchers.IO) {
            runCatching { lyricsRepository.fetch(song) }.getOrNull()
        }
        // A different song may have started while the request was in flight.
        if (lyricsSongId != song.videoId) return
        _lyrics.value = when {
            data == null || data.lines.isEmpty() -> LyricsUiState(
                message = if (data?.instrumental == true) {
                    "This track is instrumental"
                } else {
                    "No lyrics available for this song"
                },
            )
            else -> LyricsUiState(lines = data.lines, synced = data.synced)
        }
    }

    private fun updateActiveLyric() {
        val state = _lyrics.value
        if (!state.synced || state.lines.isEmpty()) return
        val now = _position.value
        var index = -1
        for (i in state.lines.indices) {
            if (state.lines[i].timeMs <= now) index = i else break
        }
        if (index != _activeLyricIndex.value) _activeLyricIndex.value = index
    }

    fun retryLyrics() {
        val current = song.value ?: return
        lyricsSongId = null
        viewModelScope.launch { loadLyrics(current) }
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

    fun onToggleShuffle() = playbackManager.toggleShuffle()

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality)
        }
    }

    fun onJumpToQueue(index: Int) = playbackManager.jumpToQueueIndex(index)

    fun onMoveQueue(from: Int, to: Int) = playbackManager.moveQueueItem(from, to)

    fun onRemoveQueue(index: Int): Pair<Song?, PlaybackManager.QueueSnapshot> {
        val snapshot = playbackManager.captureQueue()
        return playbackManager.removeFromQueue(index) to snapshot
    }

    fun onRestoreQueue(snapshot: PlaybackManager.QueueSnapshot) =
        playbackManager.restoreQueue(snapshot)

    fun cleanError() = playbackManager.resetError()

    fun toggleRepeatMode() = playbackManager.cycleRepeatMode()
}
