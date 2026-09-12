package com.vibeon.music.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.vibeon.music.data.local.LibraryRepository
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.data.settings.AudioQuality
import com.vibeon.music.data.settings.SettingsRepository
import com.vibeon.music.data.social.FriendsRepository
import com.vibeon.music.domain.model.Song
import com.vibeon.music.domain.model.StreamUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val friendsRepository: FriendsRepository,
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSec: StateFlow<Long?> = _sleepTimerRemainingSec.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    val player: ExoPlayer by lazy {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("VibeOn/${com.vibeon.music.BuildConfig.VERSION_NAME} (Android)")
            .setAllowCrossProtocolRedirects(true)
        // Muxed (progressive) streams contain a video track; without a surface the
        // video renderer stalls the player, so select audio-only tracks.
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setRendererDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, true))
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setTrackSelector(trackSelector)
            .build()
            .also { exo -> exo.addListener(playerListener) }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            syncNowPlaying()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_READY) {
                _error.value = null
            } else if (playbackState == Player.STATE_ENDED) {
                _isPlaying.value = false
                playNext()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let { id ->
                val index = _queue.value.indexOfFirst { it.videoId == id }
                if (index >= 0) {
                    _currentSong.value = _queue.value[index]
                    _queueIndex.value = index
                    recordAndResume(_queue.value[index])
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message?.let { "Playback error: $it" } ?: "Playback error"
        }
    }

    fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        _queue.value = songs
        _queueIndex.value = startIndex.coerceIn(0, songs.lastIndex)
        scope.launch { runCatching { startAt(_queueIndex.value) } }
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun playNext() {
        val next = _queueIndex.value + 1
        if (next in _queue.value.indices) {
            _queueIndex.value = next
            scope.launch { runCatching { startAt(next) } }
        }
    }

    fun playPrevious() {
        val prev = _queueIndex.value - 1
        if (prev in _queue.value.indices) {
            _queueIndex.value = prev
            scope.launch { runCatching { startAt(prev) } }
        }
    }

    fun togglePlayPause() {
        if (player.mediaItemCount == 0) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        if (player.duration > 0) player.seekTo(positionMs.coerceIn(0, player.duration))
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _currentSong.value = null
        _isPlaying.value = false
        _error.value = null
        _queueIndex.value = -1
        scope.launch { friendsRepository.clearNowPlaying() }
    }

    fun resetError() {
        _error.value = null
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSec.value = null
        if (minutes == null || minutes <= 0) {
            _sleepTimerMinutes.value = null
            return
        }
        _sleepTimerMinutes.value = minutes
        val totalSeconds = minutes * 60L
        _sleepTimerRemainingSec.value = totalSeconds
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSec.value = remaining
            }
            _sleepTimerMinutes.value = null
            _sleepTimerRemainingSec.value = null
            if (remaining <= 0 && player.mediaItemCount > 0) {
                player.pause()
            }
        }
    }

    private suspend fun startAt(index: Int) {
        val song = _queue.value.getOrNull(index) ?: return
        _currentSong.value = song
        _error.value = null
        try {
            val response = musicRepository.getStreams(song) ?: run {
                _error.value = "Could not load playback for ${song.title}"
                return
            }
            if (response.playabilityStatus != "OK") {
                _error.value = response.playabilityError ?: "Playback unavailable"
                return
            }
            val quality = settingsRepository.audioQuality.first()
            val stream = musicRepository.pickStream(response.streams, quality) ?: run {
                _error.value = "No audio stream available"
                return
            }
            player.setMediaItem(buildMediaItem(song, stream))
            player.prepare()
            player.play()
        } catch (t: Exception) {
            _error.value = "Playback error: ${t.message ?: "unexpected"}"
        }
    }

    private fun buildMediaItem(song: Song, stream: StreamUrl): MediaItem {
        val artistNames = song.artists.joinToString(", ") { it.name }
        return MediaItem.Builder()
            .setMediaId(song.videoId)
            .setUri(stream.url)
            .setMimeType(stream.mimeType.ifBlank { "audio/mp4" })
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(artistNames.ifBlank { "Unknown artist" })
                    .setArtworkUri(song.thumbnailUrl?.let { Uri.parse(it) })
                    .build()
            )
            .build()
    }

    private fun recordAndResume(song: Song) {
        scope.launch { libraryRepository.addToRecent(song) }
        scope.launch { friendsRepository.recordPlay(song) }
        syncNowPlaying()
    }

    private fun syncNowPlaying() {
        scope.launch {
            val song = _currentSong.value
            val playing = _isPlaying.value && song != null
            if (song != null) {
                friendsRepository.updateNowPlaying(song, playing)
            } else {
                friendsRepository.clearNowPlaying()
            }
        }
    }
}