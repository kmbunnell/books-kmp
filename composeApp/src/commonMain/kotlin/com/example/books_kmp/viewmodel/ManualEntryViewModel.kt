package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.SaveManualBookError
import com.example.books_kmp.domain.library.SaveManualBookUseCase
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
    val showDuplicateDialog: Boolean = false,
)

sealed interface ManualEntryIntent {
    data class SaveBook(val title: String, val author: String) : ManualEntryIntent

    data object Cancel : ManualEntryIntent

    data object AddAnyway : ManualEntryIntent

    data object DismissDuplicateDialog : ManualEntryIntent
}

sealed interface ManualEntryEffect {
    data object NavigateToLibrary : ManualEntryEffect

    data object NavigateBack : ManualEntryEffect

    data object ShowError : ManualEntryEffect
}

sealed interface ManualEntryError {
    data object TitleRequired : ManualEntryError

    data object AuthorRequired : ManualEntryError
}

class ManualEntryViewModel(
    private val saveManualBookUseCase: SaveManualBookUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ManualEntryUiState())
    val uiState: StateFlow<ManualEntryUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ManualEntryEffect>()
    val effects: SharedFlow<ManualEntryEffect> = _effects.asSharedFlow()

    private data class PendingEntry(val title: String, val author: String)

    private var pendingEntry: PendingEntry? = null

    fun onIntent(intent: ManualEntryIntent) {
        when (intent) {
            is ManualEntryIntent.SaveBook -> {
                if (_uiState.value.isLoading) return
                viewModelScope.launch { handleSaveBook(intent.title, intent.author) }
            }
            ManualEntryIntent.Cancel -> viewModelScope.launch { _effects.emit(ManualEntryEffect.NavigateBack) }
            ManualEntryIntent.AddAnyway -> {
                if (_uiState.value.isLoading) return
                viewModelScope.launch { handleAddAnyway() }
            }
            ManualEntryIntent.DismissDuplicateDialog -> _uiState.update { it.copy(showDuplicateDialog = false) }
        }
    }

    private suspend fun handleSaveBook(
        title: String,
        author: String,
    ) {
        val titleError = if (title.isBlank()) ManualEntryError.TitleRequired else null
        val authorError = if (author.isBlank()) ManualEntryError.AuthorRequired else null

        if (titleError != null || authorError != null) {
            _uiState.update { it.copy(titleError = titleError, authorError = authorError) }
            return
        }

        pendingEntry = PendingEntry(title, author)
        _uiState.update { it.copy(isLoading = true, titleError = null, authorError = null) }

        when (val result = saveManualBookUseCase(title, author)) {
            is Result.Success -> {
                _uiState.update { it.copy(isLoading = false) }
                _effects.emit(ManualEntryEffect.NavigateToLibrary)
            }
            is Result.Failure -> {
                val error = result.error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showDuplicateDialog = error is SaveManualBookError.DuplicateTitle,
                    )
                }
                if (error !is SaveManualBookError.DuplicateTitle) {
                    _effects.emit(ManualEntryEffect.ShowError)
                }
            }
        }
    }

    private suspend fun handleAddAnyway() {
        val entry = pendingEntry ?: return
        _uiState.update { it.copy(isLoading = true, showDuplicateDialog = false) }
        when (val result = saveManualBookUseCase(entry.title, entry.author, forceAdd = true)) {
            is Result.Success -> {
                _uiState.update { it.copy(isLoading = false) }
                _effects.emit(ManualEntryEffect.NavigateToLibrary)
            }
            is Result.Failure -> {
                _uiState.update { it.copy(isLoading = false) }
                _effects.emit(ManualEntryEffect.ShowError)
            }
        }
    }
}
