package com.example.books_kmp.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.TagRepository
import com.example.books_kmp.domain.tags.ToggleBookTagUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val book: Book? = null,
    val allTags: List<Tag> = emptyList(),
    val appliedTagIds: Set<String> = emptySet(),
    val inFlightTagIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface BookDetailError {
    data object ToggleFailed : BookDetailError

    data object DeleteFailed : BookDetailError
}

sealed interface BookDetailIntent {
    data class ToggleTag(val tagId: String) : BookDetailIntent

    data object Reload : BookDetailIntent

    data object DeleteBook : BookDetailIntent

    data object ConfirmDelete : BookDetailIntent

    data object DismissDelete : BookDetailIntent
}

sealed interface BookDetailEffect {
    data object NavigateUp : BookDetailEffect

    data class ShowError(val error: BookDetailError) : BookDetailEffect
}

class BookDetailViewModel(
    private val bookId: String,
    private val tagRepository: TagRepository,
    private val bookRepository: BookRepository,
    private val toggleBookTagUseCase: ToggleBookTagUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BookDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.tagsFlow.filterNotNull().collect { tags ->
                _uiState.update { it.copy(allTags = tags.sortedBy { tag -> tag.name }) }
            }
        }
        load()
    }

    fun onIntent(intent: BookDetailIntent) {
        when (intent) {
            is BookDetailIntent.ToggleTag -> handleToggleTag(intent.tagId)
            BookDetailIntent.Reload -> load()
            BookDetailIntent.DeleteBook ->
                _uiState.update { it.copy(showDeleteConfirm = true) }
            BookDetailIntent.DismissDelete ->
                _uiState.update { it.copy(showDeleteConfirm = false) }
            BookDetailIntent.ConfirmDelete -> handleDelete()
        }
    }

    private fun load() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            val tagsDeferred = async { tagRepository.getTags() }
            val bookDeferred = async { bookRepository.getBookById(bookId) }
            val tagsResult = tagsDeferred.await()
            val bookResult = bookDeferred.await()
            val book = (bookResult as? Result.Success)?.data
            val tags = (tagsResult as? Result.Success)?.data
            if (book != null && tags != null) {
                _uiState.update {
                    it.copy(
                        book = book,
                        allTags = tags.sortedBy { tag -> tag.name },
                        appliedTagIds = book.tags.toSet(),
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
            when (toggleBookTagUseCase(bookId, tagId, wasApplied)) {
                is Result.Success ->
                    _uiState.update { it.copy(inFlightTagIds = it.inFlightTagIds - tagId) }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            inFlightTagIds = it.inFlightTagIds - tagId,
                            appliedTagIds = if (wasApplied) it.appliedTagIds + tagId else it.appliedTagIds - tagId,
                        )
                    }
                    _effects.send(BookDetailEffect.ShowError(BookDetailError.ToggleFailed))
                }
            }
        }
    }

    private fun handleDelete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, showDeleteConfirm = false) }
        viewModelScope.launch {
            when (bookRepository.deleteBook(bookId)) {
                is Result.Success -> _effects.send(BookDetailEffect.NavigateUp)
                is Result.Failure -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _effects.send(BookDetailEffect.ShowError(BookDetailError.DeleteFailed))
                }
            }
        }
    }
}
