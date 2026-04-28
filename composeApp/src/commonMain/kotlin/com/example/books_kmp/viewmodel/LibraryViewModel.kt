package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tags: List<Tag> = emptyList(),
    val activeFilterTagIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

sealed interface LibraryIntent {
    data class ToggleFilter(val tagId: String) : LibraryIntent

    data object RefreshTags : LibraryIntent
}

class LibraryViewModel(private val tagRepository: TagRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.ToggleFilter ->
                _uiState.update {
                    val newSet =
                        if (intent.tagId in it.activeFilterTagIds) {
                            it.activeFilterTagIds - intent.tagId
                        } else {
                            it.activeFilterTagIds + intent.tagId
                        }
                    it.copy(activeFilterTagIds = newSet)
                }
            LibraryIntent.RefreshTags -> loadTags()
        }
    }

    private fun loadTags() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = tagRepository.getTags()) {
                is Result.Success -> {
                    val fetchedIds = result.data.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            tags = result.data,
                            activeFilterTagIds = it.activeFilterTagIds intersect fetchedIds,
                            isLoading = false,
                            loadFailed = false,
                        )
                    }
                }
                is Result.Failure ->
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }
}
