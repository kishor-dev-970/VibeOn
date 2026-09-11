package com.vibeon.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeon.music.data.music.MusicRepository
import com.vibeon.music.domain.model.HomeChip
import com.vibeon.music.domain.model.HomeSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

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

    init {
        loadHome()
    }

    fun selectChip(chip: HomeChip) {
        if (_selectedChip.value == chip.label) return
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
                _error.value = null
            }
            _refreshing.value = false
        }
    }

    fun loadMore() {
        val token = _continuation.value ?: return
        viewModelScope.launch {
            val result = musicRepository.fetchHomeMore(token)
            if (result != null) {
                val sections = result.sections.filter { it.items.isNotEmpty() }.toMutableList()
                if (sections.isNotEmpty()) {
                    val existing = _sections.value.toMutableList()
                    sections.forEach { section ->
                        val index = existing.indexOfFirst { it.title == section.title }
                        if (index >= 0) {
                            existing[index] = existing[index].copy(items = existing[index].items + section.items)
                        } else {
                            existing.add(section)
                        }
                    }
                    _sections.value = existing
                }
                _continuation.value = result.continuation
            }
        }
    }
}