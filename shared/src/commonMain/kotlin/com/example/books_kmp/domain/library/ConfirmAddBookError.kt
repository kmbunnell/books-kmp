package com.example.books_kmp.domain.library

sealed interface ConfirmAddBookError {
    data object NetworkError : ConfirmAddBookError
}
