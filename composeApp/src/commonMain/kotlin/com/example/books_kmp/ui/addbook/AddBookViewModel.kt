package com.example.books_kmp.ui.addbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.AddBookError
import com.example.books_kmp.domain.library.AddBookUseCase
import com.example.books_kmp.domain.library.BarcodeScanError
import com.example.books_kmp.domain.library.LookupBookUseCase
import com.example.books_kmp.domain.library.LookupByTitleError
import com.example.books_kmp.domain.library.LookupByTitleUseCase
import com.example.books_kmp.domain.model.BookLookupData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LookupMode { ISBN, Title }

data class AddBookUiState(
    val lookupMode: LookupMode = LookupMode.ISBN,
    val isbn: String = "",
    val titleQuery: String = "",
    val titleResults: List<BookLookupData> = emptyList(),
    val isLoading: Boolean = false,
    val error: AddBookScreenError? = null,
    val showDuplicateDialog: Boolean = false,
    val foundBook: BookLookupData? = null,
    val isScanning: Boolean = false,
    val isbnFormatError: Boolean = false,
    val pendingAddAndTag: Boolean = false,
)

sealed interface AddBookScreenError {
    data object NetworkError : AddBookScreenError

    data object Unauthenticated : AddBookScreenError

    data object RateLimited : AddBookScreenError

    data object NotFound : AddBookScreenError

    data object LibraryLimitReached : AddBookScreenError

    data object ScanCameraPermissionDenied : AddBookScreenError

    data object ScanHardwareUnavailable : AddBookScreenError

    data object ScanUnknownError : AddBookScreenError
}

sealed interface AddBookIntent {
    data class IsbnChanged(val isbn: String) : AddBookIntent

    data class LookupIsbn(val isbn: String) : AddBookIntent

    data object ConfirmBook : AddBookIntent

    data object AddAndTag : AddBookIntent

    data object CancelBookPreview : AddBookIntent

    data object DismissDuplicateDialog : AddBookIntent

    data object AddAnyway : AddBookIntent

    data object Retry : AddBookIntent

    data object EnterManually : AddBookIntent

    data object StartScan : AddBookIntent

    data class BarcodeScanned(val isbn: String) : AddBookIntent

    data object ScanDismissed : AddBookIntent

    data class ScanFailed(val error: BarcodeScanError) : AddBookIntent

    data class SetLookupMode(val mode: LookupMode) : AddBookIntent

    data class TitleChanged(val title: String) : AddBookIntent

    data class LookupByTitle(val title: String) : AddBookIntent

    data class SelectTitleResult(val book: BookLookupData) : AddBookIntent
}

sealed interface AddBookEffect {
    data object BookAdded : AddBookEffect

    data object NavigateToManualEntry : AddBookEffect

    data class NavigateToBookDetail(val bookId: String) : AddBookEffect
}

