package com.vibeon.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.data.music.SearchFilter
import com.vibeon.music.domain.model.SearchResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.SONGS)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
        _searching.value = true
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            if (value.isBlank()) {
                _results.value = SearchResults()
                _searching.value = false
                _error.value = null
                return@launch
            }
            performSearch()
        }
    }

    fun onFilterChange(value: SearchFilter) {
        _filter.value = value
        if (_query.value.isBlank()) return
        viewModelScope.launch { performSearch() }
    }

    fun loadMore(continuation: String) {
        viewModelScope.launch {
            val more = musicRepository.searchMore(continuation)
            if (more.songs.isNotEmpty()) {
                _results.value = _results.value.copy(
                    songs = _results.value.songs + more.songs
                )
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun performSearch() {
        _loading.value = true
        _error.value = null
        val result = musicRepository.search(_query.value.trim(), _filter.value)
        _results.value = result
        _loading.value = false
        _searching.value = false
        if (result.isEmpty) {
            _error.value = "No results for \"${_query.value.trim()}\""
        }
    }
}