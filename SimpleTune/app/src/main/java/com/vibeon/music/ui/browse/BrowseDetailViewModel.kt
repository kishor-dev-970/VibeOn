package com.vibeon.music.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.domain.model.Album
import com.vibeon.music.domain.model.Artist
import com.vibeon.music.domain.model.ItemType
import com.vibeon.music.domain.model.Playlist
import com.vibeon.music.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
) : ViewModel() {

    val browseId: String = savedStateHandle.get<String>("browseId") ?: ""
    val type: ItemType = when (savedStateHandle.get<String>("type")) {
        "ALBUM" -> ItemType.ALBUM
        "PLAYLIST" -> ItemType.PLAYLIST
        "ARTIST" -> ItemType.ARTIST
        else -> ItemType.ARTIST
    }

    data class UiState(
        val loading: Boolean = true,
        val album: Album? = null,
        val playlist: Playlist? = null,
        val artist: Artist? = null,
        val songs: List<Song> = emptyList(),
        val continuation: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (type) {
                ItemType.ALBUM -> musicRepository.fetchAlbum(browseId)?.let { r ->
                    _state.value = UiState(loading = false, album = r.album, songs = r.songs, continuation = r.continuation)
                }
                ItemType.PLAYLIST -> musicRepository.fetchPlaylist(browseId)?.let { r ->
                    _state.value = UiState(loading = false, playlist = r.playlist, songs = r.songs, continuation = r.continuation)
                }
                else -> musicRepository.fetchArtist(browseId)?.let { r ->
                    _state.value = UiState(loading = false, artist = r.artist, songs = r.songs)
                }
            }
        }
    }

    fun loadMore() {
        val continuation = _state.value.continuation ?: return
        viewModelScope.launch {
            val (newSongs, next) = musicRepository.fetchSongsContinuation(continuation)
            _state.value = _state.value.copy(
                songs = _state.value.songs + newSongs,
                continuation = next,
            )
        }
    }
}