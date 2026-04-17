package com.example.books_kmp.domain.library

sealed interface ConfirmAddBookError {
    data class NetworkError(val cause: Throwable) : ConfirmAddBookError
}
