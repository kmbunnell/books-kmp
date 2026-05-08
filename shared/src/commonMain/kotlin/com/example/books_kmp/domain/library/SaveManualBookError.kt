package com.example.books_kmp.domain.library

sealed interface SaveManualBookError {
    data object SaveFailed : SaveManualBookError

    data object DuplicateTitle : SaveManualBookError
}
