package com.example.books_kmp.domain.library

sealed interface LoadBooksError {
    data class NetworkError(val cause: Throwable) : LoadBooksError
}
