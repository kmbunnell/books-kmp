package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.BookRepository
import com.example.books_kmp.domain.model.Book
import com.example.books_kmp.domain.model.Tag
import com.example.books_kmp.domain.tags.TagRepository
import com.example.books_kmp.util.normalise
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val activeFilterTagIds: Set<String> = emptySet(),
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

sealed interface LibraryIntent {
    data class ToggleFilter(val tagId: String) : LibraryIntent

    data class ChangeSortOrder(val order: SortOrder) : LibraryIntent

    data class ChangeSearchQuery(val query: String) : LibraryIntent

    data object ClearFilters : LibraryIntent

    data object Refresh : LibraryIntent
}

enum class SortOrder { TITLE_ASC, AUTHOR_ASC }

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
                _uiState.update { state ->
                    val newSet =
                        if (intent.tagId in state.activeFilterTagIds) {
                            state.activeFilterTagIds - intent.tagId
                        } else {
                            state.activeFilterTagIds + intent.tagId
                        }
                    state.copy(
                        activeFilterTagIds = newSet,
                        filteredBooks = computeFilteredBooks(state.books, newSet, state.sortOrder, state.searchQuery),
                    )
                }
            is LibraryIntent.ChangeSortOrder ->
                _uiState.update { state ->
                    state.copy(
                        sortOrder = intent.order,
                        filteredBooks =
                            computeFilteredBooks(
                                state.books,
                                state.activeFilterTagIds,
                                intent.order,
                                state.searchQuery
                            ),
                    )
                }
            is LibraryIntent.ChangeSearchQuery ->
                _uiState.update { state ->
                    state.copy(
                        searchQuery = intent.query,
                        filteredBooks =
                            computeFilteredBooks(
                                state.books,
                                state.activeFilterTagIds,
                                state.sortOrder,
                                intent.query
                            ),
                    )
                }
            LibraryIntent.ClearFilters ->
                _uiState.update { state ->
                    state.copy(
                        activeFilterTagIds = emptySet(),
                        searchQuery = "",
                        filteredBooks = computeFilteredBooks(state.books, emptySet(), state.sortOrder, ""),
                    )
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
                    _uiState.update { state ->
                        val newActiveIds = state.activeFilterTagIds intersect fetchedIds
                        state.copy(
                            books = booksResult.data,
                            tags = tagsResult.data,
                            activeFilterTagIds = newActiveIds,
                            filteredBooks =
                                computeFilteredBooks(
                                    booksResult.data,
                                    newActiveIds,
                                    state.sortOrder,
                                    state.searchQuery
                                ),
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

    private fun computeFilteredBooks(
        books: List<Book>,
        activeFilterTagIds: Set<String>,
        sortOrder: SortOrder,
        searchQuery: String,
    ): List<Book> {
        var result = books
        if (activeFilterTagIds.isNotEmpty()) {
            result = result.filter { book -> book.tags.containsAll(activeFilterTagIds) }
        }
        if (searchQuery.isNotBlank()) {
            val normQuery = normalise(searchQuery).lowercase()
            result =
                result.filter { book ->
                    normalise(book.title).lowercase().contains(normQuery) ||
                        book.authors.any { normalise(it).lowercase().contains(normQuery) }
                }
        }
        result =
            when (sortOrder) {
                SortOrder.TITLE_ASC -> result.sortedBy { it.title }
                SortOrder.AUTHOR_ASC ->
                    result.sortedBy {
                        it.authors.firstOrNull()?.trim()?.split(" ")?.lastOrNull() ?: ""
                    }
            }
        return result
    }
}
