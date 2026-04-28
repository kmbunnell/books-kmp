package com.example.books_kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.books_kmp.domain.Result
import com.example.books_kmp.domain.library.AddBookError
import com.example.books_kmp.domain.library.BarcodeScanError
import com.example.books_kmp.domain.library.ConfirmAddBookUseCase
import com.example.books_kmp.domain.library.LookupBookUseCase
import com.example.books_kmp.domain.model.BookLookupData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookUiState(
    val isbn: String = "",
    val isLoading: Boolean = false,
    val error: AddBookScreenError? = null,
    val showDuplicateDialog: Boolean = false,
    val foundBook: BookLookupData? = null,
    val isScanning: Boolean = false,
)

sealed interface AddBookScreenError {
    data object NetworkError : AddBookScreenError

    data object RateLimited : AddBookScreenError

    data object NotFound : AddBookScreenError

    data object ScanCameraPermissionDenied : AddBookScreenError

    data object ScanHardwareUnavailable : AddBookScreenError

    data object ScanUnknownError : AddBookScreenError
}

sealed interface AddBookIntent {
    data class IsbnChanged(val isbn: String) : AddBookIntent

    data class LookupIsbn(val isbn: String) : AddBookIntent

    data object ConfirmBook : AddBookIntent

    data object CancelBookPreview : AddBookIntent

    data object DismissDuplicateDialog : AddBookIntent

    data object Retry : AddBookIntent

    data object EnterManually : AddBookIntent

    data object StartScan : AddBookIntent

    data class BarcodeScanned(val isbn: String) : AddBookIntent

    data object ScanDismissed : AddBookIntent

    data class ScanFailed(val error: BarcodeScanError) : AddBookIntent
}

sealed interface AddBookEffect {
    data object BookAdded : AddBookEffect

    data object NavigateToManualEntry : AddBookEffect
}

class AddBookViewModel(
    private val lookupBookUseCase: LookupBookUseCase,
    private val confirmAddBookUseCase: ConfirmAddBookUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddBookUiState())
    val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AddBookEffect>()
    val effects: SharedFlow<AddBookEffect> = _effects.asSharedFlow()

    fun onIntent(intent: AddBookIntent) {
        when (intent) {
            is AddBookIntent.IsbnChanged -> _uiState.update { it.copy(isbn = intent.isbn) }
            AddBookIntent.CancelBookPreview -> _uiState.update { it.copy(foundBook = null, isbn = "") }
            AddBookIntent.DismissDuplicateDialog ->
                _uiState.update { it.copy(showDuplicateDialog = false, isbn = "") }
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
            is AddBookIntent.LookupIsbn -> viewModelScope.launch { handleLookupIsbn(intent.isbn) }
            AddBookIntent.ConfirmBook -> viewModelScope.launch { handleConfirmBook() }
            AddBookIntent.Retry -> viewModelScope.launch { handleLookupIsbn(_uiState.value.isbn) }
            AddBookIntent.EnterManually -> viewModelScope.launch { _effects.emit(AddBookEffect.NavigateToManualEntry) }
            is AddBookIntent.BarcodeScanned ->
                viewModelScope.launch {
                    _uiState.update { it.copy(isScanning = false, isLoading = true, error = null, foundBook = null) }
                    handleLookupIsbn(intent.isbn)
                }
        }
    }

    private suspend fun handleLookupIsbn(isbn: String) {
        _uiState.update { it.copy(isLoading = true, error = null, foundBook = null) }
        when (val result = lookupBookUseCase(isbn)) {
            is Result.Success -> {
                _uiState.update { it.copy(isLoading = false, foundBook = result.data) }
            }
            is Result.Failure -> {
                _uiState.update { it.copy(isLoading = false) }
                when (result.error) {
                    AddBookError.Duplicate -> _uiState.update { it.copy(showDuplicateDialog = true) }
                    AddBookError.NotFound -> _uiState.update { it.copy(error = AddBookScreenError.NotFound) }
                    is AddBookError.NetworkError -> _uiState.update { it.copy(error = AddBookScreenError.NetworkError) }
                    AddBookError.RateLimited -> _uiState.update { it.copy(error = AddBookScreenError.RateLimited) }
                    AddBookError.MalformedResponse ->
                        _uiState.update {
                            it.copy(
                                error = AddBookScreenError.NetworkError
                            )
                        }
                }
            }
        }
    }

    private suspend fun handleConfirmBook() {
        val lookup = _uiState.value.foundBook ?: return
        _uiState.update { it.copy(isLoading = true) }
        when (val result = confirmAddBookUseCase(lookup)) {
            is Result.Success -> {
                _uiState.update { AddBookUiState() }
                _effects.emit(AddBookEffect.BookAdded)
            }
            is Result.Failure -> {
                _uiState.update { it.copy(isLoading = false, error = AddBookScreenError.NetworkError) }
            }
        }
    }
}
