package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.TagRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val activeFilterTagIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
) {
    val filteredBooks: List<Book>
        get() =
            if (activeFilterTagIds.isEmpty()) books
            else books.filter { book -> book.tags.any { it in activeFilterTagIds } }
}

sealed interface LibraryIntent {
    data class ToggleFilter(val tagId: String) : LibraryIntent

    data object Refresh : LibraryIntent
}

class LibraryViewModel(
    private val bookRepository: BookRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
    }

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
            LibraryIntent.Refresh -> loadLibrary()
        }
    }

    private fun loadLibrary() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            coroutineScope {
                val booksDeferred = async { bookRepository.getBooksByUser() }
                val tagsDeferred = async { tagRepository.getTags() }
                val booksResult = booksDeferred.await()
                val tagsResult = tagsDeferred.await()
                if (booksResult is Result.Success && tagsResult is Result.Success) {
                    val fetchedIds = tagsResult.data.map { it.id }.toSet()
                    _uiState.update {
                        it.copy(
                            books = booksResult.data,
                            tags = tagsResult.data,
                            activeFilterTagIds = it.activeFilterTagIds intersect fetchedIds,
                            isLoading = false,
                            loadFailed = false,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            books = emptyList(),
                            tags = emptyList(),
                            isLoading = false,
                            loadFailed = true,
                        )
                    }
                }
            }
        }
    }
}
