package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.TagRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val allTags: List<Tag> = emptyList(),
    val appliedTagIds: Set<String> = emptySet(),
    val inFlightTagIds: Set<String> = emptySet(),
    val tagToggleError: BookDetailError? = null,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

sealed interface BookDetailError {
    data object ToggleFailed : BookDetailError
}

sealed interface BookDetailIntent {
    data class ToggleTag(val tagId: String) : BookDetailIntent

    data object DismissTagToggleError : BookDetailIntent

    data object Reload : BookDetailIntent
}

class BookDetailViewModel(
    private val bookId: String,
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: BookDetailIntent) {
        when (intent) {
            is BookDetailIntent.ToggleTag -> handleToggleTag(intent.tagId)
            BookDetailIntent.DismissTagToggleError ->
                _uiState.update { it.copy(tagToggleError = null) }
            BookDetailIntent.Reload -> load()
        }
    }

    private fun load() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val tagsDeferred = async { tagRepository.getTags() }
            val appliedDeferred = async { tagRepository.getTagsForBook(bookId) }
            val tagsResult = tagsDeferred.await()
            val appliedResult = appliedDeferred.await()
            if (tagsResult is Result.Success && appliedResult is Result.Success) {
                _uiState.update {
                    it.copy(
                        allTags = tagsResult.data,
                        appliedTagIds = appliedResult.data.map { it.id }.toSet(),
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private fun handleToggleTag(tagId: String) {
        val current = _uiState.value
        if (tagId in current.inFlightTagIds) return

        val wasApplied = tagId in current.appliedTagIds

        _uiState.update {
            it.copy(
                inFlightTagIds = it.inFlightTagIds + tagId,
                appliedTagIds = if (wasApplied) it.appliedTagIds - tagId else it.appliedTagIds + tagId,
            )
        }

        viewModelScope.launch {
            val result =
                if (wasApplied) {
                    tagRepository.removeTagFromBook(bookId, tagId)
                } else {
                    tagRepository.addTagToBook(bookId, tagId)
                }

            when (result) {
                is Result.Success ->
                    _uiState.update { it.copy(inFlightTagIds = it.inFlightTagIds - tagId) }
                is Result.Failure ->
                    _uiState.update {
                        it.copy(
                            inFlightTagIds = it.inFlightTagIds - tagId,
                            appliedTagIds = if (wasApplied) it.appliedTagIds + tagId else it.appliedTagIds - tagId,
                            tagToggleError = BookDetailError.ToggleFailed,
                        )
                    }
            }
        }
    }
}
