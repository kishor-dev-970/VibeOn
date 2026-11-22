package com.vibeon.music.ui.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.domain.model.Artist
import com.vibeon.music.domain.model.BrowseItem
import com.vibeon.music.domain.model.ExploreTile
import com.vibeon.music.domain.model.HomeSection
import com.vibeon.music.domain.model.Song
import com.vibeon.music.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Powers both the Discover landing ("discover") and the Genres entry point
 * ("genres"). Tapping a tile drills into a category in place, so no fragile
 * encoded `params` have to survive navigation arguments.
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    private var mode: String = savedStateHandle.get<String>("mode") ?: "discover"
    private var browseId: String? = savedStateHandle.get<String>("browseId")
    private var params: String? = savedStateHandle.get<String>("params")

    private val landingTitle: String
        get() = if (mode == "genres") "Genres" else "Discover"

    private val _title = MutableStateFlow(savedStateHandle.get<String>("label") ?: landingTitle)
    val title: StateFlow<String> = _title.asStateFlow()

    private val _inCategory = MutableStateFlow(browseId != null)
    val inCategory: StateFlow<Boolean> = _inCategory.asStateFlow()

    private val _tiles = MutableStateFlow<List<ExploreTile>>(emptyList())
    val tiles: StateFlow<List<ExploreTile>> = _tiles.asStateFlow()

    private val _sections = MutableStateFlow<List<HomeSection>>(emptyList())
    val sections: StateFlow<List<HomeSection>> = _sections.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun setMode(value: String) {
        if (mode == value) return
        mode = value
        browseId = null
        params = null
        _inCategory.value = false
        _title.value = landingTitle
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = when {
                browseId != null -> musicRepository.fetchExploreCategory(browseId!!, params)
                mode == "genres" -> musicRepository.fetchExploreCategory("FEmusic_moods_and_genres", null)
                else -> musicRepository.fetchExplore()
            }
            if (result != null) {
                _tiles.value = result.tiles
                _sections.value = result.sections
            } else {
                _error.value = "Couldn't load $_title. Check your connection."
            }
            _loading.value = false
        }
    }

    fun openCategory(tile: ExploreTile) {
        browseId = tile.browseId
        params = tile.params
        _inCategory.value = true
        _title.value = tile.label
        load()
    }

    fun backToLanding() {
        browseId = null
        params = null
        _inCategory.value = false
        _title.value = landingTitle
        load()
    }

    fun playItem(item: BrowseItem) {
        val videoId = item.videoId ?: return
        playbackManager.playSong(
            Song(
                videoId = videoId,
                title = item.title,
                artists = (item.artists.map { Artist(name = it) }.ifEmpty {
                    item.subtitle.split(" • ", " · ").firstOrNull()?.let { listOf(Artist(name = it)) } ?: emptyList()
                }),
                thumbnailUrl = item.thumbnailUrl,
            )
        )
    }
}
