package com.example.books_kmp.ui.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.SaveManualBookError
import com.example.books_kmp.domain.library.SaveManualBookUseCase
import com.example.books_kmp.ui.util.launchIfIdle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManualEntryUiState(
    val isLoading: Boolean = false,
    val titleError: ManualEntryError? = null,
    val authorError: ManualEntryError? = null,
    val isbnError: ManualEntryError? = null,
    val showDuplicateDialog: Boolean = false,
)

sealed interface ManualEntryIntent {
    data class SaveBook(val title: String, val author: String, val isbn: String?) : ManualEntryIntent

    data object Cancel : ManualEntryIntent

    data object AddAnyway : ManualEntryIntent

    data object DismissDuplicateDialog : ManualEntryIntent
}

sealed interface ManualEntryEffect {
    data object NavigateToLibrary : ManualEntryEffect

    data object NavigateBack : ManualEntryEffect

    data class ShowError(val error: ManualEntryError) : ManualEntryEffect
}

sealed interface ManualEntryError {
    data object TitleRequired : ManualEntryError

    data object AuthorRequired : ManualEntryError

    data object SaveFailed : ManualEntryError

    data object IsbnInvalid : ManualEntryError
}

class ManualEntryViewModel(
    private val saveManualBookUseCase: SaveManualBookUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManualEntryUiState())
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ManualEntryEffect>()
    val effects: SharedFlow<ManualEntryEffect> = _effects.asSharedFlow()

    private data class PendingEntry(val title: String, val author: String, val isbn: String?)

    private var pendingEntry: PendingEntry? = null

    fun onIntent(intent: ManualEntryIntent) {
        when (intent) {
            is ManualEntryIntent.SaveBook ->
                launchIfIdle({
                    _uiState.value.isLoading
                }) { handleSaveBook(intent.title, intent.author, intent.isbn) }
            ManualEntryIntent.Cancel -> viewModelScope.launch { _effects.emit(ManualEntryEffect.NavigateBack) }
            ManualEntryIntent.AddAnyway -> launchIfIdle({ _uiState.value.isLoading }) { handleAddAnyway() }
            ManualEntryIntent.DismissDuplicateDialog -> {
                pendingEntry = null
                _uiState.update { it.copy(showDuplicateDialog = false) }
            }
        }
    }

    private suspend fun handleSaveBook(
        title: String,
        author: String,
        isbn: String?,
    ) {
        val titleError = if (title.isBlank()) ManualEntryError.TitleRequired else null
        val authorError = if (author.isBlank()) ManualEntryError.AuthorRequired else null
        val isbnError = if (isbn != null && isbn.length !in setOf(10, 13)) ManualEntryError.IsbnInvalid else null

        if (titleError != null || authorError != null || isbnError != null) {
            _uiState.update { it.copy(titleError = titleError, authorError = authorError, isbnError = isbnError) }
            return
        }

        pendingEntry = PendingEntry(title, author, isbn)
        _uiState.update { it.copy(isLoading = true, titleError = null, authorError = null, isbnError = null) }

        when (val result = saveManualBookUseCase(title, author, isbn)) {
            is Result.Success -> {
                _uiState.update { it.copy(isLoading = false) }
                _effects.emit(ManualEntryEffect.NavigateToLibrary)
            }
            is Result.Failure -> {
                when (result.error) {
                    SaveManualBookError.DuplicateTitle ->
                        _uiState.update { it.copy(isLoading = false, showDuplicateDialog = true) }
                    SaveManualBookError.InvalidIsbn ->
                        _uiState.update { it.copy(isLoading = false, isbnError = ManualEntryError.IsbnInvalid) }
                    SaveManualBookError.SaveFailed -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effects.emit(ManualEntryEffect.ShowError(ManualEntryError.SaveFailed))
                    }
                }
            }
        }
    }

    private suspend fun handleAddAnyway() {
        val entry = pendingEntry ?: return
        _uiState.update { it.copy(isLoading = true, showDuplicateDialog = false) }
        when (val result = saveManualBookUseCase(entry.title, entry.author, entry.isbn, forceAdd = true)) {
            is Result.Success -> {
                _uiState.update { it.copy(isLoading = false) }
                _effects.emit(ManualEntryEffect.NavigateToLibrary)
            }
            is Result.Failure -> {
                when (result.error) {
                    SaveManualBookError.InvalidIsbn ->
                        _uiState.update { it.copy(isLoading = false, isbnError = ManualEntryError.IsbnInvalid) }
                    SaveManualBookError.SaveFailed,
                    SaveManualBookError.DuplicateTitle -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effects.emit(ManualEntryEffect.ShowError(ManualEntryError.SaveFailed))
                    }
                }
            }
        }
    }
}
