package com.vibeon.music.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.local.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistCount(val name: String, val count: Int)

data class ListeningStats(
    val uniqueSongs: Int = 0,
    val favorites: Int = 0,
    val totalMinutes: Long = 0,
    val topArtists: List<ArtistCount> = emptyList(),
)

@HiltViewModel
class ListeningStatsViewModel @Inject constructor(
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _stats = MutableStateFlow(ListeningStats())
    val stats: StateFlow<ListeningStats> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            combine(libraryRepository.recent, libraryRepository.favorites) { recent, favorites ->
                val artistCounts = recent
                    .flatMap { song -> song.artists.map { it.name } }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .take(8)
                    .map { ArtistCount(it.first, it.second) }
                ListeningStats(
                    uniqueSongs = recent.size,
                    favorites = favorites.size,
                    totalMinutes = recent.sumOf { it.duration / 60 },
                    topArtists = artistCounts,
                )
            }.collect { _stats.value = it }
        }
    }
}
