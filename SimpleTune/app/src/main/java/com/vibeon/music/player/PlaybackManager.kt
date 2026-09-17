package com.vibeon.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
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

    private var mediaController: MediaController? = null

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

    // Increments on every user-initiated play/next/previous so stale load
    // coroutines (which can race a completed song and drop the next track)
    // abort instead of fighting the latest request.
    private var playGeneration = 0

    // Maximum tracks to probe when resolving the "next" window (skips unplayable ones).
    private val preloadAttempts = 3

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
                    armNextTrack()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message?.let { "Playback error: $it" } ?: "Playback error"
            // Transient per-track failures (network drop, expired URL, codec hiccup) must not
            // silently halt the queue: skip on to the next track.
            playNext()
        }
    }

    fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        _queue.value = songs
        playAt(startIndex.coerceIn(0, songs.lastIndex))
    }

    fun addToQueue(song: Song) {
        _queue.value = _queue.value + song
    }

    fun playNext() {
        val next = _queueIndex.value + 1
        if (next in _queue.value.indices) playAt(next)
    }

    fun playPrevious() {
        val prev = _queueIndex.value - 1
        if (prev in _queue.value.indices) playAt(prev)
    }

    private fun playAt(index: Int) {
        val generation = ++playGeneration
        _queueIndex.value = index
        scope.launch { runCatching { startAt(index, generation) } }
    }

    fun togglePlayPause() {
        if (player.mediaItemCount == 0) return
        if (player.isPlaying) player.pause() else {
            ensureMediaSession()
            player.play()
        }
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
        releaseMediaSession()
    }

    /**
     * Connects a MediaController to the PlaybackService session. In media3 1.5.x a session created
     * inside a MediaSessionService only gets attached (and its media notification posted) once a
     * controller connects to it, so we keep one connected for the whole playback session. The
     * connection also starts the service, which then promotes itself to the foreground while playing.
     */
    private fun ensureMediaSession() {
        if (mediaController != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val builder = MediaController.Builder(context, token)
        val connectionFuture = builder.buildAsync()
        connectionFuture.addListener(
            {
                runCatching { mediaController = connectionFuture.get() }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun releaseMediaSession() {
        val controller = mediaController ?: return
        mediaController = null
        controller.release()
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

    private suspend fun startAt(index: Int, generation: Int) {
        val song = _queue.value.getOrNull(index) ?: return
        _currentSong.value = song
        _error.value = null
        val response = runCatching { musicRepository.getStreams(song) }.getOrNull()
        if (generation != playGeneration || _queueIndex.value != index) return
        val stream = if (response != null && response.playabilityStatus == "OK") {
            runCatching {
                musicRepository.pickStream(response.streams, settingsRepository.audioQuality.first())
            }.getOrNull()
        } else {
            response?.playabilityError?.takeIf { it.isNotBlank() }?.let { _error.value = it }
            null
        }
        if (stream == null) {
            if (_error.value == null) _error.value = "Could not load playback for ${song.title}"
            // Skip unplayable tracks instead of stalling the queue at the end of a song.
            if (index + 1 in _queue.value.indices) playAt(index + 1)
            return
        }
        try {
            player.stop()
            player.clearMediaItems()
            player.setMediaItem(buildMediaItem(song, stream))
            player.prepare()
            ensureMediaSession()
            player.play()
            // Keep a second resolved track on the timeline so the media session exposes a
            // multi-item queue: modern SystemUI derives the Next control from the timeline
            // (not notification actions) and hides it for a single-item queue, and ExoPlayer
            // then auto-advances without a gap.
            armNextTrack()
        } catch (t: Exception) {
            _error.value = t.message?.let { "Playback error: $it" } ?: "Playback error"
        }
    }

    /**
     * Keeps the player timeline at two resolved windows (current + next). Called after starting
     * a track and after every transition into a loaded window. Trims played windows that are
     * before the current one and appends the following track once it is resolved, so the queue
     * stays tight and the Next control never disappears between songs.
     */
    private fun armNextTrack() {
        if (player.mediaItemCount == 0) return
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) return
        val from = _queueIndex.value + 1
        if (from !in _queue.value.indices) return
        val generation = playGeneration
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        scope.launch {
            val next = resolveNextPlayable(from, generation) ?: return@launch
            if (generation != playGeneration) return@launch
            player.addMediaItem(next.second)
        }
    }

    /**
     * Resolves the next playable track starting at [fromIndex], skipping unplayable ones (same
     * skip logic as [startAt], bounded by [preloadAttempts]). Returns the queue index and its
     * built MediaItem, or null when nothing further in the queue can play.
     */
    private suspend fun resolveNextPlayable(fromIndex: Int, generation: Int): Pair<Int, MediaItem>? {
        val queue = _queue.value
        var i = fromIndex
        var attempts = 0
        while (i in queue.indices && attempts < preloadAttempts) {
            if (generation != playGeneration) return null
            val song = queue[i]
            val response = runCatching { musicRepository.getStreams(song) }.getOrNull()
            if (generation != playGeneration) return null
            val stream = if (response != null && response.playabilityStatus == "OK") {
                runCatching { musicRepository.pickStream(response.streams, settingsRepository.audioQuality.first()) }.getOrNull()
            } else {
                null
            }
            if (stream != null) return i to buildMediaItem(song, stream)
            attempts++
            i++
        }
        return null
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