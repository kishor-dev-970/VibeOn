package com.vibeon.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.local.LibraryRepository
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.domain.model.Artist
import com.vibeon.music.domain.model.BrowseItem
import com.vibeon.music.domain.model.HomeChip
import com.vibeon.music.domain.model.HomeSection
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val libraryRepository: LibraryRepository,
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _listeningMinutes = MutableStateFlow(0L)
    val listeningMinutes: StateFlow<Long> = _listeningMinutes.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepository.favorites.collect { list ->
                _favorites.value = list.map { it.videoId }.toSet()
            }
        }
        viewModelScope.launch {
            libraryRepository.recent.collect { list ->
                _listeningMinutes.value = list.sumOf { it.duration / 60 }
            }
        }
    }

    private val _sections = MutableStateFlow<List<HomeSection>>(emptyList())
    val sections: StateFlow<List<HomeSection>> = _sections.asStateFlow()

    private val _chips = MutableStateFlow<List<HomeChip>>(emptyList())
    val chips: StateFlow<List<HomeChip>> = _chips.asStateFlow()

    private val _selectedChip = MutableStateFlow<String?>(null)
    val selectedChip: StateFlow<String?> = _selectedChip.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation.asStateFlow()

    // Continuation tokens are bound to the visitor id YouTube returned with the
    // browse response; echo it on the next page request or the feed comes back empty.
    private val _continuationVisitor = MutableStateFlow<String?>(null)

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    init {
        loadHome()
    }

    fun selectChip(chip: HomeChip) {
        // Always reload, including when the chip is already selected: a re-tap is how
        // the user asks for a fresh set of songs in that genre.
        _selectedChip.value = chip.label
        loadHome(chip.params)
    }

    fun loadHome(params: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = musicRepository.fetchHome(params)
            if (result != null) {
                _sections.value = result.sections
                _chips.value = result.chips
                _continuation.value = result.continuation
                _continuationVisitor.value = result.visitorData
            }
            if (_sections.value.isEmpty()) {
                _error.value = "Could not load home. Check your connection."
            }
            _loading.value = false
        }
    }

    fun refresh() {
        val currentParams = _chips.value.firstOrNull { it.label == _selectedChip.value }?.params
        viewModelScope.launch {
            _refreshing.value = true
            val result = musicRepository.fetchHome(currentParams)
            if (result != null) {
                _sections.value = result.sections
                _chips.value = result.chips
                _continuation.value = result.continuation
                _continuationVisitor.value = result.visitorData
                _error.value = null
            }
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val token = _continuation.value ?: return
        if (!_loadingMore.compareAndSet(false, true)) return
        viewModelScope.launch {
            val result = musicRepository.fetchHomeMore(token, _continuationVisitor.value)
            if (result != null) {
                val sections = result.sections.filter { it.items.isNotEmpty() }.toMutableList()
                if (sections.isNotEmpty()) {
                    val existing = _sections.value.toMutableList()
                    sections.forEach { section ->
                        val index = existing.indexOfFirst { it.title == section.title }
                        if (index >= 0) {
                            // Rotation can shift which cards were already shown, so drop
                            // anything the continuation sends back that we already have.
                            val known = existing[index].items.map {
                                it.videoId ?: it.browseId ?: it.title
                            }.toSet()
                            val fresh = section.items.filterNot { item ->
                                (item.videoId ?: item.browseId ?: item.title) in known
                            }
                            if (fresh.isNotEmpty()) {
                                existing[index] = existing[index].copy(
                                    items = existing[index].items + fresh,
                                )
                            }
                        } else {
                            existing.add(section)
                        }
                    }
                    _sections.value = existing
                }
                // Set the next token before releasing the guard so a second trigger
                // can't load the same page twice.
                _continuation.value = result.continuation
                // Keep the same visitor id across pages — it is bound to the token chain.
            }
            _loadingMore.value = false
        }
    }

    fun playSection(section: HomeSection) {
        val songs = section.items
            .filter { it.videoId != null }
            .map { it.toSong() }
        if (songs.isEmpty()) return
        playbackManager.playQueue(songs, 0)
    }

    /** "Infinite Radio": an endless-feeling shuffled queue from the whole feed. */
    fun playInfiniteRadio() {
        val songs = _sections.value
            .flatMap { it.items }
            .filter { it.videoId != null }
            .map { it.toSong() }
            .distinctBy { it.videoId }
        if (songs.isNotEmpty()) playbackManager.playQueue(songs.shuffled(), 0)
    }

    fun playSong(item: BrowseItem) {
        if (item.videoId == null) return
        playbackManager.playSong(item.toSong())
    }

    fun toggleLike(item: BrowseItem) {
        if (item.videoId == null) return
        viewModelScope.launch {
            val liked = libraryRepository.toggleFavorite(item.toSong())
            _favorites.value = if (liked) {
                _favorites.value + item.videoId
            } else {
                _favorites.value - item.videoId
            }
        }
    }

    fun addToQueue(item: BrowseItem) {
        if (item.videoId == null) return
        playbackManager.addToQueue(item.toSong())
    }

    private fun BrowseItem.toSong() = Song(
        videoId = videoId.orEmpty(),
        title = title,
        artists = (artists.map { Artist(name = it) }.ifEmpty {
            subtitle.split(" • ", " · ").firstOrNull()?.let { listOf(Artist(name = it)) } ?: emptyList()
        }),
        thumbnailUrl = thumbnailUrl,
    )
}