class AddBookViewModel(
    private val lookupBookUseCase: LookupBookUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val lookupByTitleUseCase: LookupByTitleUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AddBookEffect>()
    val effects: SharedFlow<AddBookEffect> = _effects.asSharedFlow()

    fun onIntent(intent: AddBookIntent) {
        when (intent) {
            is AddBookIntent.IsbnChanged ->
                _uiState.update { it.copy(isbn = intent.isbn, isbnFormatError = false) }
            is AddBookIntent.TitleChanged -> _uiState.update { it.copy(titleQuery = intent.title) }
            is AddBookIntent.SetLookupMode ->
                _uiState.update {
                    it.copy(
                        lookupMode = intent.mode,
                        isbn = "",
                        titleQuery = "",
                        titleResults = emptyList(),
                        error = null,
                        foundBook = null,
                        isbnFormatError = false,
                    )
                }
            is AddBookIntent.SelectTitleResult ->
                _uiState.update { it.copy(foundBook = intent.book) }
            AddBookIntent.CancelBookPreview -> _uiState.update { it.copy(foundBook = null, isbn = "") }
            AddBookIntent.DismissDuplicateDialog -> {
                _uiState.update {
                    it.copy(
                        showDuplicateDialog = false,
                        foundBook = null,
                        isbn = "",
                        pendingAddAndTag = false,
                    )
                }
            }
            AddBookIntent.StartScan ->
                _uiState.update { it.copy(isScanning = true, isbn = "", error = null, foundBook = null) }
            AddBookIntent.ScanDismissed -> _uiState.update { it.copy(isScanning = false) }
            is AddBookIntent.ScanFailed ->
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        error =
                            when (intent.error) {
                                BarcodeScanError.CameraPermissionDenied ->
                                    AddBookScreenError.ScanCameraPermissionDenied
                                BarcodeScanError.HardwareUnavailable ->
                                    AddBookScreenError.ScanHardwareUnavailable
                                is BarcodeScanError.Unknown -> AddBookScreenError.ScanUnknownError
                                BarcodeScanError.Cancelled -> null
                            },
                    )
                }
            is AddBookIntent.LookupIsbn -> launchIfIdle { handleLookupIsbn(intent.isbn) }
            is AddBookIntent.LookupByTitle -> launchIfIdle { handleLookupByTitle(intent.title) }
            AddBookIntent.ConfirmBook -> launchIfIdle { handleConfirmBook() }
            AddBookIntent.AddAndTag -> launchIfIdle { handleAddAndTag() }
            AddBookIntent.AddAnyway -> launchIfIdle { handleAddAnyway() }
            AddBookIntent.Retry ->
                launchIfIdle {
                    val mode = _uiState.value.lookupMode
                    val query = if (mode == LookupMode.Title) _uiState.value.titleQuery else _uiState.value.isbn
                    if (query.isBlank()) return@launchIfIdle
                    if (mode == LookupMode.Title) handleLookupByTitle(query) else handleLookupIsbn(query)
                }
            AddBookIntent.EnterManually -> viewModelScope.launch { _effects.emit(AddBookEffect.NavigateToManualEntry) }
            is AddBookIntent.BarcodeScanned -> {
                _uiState.update { it.copy(isScanning = false) }
                launchIfIdle { handleLookupIsbn(intent.isbn) }
            }
        }
    }

    private fun AddBookError.toScreenError(): AddBookScreenError? =
        when (this) {
            is AddBookError.DuplicateTitle -> null
            is AddBookError.Duplicate -> AddBookScreenError.NetworkError
            AddBookError.NotFound -> AddBookScreenError.NetworkError
            AddBookError.NetworkError -> AddBookScreenError.NetworkError
            AddBookError.LibraryLimitReached -> AddBookScreenError.LibraryLimitReached
            AddBookError.MalformedResponse -> AddBookScreenError.NetworkError
            AddBookError.Unauthenticated -> AddBookScreenError.Unauthenticated
            AddBookError.RateLimited -> AddBookScreenError.RateLimited
        }

    private fun launchIfIdle(block: suspend () -> Unit) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch { block() }
    }

    private suspend fun handleLookupIsbn(isbn: String) {
        val trimmed = isbn.trim()
        if (trimmed.length != 10 && trimmed.length != 13) {
            _uiState.update { it.copy(isbnFormatError = true) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, foundBook = null, isbnFormatError = false) }
        when (val result = lookupBookUseCase(trimmed)) {
            is Result.Success -> {
                _uiState.update { it.copy(isLoading = false, foundBook = result.data) }
            }
            is Result.Failure -> {
                val error = result.error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        foundBook =
                            if (error is AddBookError.Duplicate) error.lookupData else null,
                        showDuplicateDialog =
                            error is AddBookError.Duplicate || error is AddBookError.DuplicateTitle,
                        error =
                            when (error) {
                                is AddBookError.Duplicate,
                                is AddBookError.DuplicateTitle,
                                -> null
                                AddBookError.NotFound -> AddBookScreenError.NotFound
                                AddBookError.Unauthenticated -> AddBookScreenError.Unauthenticated
                                AddBookError.LibraryLimitReached -> AddBookScreenError.LibraryLimitReached
                                AddBookError.NetworkError,
                                AddBookError.MalformedResponse,
                                -> AddBookScreenError.NetworkError
                                AddBookError.RateLimited -> AddBookScreenError.RateLimited
                            },
                    )
                }
            }
        }
    }

    private suspend fun handleLookupByTitle(title: String) {
        _uiState.update { it.copy(isLoading = true, error = null, titleResults = emptyList(), foundBook = null) }
        when (val result = lookupByTitleUseCase(title)) {
            is Result.Success ->
                _uiState.update { it.copy(isLoading = false, titleResults = result.data.take(20)) }
            is Result.Failure ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error =
                            when (result.error) {
                                LookupByTitleError.NotFound -> AddBookScreenError.NotFound
                                LookupByTitleError.Unauthenticated -> AddBookScreenError.Unauthenticated
                                LookupByTitleError.RateLimited -> AddBookScreenError.RateLimited
                                LookupByTitleError.NetworkError,
                                LookupByTitleError.MalformedResponse,
                                -> AddBookScreenError.NetworkError
                            },
                    )
                }
        }
    }

    private suspend fun handleConfirmBook() {
        val lookup = _uiState.value.foundBook ?: return
        _uiState.update { it.copy(isLoading = true) }
        when (val result = addBookUseCase(lookup)) {
            is Result.Success -> {
                val mode = _uiState.value.lookupMode
                _uiState.update { AddBookUiState(lookupMode = mode) }
                _effects.emit(AddBookEffect.BookAdded)
            }
            is Result.Failure -> {
                val error = result.error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showDuplicateDialog = error is AddBookError.DuplicateTitle,
                        error = error.toScreenError(),
                    )
                }
            }
        }
    }

    private suspend fun handleAddAndTag() {
        val lookup = _uiState.value.foundBook ?: return
        _uiState.update { it.copy(isLoading = true) }
        when (val result = addBookUseCase(lookup)) {
            is Result.Success -> {
                val mode = _uiState.value.lookupMode
                _uiState.update { AddBookUiState(lookupMode = mode) }
                _effects.emit(AddBookEffect.NavigateToBookDetail(result.data.id))
            }
            is Result.Failure -> {
                val error = result.error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showDuplicateDialog = error is AddBookError.DuplicateTitle,
                        pendingAddAndTag = error is AddBookError.DuplicateTitle,
                        error = error.toScreenError(),
                    )
                }
            }
        }
    }

    private suspend fun handleAddAnyway() {
        val lookup = _uiState.value.foundBook ?: return
        _uiState.update { it.copy(isLoading = true, showDuplicateDialog = false) }
        when (val result = addBookUseCase(lookup, forceAdd = true)) {
            is Result.Success -> {
                val mode = _uiState.value.lookupMode
                val wasPendingAddAndTag = _uiState.value.pendingAddAndTag
                _uiState.update { AddBookUiState(lookupMode = mode) }
                if (wasPendingAddAndTag) {
                    _effects.emit(AddBookEffect.NavigateToBookDetail(result.data.id))
                } else {
                    _effects.emit(AddBookEffect.BookAdded)
                }
            }
            is Result.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingAddAndTag = false,
                        error =
                            when (result.error) {
                                is AddBookError.DuplicateTitle,
                                is AddBookError.Duplicate,
                                AddBookError.LibraryLimitReached -> AddBookScreenError.LibraryLimitReached
                                AddBookError.NotFound,
                                AddBookError.NetworkError,
                                AddBookError.MalformedResponse -> AddBookScreenError.NetworkError
                                AddBookError.Unauthenticated -> AddBookScreenError.Unauthenticated
                                AddBookError.RateLimited -> AddBookScreenError.RateLimited
                            },
                    )
                }
            }
        }
    }
}
