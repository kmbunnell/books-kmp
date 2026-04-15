package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
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
)

sealed interface ManualEntryIntent {
    data class SaveBook(val title: String, val author: String) : ManualEntryIntent

    data object Cancel : ManualEntryIntent
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

    fun onIntent(intent: ManualEntryIntent) {
        viewModelScope.launch {
            when (intent) {
                is ManualEntryIntent.SaveBook -> handleSaveBook(intent.title, intent.author)
                ManualEntryIntent.Cancel -> _effects.emit(ManualEntryEffect.NavigateBack)
            }
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

        _uiState.update { it.copy(isLoading = true, titleError = null, authorError = null) }

        when (val result = saveManualBookUseCase(title, author)) {
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
